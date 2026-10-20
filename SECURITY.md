# Security Policy

PauzeChats is intended for a small, invite-only group of friends. Privacy is a product requirement, not a marketing label.

## Non-negotiable requirements

### Direct messages

DM plaintext must exist only on endpoint devices.

The server may handle:
- ciphertext
- routing metadata required to deliver ciphertext
- public device keys or pre-key material required by the selected protocol
- delivery state
- abuse and rate-limit metadata

The server must not receive DM plaintext.

### Key storage

Device private keys must be protected by Android Keystore where possible. Keys must never be stored in SharedPreferences, source code, logs, analytics payloads, or crash reports.

### No homemade cryptography

PauzeChats will use an established, reviewable cryptographic protocol/library. We will not invent a custom encryption algorithm, custom key agreement primitive, or ad-hoc ratchet.

### Threat model

The first release is designed around the following assumptions:
- The backend can be compromised.
- Database contents can be copied by an attacker.
- Transport traffic can be observed, but TLS is still required.
- A compromised endpoint can expose plaintext.
- Push notification payloads must not contain DM plaintext.
- Invite links are credentials and must be revocable.

## Current implementation status

The Matrix integration is under review on the `phase-2` integration line. The base integration draft PR to `main` and the security follow-up draft PR targeting `phase-2` are both unmerged. The following status describes the proposed integration work; it is not a claim about the released `main` branch.

Implemented in the integration work:
- Messaging sessions and room access use Matrix Rust SDK Kotlin bindings `org.matrix.rustcomponents:sdk-android:26.09.9`.
- The app reads the SDK's room encryption state and shows `ENCRYPTED`, `NOT_ENCRYPTED`, or `UNKNOWN` in the conversation list. A failed state query remains `UNKNOWN`.
- The app reads the SDK's current device verification state and listens for state changes.
- The profile screen can start or respond to the SDK's current-device verification request, display the SDK-provided SAS decimals or emoji, and only call the SDK approval method after the user confirms the displayed values match. The SDK owns the cryptographic protocol and key checks.
- Matrix session credentials are stored encrypted with AES-GCM using a key managed by Android Keystore. The SDK's own session data is kept in app-private storage.
- The app uses the Matrix SDK's cryptographic implementation; no custom message encryption or key agreement is implemented.

Not implemented or not verified:
- The verification flow has not yet been validated end to end between two live devices or across homeservers. Unit tests cover the app's state transitions and explicit-confirmation gate, not cryptographic interoperability.
- The Kotlin bindings used here do not expose a general device-list API through this integration. PauzeChats does not enumerate or manage other devices.
- The app does not yet require encryption when creating or entering a room, and it does not prevent sending in a room reported as unencrypted. Reading an encrypted-room flag is not proof that message sending, receiving, and key sharing work correctly.
- Encrypted local message history, backup/recovery, media encryption, push-notification privacy, server authorization/replay protection, interoperability coverage, and independent security review remain incomplete.

The security model and complete E2EE behavior are therefore not complete. Do not describe PauzeChats as end-to-end encrypted or production-safe based on the SDK dependency, room status display, or verification UI alone.

## Phase 2 messaging boundary

The selected messaging foundation is the Matrix Rust SDK through maintained Android/Kotlin bindings. The SDK's encryption state machine is an implementation dependency, not a claim that PauzeChats is already E2EE-complete.

Matrix identifiers, device identifiers, room identifiers, and event identifiers are internal implementation details and must not be exposed as public Discord-style IDs.

## Before public release

Required gates:
1. Complete DM protocol and key lifecycle.
2. Device registration and end-to-end validation of the user-confirmed device verification flow, including state changes between two devices.
3. Server-side authorization and replay protection.
4. Encrypted local message database.
5. Secure push-notification design.
6. Media encryption.
7. Automated crypto interoperability tests covering encrypted-room creation, send/receive, key sharing, and verification.
8. Dependency and release-signing review.
9. Independent security review appropriate to the release scope.

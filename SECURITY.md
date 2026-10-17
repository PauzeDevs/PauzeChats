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

The Matrix integration is currently on the `phase-2` integration branch and its draft PR to `main` is not merged. The following status describes that integration branch; it is not a claim about the released `main` branch.

Implemented on `phase-2`:
- Messaging sessions and room access use Matrix Rust SDK Kotlin bindings `org.matrix.rustcomponents:sdk-android:26.09.9`.
- The app reads the SDK's room encryption state and records `ENCRYPTED` or `NOT_ENCRYPTED` in the conversation model. The model also supports `UNKNOWN` when state cannot be determined.
- The app reads the SDK's current device verification state (`VERIFIED`, `UNVERIFIED`, or `UNKNOWN`) and can start a Matrix verification request for the current device.
- Matrix session credentials are stored encrypted with AES-GCM using a key managed by Android Keystore. The SDK's own session data is kept in app-private storage.
- The app uses the Matrix SDK's cryptographic implementation; no custom message encryption or key agreement is implemented.

Not implemented or not verified:
- Verification is not a completed user flow. The app does not yet present or confirm SAS emoji/decimal data, handle incoming requests, accept or reject them through a user decision, or demonstrate that the verification state updates end to end.
- The Kotlin bindings used here do not expose a general device-list API through this integration. PauzeChats does not enumerate or manage other devices.
- The app does not yet require encryption when creating or entering a room, and it does not prevent sending in a room reported as unencrypted. Reading an encrypted-room flag is not proof that message sending, receiving, and key sharing work correctly.
- Encrypted local message history, backup/recovery, media encryption, push-notification privacy, server authorization/replay protection, interoperability coverage, and independent security review remain incomplete.

The security model and complete E2EE behavior are therefore not complete. Do not describe PauzeChats as end-to-end encrypted or production-safe based on the SDK dependency, status checks, or verification-request capability alone.

## Phase 2 messaging boundary

The selected messaging foundation is the Matrix Rust SDK through maintained Android/Kotlin bindings. The SDK's encryption state machine is an implementation dependency, not a claim that PauzeChats is already E2EE-complete.

Matrix identifiers, device identifiers, room identifiers, and event identifiers are internal implementation details and must not be exposed as public Discord-style IDs.

## Before public release

Required gates:
1. Complete DM protocol and key lifecycle.
2. Device registration and a complete, user-confirmed device verification flow, including SAS comparison and state-change handling.
3. Server-side authorization and replay protection.
4. Encrypted local message database.
5. Secure push-notification design.
6. Media encryption.
7. Automated crypto interoperability tests covering encrypted-room creation, send/receive, key sharing, and verification.
8. Dependency and release-signing review.
9. Independent security review appropriate to the release scope.

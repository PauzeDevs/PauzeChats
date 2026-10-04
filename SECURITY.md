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

The repository currently contains:
- a CryptoProvider boundary
- a typed EncryptedPayload wire model
- an Android Keystore helper for local key material
- no pretend E2EE implementation

This is deliberate. Security claims will only be made after the full protocol is implemented and tested.

## Phase 2 messaging boundary

The selected messaging foundation is the Matrix Rust SDK through maintained Android/Kotlin bindings. The SDK's encryption state machine is an implementation dependency, not a claim that PauzeChats is already E2EE-complete.

Matrix identifiers, device identifiers, room identifiers, and event identifiers are internal implementation details and must not be exposed as public Discord-style IDs.

## Before public release

Required gates:
1. Complete DM protocol and key lifecycle.
2. Device registration and verification.
3. Server-side authorization and replay protection.
4. Encrypted local message database.
5. Secure push-notification design.
6. Media encryption.
7. Automated crypto interoperability tests.
8. Dependency and release-signing review.
9. Independent security review appropriate to the release scope.

# PauzeChats Architecture

## Product model

PauzeChats has two different security domains.

### DMs

DMs are E2EE.

Device A -> encrypt/session state -> ciphertext -> relay/realtime -> ciphertext -> decrypt/session state -> Device B

The backend is a relay and authorization layer. It is not a DM plaintext processor.

### Communities

Communities behave more like private Discord servers:
- servers are invite-only
- text channels
- voice channels later
- roles and permissions
- nicknames
- reactions
- threads later
- custom server assets later

Community messages are a separate security policy from E2EE DMs because server-side features such as permissions and moderation need server-visible metadata/content unless a different private-community design is adopted.

## Identity

PauzeChats should minimize public identifiers.

The product should use an internal immutable user/device identifier and an app-level username for addressing people. Phone numbers are not part of the public identity model.

There is no public user directory.

## Authentication

Initial onboarding is invite-gated:

Invite link -> server validates invite -> account/device registration -> device key registration -> session established

Invite codes must be high entropy, revocable, rate-limited, and stored server-side as hashes rather than reusable plaintext secrets where practical.

## Backend boundary

The backend is responsible for:
- invite validation
- authentication and session lifecycle
- device registration
- authorization
- ciphertext relay/storage for DMs
- community membership
- channel and role permissions
- realtime delivery
- push-token registration
- rate limiting and abuse controls

It must not contain a code path that accepts DM plaintext from a client.

## Technology direction

Android:
- Kotlin
- Jetpack Compose
- Android Keystore

Backend direction:
- API + WebSocket/realtime transport
- PostgreSQL-compatible persistence
- object storage for encrypted media

The final backend provider is intentionally not hard-coded into this first commit. We can choose a managed service for the friends-only alpha and keep the server contract portable.

## Versioning

The project is currently pre-alpha. Breaking changes are expected until the first end-to-end vertical slice is complete.

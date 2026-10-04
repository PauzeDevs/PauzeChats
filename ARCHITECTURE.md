# PauzeChats Architecture

## Product model

PauzeChats is a private social messenger for people who know each other.

### 1-to-1 chats

Direct conversations are end-to-end encrypted.

Device A -> encrypt/session state -> ciphertext -> relay/realtime -> ciphertext -> decrypt/session state -> Device B

The backend is an authorization, delivery, and encrypted-data transport layer. It must not receive 1-to-1 plaintext message bodies.

### Group chats

Group conversations are a first-class messaging feature. They are not Discord-style servers and do not contain channels, roles, or community infrastructure.

Group requirements:
- private membership
- group name and profile image
- member management
- encrypted message delivery
- replies, reactions, edits, and deletes

The exact production group E2EE protocol will be selected and implemented before the app advertises group E2EE.

## Identity

The public identity model is intentionally small:
- email address for account authentication
- unique username for finding and adding friends
- optional display name
- optional bio
- profile picture, including GIF/WebP/PNG support
- internal immutable user identifier that is never exposed as a Discord-style public ID

There is no public user directory or random-user discovery.

## Authentication

Phase 1 account flow:

Email + password -> account session -> choose/check unique username -> profile setup

Friend discovery is username-based and should require an explicit friend request/acceptance flow.

Password reset, session revocation, and device/session management are security requirements, not optional polish.

## Backend boundary

The backend is responsible for:
- account authentication and session lifecycle
- unique username reservation/search
- profile metadata
- friend requests and relationships
- chat membership and authorization
- encrypted message ciphertext relay/storage
- realtime delivery
- push-token registration
- encrypted media metadata/storage
- rate limiting and abuse controls

The backend must not expose a code path that accepts 1-to-1 DM plaintext.

## Presence and activity

PauzeChats will implement its own presence/activity system rather than copying Discord's public identity model.

Planned integrations:
- Amazon Music listening activity
- games / game activity
- custom status

Activity visibility will be controlled by the user and will be treated as optional profile/presence data.

## Android foundation

- Kotlin
- Jetpack Compose
- Android Keystore for device-local key material
- encrypted local storage for sensitive app state

The UI should not claim E2EE until the complete protocol, key lifecycle, device verification, local storage, and server contract are implemented and tested.

## Messaging technology decision

Phase 2 uses the Matrix Rust SDK through maintained Kotlin/Android bindings. PauzeChats will isolate Matrix behind application-owned messaging interfaces so Matrix identifiers and SDK types do not become public product concepts.

The initial deployment target is a private, invite-only Synapse homeserver. Federation will remain disabled or explicitly constrained unless a future product requirement justifies it.

See docs/decisions/0002-messaging-stack.md for the full decision.

## Technology direction

Backend direction:
- HTTPS API
- realtime transport
- PostgreSQL-compatible persistence
- object storage for encrypted media

The exact provider can be chosen after the account and messaging contracts are stable.

## Explicit non-goals

PauzeChats does not contain:
- Discord-style servers
- text or voice channels
- roles
- server permissions
- server discovery
- community directories

## Versioning

The project is pre-alpha. Breaking changes are expected while Phase 1 is being built.

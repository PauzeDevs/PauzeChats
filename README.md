# PauzeChats

Private. Invite-only. Built for friends.

PauzeChats is an Android-first private social messenger focused on personal and group conversations, friend-by-username discovery, lightweight profiles, and custom activity presence.

## Product scope

### Messaging
- 1-to-1 personal chats
- private group chats
- E2EE as a hard security requirement
- replies, reactions, edits, and deletes
- media/file messaging
- push notifications without exposing private message plaintext

### Accounts
- email + password login
- unique username for adding friends
- friend requests
- profile with display name, bio, and animated/static avatar support

### Presence
- Amazon Music activity
- game activity
- custom status
- user-controlled activity visibility

## Explicit non-goals

There are no Discord-style servers, text channels, voice channels, roles, community discovery, or server permissions.

## Phase 1 — account and identity foundation

Phase 1 established the account and social foundation:

1. Real email/password authentication.
2. Username uniqueness and friend-request primitives.
3. Profile persistence and editing.
4. Authenticated API boundaries and session handling.
5. Security-sensitive interfaces prepared for later messaging and E2EE work.

Authentication is not implemented with hard-coded credentials.

## Phase 2 — private messaging foundation

Phase 2 is now active and builds the real messaging layer on top of the authenticated account system.

### Completed
- Matrix Rust SDK Android integration.
- Explicit private Matrix homeserver configuration.
- Matrix session initialization from the authenticated PauzeChats account.
- Encrypted local Matrix session persistence through the existing session store.
- App-owned messaging interfaces separating PauzeChats from the Matrix SDK.
- Matrix-backed conversation listing.
- Direct and group conversation classification.
- Background Matrix sync with idempotent startup.
- Chat UI connected to the Matrix conversation repository.
- Android, backend, and unit-test CI validation for the messaging foundation.

### In progress
- Chat detail screen.
- Message history.
- Message sending and delivery state.
- Device verification and complete E2EE key lifecycle.
- Media messaging and push notification integration.

**Security status:** PauzeChats does not claim full E2EE until the complete protocol, key lifecycle, device verification, local key storage, and server contract are implemented and tested. No custom cryptography is used.

## Current Android foundation

- Kotlin
- Jetpack Compose
- Android Gradle Plugin 9.4.x
- Gradle 9.6
- JDK 17
- Android API 37
- Matrix Rust SDK
- Android Keystore security boundary
- GitHub Actions build/test workflow

## Architecture

PauzeChats owns the application-level messaging contracts while Matrix is isolated behind implementation-specific adapters.

The backend is responsible for authentication, account metadata, friend relationships, conversation authorization, ciphertext relay/storage, realtime infrastructure, push tokens, encrypted media metadata/storage, and rate limiting/abuse controls.

The Android client is responsible for authenticated session state, local sensitive-key boundaries, Matrix client integration, conversation presentation, and future message composition.

See [ARCHITECTURE.md](ARCHITECTURE.md) and [SECURITY.md](SECURITY.md) for the current boundaries and security requirements.

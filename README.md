# PauzeChats

Private. Invite-only. Built for friends.

PauzeChats is an Android-first private social messenger focused on encrypted personal and group chats, friend-by-username discovery, lightweight profiles, and custom activity presence.

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

## Phase 1

Phase 1 is the account and identity foundation:

1. Build a real email/password account flow.
2. Establish username uniqueness and friend-request primitives.
3. Add profile persistence and editing.
4. Establish authenticated API boundaries and session handling.
5. Keep security-sensitive interfaces ready for later E2EE integration.

Authentication will not be faked with hard-coded credentials. E2EE will not be claimed until the real protocol and key lifecycle are implemented and tested.

## Current Android foundation

- Jetpack Compose
- Android Gradle Plugin 9.4.x
- Gradle 9.6
- JDK 17
- Android API 37
- Android Keystore security boundary
- GitHub Actions build/test workflow

See ARCHITECTURE.md and SECURITY.md for the current security boundaries.

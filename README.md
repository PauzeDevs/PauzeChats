# PauzeChats

<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Inter&weight=600&size=22&duration=2600&pause=700&color=4C8DFF&center=true&vCenter=true&width=680&lines=PauzeChats" alt="Animated header: PauzeChats">
</p>


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

## Build and check locally

The repository has an Android app and a separate API backend. The current product-scope list describes the intended product; chat encryption and the broader messaging feature set are not all represented as completed functionality. See [ARCHITECTURE.md](ARCHITECTURE.md) and [SECURITY.md](SECURITY.md) for implementation boundaries.

### Android app

Requires JDK 17, Android SDK 37, and Gradle 9.6. From Android Studio, open the repository, sync Gradle, and build the `:app:assembleDebug` task. The app's `pauzeApiBaseUrl` Gradle property supplies the API base URL and defaults to an empty value.

### Backend API

Requires Node.js 24 and a PostgreSQL database. From the `backend/` directory, install dependencies with `npm install`, run syntax checks with `npm run check`, run unit tests with `npm test`, and start the API with `npm start`. Backend configuration is defined in [backend/.env.example](backend/.env.example): `PORT` (default 8080), `DATABASE_URL`, and `ACCESS_TOKEN_SECRET`. The example file is a reference; the server does not automatically load `.env`, so provide these values in the process environment or configure Node to load the file. Apply `backend/migrations/001_account_foundation.sql` to PostgreSQL before starting the API. Android builds can set `pauzeApiBaseUrl` as a Gradle project property.

GitHub Actions builds the Android debug APK and runs Android unit tests. A separate workflow installs backend dependencies, runs its syntax check, and runs the Node test suite.

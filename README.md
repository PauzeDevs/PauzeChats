# PauzeChats

Private. Invite-only. Built for friends.

PauzeChats is an Android-first private communications app combining encrypted direct messaging with Discord-style private communities.

## Current status

This repository contains the first production foundation:

- Android app shell using Jetpack Compose
- Privacy-first application architecture
- Invite-only product policy
- No public discovery or public user directory
- Security boundaries for end-to-end encrypted DMs
- Local Android Keystore foundation
- Explicit separation between DM encryption and community infrastructure

Messaging and authentication are intentionally not faked. The app will not claim a message is end-to-end encrypted until the complete protocol, key lifecycle, device verification, and server contract are implemented and tested.

## Product rules

- DMs must be end-to-end encrypted.
- Communities are private and accessed through invite links.
- No public user discovery.
- Presence/activity is limited to status-style activity.
- The product should not expose unnecessary public identifiers.
- Secrets, private keys, tokens, and production credentials must never be committed.

## Build

Requirements:

- Android Studio with a compatible Android 17 / API 37 SDK
- JDK 17
- Gradle 9.6
- Android Gradle Plugin 9.4.x

Open the repository in Android Studio and sync the Gradle project.

## Security status

The codebase currently provides interfaces and local key-store primitives only. A production E2EE protocol still needs to be selected, implemented, interoperably tested, and independently reviewed before any release that advertises E2EE.

See SECURITY.md and ARCHITECTURE.md.

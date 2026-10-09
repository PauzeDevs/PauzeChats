# Minimal Messenger Scope

Copyright © 2026 Aarav Singh (Pauze). All rights reserved.

## Product goal

PauzeChats is a private Android messenger for a group of 4–5 people. The UI should feel like a simple native messaging app, not a WhatsApp feature clone or a Discord community product.

## Keep

- Existing sign-in and account foundation
- One-to-one and small private group conversations
- End-to-end encryption through the maintained Matrix Rust SDK
- Animated profile pictures (GIF)
- A compact sticker picker
- Minimal chat list, chat detail, and profile settings

## Do not add

- Servers, channels, public communities, public discovery, or feeds
- Music/game activity integrations, stories, calls, or payments
- Paid SDKs or paid infrastructure

## Implementation notes

- `AnimatedAvatar` is a reusable circular avatar composable that supports static images and GIFs.
- Conversation summaries now expose an optional `avatarUrl`; it defaults to null to preserve existing callers.
- `StickerPicker` is a reusable starter tray that returns a typed sticker selection to the caller.
- These UI primitives do not themselves send messages. Sticker selection must be connected to Matrix encrypted room events before it is considered a working chat feature.
- Existing repository documentation explicitly says full E2EE is not complete yet. Do not claim the app is E2EE-ready until device verification, key lifecycle, encrypted local storage, and interoperability tests pass.

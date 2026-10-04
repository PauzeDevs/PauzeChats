# ADR 0002: Messaging foundation

## Status

Accepted for Phase 2 implementation.

## Decision

PauzeChats will use the Matrix ecosystem as the messaging transport and end-to-end-encryption foundation.

Android will integrate the Matrix Rust SDK through its maintained Kotlin/Android bindings. PauzeChats UI and application services will depend on PauzeChats-owned interfaces rather than exposing Matrix types throughout the product.

The initial deployment target is a private, invite-only Synapse homeserver controlled by PauzeChats. Federation remains disabled or explicitly constrained until there is a product reason to enable it.

## Why

The project must not implement a custom encryption protocol or ratchet. The selected stack already provides client-side encryption state management, sync, room state, and established protocol behavior.

The Matrix Rust SDK is used by Element X and is documented as production-ready. Its Android distribution is available through the Matrix Rust Components Kotlin project. Sources: matrix-rust-sdk and Element X Android documentation.

This choice also gives PauzeChats a path to private 1-to-1 and private group conversations without introducing Discord-style servers, channels, roles, or public community discovery.

## Product mapping

- PauzeChats account identity remains owned by the existing account API.
- Username-based friend discovery remains owned by the existing social API.
- A PauzeChats conversation maps to a private Matrix room.
- Direct chats use Matrix's established end-to-end encryption.
- Group chats use Matrix room membership and encryption state.
- Matrix user IDs, room IDs, event IDs, and server names are internal implementation details and must not become a Discord-style public identity system.
- Invite links remain PauzeChats credentials and are separate from Matrix room invites.

## Boundary

The UI must not depend directly on Matrix SDK classes.

Planned internal interfaces:
- MessagingSession
- ConversationRepository
- MessageRepository
- MessagingSyncController
- DeviceVerificationRepository

The Matrix adapter will be the only layer allowed to translate between these interfaces and Matrix SDK types.

## E2EE release gate

PauzeChats will not advertise E2EE until all of the following are implemented and tested:

1. Account-to-Matrix identity binding.
2. Device/key lifecycle.
3. Device verification.
4. Encrypted local message storage.
5. Secure sync and retry behavior.
6. Push notifications that do not expose DM plaintext.
7. Encrypted media handling.
8. Interoperability tests for the selected Matrix encryption flows.

## Rejected alternatives

### Homemade protocol

Rejected because the project explicitly forbids custom cryptography.

### Direct use of Signal libsignal

Not selected for this phase because the official Signal repository does not support arbitrary third-party use as a stable application integration boundary.

### Building a new bespoke message transport first

Rejected because it would duplicate synchronization, room state, and encryption machinery that the selected Matrix stack already provides.

## Consequences

Positive:
- established E2EE and sync primitives
- private 1-to-1 and group room model
- a realistic Android implementation path
- room for future desktop/web clients through the same messaging model

Tradeoffs:
- Matrix introduces protocol concepts and dependencies that must be hidden behind the application boundary
- the private homeserver becomes part of the deployment architecture
- push, media, and identity binding still require PauzeChats-specific implementation and testing

## Rollout

Phase 2 starts with SDK integration and a no-UI messaging session boundary. Conversation UI and message composition will be added only after the SDK can initialize, persist its session, and sync against the private homeserver.
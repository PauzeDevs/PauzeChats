# ADR 0003: RCS compatibility and transport boundary

## Status
Assessment in progress. This document does not claim RCS is implemented.

## Context
PauzeChats currently uses the Matrix Rust SDK as its app-to-app messaging foundation. RCS is a carrier/IMS messaging transport, not a drop-in endpoint that can be substituted for Matrix room messaging. A normal third-party app cannot simply route Matrix messages through Google Messages' RCS infrastructure.

Android API 37 exposes `AlternativeMessageTransportService`, which allows a **default SMS application** to handle a system-requested transport upgrade (for example, SMS/MMS to RCS). This is a framework hand-off point; it does not provide an RCS stack, carrier provisioning, IMS registration, credentials, or a Google Messages backend.

## Decision
- Keep Matrix as the existing PauzeChats account-to-account transport while evaluating options.
- Do not remove the Matrix adapter or label chats “RCS” until an actual RCS provider/stack and supported device/carrier flow are available and tested.
- Keep animated avatars and sticker UI independent of transport.
- If PauzeChats is intended to replace the phone's SMS app, treat that as separate scope: implement the default-SMS role, SMS/MMS storage and intents, then investigate the API 37 alternative-transport contract and carrier/OEM availability.
- If PauzeChats is intended only for private chats between PauzeChats users, continue Matrix and implement message history, message sending, encrypted sticker events, and media there rather than misrepresenting it as RCS.

## Release gates
1. Decide whether the target is a default SMS/RCS client or an app-to-app private messenger.
2. Validate actual RCS capability on a supported physical device, carrier, and SIM; API presence alone is insufficient.
3. Test interoperability with Google Messages and another RCS client, including fallback behavior.
4. Keep Matrix E2EE claims separate from carrier RCS encryption semantics.
5. Do not ship a dead service stub or a UI-only “RCS” toggle.

## References
- Android `AlternativeMessageTransportService`: https://developer.android.com/reference/android/service/messaging/AlternativeMessageTransportService
- [ADR 0002: Matrix messaging stack](0002-messaging-stack.md)

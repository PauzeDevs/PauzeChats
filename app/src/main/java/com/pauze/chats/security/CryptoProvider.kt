package com.pauze.chats.security

/**
 * Boundary for the production DM encryption implementation.
 *
 * The concrete implementation must provide:
 * - authenticated encryption
 * - device/key registration
 * - session establishment
 * - forward secrecy / post-compromise recovery appropriate to the selected protocol
 * - safe key rotation
 *
 * No UI or networking code should implement cryptographic primitives directly.
 */
interface CryptoProvider {
    fun encrypt(
        conversationId: String,
        senderDeviceId: String,
        recipientDeviceId: String,
        plaintext: ByteArray,
        associatedData: ByteArray
    ): EncryptedPayload

    fun decrypt(
        payload: EncryptedPayload,
        associatedData: ByteArray
    ): ByteArray
}

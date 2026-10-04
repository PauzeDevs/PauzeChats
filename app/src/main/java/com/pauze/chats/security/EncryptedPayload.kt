package com.pauze.chats.security

/**
 * Wire representation of an encrypted message.
 *
 * There is intentionally no plaintext field here. The transport layer should only
 * accept EncryptedPayload values for DM message bodies.
 */
data class EncryptedPayload(
    val protocolVersion: Int,
    val conversationId: String,
    val senderDeviceId: String,
    val recipientDeviceId: String,
    val ciphertextBase64: String,
    val associatedDataBase64: String,
    val messageId: String,
    val createdAtEpochMillis: Long
)

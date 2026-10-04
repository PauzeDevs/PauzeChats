/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging

data class ConversationId(
    val value: String
)

data class ConversationSummary(
    val id: ConversationId,
    val title: String,
    val isDirect: Boolean,
    val unreadCount: Int
)

data class MessageId(
    val value: String
)

data class MessageSummary(
    val id: MessageId,
    val conversationId: ConversationId,
    val senderName: String,
    val bodyPreview: String,
    val sentAtEpochSeconds: Long
)

interface MessagingSession {
    val isInitialized: Boolean

    suspend fun initialize(): Result<Unit>

    suspend fun signOut(): Result<Unit>
}

interface ConversationRepository {
    suspend fun listConversations(): Result<List<ConversationSummary>>
}

interface MessageRepository {
    suspend fun listRecentMessages(
        conversationId: ConversationId
    ): Result<List<MessageSummary>>
}

interface MessagingSyncController {
    suspend fun start(): Result<Unit>

    suspend fun stop(): Result<Unit>
}

interface DeviceVerificationRepository {
    suspend fun listVerifiableDevices(): Result<List<String>>
}

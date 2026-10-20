/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class ConversationId(
    val value: String
)

data class ConversationSummary(
    val id: ConversationId,
    val title: String,
    val isDirect: Boolean,
    val unreadCount: Int,
    val encryptionStatus: RoomEncryptionStatus = RoomEncryptionStatus.UNKNOWN,
    val avatarUrl: String? = null
)

enum class RoomEncryptionStatus {
    ENCRYPTED,
    NOT_ENCRYPTED,
    UNKNOWN
}

enum class DeviceVerificationStatus {
    VERIFIED,
    UNVERIFIED,
    UNKNOWN
}

enum class DeviceVerificationStage {
    IDLE,
    REQUESTED,
    INCOMING_REQUEST,
    READY_TO_START_SAS,
    WAITING_FOR_SAS,
    SAS_COMPARISON,
    WAITING_FOR_CONFIRMATION,
    COMPLETED,
    DECLINED,
    CANCELLED,
    FAILED
}

enum class SasDisplayMode {
    DECIMALS,
    EMOJI
}

data class DeviceVerificationFlowState(
    val isReady: Boolean = false,
    val deviceStatus: DeviceVerificationStatus = DeviceVerificationStatus.UNKNOWN,
    val stage: DeviceVerificationStage = DeviceVerificationStage.IDLE,
    val otherDeviceName: String? = null,
    val sasMode: SasDisplayMode? = null,
    val sasValues: List<String> = emptyList(),
    val errorMessage: String? = null
)

data class MessageId(
    val value: String
)

data class MessageSummary(
    val id: MessageId,
    val conversationId: ConversationId,
    val senderName: String,
    val bodyPreview: String,
    val sentAtEpochSeconds: Long,
    val isOwn: Boolean = false
)

data class MessagingSessionConfig(
    val accountId: String,
    val accessToken: String,
    val homeserverUrl: String
)

interface MessagingSession {
    val isInitialized: Boolean

    suspend fun initialize(config: MessagingSessionConfig): Result<Unit>

    suspend fun signOut(): Result<Unit>
}

interface ConversationRepository {
    suspend fun listConversations(): Result<List<ConversationSummary>>
}

interface MessageRepository {
    fun observeMessages(conversationId: ConversationId): Flow<List<MessageSummary>>
    suspend fun sendText(conversationId: ConversationId, body: String): Result<Unit>
}

interface MessagingSyncController {
    suspend fun start(): Result<Unit>

    suspend fun stop(): Result<Unit>
}

interface DeviceVerificationRepository {
    val state: StateFlow<DeviceVerificationFlowState>

    suspend fun attach(): Result<Unit>

    suspend fun requestCurrentDeviceVerification(): Result<Unit>

    suspend fun acceptIncomingRequest(): Result<Unit>

    suspend fun startSasVerification(): Result<Unit>

    suspend fun confirmSasMatches(): Result<Unit>

    suspend fun declineIncomingRequest(): Result<Unit>

    suspend fun cancelVerification(): Result<Unit>
}

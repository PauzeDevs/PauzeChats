/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import com.pauze.chats.messaging.ConversationId
import com.pauze.chats.messaging.MessageId
import com.pauze.chats.messaging.MessageRepository
import com.pauze.chats.messaging.MessageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.MessageType
import org.matrix.rustcomponents.sdk.MsgLikeKind
import org.matrix.rustcomponents.sdk.TimelineDiff
import org.matrix.rustcomponents.sdk.TimelineItem

class MatrixMessageRepository(
    private val clientProvider: () -> Client?
) : MessageRepository {
    override suspend fun listRecentMessages(
        conversationId: ConversationId
    ): Result<List<MessageSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val client = clientProvider()
                ?: error("Matrix messaging session is not initialized")

            val room = client.rooms().firstOrNull { it.id() == conversationId.value }
                ?: error("Conversation not found")

            val diffs = room.timeline()
                .timelineDiffFlow()
                .first()

            val items = diffs.flatMap(::itemsFromDiff)

            items.mapNotNull { item ->
                val event = item.asEvent() ?: return@mapNotNull null
                val content = event.content()

                val message = (content as? org.matrix.rustcomponents.sdk.TimelineItemContent.MsgLike)
                    ?.content
                    ?.kind as? MsgLikeKind.Message
                    ?: return@mapNotNull null

                val body = when (val messageType = message.content.msgType) {
                    is MessageType.Text -> messageType.content.body
                    is MessageType.Emote -> messageType.content.body
                    is MessageType.Notice -> messageType.content.body
                    else -> null
                } ?: return@mapNotNull null

                MessageSummary(
                    id = MessageId(event.eventOrTransactionId.toString()),
                    conversationId = conversationId,
                    senderName = event.sender(),
                    bodyPreview = body,
                    sentAtEpochSeconds = event.timestamp().toLong() / 1_000
                )
            }.takeLast(100)
        }
    }

    private fun itemsFromDiff(diff: TimelineDiff): List<TimelineItem> =
        when (diff) {
            is TimelineDiff.Append -> diff.values
            is TimelineDiff.Reset -> diff.values
            is TimelineDiff.PushBack -> listOf(diff.value)
            is TimelineDiff.PushFront -> listOf(diff.value)
            is TimelineDiff.Insert -> listOf(diff.value)
            is TimelineDiff.Set -> listOf(diff.value)
            else -> emptyList()
        }
}

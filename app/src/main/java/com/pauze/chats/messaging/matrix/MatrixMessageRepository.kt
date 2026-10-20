/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import com.pauze.chats.messaging.ConversationId
import com.pauze.chats.messaging.MessageId
import com.pauze.chats.messaging.MessageRepository
import com.pauze.chats.messaging.MessageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.MessageContent
import org.matrix.rustcomponents.sdk.MessageType
import org.matrix.rustcomponents.sdk.MsgLikeContent
import org.matrix.rustcomponents.sdk.MsgLikeKind
import org.matrix.rustcomponents.sdk.TaskHandle
import org.matrix.rustcomponents.sdk.TextMessageContent
import org.matrix.rustcomponents.sdk.Timeline
import org.matrix.rustcomponents.sdk.TimelineDiff
import org.matrix.rustcomponents.sdk.TimelineItem
import org.matrix.rustcomponents.sdk.TimelineItemContent
import org.matrix.rustcomponents.sdk.TimelineListener
import org.matrix.rustcomponents.sdk.use

/**
 * Matrix timeline adapter. Message events are sent through the SDK timeline so
 * encrypted rooms use the SDK's encryption and send queue rather than raw HTTP.
 *
 * Emoji stickers currently use the same encrypted room-message event as text;
 * image-backed m.sticker events require a media-upload flow and are not claimed
 * by this implementation.
 */
class MatrixMessageRepository(
    private val clientProvider: () -> Client?
) : MessageRepository {
    override fun observeMessages(conversationId: ConversationId): Flow<List<MessageSummary>> = callbackFlow {
        val client = clientProvider()
        if (client == null) {
            close(IllegalStateException("Matrix messaging session is not initialized"))
            return@callbackFlow
        }

        val room = client.getRoom(conversationId.value)
        if (room == null) {
            close(IllegalArgumentException("Conversation is no longer available"))
            return@callbackFlow
        }

        try {
            room.use { activeRoom ->
                activeRoom.timeline().use { timeline ->
                    // Keep null entries for virtual/non-message timeline items so that
                    // TimelineDiff indices continue to match the SDK timeline indices.
                    val items = mutableListOf<MessageSummary?>()
                    val listener = object : TimelineListener {
                        override fun onUpdate(diff: List<TimelineDiff>) {
                            runCatching {
                                applyDiffs(items, diff, conversationId)
                                trySendBlocking(items.filterNotNull())
                            }.onFailure { close(it) }
                        }
                    }
                    val task: TaskHandle = timeline.addListener(listener)
                    awaitClose {
                        task.cancel()
                    }
                }
            }
        } catch (error: Throwable) {
            close(error)
        }
    }

    override suspend fun sendText(
        conversationId: ConversationId,
        body: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanBody = body.trim()
            require(cleanBody.isNotEmpty()) { "Message cannot be empty" }
            require(cleanBody.length <= 10_000) { "Message is too long" }

            val client = clientProvider()
                ?: error("Matrix messaging session is not initialized")
            val room = client.getRoom(conversationId.value)
                ?: error("Conversation is no longer available")

            room.use { activeRoom ->
                activeRoom.timeline().use { timeline ->
                    timeline.send(textMessageContent(cleanBody))
                }
            }
            Unit
        }
    }

    private fun applyDiffs(
        items: MutableList<MessageSummary?>,
        diffs: List<TimelineDiff>,
        conversationId: ConversationId
    ) {
        diffs.forEach { diff ->
            when (diff) {
                is TimelineDiff.Append -> diff.values.forEach { items.add(it.toSummary(conversationId)) }
                is TimelineDiff.PushBack -> items.add(diff.value.toSummary(conversationId))
                is TimelineDiff.PushFront -> items.add(0, diff.value.toSummary(conversationId))
                is TimelineDiff.Set -> items[diff.index.toInt()] = diff.value.toSummary(conversationId)
                is TimelineDiff.Insert -> items.add(diff.index.toInt(), diff.value.toSummary(conversationId))
                is TimelineDiff.Remove -> items.removeAt(diff.index.toInt())
                TimelineDiff.PopFront -> if (items.isNotEmpty()) items.removeAt(0)
                TimelineDiff.PopBack -> if (items.isNotEmpty()) items.removeAt(items.lastIndex)
                is TimelineDiff.Reset -> {
                    items.clear()
                    diff.values.forEach { items.add(it.toSummary(conversationId)) }
                }
                is TimelineDiff.Truncate -> {
                    val newSize = diff.length.toInt().coerceIn(0, items.size)
                    items.subList(newSize, items.size).clear()
                }
                TimelineDiff.Clear -> items.clear()
            }
        }
    }

    private fun TimelineItem.toSummary(conversationId: ConversationId): MessageSummary? = use itemUse@{
        val stableId = uniqueId().id
        val event = asEvent() ?: return@itemUse null
        event.use eventUse@{
            val eventContent = event.content
            eventContent.use contentUse@{
                val messageLike = it as? TimelineItemContent.MsgLike ?: return@contentUse null
                val message = messageLike.content.kind as? MsgLikeKind.Message
                    ?: return@contentUse null
                val senderId = event.sender
                val senderName = senderId.substringAfter('@').substringBefore(':').ifBlank { senderId }
                MessageSummary(
                    id = MessageId(stableId),
                    conversationId = conversationId,
                    senderName = senderName,
                    bodyPreview = message.content.body,
                    sentAtEpochSeconds = event.timestamp.toLong()
                )
            }
        }
    }

    private fun textMessageContent(body: String): TimelineItemContent =
        TimelineItemContent.MsgLike(
            content = MsgLikeContent(
                kind = MsgLikeKind.Message(
                    content = MessageContent(
                        msgType = MessageType.Text(
                            content = TextMessageContent(body = body, formatted = null)
                        ),
                        body = body,
                        isEdited = false,
                        mentions = null
                    )
                ),
                threadRoot = null,
                inReplyTo = null,
                threadSummary = null
            )
        )
}

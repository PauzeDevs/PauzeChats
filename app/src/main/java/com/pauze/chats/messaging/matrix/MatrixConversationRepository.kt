/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import com.pauze.chats.messaging.ConversationId
import com.pauze.chats.messaging.ConversationRepository
import com.pauze.chats.messaging.ConversationSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.rustcomponents.sdk.Client

class MatrixConversationRepository(
    private val clientProvider: () -> Client?
) : ConversationRepository {
    override suspend fun listConversations(): Result<List<ConversationSummary>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val client = clientProvider()
                    ?: error("Matrix messaging session is not initialized")

                client.rooms().map { room ->
                    ConversationSummary(
                        id = ConversationId(room.id()),
                        title = room.displayName() ?: "Conversation",
                        isDirect = room.isDirect(),
                        unreadCount = 0
                    )
                }
            }
        }
}

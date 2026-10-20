/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import android.net.Uri
import com.pauze.chats.BuildConfig
import com.pauze.chats.messaging.ConversationId
import com.pauze.chats.messaging.ConversationRepository
import com.pauze.chats.messaging.ConversationSummary
import com.pauze.chats.messaging.RoomEncryptionStatus
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

                client.rooms()
                    .map { room ->
                        ConversationSummary(
                            id = ConversationId(room.id()),
                            title = room.displayName() ?: "Conversation",
                            isDirect = room.isDirect(),
                            unreadCount = 0,
                            encryptionStatus = runCatching { room.isEncrypted() }
                                .getOrNull()
                                .toRoomEncryptionStatus(),
                            avatarUrl = mediaDownloadUrl(room.avatarUrl())
                        )
                    }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            }
        }

    private fun mediaDownloadUrl(mxcUrl: String?): String? {
        if (mxcUrl.isNullOrBlank()) return null

        val uri = Uri.parse(mxcUrl)
        if (uri.scheme != "mxc") {
            return mxcUrl.takeIf { it.startsWith("https://") }
        }

        val serverName = uri.host ?: return null
        val mediaId = uri.pathSegments.singleOrNull() ?: return null
        val homeserver = BuildConfig.PAUZE_MATRIX_HOMESERVER_URL
            .trimEnd('/')
            .takeIf(String::isNotBlank) ?: return null

        return "$homeserver/_matrix/client/v1/media/download/" +
            Uri.encode(serverName) + "/" + Uri.encode(mediaId)
    }
}

internal fun Boolean?.toRoomEncryptionStatus(): RoomEncryptionStatus =
    when (this) {
        true -> RoomEncryptionStatus.ENCRYPTED
        false -> RoomEncryptionStatus.NOT_ENCRYPTED
        null -> RoomEncryptionStatus.UNKNOWN
    }

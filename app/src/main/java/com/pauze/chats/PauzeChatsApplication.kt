/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats

import android.app.Application
import com.pauze.chats.account.NetworkAuthRepository
import com.pauze.chats.account.NetworkSocialRepository
import com.pauze.chats.messaging.matrix.MatrixAuthApiClient
import com.pauze.chats.messaging.matrix.MatrixConversationRepository
import com.pauze.chats.messaging.matrix.MatrixMessagingSession
import com.pauze.chats.messaging.matrix.MatrixMessagingSyncController
import com.pauze.chats.messaging.matrix.MatrixSdkClientFactory
import com.pauze.chats.messaging.matrix.MatrixSdkPlatform
import com.pauze.chats.messaging.matrix.MatrixSessionStore

class PauzeChatsApplication : Application() {
    lateinit var authRepository: NetworkAuthRepository
        private set

    lateinit var socialRepository: NetworkSocialRepository
        private set

    lateinit var messagingSession: MatrixMessagingSession
        private set

    lateinit var conversationRepository: MatrixConversationRepository
        private set

    lateinit var messagingSyncController: MatrixMessagingSyncController
        private set

    override fun onCreate() {
        super.onCreate()

        MatrixSdkPlatform.initialize()

        authRepository = NetworkAuthRepository(this)
        socialRepository = NetworkSocialRepository(authRepository)

        val matrixClientFactory = MatrixSdkClientFactory(this)
        val matrixAuthApiClient = MatrixAuthApiClient()
        val matrixSessionStore = MatrixSessionStore(this)

        messagingSession = MatrixMessagingSession(
            clientFactory = matrixClientFactory,
            authApiClient = matrixAuthApiClient,
            sessionStore = matrixSessionStore
        )
        conversationRepository = MatrixConversationRepository {
            messagingSession.clientOrNull()
        }
        messagingSyncController = MatrixMessagingSyncController {
            messagingSession.clientOrNull()
        }
    }
}

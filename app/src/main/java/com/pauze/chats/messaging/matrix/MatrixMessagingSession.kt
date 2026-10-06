/* 
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import com.pauze.chats.messaging.MessagingSession
import com.pauze.chats.messaging.MessagingSessionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.rustcomponents.sdk.Client

class MatrixMessagingSession(
    private val clientFactory: MatrixSdkClientFactory,
    private val authApiClient: MatrixAuthApiClient,
    private val sessionStore: MatrixSessionStore
) : MessagingSession {
    private var currentClient: Client? = null

    override val isInitialized: Boolean
        get() = currentClient != null

    fun clientOrNull(): Client? = currentClient

    override suspend fun initialize(
        config: MessagingSessionConfig
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val client = clientFactory
                .build(config.accountId, config.homeserverUrl)
                .getOrThrow()

            val storedSession = sessionStore.read()
            if (storedSession != null &&
                storedSession.homeserverUrl == config.homeserverUrl
            ) {
                val restored = runCatching {
                    client.restoreSession(storedSession)
                    true
                }.getOrDefault(false)

                if (!restored) {
                    loginWithPauzeSession(
                        client = client,
                        config = config,
                        existingDeviceId = storedSession.deviceId
                    )
                }
            } else {
                loginWithPauzeSession(
                    client = client,
                    config = config,
                    existingDeviceId = null
                )
            }

            sessionStore.save(client.session())
            currentClient = client
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            currentClient?.logout()
            currentClient = null
            sessionStore.clear()
        }
    }

    private suspend fun loginWithPauzeSession(
        client: Client,
        config: MessagingSessionConfig,
        existingDeviceId: String?
    ) {
        val bootstrap = authApiClient.requestBootstrapToken(config.accessToken)

        client.customLoginWithJwt(
            bootstrap.token,
            "PauzeChats Android",
            existingDeviceId
        )
    }
}

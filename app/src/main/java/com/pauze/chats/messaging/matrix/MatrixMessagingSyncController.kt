/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.SyncListenerV2
import org.matrix.rustcomponents.sdk.SyncResponseV2
import org.matrix.rustcomponents.sdk.SyncSettingsV2
import org.matrix.rustcomponents.sdk.TaskHandle

class MatrixMessagingSyncController(
    private val clientProvider: () -> Client?
) {
    private var taskHandle: TaskHandle? = null

    suspend fun start(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(taskHandle == null || taskHandle?.isFinished == true) {
                "Matrix sync is already running"
            }

            val client = clientProvider()
                ?: error("Matrix messaging session is not initialized")

            taskHandle = client.syncV2(
                SyncSettingsV2(
                    timeoutMs = 30_000L,
                    fullState = false
                ),
                MatrixSyncListener
            )
        }
    }

    suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            taskHandle?.cancel()
            taskHandle = null
        }
    }

    private object MatrixSyncListener : SyncListenerV2 {
        override fun onUpdate(response: SyncResponseV2) {
            // The SDK state store receives the sync response. Room repositories
            // read the resulting state after this callback.
        }
    }
}

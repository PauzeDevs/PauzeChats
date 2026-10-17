/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import com.pauze.chats.messaging.DeviceVerificationRepository
import com.pauze.chats.messaging.DeviceVerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.VerificationState

/** Narrow seam around the SDK calls so repository behavior can be tested without native SDK state. */
internal interface MatrixVerificationApi {
    fun currentDeviceStatus(): DeviceVerificationStatus

    suspend fun requestCurrentDeviceVerification()
}

class MatrixDeviceVerificationRepository(
    private val apiProvider: () -> MatrixVerificationApi?
) : DeviceVerificationRepository {
    override suspend fun currentDeviceStatus(): Result<DeviceVerificationStatus> =
        withContext(Dispatchers.IO) {
            runCatching {
                val api = apiProvider()
                    ?: error("Matrix messaging session is not initialized")
                api.currentDeviceStatus()
            }
        }

    override suspend fun requestCurrentDeviceVerification(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val api = apiProvider()
                    ?: error("Matrix messaging session is not initialized")
                api.requestCurrentDeviceVerification()
            }
        }
}

internal class SdkMatrixVerificationApi(
    private val client: Client
) : MatrixVerificationApi {
    override fun currentDeviceStatus(): DeviceVerificationStatus =
        client.encryption().verificationState().toDeviceVerificationStatus()

    override suspend fun requestCurrentDeviceVerification() {
        client.getSessionVerificationController().requestDeviceVerification()
    }
}

internal fun VerificationState.toDeviceVerificationStatus(): DeviceVerificationStatus =
    when (this) {
        VerificationState.VERIFIED -> DeviceVerificationStatus.VERIFIED
        VerificationState.UNVERIFIED -> DeviceVerificationStatus.UNVERIFIED
        VerificationState.UNKNOWN -> DeviceVerificationStatus.UNKNOWN
    }

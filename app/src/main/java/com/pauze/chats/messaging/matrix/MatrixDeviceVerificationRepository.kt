/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import com.pauze.chats.messaging.DeviceVerificationFlowState
import com.pauze.chats.messaging.DeviceVerificationRepository
import com.pauze.chats.messaging.DeviceVerificationStage
import com.pauze.chats.messaging.DeviceVerificationStatus
import com.pauze.chats.messaging.SasDisplayMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.SessionVerificationController
import org.matrix.rustcomponents.sdk.SessionVerificationControllerDelegate
import org.matrix.rustcomponents.sdk.SessionVerificationData
import org.matrix.rustcomponents.sdk.SessionVerificationRequestDetails
import org.matrix.rustcomponents.sdk.TaskHandle
import org.matrix.rustcomponents.sdk.VerificationState
import org.matrix.rustcomponents.sdk.VerificationStateListener

internal sealed interface MatrixVerificationEvent {
    data class RequestReceived(val deviceName: String?) : MatrixVerificationEvent
    data object RequestAccepted : MatrixVerificationEvent
    data object SasStarted : MatrixVerificationEvent
    data class SasReceived(
        val mode: SasDisplayMode,
        val values: List<String>
    ) : MatrixVerificationEvent
    data object Failed : MatrixVerificationEvent
    data object Cancelled : MatrixVerificationEvent
    data class StatusChanged(val status: DeviceVerificationStatus) : MatrixVerificationEvent
    data class Finished(val status: DeviceVerificationStatus) : MatrixVerificationEvent
}

/** Adapter around the pinned Rust SDK controller; all cryptographic work stays in the SDK. */
internal interface MatrixVerificationApi {
    suspend fun attach(onEvent: (MatrixVerificationEvent) -> Unit): DeviceVerificationStatus

    suspend fun requestCurrentDeviceVerification()

    suspend fun acceptIncomingRequest()

    suspend fun startSasVerification()

    suspend fun confirmSasMatches()

    suspend fun declineIncomingRequest()

    suspend fun cancelVerification()
}

class MatrixDeviceVerificationRepository internal constructor(
    private val apiProvider: () -> MatrixVerificationApi?
) : DeviceVerificationRepository {
    private val _state = MutableStateFlow(DeviceVerificationFlowState())
    override val state = _state.asStateFlow()
    private var attachedApi: MatrixVerificationApi? = null

    override suspend fun attach(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val api = attachedApi ?: apiProvider()
                ?: error("Matrix messaging session is not initialized")
            if (api === attachedApi) return@runCatching
            val status = api.attach(::handleEvent)
            attachedApi = api
            _state.update {
                it.copy(
                    isReady = true,
                    deviceStatus = status,
                    stage = if (it.stage == DeviceVerificationStage.FAILED) {
                        DeviceVerificationStage.IDLE
                    } else {
                        it.stage
                    },
                    errorMessage = null
                )
            }
        }.onFailure(::setFailure)
    }

    override suspend fun requestCurrentDeviceVerification(): Result<Unit> =
        runAction(DeviceVerificationStage.REQUESTED) {
            it.requestCurrentDeviceVerification()
        }

    override suspend fun acceptIncomingRequest(): Result<Unit> =
        runAction(
            requiredStage = DeviceVerificationStage.INCOMING_REQUEST,
            stage = DeviceVerificationStage.READY_TO_START_SAS
        ) {
            it.acceptIncomingRequest()
        }

    override suspend fun startSasVerification(): Result<Unit> =
        runAction(
            requiredStage = DeviceVerificationStage.READY_TO_START_SAS,
            stage = DeviceVerificationStage.WAITING_FOR_SAS
        ) {
            it.startSasVerification()
        }

    override suspend fun confirmSasMatches(): Result<Unit> {
        if (_state.value.stage != DeviceVerificationStage.SAS_COMPARISON ||
            _state.value.sasValues.isEmpty()
        ) {
            return invalidStageResult("There is no SAS comparison to confirm.")
        }
        return runAction(stage = DeviceVerificationStage.WAITING_FOR_CONFIRMATION) {
            it.confirmSasMatches()
        }
    }

    override suspend fun declineIncomingRequest(): Result<Unit> =
        runAction(
            requiredStage = DeviceVerificationStage.INCOMING_REQUEST,
            stage = DeviceVerificationStage.DECLINED
        ) {
            it.declineIncomingRequest()
        }

    override suspend fun cancelVerification(): Result<Unit> =
        runAction(DeviceVerificationStage.CANCELLED) {
            it.cancelVerification()
        }

    private suspend fun runAction(
        stage: DeviceVerificationStage,
        requiredStage: DeviceVerificationStage? = null,
        action: suspend (MatrixVerificationApi) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (requiredStage != null && _state.value.stage != requiredStage) {
            return@withContext invalidStageResult(
                "This verification action is not available in the current state."
            )
        }
        runCatching {
            _state.update {
                it.copy(
                    stage = stage,
                    otherDeviceName = if (stage == DeviceVerificationStage.REQUESTED) {
                        null
                    } else {
                        it.otherDeviceName
                    },
                    sasMode = if (stage == DeviceVerificationStage.REQUESTED) null else it.sasMode,
                    sasValues = if (stage == DeviceVerificationStage.REQUESTED) {
                        emptyList()
                    } else {
                        it.sasValues
                    },
                    errorMessage = null
                )
            }
            action(requireApi())
        }.onFailure(::setFailure)
    }

    private fun invalidStageResult(message: String): Result<Unit> =
        Result.failure(IllegalStateException(message))

    private fun requireApi(): MatrixVerificationApi = attachedApi
        ?: error("Matrix verification controller is not attached")

    private fun setFailure(error: Throwable) {
        _state.update {
            it.copy(
                stage = DeviceVerificationStage.FAILED,
                sasMode = null,
                sasValues = emptyList(),
                errorMessage = error.message?.takeIf(String::isNotBlank)
                    ?: "Matrix device verification failed."
            )
        }
    }

    private fun handleEvent(event: MatrixVerificationEvent) {
        _state.update { current ->
            when (event) {
                is MatrixVerificationEvent.RequestReceived -> current.copy(
                    stage = DeviceVerificationStage.INCOMING_REQUEST,
                    otherDeviceName = event.deviceName,
                    sasMode = null,
                    sasValues = emptyList(),
                    errorMessage = null
                )
                MatrixVerificationEvent.RequestAccepted -> current.copy(
                    stage = DeviceVerificationStage.READY_TO_START_SAS,
                    errorMessage = null
                )
                MatrixVerificationEvent.SasStarted -> current.copy(
                    stage = DeviceVerificationStage.WAITING_FOR_SAS,
                    errorMessage = null
                )
                is MatrixVerificationEvent.SasReceived -> current.copy(
                    stage = DeviceVerificationStage.SAS_COMPARISON,
                    sasMode = event.mode,
                    sasValues = event.values,
                    errorMessage = null
                )
                MatrixVerificationEvent.Failed -> current.copy(
                    stage = DeviceVerificationStage.FAILED,
                    sasMode = null,
                    sasValues = emptyList(),
                    errorMessage = "The other device could not complete verification."
                )
                MatrixVerificationEvent.Cancelled -> current.copy(
                    stage = DeviceVerificationStage.CANCELLED,
                    sasMode = null,
                    sasValues = emptyList(),
                    errorMessage = null
                )
                is MatrixVerificationEvent.StatusChanged -> current.copy(
                    deviceStatus = event.status
                )
                is MatrixVerificationEvent.Finished -> current.copy(
                    deviceStatus = event.status,
                    stage = DeviceVerificationStage.COMPLETED,
                    sasMode = null,
                    sasValues = emptyList(),
                    errorMessage = null
                )
            }
        }
    }
}

internal class SdkMatrixVerificationApi(
    private val client: Client
) : MatrixVerificationApi {
    private var controller: SessionVerificationController? = null
    private var delegate: SessionVerificationControllerDelegate? = null
    private var statusListener: TaskHandle? = null

    override suspend fun attach(
        onEvent: (MatrixVerificationEvent) -> Unit
    ): DeviceVerificationStatus {
        controller = client.getSessionVerificationController().also { sdkController ->
            delegate = sdkControllerDelegate(client, onEvent)
            sdkController.setDelegate(delegate!!)
        }
        val encryption = client.encryption()
        statusListener?.cancel()
        statusListener = encryption.verificationStateListener(
            object : VerificationStateListener {
                override fun onUpdate(state: VerificationState) {
                    onEvent(MatrixVerificationEvent.StatusChanged(state.toDeviceVerificationStatus()))
                }
            }
        )
        return encryption.verificationState().toDeviceVerificationStatus()
    }

    override suspend fun requestCurrentDeviceVerification() {
        requireController().requestDeviceVerification()
    }

    override suspend fun acceptIncomingRequest() {
        requireController().acceptVerificationRequest()
    }

    override suspend fun startSasVerification() {
        requireController().startSasVerification()
    }

    override suspend fun confirmSasMatches() {
        requireController().approveVerification()
    }

    override suspend fun declineIncomingRequest() {
        requireController().declineVerification()
    }

    override suspend fun cancelVerification() {
        requireController().cancelVerification()
    }

    private fun requireController(): SessionVerificationController =
        controller ?: error("Matrix verification controller is not attached")

    private fun sdkControllerDelegate(
        client: Client,
        onEvent: (MatrixVerificationEvent) -> Unit
    ) = object : SessionVerificationControllerDelegate {
        override fun didReceiveVerificationRequest(
            details: SessionVerificationRequestDetails
        ) {
            onEvent(MatrixVerificationEvent.RequestReceived(details.deviceDisplayName))
        }

        override fun didAcceptVerificationRequest() {
            onEvent(MatrixVerificationEvent.RequestAccepted)
        }

        override fun didStartSasVerification() {
            onEvent(MatrixVerificationEvent.SasStarted)
        }

        override fun didReceiveVerificationData(data: SessionVerificationData) {
            onEvent(data.toMatrixVerificationEvent())
        }

        override fun didFail() {
            onEvent(MatrixVerificationEvent.Failed)
        }

        override fun didCancel() {
            onEvent(MatrixVerificationEvent.Cancelled)
        }

        override fun didFinish() {
            onEvent(
                MatrixVerificationEvent.Finished(
                    client.encryption().verificationState().toDeviceVerificationStatus()
                )
            )
        }
    }
}

internal fun SessionVerificationData.toMatrixVerificationEvent(): MatrixVerificationEvent =
    try {
        when (this) {
            is SessionVerificationData.Decimals -> MatrixVerificationEvent.SasReceived(
                mode = SasDisplayMode.DECIMALS,
                values = values.map { it.toString() }
            )
            is SessionVerificationData.Emojis -> MatrixVerificationEvent.SasReceived(
                mode = SasDisplayMode.EMOJI,
                values = emojis.map { emoji -> "${emoji.symbol()} ${emoji.description()}" }
            )
            else -> MatrixVerificationEvent.Failed
        }
    } finally {
        destroy()
    }

internal fun VerificationState.toDeviceVerificationStatus(): DeviceVerificationStatus =
    when (this) {
        VerificationState.VERIFIED -> DeviceVerificationStatus.VERIFIED
        VerificationState.UNVERIFIED -> DeviceVerificationStatus.UNVERIFIED
        VerificationState.UNKNOWN -> DeviceVerificationStatus.UNKNOWN
    }

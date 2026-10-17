/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import com.pauze.chats.messaging.DeviceVerificationStage
import com.pauze.chats.messaging.DeviceVerificationStatus
import com.pauze.chats.messaging.RoomEncryptionStatus
import com.pauze.chats.messaging.SasDisplayMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.matrix.rustcomponents.sdk.SessionVerificationData
import org.matrix.rustcomponents.sdk.VerificationState

class MatrixSecurityMappingTest {
    @Test
    fun mapsSdkVerificationStateWithoutTreatingUnknownAsVerified() {
        assertEquals(
            DeviceVerificationStatus.VERIFIED,
            VerificationState.VERIFIED.toDeviceVerificationStatus()
        )
        assertEquals(
            DeviceVerificationStatus.UNVERIFIED,
            VerificationState.UNVERIFIED.toDeviceVerificationStatus()
        )
        assertEquals(
            DeviceVerificationStatus.UNKNOWN,
            VerificationState.UNKNOWN.toDeviceVerificationStatus()
        )
    }

    @Test
    fun mapsRoomEncryptionStateExplicitly() {
        assertEquals(RoomEncryptionStatus.ENCRYPTED, true.toRoomEncryptionStatus())
        assertEquals(RoomEncryptionStatus.NOT_ENCRYPTED, false.toRoomEncryptionStatus())
        assertEquals(RoomEncryptionStatus.UNKNOWN, null.toRoomEncryptionStatus())
    }

    @Test
    fun convertsSdkDecimalSasIntoDisplayValues() {
        val sas = SessionVerificationData.Decimals(
            listOf(123u.toUShort(), 456u.toUShort(), 789u.toUShort())
        )

        assertEquals(
            MatrixVerificationEvent.SasReceived(
                SasDisplayMode.DECIMALS,
                listOf("123", "456", "789")
            ),
            sas.toMatrixVerificationEvent()
        )
    }

    @Test
    fun requiresExplicitSasConfirmationBeforeMarkingDeviceVerified() = runBlocking {
        val api = FakeVerificationApi()
        val repository = MatrixDeviceVerificationRepository { api }

        assertTrue(repository.attach().isSuccess)
        assertTrue(repository.state.value.isReady)
        assertEquals(DeviceVerificationStatus.UNVERIFIED, repository.state.value.deviceStatus)
        assertFalse(repository.confirmSasMatches().isSuccess)
        assertEquals(0, api.confirmCount)

        assertTrue(repository.requestCurrentDeviceVerification().isSuccess)
        assertEquals(DeviceVerificationStage.REQUESTED, repository.state.value.stage)

        api.emit(MatrixVerificationEvent.RequestReceived("Aarav's tablet"))
        assertEquals(DeviceVerificationStage.INCOMING_REQUEST, repository.state.value.stage)
        assertEquals("Aarav's tablet", repository.state.value.otherDeviceName)

        assertTrue(repository.acceptIncomingRequest().isSuccess)
        assertEquals(DeviceVerificationStage.READY_TO_START_SAS, repository.state.value.stage)
        assertTrue(repository.startSasVerification().isSuccess)

        api.emit(
            MatrixVerificationEvent.SasReceived(
                SasDisplayMode.DECIMALS,
                listOf("123", "456", "789")
            )
        )
        assertEquals(DeviceVerificationStage.SAS_COMPARISON, repository.state.value.stage)
        assertEquals(listOf("123", "456", "789"), repository.state.value.sasValues)
        assertEquals(DeviceVerificationStatus.UNVERIFIED, repository.state.value.deviceStatus)

        assertTrue(repository.confirmSasMatches().isSuccess)
        assertEquals(1, api.confirmCount)
        assertEquals(
            DeviceVerificationStage.WAITING_FOR_CONFIRMATION,
            repository.state.value.stage
        )
        assertEquals(DeviceVerificationStatus.UNVERIFIED, repository.state.value.deviceStatus)

        api.status = DeviceVerificationStatus.VERIFIED
        api.emit(MatrixVerificationEvent.Finished(api.status))
        assertEquals(DeviceVerificationStage.COMPLETED, repository.state.value.stage)
        assertEquals(DeviceVerificationStatus.VERIFIED, repository.state.value.deviceStatus)
    }

    @Test
    fun doesNotProceedWhenNoSdkSessionIsAttached() = runBlocking {
        val repository = MatrixDeviceVerificationRepository { null }

        assertFalse(repository.attach().isSuccess)
        assertFalse(repository.requestCurrentDeviceVerification().isSuccess)
        assertEquals(DeviceVerificationStage.FAILED, repository.state.value.stage)
    }

    @Test
    fun observesVerificationStatusChangesFromSdk() = runBlocking {
        val api = FakeVerificationApi()
        val repository = MatrixDeviceVerificationRepository { api }
        repository.attach().getOrThrow()

        api.emit(MatrixVerificationEvent.StatusChanged(DeviceVerificationStatus.VERIFIED))

        assertEquals(DeviceVerificationStatus.VERIFIED, repository.state.value.deviceStatus)
    }

    private class FakeVerificationApi : MatrixVerificationApi {
        private var listener: ((MatrixVerificationEvent) -> Unit)? = null
        var status = DeviceVerificationStatus.UNVERIFIED
        var confirmCount = 0

        override suspend fun attach(
            onEvent: (MatrixVerificationEvent) -> Unit
        ): DeviceVerificationStatus {
            listener = onEvent
            return status
        }

        override suspend fun requestCurrentDeviceVerification() = Unit
        override suspend fun acceptIncomingRequest() = Unit
        override suspend fun startSasVerification() = Unit
        override suspend fun confirmSasMatches() {
            confirmCount += 1
        }
        override suspend fun declineIncomingRequest() = Unit
        override suspend fun cancelVerification() = Unit

        fun emit(event: MatrixVerificationEvent) {
            listener?.invoke(event)
        }
    }
}

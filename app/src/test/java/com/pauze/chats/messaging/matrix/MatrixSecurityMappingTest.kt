/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import com.pauze.chats.messaging.DeviceVerificationStatus
import com.pauze.chats.messaging.RoomEncryptionStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
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
    fun reportsRoomEncryptionStateExplicitly() {
        assertEquals(RoomEncryptionStatus.ENCRYPTED, true.toRoomEncryptionStatus())
        assertEquals(RoomEncryptionStatus.NOT_ENCRYPTED, false.toRoomEncryptionStatus())
    }

    @Test
    fun verificationRequestUsesSdkApiAndPropagatesFailure() = runBlocking {
        var requested = false
        val api = object : MatrixVerificationApi {
            override fun currentDeviceStatus() = DeviceVerificationStatus.UNVERIFIED

            override suspend fun requestCurrentDeviceVerification() {
                requested = true
            }
        }
        val repository = MatrixDeviceVerificationRepository { api }

        assertEquals(
            DeviceVerificationStatus.UNVERIFIED,
            repository.currentDeviceStatus().getOrThrow()
        )
        assertTrue(repository.requestCurrentDeviceVerification().isSuccess)
        assertTrue(requested)

        val unavailable = MatrixDeviceVerificationRepository { null }
        assertFalse(unavailable.currentDeviceStatus().isSuccess)
        assertFalse(unavailable.requestCurrentDeviceVerification().isSuccess)
    }
}

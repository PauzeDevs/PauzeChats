/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pauze.chats.messaging.DeviceVerificationRepository
import kotlinx.coroutines.launch

class DeviceVerificationViewModel(
    private val repository: DeviceVerificationRepository
) : ViewModel() {
    val state = repository.state

    fun requestVerification() = run(repository::requestCurrentDeviceVerification)

    fun acceptRequest() = run(repository::acceptIncomingRequest)

    fun startSasVerification() = run(repository::startSasVerification)

    fun confirmSasMatches() = run(repository::confirmSasMatches)

    fun declineRequest() = run(repository::declineIncomingRequest)

    fun cancelVerification() = run(repository::cancelVerification)

    private fun run(action: suspend () -> Result<Unit>) {
        viewModelScope.launch { action() }
    }

    class Factory(
        private val repository: DeviceVerificationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DeviceVerificationViewModel::class.java))
            return DeviceVerificationViewModel(repository) as T
        }
    }
}

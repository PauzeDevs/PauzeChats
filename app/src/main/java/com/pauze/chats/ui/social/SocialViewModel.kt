package com.pauze.chats.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pauze.chats.account.AuthApiException
import com.pauze.chats.account.FriendProfile
import com.pauze.chats.account.NetworkSocialRepository
import com.pauze.chats.account.PendingFriendRequest
import com.pauze.chats.account.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SocialUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val friends: List<FriendProfile> = emptyList(),
    val incomingRequests: List<PendingFriendRequest> = emptyList(),
    val lookupResult: FriendProfile? = null,
    val isSaving: Boolean = false,
    val message: String? = null
)

class SocialViewModel(
    private val repository: NetworkSocialRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SocialUiState())
    val state: StateFlow<SocialUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.isSaving) return

        _state.value = _state.value.copy(isLoading = true, message = null)

        viewModelScope.launch {
            val profile = repository.me().getOrNull()
            val friends = repository.friends().getOrElse { emptyList() }
            val requests = repository.incomingRequests().getOrElse { emptyList() }

            _state.value = _state.value.copy(
                isLoading = false,
                profile = profile ?: _state.value.profile,
                friends = friends,
                incomingRequests = requests
            )
        }
    }

    fun lookup(username: String) {
        val query = username.trim()
        if (query.isBlank()) {
            _state.value = _state.value.copy(
                lookupResult = null,
                message = "Enter a username first."
            )
            return
        }

        _state.value = _state.value.copy(message = null)

        viewModelScope.launch {
            repository.lookupUsername(query).fold(
                onSuccess = { profile ->
                    _state.value = _state.value.copy(
                        lookupResult = profile,
                        message = null
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        lookupResult = null,
                        message = errorMessage(error)
                    )
                }
            )
        }
    }

    fun sendFriendRequest(username: String) {
        viewModelScope.launch {
            repository.sendFriendRequest(username).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        lookupResult = null,
                        message = "Friend request sent."
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        message = errorMessage(error)
                    )
                }
            )
        }
    }

    fun respondToRequest(
        requestId: String,
        action: String
    ) {
        viewModelScope.launch {
            repository.respondToRequest(requestId, action).fold(
                onSuccess = {
                    _state.value = _state.value.copy(message = null)
                    refresh()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        message = errorMessage(error)
                    )
                }
            )
        }
    }

    fun saveProfile(
        displayName: String,
        bio: String
    ) {
        if (_state.value.isSaving) return

        _state.value = _state.value.copy(
            isSaving = true,
            message = null
        )

        viewModelScope.launch {
            repository.updateProfile(displayName, bio).fold(
                onSuccess = { profile ->
                    _state.value = _state.value.copy(
                        profile = profile,
                        isSaving = false,
                        message = "Profile updated."
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isSaving = false,
                        message = errorMessage(error)
                    )
                }
            )
        }
    }

    private fun errorMessage(error: Throwable): String =
        when (error) {
            is AuthApiException -> when (error.errorCode) {
                "api_not_configured" ->
                    "PauzeChats is not connected to its backend yet."
                "not_authenticated" ->
                    "Your session expired. Sign in again."
                "user_not_found" ->
                    "No user was found with that username."
                "already_friends" ->
                    "You're already friends."
                "incoming_request_exists" ->
                    "That user already sent you a request."
                "friend_request_exists" ->
                    "A friend request already exists."
                "cannot_add_self" ->
                    "You can't add yourself."
                "invalid_profile" ->
                    "Check your display name and bio."
                else ->
                    "Couldn't complete that request."
            }
            else -> "Couldn't connect to PauzeChats."
        }

    class Factory(
        private val repository: NetworkSocialRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SocialViewModel::class.java))
            return SocialViewModel(repository) as T
        }
    }
}

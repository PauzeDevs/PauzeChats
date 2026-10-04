package com.pauze.chats.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pauze.chats.account.AccountCredentials
import com.pauze.chats.account.AuthApiException
import com.pauze.chats.account.AuthRepository
import com.pauze.chats.account.RegistrationRequest
import com.pauze.chats.account.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthMode {
    SIGN_IN,
    SIGN_UP
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val isBusy: Boolean = true,
    val isAuthenticated: Boolean = false,
    val profile: UserProfile? = null,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        restoreSession()
    }

    fun setMode(mode: AuthMode) {
        _state.value = _state.value.copy(
            mode = mode,
            errorMessage = null
        )
    }

    fun signIn(email: String, password: String) {
        runAuth {
            repository.signIn(
                AccountCredentials(email = email, password = password)
            )
        }
    }

    fun signUp(
        email: String,
        password: String,
        username: String,
        displayName: String,
        bio: String,
        inviteCode: String
    ) {
        runAuth {
            repository.signUp(
                RegistrationRequest(
                    email = email,
                    password = password,
                    username = username,
                    displayName = displayName.ifBlank { null },
                    bio = bio.ifBlank { null }
                ),
                inviteCode = inviteCode
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val session = repository.currentSession()
            _state.value = _state.value.copy(
                isBusy = false,
                isAuthenticated = session != null
            )
        }
    }

    private fun runAuth(
        operation: suspend () -> Result<com.pauze.chats.account.Session>
    ) {
        if (_state.value.isBusy) return

        _state.value = _state.value.copy(
            isBusy = true,
            errorMessage = null
        )

        viewModelScope.launch {
            operation().fold(
                onSuccess = {
                    val profile = runCatching {
                        repository.currentSession()?.let { current ->
                            if (current.userId == it.userId) {
                                null
                            } else {
                                null
                            }
                        }
                    }.getOrNull()

                    _state.value = _state.value.copy(
                        isBusy = false,
                        isAuthenticated = true,
                        profile = profile,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isBusy = false,
                        errorMessage = errorMessage(error)
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
                "invalid_credentials" ->
                    "The email or password is incorrect."
                "invalid_invite" ->
                    "That invite link or code is invalid or expired."
                "email_or_username_taken" ->
                    "That email or username is already in use."
                "invalid_registration" ->
                    "Check your registration details and try again."
                else ->
                    "Authentication failed. Try again."
            }
            else -> "Couldn't connect to PauzeChats. Check your internet connection."
        }

    class Factory(
        private val repository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java))
            return AuthViewModel(repository) as T
        }
    }
}

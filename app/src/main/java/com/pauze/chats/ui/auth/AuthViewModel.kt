/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pauze.chats.account.AccountCredentials
import com.pauze.chats.account.AccountValidation
import com.pauze.chats.account.AuthApiException
import com.pauze.chats.account.AuthRepository
import com.pauze.chats.account.RegistrationRequest
import com.pauze.chats.account.Session
import com.pauze.chats.account.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val errorMessage: String? = null,
    val isBackendConfigured: Boolean = false,
    val rateLimitedMode: AuthMode? = null,
    val rateLimitSecondsRemaining: Long = 0
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        AuthUiState(isBackendConfigured = repository.isBackendConfigured)
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    private var rateLimitJob: Job? = null

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
        val normalizedEmail = AccountValidation.normalizeEmail(email)
        if (normalizedEmail == null) {
            showInputError("Enter a valid email address.")
            return
        }
        if (password.isBlank() || password.length > 128) {
            showInputError("Enter your password.")
            return
        }

        runAuth(AuthMode.SIGN_IN) {
            repository.signIn(
                AccountCredentials(email = normalizedEmail, password = password)
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
        val normalizedEmail = AccountValidation.normalizeEmail(email)
        val normalizedUsername = AccountValidation.normalizeUsername(username)
        val normalizedInviteCode = inviteCode.trim()
        if (
            normalizedEmail == null ||
            normalizedUsername == null ||
            !AccountValidation.validatePassword(password) ||
            !AccountValidation.validateDisplayName(displayName.ifBlank { null }) ||
            !AccountValidation.validateBio(bio) ||
            normalizedInviteCode.length !in 16..256
        ) {
            showInputError("Check the details and invite code, then try again.")
            return
        }

        runAuth(AuthMode.SIGN_UP) {
            repository.signUp(
                RegistrationRequest(
                    email = normalizedEmail,
                    password = password,
                    username = normalizedUsername,
                    displayName = displayName.trim().ifBlank { null },
                    bio = bio.trim().ifBlank { null }
                ),
                inviteCode = normalizedInviteCode
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun signOut() {
        if (_state.value.isBusy) return

        _state.value = _state.value.copy(
            isBusy = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = runCatching { repository.signOut() }
                .getOrElse { Result.failure(it) }
            result.fold(
                onSuccess = {
                    rateLimitJob?.cancel()
                    _state.value = AuthUiState(
                        mode = AuthMode.SIGN_IN,
                        isBusy = false,
                        isAuthenticated = false,
                        isBackendConfigured = repository.isBackendConfigured
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

    private fun restoreSession() {
        viewModelScope.launch {
            val session = runCatching { repository.currentSession() }.getOrNull()
            _state.value = _state.value.copy(
                isBusy = false,
                isAuthenticated = session != null,
                isBackendConfigured = repository.isBackendConfigured
            )
        }
    }

    private fun runAuth(
        mode: AuthMode,
        operation: suspend () -> Result<Session>
    ) {
        val current = _state.value
        if (
            current.isBusy ||
            (current.rateLimitedMode == mode && current.rateLimitSecondsRemaining > 0)
        ) {
            return
        }

        _state.value = current.copy(
            isBusy = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = runCatching { operation() }
                .getOrElse { Result.failure(it) }
            result.fold(
                onSuccess = {
                    rateLimitJob?.cancel()
                    _state.value = _state.value.copy(
                        isBusy = false,
                        isAuthenticated = true,
                        errorMessage = null,
                        rateLimitedMode = null,
                        rateLimitSecondsRemaining = 0
                    )
                },
                onFailure = { error ->
                    handleAuthFailure(mode, error)
                }
            )
        }
    }

    private fun showInputError(message: String) {
        if (_state.value.isBusy) return
        _state.value = _state.value.copy(errorMessage = message)
    }

    private fun handleAuthFailure(mode: AuthMode, error: Throwable) {
        val retryAfterSeconds = (error as? AuthApiException)
            ?.takeIf { it.errorCode == "rate_limited" }
            ?.retryAfterSeconds
            ?.coerceIn(1L, MAX_RATE_LIMIT_SECONDS)
            ?: 0L
        val message = errorMessage(error)

        _state.value = _state.value.copy(
            isBusy = false,
            errorMessage = message,
            rateLimitedMode = mode.takeIf { retryAfterSeconds > 0 },
            rateLimitSecondsRemaining = retryAfterSeconds
        )

        rateLimitJob?.cancel()
        if (retryAfterSeconds > 0) {
            rateLimitJob = viewModelScope.launch {
                var remaining = retryAfterSeconds
                while (remaining > 0) {
                    delay(1_000)
                    remaining -= 1
                    _state.value = _state.value.copy(
                        rateLimitSecondsRemaining = remaining
                    )
                }
                val current = _state.value
                _state.value = current.copy(
                    rateLimitedMode = null,
                    errorMessage = current.errorMessage.takeUnless { it == message }
                )
            }
        }
    }

    private fun errorMessage(error: Throwable): String =
        when (error) {
            is AuthApiException -> when {
                error.errorCode == "api_not_configured" ->
                    "This build is not connected to the PauzeChats account service."
                error.errorCode == "invalid_credentials" ->
                    "The email or password is incorrect."
                error.errorCode == "invalid_invite" ->
                    "That invite link or code is invalid or expired."
                error.errorCode == "email_or_username_taken" ->
                    "That email or username is already in use."
                error.errorCode == "invalid_registration" ->
                    "Check your registration details and try again."
                error.errorCode == "rate_limited" ->
                    "Too many attempts. Wait before trying again."
                error.httpStatus >= 500 ->
                    "The account service is temporarily unavailable. Try again shortly."
                else ->
                    "Authentication failed. Try again."
            }
            else -> "Couldn't reach PauzeChats. Check your connection and try again."
        }

    override fun onCleared() {
        rateLimitJob?.cancel()
        super.onCleared()
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

    private companion object {
        const val MAX_RATE_LIMIT_SECONDS = 24 * 60 * 60L
    }
}

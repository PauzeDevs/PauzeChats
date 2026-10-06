/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pauze.chats.BuildConfig
import com.pauze.chats.account.AuthRepository
import com.pauze.chats.messaging.MessagingSessionConfig
import com.pauze.chats.messaging.matrix.MatrixConversationRepository
import com.pauze.chats.messaging.matrix.MatrixMessagingSession
import com.pauze.chats.messaging.matrix.MatrixMessagingSyncController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatsUiState(
    val isLoading: Boolean = true,
    val conversations: List<com.pauze.chats.messaging.ConversationSummary> = emptyList(),
    val message: String? = null
)

class ChatsViewModel(
    private val authRepository: AuthRepository,
    private val messagingSession: MatrixMessagingSession,
    private val conversationRepository: MatrixConversationRepository,
    private val messagingSyncController: MatrixMessagingSyncController
) : ViewModel() {
    private val _state = MutableStateFlow(ChatsUiState())
    val state: StateFlow<ChatsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, message = null)

        viewModelScope.launch {
            val session = authRepository.currentSession()
            if (session == null) {
                _state.value = ChatsUiState(
                    isLoading = false,
                    message = "Your account session is unavailable. Sign in again."
                )
                return@launch
            }

            if (BuildConfig.PAUZE_MATRIX_HOMESERVER_URL.isBlank()) {
                _state.value = ChatsUiState(
                    isLoading = false,
                    message = "Messaging is not configured yet."
                )
                return@launch
            }

            messagingSession.initialize(
                MessagingSessionConfig(
                    accountId = session.userId.value,
                    accessToken = session.accessToken,
                    homeserverUrl = BuildConfig.PAUZE_MATRIX_HOMESERVER_URL
                )
            ).fold(
                onSuccess = {
                    messagingSyncController.start().fold(
                        onSuccess = {
                            conversationRepository.listConversations().fold(
                                onSuccess = { conversations ->
                                    _state.value = ChatsUiState(
                                        isLoading = false,
                                        conversations = conversations
                                    )
                                },
                                onFailure = { error ->
                                    _state.value = ChatsUiState(
                                        isLoading = false,
                                        message = errorMessage(error)
                                    )
                                }
                            )
                        },
                        onFailure = { error ->
                            _state.value = ChatsUiState(
                                isLoading = false,
                                message = errorMessage(error)
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _state.value = ChatsUiState(
                        isLoading = false,
                        message = errorMessage(error)
                    )
                }
            )
        }
    }

    private fun errorMessage(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() }
            ?: "Couldn't load your conversations."

    class Factory(
        private val authRepository: AuthRepository,
        private val messagingSession: MatrixMessagingSession,
        private val conversationRepository: MatrixConversationRepository,
        private val messagingSyncController: MatrixMessagingSyncController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChatsViewModel::class.java))
            return ChatsViewModel(
                authRepository,
                messagingSession,
                conversationRepository,
                messagingSyncController
            ) as T
        }
    }
}

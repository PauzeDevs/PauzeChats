/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pauze.chats.messaging.ConversationId
import com.pauze.chats.messaging.MessageRepository
import com.pauze.chats.messaging.MessageSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatDetailUiState(
    val isLoading: Boolean = true,
    val messages: List<MessageSummary> = emptyList(),
    val message: String? = null
)

class ChatDetailViewModel(
    private val messageRepository: MessageRepository,
    private val conversationId: ConversationId
) : ViewModel() {
    private val _state = MutableStateFlow(ChatDetailUiState())
    val state: StateFlow<ChatDetailUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, message = null)

        viewModelScope.launch {
            messageRepository.listRecentMessages(conversationId).fold(
                onSuccess = { messages ->
                    _state.value = ChatDetailUiState(
                        isLoading = false,
                        messages = messages
                    )
                },
                onFailure = { error ->
                    _state.value = ChatDetailUiState(
                        isLoading = false,
                        message = error.message?.takeIf { it.isNotBlank() }
                            ?: "Couldn't load messages."
                    )
                }
            )
        }
    }

    class Factory(
        private val messageRepository: MessageRepository,
        private val conversationId: ConversationId
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChatDetailViewModel::class.java))
            return ChatDetailViewModel(
                messageRepository,
                conversationId
            ) as T
        }
    }
}

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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatDetailUiState(
    val isLoading: Boolean = true,
    val messages: List<MessageSummary> = emptyList(),
    val message: String? = null,
    val draft: String = "",
    val isSending: Boolean = false,
    val showStickerPicker: Boolean = false
)

class ChatDetailViewModel(
    private val conversationId: ConversationId,
    private val messageRepository: MessageRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ChatDetailUiState())
    val state: StateFlow<ChatDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            messageRepository.observeMessages(conversationId)
                .catch { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            message = error.message?.takeIf(String::isNotBlank)
                                ?: "Couldn't load messages. Check your connection and try again."
                        )
                    }
                }
                .collect { messages ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            messages = messages,
                            message = if (it.isSending) it.message else null
                        )
                    }
                }
        }
    }

    fun setDraft(value: String) {
        _state.update { it.copy(draft = value) }
    }

    fun toggleStickerPicker() {
        _state.update { it.copy(showStickerPicker = !it.showStickerPicker) }
    }

    fun sendSticker(emoji: String) {
        _state.update { it.copy(showStickerPicker = false) }
        sendText(emoji)
    }

    fun sendText(value: String = _state.value.draft) {
        val body = value.trim()
        if (body.isEmpty() || _state.value.isSending) return

        _state.update { it.copy(isSending = true, message = null) }
        viewModelScope.launch {
            messageRepository.sendText(conversationId, body).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isSending = false,
                            draft = if (it.draft.trim() == body) "" else it.draft,
                            showStickerPicker = false,
                            message = null
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            message = error.message?.takeIf(String::isNotBlank)
                                ?: "Message could not be sent. Please try again."
                        )
                    }
                }
            )
        }
    }

    class Factory(
        private val conversationId: ConversationId,
        private val messageRepository: MessageRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChatDetailViewModel::class.java))
            return ChatDetailViewModel(conversationId, messageRepository) as T
        }
    }
}

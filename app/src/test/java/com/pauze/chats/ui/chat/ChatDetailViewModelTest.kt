/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.ui.chat

import com.pauze.chats.messaging.ConversationId
import com.pauze.chats.messaging.MessageRepository
import com.pauze.chats.messaging.MessageSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun observesTimelineAndClearsLoadingState() = runTest {
        val repository = FakeMessageRepository()
        val viewModel = ChatDetailViewModel(ConversationId("!room:example.org"), repository)

        runCurrent()
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(emptyList<MessageSummary>(), viewModel.state.value.messages)

        val message = MessageSummary(
            id = com.pauze.chats.messaging.MessageId("event-1"),
            conversationId = ConversationId("!room:example.org"),
            senderName = "Aarav",
            bodyPreview = "Hello",
            sentAtEpochSeconds = 1_800_000_000L,
            isOwn = false
        )
        repository.messages.value = listOf(message)
        runCurrent()

        assertEquals(listOf(message), viewModel.state.value.messages)
    }

    @Test
    fun successfulSendTrimsTextAndClearsDraft() = runTest {
        val repository = FakeMessageRepository()
        val viewModel = ChatDetailViewModel(ConversationId("!room:example.org"), repository)
        viewModel.setDraft("  hello Matrix  ")

        viewModel.sendText()
        advanceUntilIdle()

        assertEquals(listOf("hello Matrix"), repository.sentBodies)
        assertEquals("", viewModel.state.value.draft)
        assertFalse(viewModel.state.value.isSending)
        assertEquals(null, viewModel.state.value.message)
    }

    @Test
    fun failedSendShowsErrorAndPreservesDraft() = runTest {
        val repository = FakeMessageRepository().apply {
            sendOutcome = Result.failure(IllegalStateException("offline"))
        }
        val viewModel = ChatDetailViewModel(ConversationId("!room:example.org"), repository)
        viewModel.setDraft("please retry")

        viewModel.sendText()
        advanceUntilIdle()

        assertEquals("please retry", viewModel.state.value.draft)
        assertEquals("offline", viewModel.state.value.message)
        assertFalse(viewModel.state.value.isSending)
    }

    @Test
    fun successfulSendDoesNotEraseTextTypedWhileSendWasPending() = runTest {
        val repository = FakeMessageRepository().apply {
            pendingSend = CompletableDeferred()
        }
        val viewModel = ChatDetailViewModel(ConversationId("!room:example.org"), repository)
        viewModel.setDraft("first message")

        viewModel.sendText()
        runCurrent()
        assertEquals(listOf("first message"), repository.sentBodies)

        viewModel.setDraft("second message")
        repository.pendingSend!!.complete(Result.success(Unit))
        advanceUntilIdle()

        assertEquals("second message", viewModel.state.value.draft)
    }

    private class FakeMessageRepository : MessageRepository {
        val messages = MutableStateFlow<List<MessageSummary>>(emptyList())
        val sentBodies = mutableListOf<String>()
        var sendOutcome: Result<Unit> = Result.success(Unit)
        var pendingSend: CompletableDeferred<Result<Unit>>? = null

        override fun observeMessages(conversationId: ConversationId) = messages

        override suspend fun sendText(conversationId: ConversationId, body: String): Result<Unit> {
            sentBodies += body
            return pendingSend?.await() ?: sendOutcome
        }
    }

    private class MainDispatcherRule(
        val dispatcher: TestDispatcher = StandardTestDispatcher()
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }
}

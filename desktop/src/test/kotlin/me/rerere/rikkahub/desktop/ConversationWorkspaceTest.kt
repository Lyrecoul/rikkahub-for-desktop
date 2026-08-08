package me.rerere.rikkahub.desktop

import kotlinx.coroutines.Job
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationWorkspaceTest {
    private fun data(messages: List<ChatMessage> = listOf(ChatMessage("user", "Hello"))): DesktopData {
        val conversation = DesktopConversation(id = "conversation", messages = messages)
        return DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id)
    }

    private class TestState(initial: DesktopData) : ConversationExecutionState {
        var data: DesktopData = initial
        override fun current(): DesktopData = data
        override fun update(transform: (DesktopData) -> DesktopData) {
            data = transform(data)
        }
    }

    private fun newRegistry(): DesktopGenerationRegistry = DesktopGenerationRegistry()

    @Test
    fun editConversationAppliesTransformWhenIdle() {
        val state = TestState(data())
        val answered = mutableListOf<Pair<String, String>>()
        val resumed = mutableListOf<String>()
        val workspace = workspace(state, answered, resumed)

        val applied = workspace.editConversation("conversation") { it.copy(title = "New title") }

        assertTrue(applied)
        assertEquals("New title", state.data.conversations.single().title)
    }

    @Test
    fun editConversationRejectedWhileGenerating() {
        val state = TestState(data())
        val registry = newRegistry()
        assertTrue(registry.begin("conversation", Job()))
        val answered = mutableListOf<Pair<String, String>>()
        val resumed = mutableListOf<String>()
        val workspace = workspace(state, answered, resumed, registry)

        val applied = workspace.editConversation("conversation") { it.copy(title = "New title") }

        assertFalse(applied)
        assertEquals("新对话", state.data.conversations.single().title)
        registry.cancel("conversation")
    }

    @Test
    fun editDataRejectedWhileGenerating() {
        val state = TestState(data())
        val registry = newRegistry()
        assertTrue(registry.begin("conversation", Job()))
        val answered = mutableListOf<Pair<String, String>>()
        val resumed = mutableListOf<String>()
        val workspace = workspace(state, answered, resumed, registry)

        val applied = workspace.editData("conversation") { it.copy(selectedConversationId = "other") }

        assertFalse(applied)
        assertEquals("conversation", state.data.selectedConversationId)
        registry.cancel("conversation")
    }

    @Test
    fun answerInjectsIntoPendingChannelWithoutResuming() {
        val state = TestState(data())
        val answered = mutableListOf<Pair<String, String>>()
        val resumed = mutableListOf<String>()
        val workspace = workspace(state, answered, resumed, pendingResult = true)

        workspace.answer("conversation", DesktopToolCall(id = "call-1", name = "ask_user", arguments = "{}"), "42")

        assertEquals(listOf("call-1" to "42"), answered)
        assertTrue(resumed.isEmpty())
        assertEquals(1, state.data.conversations.single().messages.size)
    }

    @Test
    fun answerIgnoredWhileGeneratingWithoutPendingAnswer() {
        val state = TestState(data())
        val registry = newRegistry()
        assertTrue(registry.begin("conversation", Job()))
        val answered = mutableListOf<Pair<String, String>>()
        val resumed = mutableListOf<String>()
        val workspace = workspace(state, answered, resumed, registry)

        workspace.answer("conversation", DesktopToolCall(id = "call-1", name = "ask_user", arguments = "{}"), "42")

        assertTrue(answered.isEmpty())
        assertTrue(resumed.isEmpty())
        assertEquals(1, state.data.conversations.single().messages.size)
        registry.cancel("conversation")
    }

    @Test
    fun answerResumesAfterInsertingToolMessageWhenIdle() {
        val state = TestState(data())
        val answered = mutableListOf<Pair<String, String>>()
        val resumed = mutableListOf<String>()
        val workspace = workspace(state, answered, resumed)

        workspace.answer("conversation", DesktopToolCall(id = "call-1", name = "ask_user", arguments = "{}"), "42")

        assertEquals(listOf("conversation"), resumed)
        val messages = state.data.conversations.single().messages
        assertEquals(2, messages.size)
        assertEquals("tool", messages.last().role)
        assertEquals("42", messages.last().content)
        assertEquals("call-1", messages.last().toolCallId)
    }

    @Test
    fun duplicateAnswerIsIgnored() {
        val state = TestState(
            data(messages = listOf(
                ChatMessage("user", "Hello"),
                ChatMessage("assistant", "", toolCalls = listOf(DesktopToolCall("call-1", "ask_user", "{}"))),
                ChatMessage("tool", "already answered", toolCallId = "call-1"),
            ))
        )
        val answered = mutableListOf<Pair<String, String>>()
        val resumed = mutableListOf<String>()
        val workspace = workspace(state, answered, resumed)

        workspace.answer("conversation", DesktopToolCall(id = "call-1", name = "ask_user", arguments = "{}"), "42")

        assertTrue(answered.isEmpty())
        assertTrue(resumed.isEmpty())
        assertEquals(3, state.data.conversations.single().messages.size)
    }

    private fun workspace(
        state: TestState,
        answered: MutableList<Pair<String, String>>,
        resumed: MutableList<String>,
        registry: DesktopGenerationRegistry = newRegistry(),
        pendingResult: Boolean = false,
    ): ConversationWorkspace = ConversationWorkspace(
        state = state,
        registry = registry,
        answerIfPending = { callId, answer ->
            if (pendingResult) answered += callId to answer
            pendingResult
        },
        resume = { conversationId -> resumed += conversationId },
    )
}

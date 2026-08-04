package me.rerere.rikkahub.desktop

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationExecutionTest {
    @Test
    fun continuesAfterToolCallWithToolResult() = runBlocking {
        val conversation = DesktopConversation(id = "conversation", messages = listOf(ChatMessage("user", "Search")))
        var data = DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id)
        val adapter = FakeConversationExecutionAdapter(
            flowOf(StreamDelta(toolCallDeltas = listOf(DesktopToolCallDelta(0, "call-1", "current_time")))),
            flowOf(StreamDelta(content = "It is noon")),
            toolResults = listOf(ChatMessage("tool", "12:00", toolCallId = "call-1"))
        )
        val execution = ConversationExecution(adapter, { data }, { data = it }, { _, _ -> })

        val result = execution.execute(ConversationExecutionCommand(conversation.id, conversation.messages))

        assertTrue(result.completed)
        assertEquals(2, adapter.streamRequests.size)
        assertEquals(listOf("user", "assistant", "tool", "assistant"), data.conversations.single().messages.map(ChatMessage::role))
        assertEquals("It is noon", data.conversations.single().messages.last().content)
    }

    @Test
    fun cancellationClearsUnresolvedToolCallAfterVisibleContent() = runBlocking {
        val conversation = DesktopConversation(id = "conversation", messages = listOf(ChatMessage("user", "Search")))
        var data = DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id)
        val adapter = FakeConversationExecutionAdapter(flow {
            emit(StreamDelta(content = "Working", toolCallDeltas = listOf(DesktopToolCallDelta(0, "call-1", "search"))))
            throw CancellationException()
        })
        val execution = ConversationExecution(adapter, { data }, { data = it }, { _, _ -> })

        val result = execution.execute(ConversationExecutionCommand(conversation.id, conversation.messages))

        assertTrue(!result.completed)
        val message = data.conversations.single().messages.last()
        assertEquals("Working", message.content)
        assertTrue(message.toolCalls.isEmpty())
    }

    @Test
    fun streamsResponseIntoAssistantMessage() = runBlocking {
        val conversation = DesktopConversation(id = "conversation", messages = listOf(ChatMessage("user", "Hello")))
        var data = DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id)
        val errors = mutableListOf<String>()
        val execution = ConversationExecution(
            adapter = FakeConversationExecutionAdapter(flowOf(StreamDelta(content = "Hi there"))),
            currentData = { data },
            updateData = { data = it },
            reportError = { _, message -> errors += message }
        )

        val result = execution.execute(
            ConversationExecutionCommand(
                conversationId = conversation.id,
                requestMessages = conversation.messages
            )
        )

        assertTrue(result.completed)
        assertTrue(errors.isEmpty())
        assertEquals("Hi there", data.conversations.single().messages.last().content)
        assertEquals("assistant", data.conversations.single().messages.last().role)
    }

    private class FakeConversationExecutionAdapter(
        private vararg val responses: Flow<StreamDelta>,
        private val toolResults: List<ChatMessage> = emptyList()
    ) : ConversationExecutionAdapter {
        val streamRequests = mutableListOf<List<ChatMessage>>()

        override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> {
            streamRequests += messages
            return responses[streamRequests.lastIndex]
        }

        override fun toolsAreCurrent(server: DesktopMcpServer): Boolean = true

        override suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> = emptyList()

        override suspend fun executeToolCalls(
            config: DesktopConfig,
            calls: List<DesktopToolCall>
        ): List<ChatMessage> = toolResults
    }
}

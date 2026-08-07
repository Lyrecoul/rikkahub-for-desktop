package me.rerere.rikkahub.desktop

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConversationExecutionTest {
    @Test
    fun continuesAfterToolCallWithToolResult() = runBlocking {
        val conversation = DesktopConversation(id = "conversation", messages = listOf(ChatMessage("user", "Search")))
        var data = DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id)
        val adapter = FakeAdapters(
            flowOf(StreamDelta(toolCallDeltas = listOf(DesktopToolCallDelta(0, "call-1", "current_time")))),
            flowOf(StreamDelta(content = "It is noon")),
            toolResults = listOf(ChatMessage("tool", "12:00", toolCallId = "call-1"))
        )
        val execution = execution(adapter, { data = it }, data)

        val result = execution.execute(ConversationExecutionRequest(conversation.id, conversation.messages))

        assertEquals(ConversationExecutionOutcome.Completed, result)
        assertEquals(2, adapter.streamRequests.size)
        assertEquals(listOf("user", "assistant", "tool", "assistant"), data.conversations.single().messages.map(ChatMessage::role))
        assertEquals("It is noon", data.conversations.single().messages.last().content)
    }

    @Test
    fun reportsToolFailureAndRemovesUnresolvedAssistant() = runBlocking {
        var data = testData()
        val adapter = FakeAdapters(flowOf(StreamDelta(toolCallDeltas = listOf(DesktopToolCallDelta(0, "call-1", "search")))), executeFailure = IllegalStateException("tool failed"))
        val execution = execution(adapter, { data = it }, data)

        val result = execution.execute(ConversationExecutionRequest("conversation", data.conversations.single().messages))

        assertIs<ConversationExecutionOutcome.Failed>(result)
        assertEquals(ConversationExecutionFailure.Execution("tool failed"), result.reason)
        assertEquals(listOf("user"), data.conversations.single().messages.map(ChatMessage::role))
    }

    @Test
    fun reportsStreamFailureAndRemovesEmptyAssistant() = runBlocking {
        var data = testData()
        val adapter = FakeAdapters(flow { error(IllegalStateException("connection refused")) })
        val execution = execution(adapter, { data = it }, data)

        val result = execution.execute(ConversationExecutionRequest("conversation", data.conversations.single().messages))

        assertEquals(ConversationExecutionFailure.Execution("connection refused"), assertIs<ConversationExecutionOutcome.Failed>(result).reason)
        assertEquals(listOf("user"), data.conversations.single().messages.map(ChatMessage::role))
    }

    @Test
    fun rejectsMissingSelectedMcpServerBeforeStartingExecution() = runBlocking {
        val assistant = DesktopAssistantProfile(id = "assistant", mcpServerIds = setOf("missing"))
        val conversation = DesktopConversation(id = "conversation", assistantId = assistant.id, messages = listOf(ChatMessage("user", "Search")))
        var data = DesktopData(assistants = listOf(assistant), selectedAssistantId = assistant.id, conversations = listOf(conversation), selectedConversationId = conversation.id)
        val adapter = FakeAdapters(flowOf(StreamDelta(content = "must not run")))
        val execution = execution(adapter, { data = it }, data)

        val result = execution.execute(ConversationExecutionRequest(conversation.id, conversation.messages))

        assertEquals(ConversationExecutionFailure.InvalidMcpConfiguration, assertIs<ConversationExecutionOutcome.Failed>(result).reason)
        assertTrue(adapter.streamRequests.isEmpty())
    }

    @Test
    fun reportsMcpSynchronizationFailure() = runBlocking {
        val server = DesktopMcpServer(id = "server", name = "Search", enabled = true)
        val assistant = DesktopAssistantProfile(id = "assistant", mcpServerIds = setOf(server.id))
        val conversation = DesktopConversation(id = "conversation", assistantId = assistant.id, messages = listOf(ChatMessage("user", "Search")))
        var data = DesktopData(assistants = listOf(assistant), selectedAssistantId = assistant.id, mcpServers = listOf(server), conversations = listOf(conversation), selectedConversationId = conversation.id)
        val adapter = FakeAdapters(flowOf(), currentTools = false, syncFailure = IllegalStateException("connection refused"))
        val execution = execution(adapter, { data = it }, data)

        val result = execution.execute(ConversationExecutionRequest(conversation.id, conversation.messages))

        assertEquals(ConversationExecutionFailure.McpSynchronization("connection refused"), assertIs<ConversationExecutionOutcome.Failed>(result).reason)
    }

    @Test
    fun rejectsMcpSynchronizationWithNoTools() = runBlocking {
        val server = DesktopMcpServer(id = "server", name = "Search", enabled = true)
        val assistant = DesktopAssistantProfile(id = "assistant", mcpServerIds = setOf(server.id))
        val conversation = DesktopConversation(id = "conversation", assistantId = assistant.id, messages = listOf(ChatMessage("user", "Search")))
        var data = DesktopData(assistants = listOf(assistant), selectedAssistantId = assistant.id, mcpServers = listOf(server), conversations = listOf(conversation), selectedConversationId = conversation.id)
        val adapter = FakeAdapters(flowOf(), currentTools = false, synchronizedTools = emptyList())
        val execution = execution(adapter, { data = it }, data)

        val result = execution.execute(ConversationExecutionRequest(conversation.id, conversation.messages))

        assertEquals(ConversationExecutionFailure.McpSynchronization("MCP server Search did not provide usable tools"), assertIs<ConversationExecutionOutcome.Failed>(result).reason)
    }

    @Test
    fun cancellationCleansUnresolvedToolCall() = runBlocking {
        var data = testData()
        val adapter = FakeAdapters(flow {
            emit(StreamDelta(content = "Working", toolCallDeltas = listOf(DesktopToolCallDelta(0, "call-1", "search"))))
            throw CancellationException()
        })
        val execution = execution(adapter, { data = it }, data)

        val result = execution.execute(ConversationExecutionRequest("conversation", data.conversations.single().messages))

        assertEquals(ConversationExecutionOutcome.Cancelled, result)
        assertEquals("Working", data.conversations.single().messages.last().content)
        assertTrue(data.conversations.single().messages.last().toolCalls.isEmpty())
    }

    @Test
    fun stopsAtConfiguredToolRoundLimit() = runBlocking {
        val assistant = DesktopAssistantProfile(id = "assistant", maxToolRounds = 1)
        val conversation = DesktopConversation(id = "conversation", assistantId = assistant.id, messages = listOf(ChatMessage("user", "Search")))
        var data = DesktopData(assistants = listOf(assistant), selectedAssistantId = assistant.id, conversations = listOf(conversation), selectedConversationId = conversation.id)
        val adapter = FakeAdapters(
            flowOf(StreamDelta(toolCallDeltas = listOf(DesktopToolCallDelta(0, "call-1", "search")))),
            flowOf(StreamDelta(toolCallDeltas = listOf(DesktopToolCallDelta(0, "call-2", "search")))),
            toolResults = listOf(ChatMessage("tool", "first result", toolCallId = "call-1"))
        )
        val execution = execution(adapter, { data = it }, data)

        val result = execution.execute(ConversationExecutionRequest(conversation.id, conversation.messages))

        assertEquals(ConversationExecutionOutcome.Stopped(ConversationExecutionStopReason.TOOL_ROUND_LIMIT), result)
        assertEquals(2, adapter.streamRequests.size)
    }

    @Test
    fun batchesRapidStreamDeltasBeforePublishingConversationState() = runBlocking {
        var data = testData()
        var updates = 0
        val adapter = FakeAdapters(flowOf(StreamDelta(content = "one "), StreamDelta(content = "two "), StreamDelta(content = "three")))
        val execution = execution(adapter, { data = it; updates++ }, data, clock = { 0L })

        val result = execution.execute(ConversationExecutionRequest("conversation", data.conversations.single().messages))

        assertEquals(ConversationExecutionOutcome.Completed, result)
        assertEquals(3, updates)
        assertEquals("one two three", data.conversations.single().messages.last().content)
    }

    @Test
    fun rejectsUnsupportedAttachmentsBeforeCreatingAssistantMessage() = runBlocking {
        val conversation = DesktopConversation(id = "conversation", messages = listOf(ChatMessage("user", "Inspect", attachments = listOf(DesktopAttachment("photo.png", "image/png", "AQID", isImage = true)))))
        var data = DesktopData(config = DesktopConfig(model = "custom-text-model"), conversations = listOf(conversation), selectedConversationId = conversation.id)
        val adapter = FakeAdapters(flowOf(StreamDelta(content = "must not run")))
        val execution = execution(adapter, { data = it }, data)

        val result = execution.execute(ConversationExecutionRequest(conversation.id, conversation.messages))

        assertIs<ConversationExecutionOutcome.Failed>(result)
        assertEquals(1, data.conversations.single().messages.size)
        assertTrue((result.reason as ConversationExecutionFailure.InvalidRequest).detail.contains("photo.png"))
        assertTrue(adapter.streamRequests.isEmpty())
    }

    private fun testData(): DesktopData {
        val conversation = DesktopConversation(id = "conversation", messages = listOf(ChatMessage("user", "Hello")))
        return DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id)
    }

    private fun execution(
        adapters: FakeAdapters,
        update: (DesktopData) -> Unit,
        initial: DesktopData,
        clock: () -> Long = System::currentTimeMillis
    ): ConversationExecution {
        var data = initial
        return ConversationExecution(
            model = adapters,
            tools = adapters,
            state = object : ConversationExecutionState {
                override fun current(): DesktopData = data
                override fun update(transform: (DesktopData) -> DesktopData) {
                    data = transform(data)
                    update(data)
                }
            },
            text = adapters,
            clock = clock
        )
    }

    private class FakeAdapters(
        private vararg val responses: Flow<StreamDelta>,
        private val currentTools: Boolean = true,
        private val synchronizedTools: List<DesktopMcpTool> = listOf(DesktopMcpTool(name = "search", description = "Search")),
        private val syncFailure: Throwable? = null,
        private val toolResults: List<ChatMessage> = emptyList(),
        private val executeFailure: Throwable? = null
    ) : ConversationModelStreamAdapter, ConversationToolRuntimeAdapter, ConversationExecutionText {
        val streamRequests = mutableListOf<List<ChatMessage>>()

        override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> {
            streamRequests += messages
            return responses[streamRequests.lastIndex]
        }

        override fun toolsAreCurrent(server: DesktopMcpServer): Boolean = currentTools
        override suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> = syncFailure?.let { throw it } ?: synchronizedTools
        override suspend fun execute(config: DesktopConfig, calls: List<DesktopToolCall>): List<ChatMessage> = executeFailure?.let { throw it } ?: toolResults
        override fun noMcpTools(server: DesktopMcpServer): String = "MCP server ${server.name} did not provide usable tools"
        override fun invalidMessageTemplate(): String = "Invalid message template"
        override fun toolRoundLimit(limit: Int): String = "Tool limit $limit"
    }
}

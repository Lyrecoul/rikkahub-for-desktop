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
    fun removesUnresolvedToolCallWhenToolExecutionFails() = runBlocking {
        val conversation = DesktopConversation(id = "conversation", messages = listOf(ChatMessage("user", "Search")))
        var data = DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id)
        val errors = mutableListOf<String>()
        val execution = ConversationExecution(
            adapter = object : ConversationExecutionAdapter {
                override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> =
                    flowOf(StreamDelta(toolCallDeltas = listOf(DesktopToolCallDelta(0, "call-1", "search"))))

                override fun toolsAreCurrent(server: DesktopMcpServer): Boolean = true
                override suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> = emptyList()
                override suspend fun executeToolCalls(
                    config: DesktopConfig,
                    calls: List<DesktopToolCall>
                ): List<ChatMessage> = error("tool failed")
            },
            currentData = { data },
            updateData = { data = it },
            reportError = { _, message -> errors += message }
        )

        val result = execution.execute(ConversationExecutionCommand(conversation.id, conversation.messages))

        assertTrue(!result.completed)
        assertEquals(listOf("user"), data.conversations.single().messages.map(ChatMessage::role))
        assertEquals("tool failed", errors.single())
    }

    @Test
    fun removesEmptyAssistantMessageWhenModelStreamFails() = runBlocking {
        val conversation = DesktopConversation(id = "conversation", messages = listOf(ChatMessage("user", "Hello")))
        var data = DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id)
        val errors = mutableListOf<String>()
        val execution = ConversationExecution(
            adapter = object : ConversationExecutionAdapter {
                override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> =
                    flow { error("connection refused") }

                override fun toolsAreCurrent(server: DesktopMcpServer): Boolean = true
                override suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> = emptyList()
                override suspend fun executeToolCalls(
                    config: DesktopConfig,
                    calls: List<DesktopToolCall>
                ): List<ChatMessage> = emptyList()
            },
            currentData = { data },
            updateData = { data = it },
            reportError = { _, message -> errors += message }
        )

        val result = execution.execute(ConversationExecutionCommand(conversation.id, conversation.messages))

        assertTrue(!result.completed)
        assertEquals(listOf("user"), data.conversations.single().messages.map(ChatMessage::role))
        assertEquals("connection refused", errors.single())
    }

    @Test
    fun identifiesTheServerWhenMcpSynchronizationReturnsNoTools() = runBlocking {
        val server = DesktopMcpServer(id = "server", name = "Search", enabled = true)
        val assistant = DesktopAssistantProfile(id = "assistant", mcpServerIds = setOf(server.id))
        val conversation = DesktopConversation(
            id = "conversation",
            assistantId = assistant.id,
            messages = listOf(ChatMessage("user", "Search"))
        )
        var data = DesktopData(
            assistants = listOf(assistant),
            selectedAssistantId = assistant.id,
            mcpServers = listOf(server),
            conversations = listOf(conversation),
            selectedConversationId = conversation.id
        )
        val errors = mutableListOf<String>()
        val execution = ConversationExecution(
            adapter = object : ConversationExecutionAdapter {
                override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> =
                    error("The model request must not start")

                override fun toolsAreCurrent(server: DesktopMcpServer): Boolean = false
                override suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> = emptyList()
                override suspend fun executeToolCalls(
                    config: DesktopConfig,
                    calls: List<DesktopToolCall>
                ): List<ChatMessage> = emptyList()
            },
            currentData = { data },
            updateData = { data = it },
            reportError = { _, message -> errors += message }
        )

        val result = execution.execute(ConversationExecutionCommand(conversation.id, conversation.messages))

        assertTrue(!result.completed)
        val noTools = desktopText(data.preferences.language, "runtime.mcp_no_tools").replace("%s", server.name)
        assertEquals(
            desktopText(data.preferences.language, "runtime.mcp_sync_failed").replace("%s", noTools),
            errors.single()
        )
    }

    @Test
    fun rejectsMissingSelectedMcpServerBeforeStartingExecution() = runBlocking {
        val assistant = DesktopAssistantProfile(id = "assistant", mcpServerIds = setOf("missing"))
        val conversation = DesktopConversation(
            id = "conversation",
            assistantId = assistant.id,
            messages = listOf(ChatMessage("user", "Search"))
        )
        var data = DesktopData(
            assistants = listOf(assistant),
            selectedAssistantId = assistant.id,
            conversations = listOf(conversation),
            selectedConversationId = conversation.id
        )
        val errors = mutableListOf<String>()
        val execution = ConversationExecution(
            adapter = object : ConversationExecutionAdapter {
                override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> =
                    error("The model request must not start")

                override fun toolsAreCurrent(server: DesktopMcpServer): Boolean = error("MCP must not synchronize")
                override suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> = emptyList()
                override suspend fun executeToolCalls(
                    config: DesktopConfig,
                    calls: List<DesktopToolCall>
                ): List<ChatMessage> = emptyList()
            },
            currentData = { data },
            updateData = { data = it },
            reportError = { _, message -> errors += message }
        )

        val result = execution.execute(ConversationExecutionCommand(conversation.id, conversation.messages))

        assertTrue(!result.completed)
        assertEquals(desktopText(data.preferences.language, "runtime.mcp_configuration_invalid"), errors.single())
    }

    @Test
    fun rejectsUnsupportedAttachmentsBeforeCreatingAssistantMessage() = runBlocking {
        val conversation = DesktopConversation(
            id = "conversation",
            messages = listOf(
                ChatMessage(
                    "user",
                    "Inspect",
                    attachments = listOf(DesktopAttachment("photo.png", "image/png", "AQID", isImage = true))
                )
            )
        )
        var data = DesktopData(
            config = DesktopConfig(model = "custom-text-model"),
            conversations = listOf(conversation),
            selectedConversationId = conversation.id
        )
        val errors = mutableListOf<String>()
        val execution = ConversationExecution(
            adapter = object : ConversationExecutionAdapter {
                override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> =
                    error("The model request must not start")

                override fun toolsAreCurrent(server: DesktopMcpServer): Boolean = true
                override suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> = emptyList()
                override suspend fun executeToolCalls(
                    config: DesktopConfig,
                    calls: List<DesktopToolCall>
                ): List<ChatMessage> = emptyList()
            },
            currentData = { data },
            updateData = { data = it },
            reportError = { _, message -> errors += message }
        )

        val result = execution.execute(ConversationExecutionCommand(conversation.id, conversation.messages))

        assertTrue(!result.completed)
        assertEquals(1, data.conversations.single().messages.size)
        assertTrue(errors.single().contains("photo.png"))
    }

    @Test
    fun reportsLocalizedErrorWhenMcpToolSynchronizationFails() = runBlocking {
        val server = DesktopMcpServer(id = "server", name = "Search", enabled = true)
        val assistant = DesktopAssistantProfile(id = "assistant", mcpServerIds = setOf(server.id))
        val conversation = DesktopConversation(
            id = "conversation",
            assistantId = assistant.id,
            messages = listOf(ChatMessage("user", "Search"))
        )
        var data = DesktopData(
            assistants = listOf(assistant),
            selectedAssistantId = assistant.id,
            mcpServers = listOf(server),
            conversations = listOf(conversation),
            selectedConversationId = conversation.id
        )
        val errors = mutableListOf<String>()
        val execution = ConversationExecution(
            adapter = object : ConversationExecutionAdapter {
                override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> =
                    error("The model request must not start")

                override fun toolsAreCurrent(server: DesktopMcpServer): Boolean = false

                override suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> =
                    error("connection refused")

                override suspend fun executeToolCalls(
                    config: DesktopConfig,
                    calls: List<DesktopToolCall>
                ): List<ChatMessage> = emptyList()
            },
            currentData = { data },
            updateData = { data = it },
            reportError = { _, message -> errors += message }
        )

        val result = execution.execute(ConversationExecutionCommand(conversation.id, conversation.messages))

        assertTrue(!result.completed)
        assertEquals(
            desktopText(data.preferences.language, "runtime.mcp_sync_failed").replace("%s", "connection refused"),
            errors.single()
        )
    }

    @Test
    fun synchronizesStaleMcpToolsBeforeStartingTheModelRequest() = runBlocking {
        val server = DesktopMcpServer(id = "server", name = "Search", enabled = true)
        val assistant = DesktopAssistantProfile(id = "assistant", mcpServerIds = setOf(server.id))
        val conversation = DesktopConversation(
            id = "conversation",
            assistantId = assistant.id,
            messages = listOf(ChatMessage("user", "Search"))
        )
        var data = DesktopData(
            assistants = listOf(assistant),
            selectedAssistantId = assistant.id,
            mcpServers = listOf(server),
            conversations = listOf(conversation),
            selectedConversationId = conversation.id
        )
        val synchronizedTool = DesktopMcpTool(name = "search", description = "Search")
        val execution = ConversationExecution(
            adapter = object : ConversationExecutionAdapter {
                override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> =
                    flowOf(StreamDelta(content = "Done"))

                override fun toolsAreCurrent(server: DesktopMcpServer): Boolean = false

                override suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> = listOf(synchronizedTool)

                override suspend fun executeToolCalls(
                    config: DesktopConfig,
                    calls: List<DesktopToolCall>
                ): List<ChatMessage> = emptyList()
            },
            currentData = { data },
            updateData = { data = it },
            reportError = { _, message -> error(message) }
        )

        val result = execution.execute(ConversationExecutionCommand(conversation.id, conversation.messages))

        assertTrue(result.completed)
        assertEquals(listOf(synchronizedTool), data.mcpServers.single().tools)
        assertTrue(data.mcpServers.single().hasCurrentTools())
    }

    @Test
    fun cancellationDuringMcpSyncDoesNotPublishAnError() = runBlocking {
        val server = DesktopMcpServer(id = "server", name = "Search", enabled = true)
        val assistant = DesktopAssistantProfile(id = "assistant", mcpServerIds = setOf(server.id))
        val conversation = DesktopConversation(
            id = "conversation",
            assistantId = assistant.id,
            messages = listOf(ChatMessage("user", "Search"))
        )
        var data = DesktopData(
            assistants = listOf(assistant),
            selectedAssistantId = assistant.id,
            mcpServers = listOf(server),
            conversations = listOf(conversation),
            selectedConversationId = conversation.id
        )
        val errors = mutableListOf<String>()
        val execution = ConversationExecution(
            adapter = object : ConversationExecutionAdapter {
                override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> = flowOf()
                override fun toolsAreCurrent(server: DesktopMcpServer): Boolean = false
                override suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> = throw CancellationException()
                override suspend fun executeToolCalls(
                    config: DesktopConfig,
                    calls: List<DesktopToolCall>
                ): List<ChatMessage> = emptyList()
            },
            currentData = { data },
            updateData = { data = it },
            reportError = { _, message -> errors += message }
        )

        val result = execution.execute(ConversationExecutionCommand(conversation.id, conversation.messages))

        assertTrue(!result.completed)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun stopsAtConfiguredToolRoundLimit() = runBlocking {
        val assistant = DesktopAssistantProfile(id = "assistant", maxToolRounds = 1)
        val conversation = DesktopConversation(
            id = "conversation",
            assistantId = assistant.id,
            messages = listOf(ChatMessage("user", "Search"))
        )
        var data = DesktopData(
            assistants = listOf(assistant),
            selectedAssistantId = assistant.id,
            conversations = listOf(conversation),
            selectedConversationId = conversation.id
        )
        val adapter = FakeConversationExecutionAdapter(
            flowOf(StreamDelta(toolCallDeltas = listOf(DesktopToolCallDelta(0, "call-1", "search")))),
            flowOf(StreamDelta(toolCallDeltas = listOf(DesktopToolCallDelta(0, "call-2", "search")))),
            toolResults = listOf(ChatMessage("tool", "first result", toolCallId = "call-1"))
        )
        val execution = ConversationExecution(adapter, { data }, { data = it }, { _, _ -> })

        val result = execution.execute(ConversationExecutionCommand(conversation.id, conversation.messages))

        assertTrue(result.completed)
        assertEquals(2, adapter.streamRequests.size)
        val messages = data.conversations.single().messages
        assertEquals("call-2", messages[messages.lastIndex - 1].toolCallId)
        assertTrue(messages.last().content.isNotBlank())
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
    fun batchesRapidStreamDeltasBeforePublishingConversationState() = runBlocking {
        val conversation = DesktopConversation(id = "conversation", messages = listOf(ChatMessage("user", "Hello")))
        var data = DesktopData(conversations = listOf(conversation), selectedConversationId = conversation.id)
        var updates = 0
        val execution = ConversationExecution(
            adapter = FakeConversationExecutionAdapter(
                flowOf(StreamDelta(content = "one "), StreamDelta(content = "two "), StreamDelta(content = "three"))
            ),
            currentData = { data },
            updateData = { data = it; updates++ },
            reportError = { _, _ -> },
            clock = { 0L }
        )

        val result = execution.execute(ConversationExecutionCommand(conversation.id, conversation.messages))

        assertTrue(result.completed)
        assertEquals(3, updates)
        assertEquals("one two three", data.conversations.single().messages.last().content)
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

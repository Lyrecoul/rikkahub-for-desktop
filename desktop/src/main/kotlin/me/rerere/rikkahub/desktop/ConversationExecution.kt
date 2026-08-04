package me.rerere.rikkahub.desktop

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

private const val StreamUpdateIntervalMillis = 50L
private const val LongStreamUpdateIntervalMillis = 200L
private const val LongStreamOutputThreshold = 6_000

internal data class ConversationExecutionCommand(
    val conversationId: String,
    val requestMessages: List<ChatMessage>,
    val title: String? = null,
    val alternativeTarget: ChatMessage? = null
)

internal data class ConversationExecutionResult(val completed: Boolean)

internal interface ConversationExecutionAdapter {
    fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta>
    fun toolsAreCurrent(server: DesktopMcpServer): Boolean
    suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool>
    suspend fun executeToolCalls(config: DesktopConfig, calls: List<DesktopToolCall>): List<ChatMessage>
}

internal class DesktopConversationExecutionAdapter(
    private val client: OpenAiClient,
    private val mcpClient: DesktopMcpClient,
    private val executeTools: suspend (DesktopConfig, List<DesktopToolCall>) -> List<ChatMessage>
) : ConversationExecutionAdapter {
    override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> =
        client.stream(config, messages)

    override fun toolsAreCurrent(server: DesktopMcpServer): Boolean = mcpClient.toolsAreCurrent(server)

    override suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> = mcpClient.syncTools(server)

    override suspend fun executeToolCalls(
        config: DesktopConfig,
        calls: List<DesktopToolCall>
    ): List<ChatMessage> = executeTools(config, calls)
}

internal class ConversationExecution(
    private val adapter: ConversationExecutionAdapter,
    private val currentData: () -> DesktopData,
    private val updateData: (DesktopData) -> Unit,
    private val reportError: (String, String) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun execute(command: ConversationExecutionCommand): ConversationExecutionResult {
        val initialData = currentData()
        val conversation = initialData.conversations.firstOrNull { it.id == command.conversationId }
            ?: return ConversationExecutionResult(completed = false)
        val assistant = initialData.assistantFor(conversation)
        val selectedServers = initialData.mcpServers.filter { it.enabled && it.id in assistant.mcpServerIds }
        if ((assistant.mcpServerIds - initialData.mcpServers.map(DesktopMcpServer::id).toSet()).isNotEmpty()) {
            return fail(command.conversationId, initialData, "runtime.mcp_configuration_invalid")
        }
        try {
            try {
                syncTools(selectedServers)
            } catch (error: Throwable) {
                reportError(
                    command.conversationId,
                    desktopText(currentData().preferences.language, "runtime.mcp_sync_failed")
                        .replace("%s", error.userFacingMessage())
                )
                return ConversationExecutionResult(completed = false)
            }
            val initialRequest = prepareRequest(command.conversationId, command.requestMessages, assistant)
                ?: return ConversationExecutionResult(completed = false)
            updateConversation(command.conversationId) {
                it.prepareGeneration(
                    requestMessages = command.requestMessages,
                    alternativeTarget = command.alternativeTarget,
                    title = command.title,
                    modelId = initialRequest.config.model
                )
            }
            var request = initialRequest.messages
            var toolRounds = 0
            while (true) {
                collectStream(command.conversationId, initialRequest.config, request)
                val toolCalls = conversation(command.conversationId)?.messages?.lastOrNull()?.toolCalls.orEmpty()
                if (toolCalls.isEmpty()) break
                finishReasoning(command.conversationId)
                if (assistant.maxToolRounds > 0 && toolRounds >= assistant.maxToolRounds) {
                    val limit = desktopText(currentData().preferences.language, "runtime.tool_round_limit")
                        .replace("%d", assistant.maxToolRounds.toString())
                    updateConversation(command.conversationId) { current ->
                        current.copy(
                            messages = current.messages + toolCalls.map { call ->
                                ChatMessage(role = "tool", content = limit, toolCallId = call.id)
                            } + ChatMessage(role = "assistant", content = limit),
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    break
                }
                toolRounds++
                val toolResults = adapter.executeToolCalls(initialRequest.config, toolCalls)
                updateConversation(command.conversationId) { current ->
                    current.copy(messages = current.messages + toolResults, updatedAt = System.currentTimeMillis())
                }
                if (toolResults.any { it.content == DesktopAgentApprovalDeniedResult }) break
                val nextRequest = prepareRequest(
                    command.conversationId,
                    requireNotNull(conversation(command.conversationId)).messages,
                    assistant
                ) ?: return ConversationExecutionResult(completed = false)
                request = nextRequest.messages
                updateConversation(command.conversationId) { current ->
                    current.copy(
                        messages = current.messages + ChatMessage(
                            role = "assistant",
                            content = "",
                            modelId = initialRequest.config.model
                        ),
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }
            complete(command.conversationId, assistant)
            return ConversationExecutionResult(completed = true)
        } catch (_: CancellationException) {
            cancel(command, assistant)
            return ConversationExecutionResult(completed = false)
        } catch (error: Throwable) {
            reportError(command.conversationId, error.userFacingMessage())
            cancel(command, assistant)
            return ConversationExecutionResult(completed = false)
        }
    }

    private suspend fun syncTools(servers: List<DesktopMcpServer>) {
        val stale = servers.filterNot(adapter::toolsAreCurrent)
        if (stale.isEmpty()) return
        val toolsByServer = stale.associate { server ->
            server.id to adapter.syncTools(server).also { tools -> check(tools.isNotEmpty()) { "${server.name} has no tools" } }
        }
        updateData(currentData().copy(mcpServers = currentData().mcpServers.map { server ->
            toolsByServer[server.id]?.let(server::withSyncedTools) ?: server
        }))
    }

    private fun prepareRequest(
        conversationId: String,
        requestMessages: List<ChatMessage>,
        assistant: DesktopAssistantProfile
    ): PreparedRequest? {
        val conversation = conversation(conversationId) ?: return null
        val requestAssistant = if (conversation.usesPromptInjections(assistant)) assistant
        else assistant.copy(promptInjections = emptyList())
        val baseConfig = currentData().configForConversation(conversation)
        val injected = requestAssistant.injectPromptMessages(requestAssistant.limitContext(requestMessages))
        val config = baseConfig.copy(
            systemPrompt = (injected.systemPrefix + baseConfig.systemPrompt + injected.systemSuffix)
                .filter(String::isNotBlank)
                .joinToString("\n\n")
        )
        config.validateAttachments(injected.messages).takeIf { it.isNotEmpty() }?.let { issues ->
            reportError(conversationId, issues.toUserMessage())
            return null
        }
        val messages = runCatching {
            requestAssistant.renderMessageTemplate(requestAssistant.transformRequestMessages(injected.messages))
        }.getOrElse { error ->
            reportError(
                conversationId,
                error.message ?: desktopText(currentData().preferences.language, "runtime.invalid_message_template")
            )
            return null
        }
        return PreparedRequest(config, messages)
    }

    private suspend fun collectStream(conversationId: String, config: DesktopConfig, request: List<ChatMessage>) {
        val content = StringBuilder()
        val reasoning = StringBuilder()
        val reasoningSignature = StringBuilder()
        val citations = mutableListOf<DesktopCitation>()
        val attachments = mutableListOf<DesktopAttachment>()
        val toolCalls = mutableListOf<DesktopToolCallDelta>()
        var modelId: String? = null
        var promptTokens: Int? = null
        var completionTokens: Int? = null
        var cachedTokens: Int? = null
        var outputLength = 0
        var lastUpdateAt = 0L

        fun flush() {
            if (
                content.isEmpty() && reasoning.isEmpty() && reasoningSignature.isEmpty() && modelId == null &&
                promptTokens == null && completionTokens == null && cachedTokens == null && citations.isEmpty() &&
                attachments.isEmpty() && toolCalls.isEmpty()
            ) return
            appendDelta(
                conversationId,
                StreamDelta(
                    content = content.toString(),
                    reasoning = reasoning.toString(),
                    reasoningSignature = reasoningSignature.toString(),
                    modelId = modelId,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    cachedTokens = cachedTokens,
                    citations = citations.toList(),
                    attachments = attachments.toList(),
                    toolCallDeltas = toolCalls.toList()
                )
            )
            content.clear()
            reasoning.clear()
            reasoningSignature.clear()
            citations.clear()
            attachments.clear()
            toolCalls.clear()
            modelId = null
            promptTokens = null
            completionTokens = null
            cachedTokens = null
        }

        try {
            adapter.stream(config, request).collect { delta ->
                content.append(delta.content)
                reasoning.append(delta.reasoning)
                reasoningSignature.append(delta.reasoningSignature)
                citations += delta.citations
                attachments += delta.attachments
                toolCalls += delta.toolCallDeltas
                modelId = delta.modelId ?: modelId
                promptTokens = delta.promptTokens ?: promptTokens
                completionTokens = delta.completionTokens ?: completionTokens
                cachedTokens = delta.cachedTokens ?: cachedTokens
                outputLength += delta.content.length + delta.reasoning.length
                val interval = if (outputLength >= LongStreamOutputThreshold) {
                    LongStreamUpdateIntervalMillis
                } else {
                    StreamUpdateIntervalMillis
                }
                val now = clock()
                if (now - lastUpdateAt >= interval) {
                    flush()
                    lastUpdateAt = now
                }
            }
        } finally {
            flush()
        }
    }

    private fun appendDelta(conversationId: String, delta: StreamDelta) {
        updateConversation(conversationId) { current ->
            val messages = current.messages.toMutableList()
            val last = messages.lastOrNull()
            if (last?.role == "assistant") {
                val receivedAt = System.currentTimeMillis()
                val reasoningStartedAt = last.reasoningStartedAt
                    ?: delta.reasoning.takeIf(String::isNotBlank)?.let { receivedAt }
                messages[messages.lastIndex] = last.copy(
                    content = last.content + delta.content,
                    reasoning = last.reasoning + delta.reasoning,
                    reasoningSignature = last.reasoningSignature + delta.reasoningSignature,
                    reasoningStartedAt = reasoningStartedAt,
                    reasoningDurationMillis = if (delta.reasoning.isNotBlank() && reasoningStartedAt != null) {
                        (receivedAt - reasoningStartedAt).coerceAtLeast(0)
                    } else last.reasoningDurationMillis,
                    modelId = delta.modelId ?: last.modelId,
                    promptTokens = delta.promptTokens ?: last.promptTokens,
                    completionTokens = delta.completionTokens ?: last.completionTokens,
                    cachedTokens = delta.cachedTokens ?: last.cachedTokens,
                    citations = (last.citations + delta.citations).distinctBy(DesktopCitation::url),
                    attachments = (last.attachments + delta.attachments).distinctBy(DesktopAttachment::data),
                    toolCalls = last.toolCalls.merge(delta.toolCallDeltas)
                )
            }
            current.copy(messages = messages, updatedAt = System.currentTimeMillis())
        }
    }

    private fun finishReasoning(conversationId: String) = updateConversation(conversationId) { current ->
        val messages = current.messages.toMutableList()
        val last = messages.lastOrNull()
        if (last?.role == "assistant") messages[messages.lastIndex] = last.completeReasoningDuration()
        current.copy(messages = messages, updatedAt = System.currentTimeMillis())
    }

    private fun complete(conversationId: String, assistant: DesktopAssistantProfile) = updateConversation(conversationId) { current ->
        val messages = current.messages.toMutableList()
        val last = messages.lastOrNull()
        if (last?.role == "assistant") {
            messages[messages.lastIndex] = assistant.transformGeneratedMessage(last.completeReasoningDuration()).completeAlternative()
        }
        current.copy(messages = messages, updatedAt = System.currentTimeMillis())
    }

    private fun cancel(command: ConversationExecutionCommand, assistant: DesktopAssistantProfile) = updateConversation(command.conversationId) { current ->
        val last = current.messages.lastOrNull()
        when {
            last?.role == "assistant" && last.toolCalls.isNotEmpty() -> {
                val cleaned = last.copy(toolCalls = emptyList())
                if (cleaned.content.isBlank() && cleaned.reasoning.isBlank()) {
                    val messages = current.messages.dropLast(1)
                    current.copy(messages = if (command.alternativeTarget == null) messages else messages + command.alternativeTarget)
                } else {
                    current.copy(
                        messages = current.messages.dropLast(1) +
                            assistant.transformGeneratedMessage(cleaned.completeReasoningDuration()).completeAlternative()
                    )
                }
            }
            last?.role == "assistant" && last.content.isBlank() && last.reasoning.isBlank() -> {
                val messages = current.messages.dropLast(1)
                current.copy(messages = if (command.alternativeTarget == null) messages else messages + command.alternativeTarget)
            }
            last?.role == "assistant" -> current.copy(
                messages = current.messages.dropLast(1) + assistant.transformGeneratedMessage(last.completeReasoningDuration())
                    .completeAlternative()
            )
            else -> current
        }.copy(updatedAt = System.currentTimeMillis())
    }

    private fun fail(conversationId: String, data: DesktopData, textKey: String): ConversationExecutionResult {
        reportError(conversationId, desktopText(data.preferences.language, textKey))
        return ConversationExecutionResult(completed = false)
    }

    private fun conversation(id: String): DesktopConversation? = currentData().conversations.firstOrNull { it.id == id }

    private fun updateConversation(id: String, transform: (DesktopConversation) -> DesktopConversation) {
        val data = currentData()
        updateData(data.copy(conversations = data.conversations.map { if (it.id == id) transform(it) else it }))
    }

    private data class PreparedRequest(val config: DesktopConfig, val messages: List<ChatMessage>)
}

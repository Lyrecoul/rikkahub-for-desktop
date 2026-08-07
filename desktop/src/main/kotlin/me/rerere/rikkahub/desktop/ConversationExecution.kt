package me.rerere.rikkahub.desktop

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

private const val StreamUpdateIntervalMillis = 50L
private const val LongStreamUpdateIntervalMillis = 200L
private const val LongStreamOutputThreshold = 6_000

internal data class ConversationExecutionRequest(
    val conversationId: String,
    val requestMessages: List<ChatMessage>,
    val title: String? = null,
    val alternativeTarget: ChatMessage? = null
)

internal sealed interface ConversationExecutionOutcome {
    data object Completed : ConversationExecutionOutcome
    data object Cancelled : ConversationExecutionOutcome
    data class Stopped(val reason: ConversationExecutionStopReason) : ConversationExecutionOutcome
    data class Failed(val reason: ConversationExecutionFailure) : ConversationExecutionOutcome
}

internal enum class ConversationExecutionStopReason { TOOL_ROUND_LIMIT, APPROVAL_DENIED }

internal sealed interface ConversationExecutionFailure {
    data object InvalidMcpConfiguration : ConversationExecutionFailure
    data class McpSynchronization(val detail: String) : ConversationExecutionFailure
    data class InvalidRequest(val detail: String) : ConversationExecutionFailure
    data class Execution(val detail: String) : ConversationExecutionFailure
}

internal interface ConversationModelStreamAdapter {
    fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta>
}

internal interface ConversationToolRuntimeAdapter {
    fun toolsAreCurrent(server: DesktopMcpServer): Boolean
    suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool>
    suspend fun execute(config: DesktopConfig, calls: List<DesktopToolCall>): List<ChatMessage>
}

internal interface ConversationUserInteractionAdapter {
    suspend fun askUser(call: DesktopToolCall): String
    suspend fun requestApproval(call: DesktopToolCall, request: DesktopAgentApprovalRequest): Boolean
}

internal interface ConversationExecutionState {
    fun current(): DesktopData
    fun update(transform: (DesktopData) -> DesktopData)
}

internal interface ConversationExecutionText {
    fun noMcpTools(server: DesktopMcpServer): String
    fun invalidMessageTemplate(): String
    fun toolRoundLimit(limit: Int): String
}

internal class DesktopConversationModelStreamAdapter(private val client: OpenAiClient) : ConversationModelStreamAdapter {
    override fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> = client.stream(config, messages)
}

internal class DesktopConversationToolRuntimeAdapter(
    private val client: OpenAiClient,
    private val mcpClient: DesktopMcpClient,
    private val assistantId: String,
    private val memoryToolHandler: (String) -> DesktopMemoryToolHandler,
    private val interaction: ConversationUserInteractionAdapter,
    private val agentRuntime: DesktopAgentRuntime
) : ConversationToolRuntimeAdapter {
    override fun toolsAreCurrent(server: DesktopMcpServer): Boolean = mcpClient.toolsAreCurrent(server)

    override suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> = mcpClient.syncTools(server)

    override suspend fun execute(config: DesktopConfig, calls: List<DesktopToolCall>): List<ChatMessage> = client.executeToolCalls(
        config,
        calls,
        memoryToolHandler(assistantId),
        mcpClient,
        interaction::askUser,
        agentRuntime,
        interaction::requestApproval
    )
}

internal class ConversationExecution(
    private val model: ConversationModelStreamAdapter,
    private val tools: ConversationToolRuntimeAdapter,
    private val state: ConversationExecutionState,
    private val text: ConversationExecutionText,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun execute(request: ConversationExecutionRequest): ConversationExecutionOutcome {
        val initialData = state.current()
        val conversation = initialData.conversations.firstOrNull { it.id == request.conversationId }
            ?: return ConversationExecutionOutcome.Failed(ConversationExecutionFailure.Execution("Conversation not found"))
        val assistant = initialData.assistantFor(conversation)
        val selectedServers = initialData.mcpServers.filter { it.enabled && it.id in assistant.mcpServerIds }
        if ((assistant.mcpServerIds - initialData.mcpServers.map(DesktopMcpServer::id).toSet()).isNotEmpty()) {
            return ConversationExecutionOutcome.Failed(ConversationExecutionFailure.InvalidMcpConfiguration)
        }
        try {
            try {
                syncTools(selectedServers)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                return ConversationExecutionOutcome.Failed(
                    ConversationExecutionFailure.McpSynchronization(error.executionDetail())
                )
            }
            val initialRequest = prepareRequest(request.conversationId, request.requestMessages, assistant)
                .getOrElse { error ->
                    return ConversationExecutionOutcome.Failed(
                        ConversationExecutionFailure.InvalidRequest(error.message.orEmpty())
                    )
                }
            updateConversation(request.conversationId) {
                it.prepareGeneration(
                    requestMessages = request.requestMessages,
                    alternativeTarget = request.alternativeTarget,
                    title = request.title,
                    modelId = initialRequest.config.model
                )
            }
            var modelRequest = initialRequest.messages
            var toolRounds = 0
            var stopReason: ConversationExecutionStopReason? = null
            while (true) {
                collectStream(request.conversationId, initialRequest.config, modelRequest)
                val toolCalls = conversation(request.conversationId)?.messages?.lastOrNull()?.toolCalls.orEmpty()
                if (toolCalls.isEmpty()) break
                finishReasoning(request.conversationId)
                if (assistant.maxToolRounds > 0 && toolRounds >= assistant.maxToolRounds) {
                    val limit = text.toolRoundLimit(assistant.maxToolRounds)
                    updateConversation(request.conversationId) { current ->
                        current.copy(
                            messages = current.messages + toolCalls.map { call ->
                                ChatMessage(role = "tool", content = limit, toolCallId = call.id)
                            } + ChatMessage(role = "assistant", content = limit),
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    stopReason = ConversationExecutionStopReason.TOOL_ROUND_LIMIT
                    break
                }
                toolRounds++
                val toolResults = tools.execute(initialRequest.config, toolCalls)
                updateConversation(request.conversationId) { current ->
                    current.copy(messages = current.messages + toolResults, updatedAt = System.currentTimeMillis())
                }
                if (toolResults.any { it.content == DesktopAgentApprovalDeniedResult }) {
                    stopReason = ConversationExecutionStopReason.APPROVAL_DENIED
                    break
                }
                val nextRequest = prepareRequest(
                    request.conversationId,
                    requireNotNull(conversation(request.conversationId)).messages,
                    assistant
                ).getOrElse { error ->
                    return ConversationExecutionOutcome.Failed(
                        ConversationExecutionFailure.InvalidRequest(error.message.orEmpty())
                    )
                }
                modelRequest = nextRequest.messages
                updateConversation(request.conversationId) { current ->
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
            complete(request.conversationId, assistant)
            return stopReason?.let(ConversationExecutionOutcome::Stopped) ?: ConversationExecutionOutcome.Completed
        } catch (_: CancellationException) {
            cancel(request, assistant)
            return ConversationExecutionOutcome.Cancelled
        } catch (error: Throwable) {
            cancel(request, assistant)
            return ConversationExecutionOutcome.Failed(ConversationExecutionFailure.Execution(error.executionDetail()))
        }
    }

    private suspend fun syncTools(servers: List<DesktopMcpServer>) {
        val stale = servers.filterNot(tools::toolsAreCurrent)
        if (stale.isEmpty()) return
        val toolsByServer = stale.associate { server ->
            server.id to tools.syncTools(server).also { synchronizedTools ->
                check(synchronizedTools.isNotEmpty()) { text.noMcpTools(server) }
            }
        }
        state.update { data ->
            data.copy(mcpServers = data.mcpServers.map { server ->
                toolsByServer[server.id]?.let(server::withSyncedTools) ?: server
            })
        }
    }

    private fun prepareRequest(
        conversationId: String,
        requestMessages: List<ChatMessage>,
        assistant: DesktopAssistantProfile
    ): Result<PreparedRequest> {
        val conversation = conversation(conversationId)
            ?: return Result.failure(ConversationExecutionPreparationException("Conversation not found"))
        val requestAssistant = if (conversation.usesPromptInjections(assistant)) assistant
        else assistant.copy(promptInjections = emptyList())
        val baseConfig = state.current().configForConversation(conversation)
        val injected = requestAssistant.injectPromptMessages(requestAssistant.limitContext(requestMessages))
        val config = baseConfig.copy(
            systemPrompt = (injected.systemPrefix + baseConfig.systemPrompt + injected.systemSuffix)
                .filter(String::isNotBlank)
                .joinToString("\n\n")
        )
        config.validateAttachments(injected.messages).takeIf { it.isNotEmpty() }?.let { issues ->
            return Result.failure(ConversationExecutionPreparationException(issues.toUserMessage()))
        }
        val messages = runCatching {
            requestAssistant.renderMessageTemplate(requestAssistant.transformRequestMessages(injected.messages))
        }.getOrElse { error ->
            return Result.failure(ConversationExecutionPreparationException(error.message ?: text.invalidMessageTemplate()))
        }
        return Result.success(PreparedRequest(config, messages))
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
                    content = content.toString(), reasoning = reasoning.toString(), reasoningSignature = reasoningSignature.toString(),
                    modelId = modelId, promptTokens = promptTokens, completionTokens = completionTokens, cachedTokens = cachedTokens,
                    citations = citations.toList(), attachments = attachments.toList(), toolCallDeltas = toolCalls.toList()
                )
            )
            content.clear(); reasoning.clear(); reasoningSignature.clear(); citations.clear(); attachments.clear(); toolCalls.clear()
            modelId = null; promptTokens = null; completionTokens = null; cachedTokens = null
        }

        try {
            model.stream(config, request).collect { delta ->
                content.append(delta.content); reasoning.append(delta.reasoning); reasoningSignature.append(delta.reasoningSignature)
                citations += delta.citations; attachments += delta.attachments; toolCalls += delta.toolCallDeltas
                modelId = delta.modelId ?: modelId; promptTokens = delta.promptTokens ?: promptTokens
                completionTokens = delta.completionTokens ?: completionTokens; cachedTokens = delta.cachedTokens ?: cachedTokens
                outputLength += delta.content.length + delta.reasoning.length
                val interval = if (outputLength >= LongStreamOutputThreshold) LongStreamUpdateIntervalMillis else StreamUpdateIntervalMillis
                val now = clock()
                if (now - lastUpdateAt >= interval) { flush(); lastUpdateAt = now }
            }
        } finally {
            flush()
        }
    }

    private fun appendDelta(conversationId: String, delta: StreamDelta) = updateConversation(conversationId) { current ->
        val messages = current.messages.toMutableList()
        val last = messages.lastOrNull()
        if (last?.role == "assistant") {
            val receivedAt = System.currentTimeMillis()
            val reasoningStartedAt = last.reasoningStartedAt ?: delta.reasoning.takeIf(String::isNotBlank)?.let { receivedAt }
            messages[messages.lastIndex] = last.copy(
                content = last.content + delta.content, reasoning = last.reasoning + delta.reasoning,
                reasoningSignature = last.reasoningSignature + delta.reasoningSignature, reasoningStartedAt = reasoningStartedAt,
                reasoningDurationMillis = if (delta.reasoning.isNotBlank() && reasoningStartedAt != null) {
                    (receivedAt - reasoningStartedAt).coerceAtLeast(0)
                } else last.reasoningDurationMillis,
                modelId = delta.modelId ?: last.modelId, promptTokens = delta.promptTokens ?: last.promptTokens,
                completionTokens = delta.completionTokens ?: last.completionTokens, cachedTokens = delta.cachedTokens ?: last.cachedTokens,
                citations = (last.citations + delta.citations).distinctBy(DesktopCitation::url),
                attachments = (last.attachments + delta.attachments).distinctBy(DesktopAttachment::data),
                toolCalls = last.toolCalls.merge(delta.toolCallDeltas)
            )
        }
        current.copy(messages = messages, updatedAt = System.currentTimeMillis())
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
        if (last?.role == "assistant") messages[messages.lastIndex] = assistant.transformGeneratedMessage(last.completeReasoningDuration()).completeAlternative()
        current.copy(messages = messages, updatedAt = System.currentTimeMillis())
    }

    private fun cancel(request: ConversationExecutionRequest, assistant: DesktopAssistantProfile) = updateConversation(request.conversationId) { current ->
        val last = current.messages.lastOrNull()
        when {
            last?.role == "assistant" && last.toolCalls.isNotEmpty() -> {
                val cleaned = last.copy(toolCalls = emptyList())
                if (cleaned.content.isBlank() && cleaned.reasoning.isBlank()) {
                    val messages = current.messages.dropLast(1)
                    current.copy(messages = if (request.alternativeTarget == null) messages else messages + request.alternativeTarget)
                } else current.copy(messages = current.messages.dropLast(1) + assistant.transformGeneratedMessage(cleaned.completeReasoningDuration()).completeAlternative())
            }
            last?.role == "assistant" && last.content.isBlank() && last.reasoning.isBlank() -> {
                val messages = current.messages.dropLast(1)
                current.copy(messages = if (request.alternativeTarget == null) messages else messages + request.alternativeTarget)
            }
            last?.role == "assistant" -> current.copy(
                messages = current.messages.dropLast(1) + assistant.transformGeneratedMessage(last.completeReasoningDuration()).completeAlternative()
            )
            else -> current
        }.copy(updatedAt = System.currentTimeMillis())
    }

    private fun conversation(id: String): DesktopConversation? = state.current().conversations.firstOrNull { it.id == id }

    private fun updateConversation(id: String, transform: (DesktopConversation) -> DesktopConversation) {
        state.update { data -> data.copy(conversations = data.conversations.map { if (it.id == id) transform(it) else it }) }
    }

    private data class PreparedRequest(val config: DesktopConfig, val messages: List<ChatMessage>)

    private class ConversationExecutionPreparationException(message: String) : IllegalArgumentException(message)
}

private fun Throwable.executionDetail(): String =
    message?.substringAfterLast(": ")?.takeIf(String::isNotBlank) ?: userFacingMessage()

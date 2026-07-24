package me.rerere.rikkahub.desktop

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class DesktopConfig(
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4.1-mini",
    val systemPrompt: String = "You are a helpful assistant.",
    val temperature: Double = 1.0,
    val topP: Double = 1.0,
    val reasoningEffort: String = "",
    val maxTokens: Int = 0,
    val requestTokenUsage: Boolean = false,
    val webSearchEnabled: Boolean = false,
    val streamOutput: Boolean = true,
    val customHeaders: List<DesktopCustomHeader> = emptyList(),
    val customBodies: List<DesktopCustomBody> = emptyList(),
    val webSearchSettings: DesktopWebSearchSettings = DesktopWebSearchSettings(),
    val localTools: Set<DesktopLocalTool> = emptySet(),
    val balanceOptions: DesktopBalanceOptions = DesktopBalanceOptions()
)

@Serializable
enum class DesktopLocalTool {
    CURRENT_TIME
}

@Serializable
enum class DesktopSearchProviderType {
    SEARXNG,
    BRAVE
}

@Serializable
data class DesktopWebSearchSettings(
    /** SearXNG instance URL. Blank keeps the provider's native web-search protocol. */
    val providerType: DesktopSearchProviderType = DesktopSearchProviderType.SEARXNG,
    val searxngUrl: String = "",
    val apiKey: String = "",
    val resultCount: Int = 5
) {
    val isConfigured: Boolean get() = when (providerType) {
        DesktopSearchProviderType.SEARXNG -> searxngUrl.isNotBlank()
        DesktopSearchProviderType.BRAVE -> apiKey.isNotBlank()
    }
}

@Serializable
data class DesktopBalanceOptions(
    val enabled: Boolean = false,
    val apiPath: String = "/credits",
    val resultPath: String = ""
)

@Serializable
data class DesktopProviderProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "OpenAI 兼容服务",
    val config: DesktopConfig = DesktopConfig(),
    val discoveredModels: List<String> = emptyList()
)

@Serializable
data class DesktopQuickMessage(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = ""
)

@Serializable
data class DesktopPresetMessage(
    val role: String = "assistant",
    val content: String = ""
)

@Serializable
data class DesktopRegexRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val enabled: Boolean = true,
    val findRegex: String = "",
    val replaceString: String = "",
    val roles: Set<String> = emptySet(),
    val visualOnly: Boolean = false
)

@Serializable
data class DesktopCustomHeader(
    val name: String = "",
    val value: String = ""
)

@Serializable
data class DesktopCustomBody(
    val key: String = "",
    val value: String = "null"
)

@Serializable
data class DesktopMemory(
    val id: String = UUID.randomUUID().toString(),
    val content: String = ""
)

@Serializable
data class DesktopAssistantProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "默认助手",
    val systemPrompt: String = "",
    val providerId: String = "",
    val model: String = "",
    val temperature: Double? = null,
    val topP: Double? = null,
    val reasoningEffort: String = "",
    val maxTokens: Int? = null,
    val contextMessageSize: Int = 0,
    val quickMessages: List<DesktopQuickMessage> = emptyList(),
    val allowConversationSystemPrompt: Boolean = false,
    val messageTemplate: String = "{{ message }}",
    val presetMessages: List<DesktopPresetMessage> = emptyList(),
    val regexRules: List<DesktopRegexRule> = emptyList(),
    val customHeaders: List<DesktopCustomHeader> = emptyList(),
    val customBodies: List<DesktopCustomBody> = emptyList(),
    val enableWebSearch: Boolean = false,
    val localTools: Set<DesktopLocalTool> = emptySet(),
    val streamOutput: Boolean = true,
    val enableMemory: Boolean = false,
    val memories: List<DesktopMemory> = emptyList()
)

@Serializable
enum class DesktopColorMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
data class DesktopPreferences(
    val colorMode: DesktopColorMode = DesktopColorMode.SYSTEM,
    val fontScale: Float = 1.0f,
    val showUserAvatar: Boolean = true,
    val showModelIcon: Boolean = true,
    val showModelName: Boolean = true,
    val showAssistantBubble: Boolean = false,
    val showMessageTimestamp: Boolean = false,
    val showReasoning: Boolean = true,
    val autoCollapseReasoning: Boolean = true,
    val codeBlockAutoWrap: Boolean = false,
    val sendOnEnter: Boolean = true,
    val enableAutoScroll: Boolean = true
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    val reasoning: String = "",
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val variants: List<DesktopMessageVariant> = emptyList(),
    val selectedVariantIndex: Int = 0,
    val attachments: List<DesktopAttachment> = emptyList(),
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val citations: List<DesktopCitation> = emptyList(),
    val toolCalls: List<DesktopToolCall> = emptyList(),
    val toolCallId: String? = null
)

@Serializable
data class DesktopToolCall(
    val id: String,
    val name: String,
    val arguments: String = "{}"
)

@Serializable
data class DesktopToolCallDelta(
    val index: Int,
    val id: String? = null,
    val name: String? = null,
    val arguments: String = ""
)

@Serializable
data class DesktopCitation(
    val url: String,
    val title: String = ""
)

@Serializable
data class DesktopAttachment(
    val name: String,
    val mimeType: String,
    val data: String,
    val isImage: Boolean = false
)

@Serializable
data class DesktopMessageVariant(
    val content: String,
    val reasoning: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val citations: List<DesktopCitation> = emptyList(),
    val toolCalls: List<DesktopToolCall> = emptyList()
)

fun ChatMessage.availableVariants(): List<DesktopMessageVariant> = variants.ifEmpty {
    listOf(
        DesktopMessageVariant(
            content = content,
            reasoning = reasoning,
            createdAt = createdAt,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            citations = citations,
            toolCalls = toolCalls
        )
    )
}

fun ChatMessage.beginAlternative(): ChatMessage = copy(
    content = "",
    reasoning = "",
    promptTokens = null,
    completionTokens = null,
    citations = emptyList(),
    toolCalls = emptyList(),
    toolCallId = null,
    createdAt = System.currentTimeMillis(),
    variants = availableVariants(),
    selectedVariantIndex = availableVariants().size
)

fun ChatMessage.completeAlternative(): ChatMessage {
    val completed = DesktopMessageVariant(
        content = content,
        reasoning = reasoning,
        createdAt = createdAt,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        citations = citations,
        toolCalls = toolCalls
    )
    val completedVariants = if (selectedVariantIndex < variants.size) {
        variants.mapIndexed { index, variant -> if (index == selectedVariantIndex) completed else variant }
    } else {
        variants + completed
    }
    return copy(variants = completedVariants, selectedVariantIndex = completedVariants.lastIndex)
}

fun ChatMessage.selectVariant(index: Int): ChatMessage {
    val choices = availableVariants()
    val selected = choices.getOrNull(index) ?: return this
    return copy(
        content = selected.content,
        reasoning = selected.reasoning,
        createdAt = selected.createdAt,
        promptTokens = selected.promptTokens,
        completionTokens = selected.completionTokens,
        citations = selected.citations,
        toolCalls = selected.toolCalls,
        variants = choices,
        selectedVariantIndex = index
    )
}

@Serializable
data class DesktopConversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val assistantId: String = "",
    val systemPrompt: String = "",
    val webSearchEnabled: Boolean? = null,
    val messages: List<ChatMessage> = emptyList(),
    /** Previous paths preserved when a message is edited or regenerated from history. */
    val branches: List<DesktopConversationBranch> = emptyList(),
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

@Serializable
data class DesktopConversationBranch(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "分支",
    val messages: List<ChatMessage>,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Preserves the visible path before replacing its tail. The active path intentionally remains
 * in [DesktopConversation.messages] so old desktop backups need no migration.
 */
fun DesktopConversation.fork(messages: List<ChatMessage>, name: String): DesktopConversation = copy(
    messages = messages,
    branches = branches + DesktopConversationBranch(
        name = name,
        messages = this.messages,
        createdAt = System.currentTimeMillis()
    ),
    updatedAt = System.currentTimeMillis()
)

fun DesktopConversation.restoreBranch(branchId: String): DesktopConversation {
    val branch = branches.firstOrNull { it.id == branchId } ?: return this
    val current = DesktopConversationBranch(name = "当前路径", messages = messages)
    return copy(
        messages = branch.messages,
        branches = branches.filterNot { it.id == branchId } + current,
        updatedAt = System.currentTimeMillis()
    )
}

fun DesktopConversation.deleteBranch(branchId: String): DesktopConversation =
    copy(branches = branches.filterNot { it.id == branchId }, updatedAt = System.currentTimeMillis())

@Serializable
data class DesktopData(
    val schemaVersion: Int = 1,
    val config: DesktopConfig = DesktopConfig(),
    val preferences: DesktopPreferences = DesktopPreferences(),
    val providers: List<DesktopProviderProfile> = listOf(DesktopProviderProfile(config = config)),
    val selectedProviderId: String = "",
    val assistants: List<DesktopAssistantProfile> = listOf(DesktopAssistantProfile()),
    val selectedAssistantId: String = "",
    val webSearchSettings: DesktopWebSearchSettings = DesktopWebSearchSettings(),
    val conversations: List<DesktopConversation> = listOf(DesktopConversation()),
    val selectedConversationId: String = conversations.first().id
)

fun DesktopData.activeProvider(): DesktopProviderProfile =
    providers.firstOrNull { it.id == selectedProviderId }
        ?: providers.firstOrNull()
        ?: DesktopProviderProfile(id = "legacy", config = config)

fun DesktopData.activeAssistant(): DesktopAssistantProfile =
    assistants.firstOrNull { it.id == selectedAssistantId }
        ?: assistants.firstOrNull()
        ?: DesktopAssistantProfile(id = "default")

fun DesktopData.assistantFor(conversation: DesktopConversation): DesktopAssistantProfile =
    assistants.firstOrNull { it.id == conversation.assistantId } ?: activeAssistant()

fun DesktopData.filteredConversations(query: String, assistantId: String?): List<DesktopConversation> =
    conversations.filter { conversation ->
        val matchesAssistant = assistantId == null || assistantFor(conversation).id == assistantId
        val matchesQuery = query.isBlank() || conversation.title.contains(query, ignoreCase = true) ||
            conversation.messages.any { message ->
                message.content.contains(query, ignoreCase = true) ||
                    message.reasoning.contains(query, ignoreCase = true) ||
                    message.attachments.any { it.name.contains(query, ignoreCase = true) }
            }
        matchesAssistant && matchesQuery
    }.sortedWith(
        compareByDescending<DesktopConversation> { it.isPinned }.thenByDescending { it.updatedAt }
    )

fun DesktopData.configForAssistant(assistant: DesktopAssistantProfile): DesktopConfig {
    val provider = providers.firstOrNull { it.id == assistant.providerId } ?: activeProvider()
    val baseSystemPrompt = assistant.systemPrompt.ifBlank { provider.config.systemPrompt }
    val memoryPrompt = assistant.memoryPrompt()
    return provider.config.copy(
        model = assistant.model.ifBlank { provider.config.model },
        systemPrompt = listOf(baseSystemPrompt, memoryPrompt).filter { it.isNotBlank() }.joinToString("\n\n"),
        temperature = assistant.temperature ?: provider.config.temperature,
        topP = assistant.topP ?: provider.config.topP,
        reasoningEffort = assistant.reasoningEffort.ifBlank { provider.config.reasoningEffort },
        maxTokens = assistant.maxTokens ?: provider.config.maxTokens,
        webSearchEnabled = assistant.enableWebSearch,
        localTools = assistant.localTools,
        streamOutput = assistant.streamOutput,
        customHeaders = assistant.customHeaders + provider.config.customHeaders,
        customBodies = assistant.customBodies + provider.config.customBodies,
        webSearchSettings = webSearchSettings
    )
}

fun DesktopData.configForConversation(conversation: DesktopConversation): DesktopConfig {
    val assistant = assistantFor(conversation)
    val config = configForAssistant(assistant)
    val conversationConfig = config.copy(
        webSearchEnabled = conversation.webSearchEnabled ?: assistant.enableWebSearch
    )
    return if (assistant.allowConversationSystemPrompt && conversation.systemPrompt.isNotBlank()) {
        conversationConfig.copy(
            systemPrompt = listOf(conversation.systemPrompt, assistant.memoryPrompt())
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
        )
    } else {
        conversationConfig
    }
}

private fun DesktopAssistantProfile.memoryPrompt(): String = if (enableMemory) {
    memories.map { it.content.trim() }.filter { it.isNotBlank() }
        .joinToString(separator = "\n- ", prefix = "Memory:\n- ")
} else {
    ""
}

fun DesktopAssistantProfile.limitContext(messages: List<ChatMessage>): List<ChatMessage> =
    if (contextMessageSize > 0) messages.takeLast(contextMessageSize) else messages

fun DesktopAssistantProfile.newConversation(): DesktopConversation = DesktopConversation(
    assistantId = id,
    messages = presetMessages.map { preset ->
        ChatMessage(role = preset.role, content = preset.content)
    }
)

fun DesktopAssistantProfile.applyRegexRules(content: String, role: String, visualOnly: Boolean): String =
    regexRules.fold(content) { transformed, rule ->
        if (!rule.enabled || rule.visualOnly != visualOnly || role !in rule.roles || rule.findRegex.isBlank()) {
            transformed
        } else {
            runCatching { transformed.replace(Regex(rule.findRegex), rule.replaceString) }
                .getOrDefault(transformed)
        }
    }

fun DesktopAssistantProfile.transformRequestMessages(messages: List<ChatMessage>): List<ChatMessage> =
    messages.map { message ->
        if (message.role == "user") {
            message.copy(content = applyRegexRules(message.content, message.role, visualOnly = false))
        } else {
            message
        }
    }

fun DesktopAssistantProfile.transformGeneratedMessage(message: ChatMessage): ChatMessage =
    message.copy(content = applyRegexRules(message.content, "assistant", visualOnly = false))

fun DesktopData.selectProviderConfig(providerId: String, model: String? = null): DesktopData {
    val currentProviders = providers.ifEmpty { listOf(activeProvider()) }
    val selectedProvider = currentProviders.firstOrNull { it.id == providerId } ?: return this
    val updatedProvider = if (model == null) {
        selectedProvider
    } else {
        selectedProvider.copy(config = selectedProvider.config.copy(model = model))
    }
    return copy(
        config = updatedProvider.config,
        providers = currentProviders.map { if (it.id == providerId) updatedProvider else it },
        selectedProviderId = providerId
    )
}

fun DesktopData.saveProviderProfile(profile: DesktopProviderProfile): DesktopData {
    val currentProviders = providers.ifEmpty { listOf(activeProvider()) }
    val activeId = activeProvider().id
    if (currentProviders.none { it.id == profile.id }) return this
    return copy(
        config = if (profile.id == activeId) profile.config else config,
        providers = currentProviders.map { if (it.id == profile.id) profile else it },
        selectedProviderId = selectedProviderId.ifBlank { activeId }
    )
}

fun DesktopData.deleteProviderProfile(providerId: String): DesktopData {
    val currentProviders = providers.ifEmpty { listOf(activeProvider()) }
    if (currentProviders.size <= 1) return this
    val remaining = currentProviders.filterNot { it.id == providerId }
    if (remaining.size == currentProviders.size) return this
    val active = if (activeProvider().id == providerId) remaining.first() else activeProvider()
    return copy(
        config = active.config,
        providers = remaining,
        selectedProviderId = active.id,
        assistants = assistants.map { assistant ->
            if (assistant.providerId == providerId) assistant.copy(providerId = active.id) else assistant
        }
    )
}

fun DesktopData.saveAssistantProfile(profile: DesktopAssistantProfile): DesktopData {
    if (assistants.none { it.id == profile.id }) return this
    return copy(
        assistants = assistants.map { if (it.id == profile.id) profile else it },
        selectedAssistantId = selectedAssistantId.ifBlank { activeAssistant().id }
    )
}

fun DesktopData.deleteAssistantProfile(assistantId: String): DesktopData {
    if (assistants.size <= 1) return this
    val remaining = assistants.filterNot { it.id == assistantId }
    if (remaining.size == assistants.size) return this
    val fallback = if (activeAssistant().id == assistantId) remaining.first() else activeAssistant()
    return copy(
        assistants = remaining,
        selectedAssistantId = fallback.id,
        conversations = conversations.map { conversation ->
            if (conversation.assistantId == assistantId) {
                conversation.copy(assistantId = fallback.id)
            } else {
                conversation
            }
        }
    )
}

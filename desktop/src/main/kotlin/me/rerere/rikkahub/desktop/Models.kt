package me.rerere.rikkahub.desktop

import kotlinx.serialization.Serializable
import dev.darkokoa.pangu.spacingText
import java.net.URI
import java.util.Locale
import java.util.UUID

internal const val DefaultDesktopTitlePrompt = """
    I will give you some dialogue content in the `<content>` block.
    You need to summarize the conversation between user and assistant into a short title.
    1. The title language should be consistent with the user's primary language
    2. Do not use punctuation or other special symbols
    3. Reply directly with the title
    4. Summarize using {locale} language
    5. The title should not exceed 10 characters

    <content>
    {content}
    </content>
"""

@Serializable
data class DesktopConfig(
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4.1-mini",
    val titleModel: String = "",
    val titlePrompt: String = DefaultDesktopTitlePrompt,
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
    val memoryEnabled: Boolean = false,
    val mcpServers: List<DesktopMcpServer> = emptyList(),
    val agent: DesktopAgentConfig? = null,
    val balanceOptions: DesktopBalanceOptions = DesktopBalanceOptions()
)

@Serializable
enum class DesktopAgentBackend { LOCAL, DOCKER }

@Serializable
data class DesktopAgentWorkspace(
    val rootPath: String = "",
    val backend: DesktopAgentBackend = DesktopAgentBackend.DOCKER,
    val dockerImage: String = "ubuntu:24.04"
)

@Serializable
data class DesktopAgentConfig(
    val workspace: DesktopAgentWorkspace,
    val enabledSkillNames: Set<String> = emptySet()
)

@Serializable
enum class DesktopLocalTool {
    CURRENT_TIME,
    ASK_USER,
    MEMORY
}

@Serializable
enum class DesktopSearchProviderType {
    SEARXNG,
    BRAVE,
    ZHIPU,
    TAVILY,
    EXA,
    FIRECRAWL,
    JINA,
    BOCHA,
    PERPLEXITY,
    SERPER,
    OLLAMA,
    METASO,
    LINKUP,
    RIKKAHUB
}

@Serializable
data class DesktopWebSearchSettings(
    /** SearXNG instance URL. Blank keeps the provider's native web-search protocol. */
    val providerType: DesktopSearchProviderType = DesktopSearchProviderType.SEARXNG,
    val searxngUrl: String = "",
    val apiKey: String = "",
    /** Keys are only held in memory; DesktopStore persists them in the system secret store. */
    val apiKeys: Map<DesktopSearchProviderType, String> = emptyMap(),
    val resultCount: Int = 5
) {
    val isConfigured: Boolean get() = when (providerType) {
        DesktopSearchProviderType.SEARXNG -> searxngUrl.isNotBlank()
        else -> apiKey.isNotBlank()
    }
}

internal fun DesktopWebSearchSettings.selectProvider(
    provider: DesktopSearchProviderType
): DesktopWebSearchSettings {
    val keys = apiKeys + (providerType to apiKey)
    return copy(providerType = provider, apiKey = keys[provider].orEmpty(), apiKeys = keys)
}

internal fun DesktopWebSearchSettings.withApiKey(value: String): DesktopWebSearchSettings =
    copy(apiKey = value, apiKeys = apiKeys + (providerType to value))

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
enum class DesktopInjectionPosition {
    BEFORE_SYSTEM_PROMPT,
    AFTER_SYSTEM_PROMPT,
    TOP_OF_CHAT,
    BOTTOM_OF_CHAT,
    AT_DEPTH
}

@Serializable
data class DesktopPromptInjection(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val enabled: Boolean = true,
    val priority: Int = 0,
    val position: DesktopInjectionPosition = DesktopInjectionPosition.AFTER_SYSTEM_PROMPT,
    val content: String = "",
    val role: String = "system",
    val injectDepth: Int = 4,
    val keywords: List<String> = emptyList(),
    val useRegex: Boolean = false,
    val caseSensitive: Boolean = false,
    val scanDepth: Int = 4,
    val constantActive: Boolean = false
)

internal data class DesktopInjectedRequest(
    val systemPrefix: List<String> = emptyList(),
    val systemSuffix: List<String> = emptyList(),
    val messages: List<ChatMessage>
)

@Serializable
data class DesktopAssistantProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "默认助手",
    val tags: Set<String> = emptySet(),
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
    val allowConversationPromptInjection: Boolean = false,
    val messageTemplate: String = "{{ message }}",
    val presetMessages: List<DesktopPresetMessage> = emptyList(),
    val regexRules: List<DesktopRegexRule> = emptyList(),
    val customHeaders: List<DesktopCustomHeader> = emptyList(),
    val customBodies: List<DesktopCustomBody> = emptyList(),
    val enableWebSearch: Boolean = false,
    val localTools: Set<DesktopLocalTool> = emptySet(),
    /** Maximum tool-call rounds per response. Zero disables the limit. */
    val maxToolRounds: Int = 8,
    val streamOutput: Boolean = true,
    val enableMemory: Boolean = false,
    val mcpServerIds: Set<String> = emptySet(),
    val useGlobalMemory: Boolean = false,
    val memories: List<DesktopMemory> = emptyList(),
    val promptInjections: List<DesktopPromptInjection> = emptyList(),
    val agentWorkspace: DesktopAgentWorkspace? = null,
    val enabledSkillNames: Set<String> = emptySet()
)

@Serializable
enum class DesktopColorMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
enum class DesktopThemeColor {
    SAKURA,
    OCEAN,
    FOREST,
    SUNSET,
    LAVENDER,
    SLATE
}

@Serializable
enum class DesktopFontFamily {
    SYSTEM,
    SANS_SERIF,
    SERIF,
    MONOSPACE
}

@Serializable
enum class DesktopLanguage {
    SYSTEM,
    ENGLISH,
    CHINESE_SIMPLIFIED,
    CHINESE_TRADITIONAL,
    JAPANESE,
    KOREAN,
    RUSSIAN,
    SPANISH,
    FRENCH,
    GERMAN,
    PORTUGUESE_BRAZIL
}

@Serializable
enum class DesktopConversationSort {
    RECENT,
    MOST_ACTIVE
}

@Serializable
data class DesktopPreferences(
    val colorMode: DesktopColorMode = DesktopColorMode.SYSTEM,
    val themeColor: DesktopThemeColor = DesktopThemeColor.SAKURA,
    val fontFamily: DesktopFontFamily = DesktopFontFamily.SYSTEM,
    val language: DesktopLanguage = DesktopLanguage.SYSTEM,
    val fontScale: Float = 1.0f,
    val showUserAvatar: Boolean = true,
    val userNickname: String = "",
    val showModelIcon: Boolean = true,
    val showModelName: Boolean = true,
    val showAssistantBubble: Boolean = false,
    val showMessageTimestamp: Boolean = false,
    val showReasoning: Boolean = true,
    val autoCollapseReasoning: Boolean = true,
    val codeBlockAutoWrap: Boolean = false,
    val enableChineseTypography: Boolean = false,
    val sendOnEnter: Boolean = true,
    val enableAutoScroll: Boolean = true,
    val enableSmoothScroll: Boolean = true,
    val showMessageJumper: Boolean = true,
    val messageJumperOnLeft: Boolean = false,
    val conversationSort: DesktopConversationSort = DesktopConversationSort.RECENT
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    val reasoning: String = "",
    val reasoningStartedAt: Long? = null,
    val reasoningDurationMillis: Long? = null,
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val variants: List<DesktopMessageVariant> = emptyList(),
    val selectedVariantIndex: Int = 0,
    val attachments: List<DesktopAttachment> = emptyList(),
    val modelId: String? = null,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val cachedTokens: Int? = null,
    val citations: List<DesktopCitation> = emptyList(),
    val toolCalls: List<DesktopToolCall> = emptyList(),
    val toolCallId: String? = null,
    val isFavorite: Boolean = false,
    val translation: String = "",
    val translationTargetLanguage: String = ""
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
enum class DesktopAttachmentKind {
    IMAGE,
    AUDIO,
    FILE
}

@Serializable
data class DesktopAttachment(
    val name: String,
    val mimeType: String,
    val data: String,
    /** Kept for compatibility with existing desktop backups. */
    val isImage: Boolean = false,
    val kind: DesktopAttachmentKind = if (isImage) DesktopAttachmentKind.IMAGE else DesktopAttachmentKind.FILE
)

@Serializable
data class DesktopMessageVariant(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val reasoning: String = "",
    val reasoningStartedAt: Long? = null,
    val reasoningDurationMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modelId: String? = null,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val cachedTokens: Int? = null,
    val citations: List<DesktopCitation> = emptyList(),
    val toolCalls: List<DesktopToolCall> = emptyList(),
    val translation: String = "",
    val translationTargetLanguage: String = ""
)

fun ChatMessage.availableVariants(): List<DesktopMessageVariant> = variants.ifEmpty {
    listOf(
        DesktopMessageVariant(
            id = id,
            content = content,
            reasoning = reasoning,
            reasoningStartedAt = reasoningStartedAt,
            reasoningDurationMillis = reasoningDurationMillis,
            createdAt = createdAt,
            modelId = modelId,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            cachedTokens = cachedTokens,
            citations = citations,
            toolCalls = toolCalls,
            translation = translation,
            translationTargetLanguage = translationTargetLanguage
        )
    )
}

/** Adds a selectable revision while keeping the message node and its shared metadata intact. */
fun ChatMessage.addVariant(content: String): ChatMessage {
    val choices = availableVariants() + DesktopMessageVariant(content = content.trim())
    return copy(
        content = content.trim(),
        reasoning = "",
        promptTokens = null,
        completionTokens = null,
        citations = emptyList(),
        toolCalls = emptyList(),
        translation = "",
        translationTargetLanguage = "",
        variants = choices,
        selectedVariantIndex = choices.lastIndex
    )
}

fun ChatMessage.beginAlternative(): ChatMessage = copy(
    content = "",
    reasoning = "",
    reasoningStartedAt = null,
    reasoningDurationMillis = null,
    modelId = null,
    promptTokens = null,
    completionTokens = null,
    cachedTokens = null,
    citations = emptyList(),
    toolCalls = emptyList(),
    toolCallId = null,
    translation = "",
    translationTargetLanguage = "",
    createdAt = System.currentTimeMillis(),
    variants = availableVariants(),
    selectedVariantIndex = availableVariants().size
)

fun ChatMessage.completeAlternative(): ChatMessage {
    val completed = DesktopMessageVariant(
        content = content,
        reasoning = reasoning,
        reasoningStartedAt = reasoningStartedAt,
        reasoningDurationMillis = reasoningDurationMillis,
        createdAt = createdAt,
        modelId = modelId,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        cachedTokens = cachedTokens,
        citations = citations,
        toolCalls = toolCalls,
        translation = translation,
        translationTargetLanguage = translationTargetLanguage
    )
    val completedVariants = if (selectedVariantIndex < variants.size) {
        variants.mapIndexed { index, variant -> if (index == selectedVariantIndex) completed else variant }
    } else {
        variants + completed
    }
    return copy(variants = completedVariants, selectedVariantIndex = completedVariants.lastIndex)
}

fun ChatMessage.completeReasoningDuration(now: Long = System.currentTimeMillis()): ChatMessage =
    if (reasoning.isBlank() || reasoningDurationMillis != null || reasoningStartedAt == null) this
    else copy(reasoningDurationMillis = (now - reasoningStartedAt).coerceAtLeast(0))

fun ChatMessage.selectVariant(index: Int): ChatMessage {
    val choices = availableVariants()
    val selected = choices.getOrNull(index) ?: return this
    return copy(
        content = selected.content,
        reasoning = selected.reasoning,
        reasoningStartedAt = selected.reasoningStartedAt,
        reasoningDurationMillis = selected.reasoningDurationMillis,
        createdAt = selected.createdAt,
        modelId = selected.modelId,
        promptTokens = selected.promptTokens,
        completionTokens = selected.completionTokens,
        cachedTokens = selected.cachedTokens,
        citations = selected.citations,
        toolCalls = selected.toolCalls,
        translation = selected.translation,
        translationTargetLanguage = selected.translationTargetLanguage,
        variants = choices,
        selectedVariantIndex = index
    )
}

private fun ChatMessage.selectedVariant(): DesktopMessageVariant {
    val choices = availableVariants()
    return choices[selectedVariantIndex.coerceIn(choices.indices)]
}

fun ChatMessage.withTranslation(value: String, language: String): ChatMessage {
    val choices = availableVariants()
    val selectedIndex = selectedVariantIndex.coerceIn(choices.indices)
    val updatedChoices = if (variants.isEmpty()) {
        emptyList()
    } else {
        choices.mapIndexed { index, variant ->
            if (index == selectedIndex) {
                variant.copy(translation = value, translationTargetLanguage = language)
            } else {
                variant
            }
        }
    }
    return copy(
        translation = value,
        translationTargetLanguage = language,
        variants = updatedChoices,
        selectedVariantIndex = selectedIndex
    )
}

@Serializable
data class DesktopConversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val assistantId: String = "",
    val folderId: String? = null,
    /** The conversation from which this one was forked, when it is a conversation branch. */
    val parentConversationId: String? = null,
    val systemPrompt: String = "",
    val webSearchEnabled: Boolean? = null,
    val promptInjectionsEnabled: Boolean? = null,
    val agentWorkspaceOverride: DesktopAgentWorkspace? = null,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val draftAttachments: List<DesktopAttachment> = emptyList(),
    val suggestions: List<String> = emptyList(),
    /** Legacy and history-compression snapshots. New message edits use [ChatMessage.variants]. */
    val branches: List<DesktopConversationBranch> = emptyList(),
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

@Serializable
data class DesktopFolder(
    val id: String = UUID.randomUUID().toString(),
    val assistantId: String,
    val name: String
)

@Serializable
data class DesktopConversationBranch(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "分支",
    val messages: List<ChatMessage>,
    val createdAt: Long = System.currentTimeMillis()
)

internal data class DesktopConversationStats(
    val messageCount: Int,
    val userMessageCount: Int,
    val assistantMessageCount: Int,
    val attachmentCount: Int,
    val characterCount: Int,
    val promptTokens: Int,
    val completionTokens: Int,
    val cachedTokens: Int
)

internal fun DesktopConversation.stats(): DesktopConversationStats = DesktopConversationStats(
    messageCount = messages.size,
    userMessageCount = messages.count { it.role == "user" },
    assistantMessageCount = messages.count { it.role == "assistant" },
    attachmentCount = messages.sumOf { it.attachments.size },
    characterCount = messages.sumOf { it.content.length + it.reasoning.length },
    promptTokens = messages.sumOf { it.promptTokens ?: 0 },
    completionTokens = messages.sumOf { it.completionTokens ?: 0 },
    cachedTokens = messages.sumOf { it.cachedTokens ?: 0 }
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
    val restoredMessages = branch.messages.map { restoredMessage ->
        val currentMessage = messages.firstOrNull { it.id == restoredMessage.id }
        val currentVariants = currentMessage?.takeIf { it.variants.isNotEmpty() }?.availableVariants()
        val historicalVariant = restoredMessage.selectedVariant()
        val selectedVariantIndex = currentVariants?.indexOfFirst { it.id == historicalVariant.id }
            ?.takeIf { it >= 0 }
            ?: currentVariants?.indexOfFirst { it.content == restoredMessage.content }
            ?: -1
        if (currentVariants != null && selectedVariantIndex >= 0) {
            restoredMessage.copy(
                variants = currentVariants,
                selectedVariantIndex = selectedVariantIndex
            )
        } else {
            restoredMessage
        }
    }
    return copy(
        messages = restoredMessages,
        branches = branches.filterNot { it.id == branchId } + current,
        suggestions = emptyList(),
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Restores the matching history snapshot when a user-message revision belongs to an older path.
 * This keeps the reply generated for that revision together with the selected user message.
 */
fun DesktopConversation.selectMessageVariantAt(messageIndex: Int, variantIndex: Int): DesktopConversation {
    val message = messages.getOrNull(messageIndex) ?: return this
    val selectedVariant = message.availableVariants().getOrNull(variantIndex) ?: return this
    val matchingBranch = branches.lastOrNull { branch ->
        branch.messages.firstOrNull { it.id == message.id }?.let { historicalMessage ->
            val historicalVariant = historicalMessage.selectedVariant()
            historicalVariant.id == selectedVariant.id || historicalVariant.content == selectedVariant.content
        } == true
    }
    if (matchingBranch != null) {
        val restored = restoreBranch(matchingBranch.id)
        val restoredIndex = restored.messages.indexOfFirst { it.id == message.id }
        if (restoredIndex < 0) return restored
        val restoredMessage = restored.messages[restoredIndex]
        val restoredVariantIndex = restoredMessage.availableVariants().indexOfFirst { it.id == selectedVariant.id }
            .takeIf { it >= 0 }
            ?: variantIndex
        return restored.copy(
            messages = restored.messages.mapIndexed { index, restoredMessage ->
                if (index == restoredIndex) restoredMessage.selectVariant(restoredVariantIndex) else restoredMessage
            },
            updatedAt = System.currentTimeMillis()
        )
    }

    return copy(
        messages = messages.mapIndexed { index, current ->
            if (index == messageIndex) current.selectVariant(variantIndex) else current
        },
        updatedAt = System.currentTimeMillis()
    )
}

fun DesktopConversation.deleteBranch(branchId: String): DesktopConversation =
    copy(branches = branches.filterNot { it.id == branchId }, updatedAt = System.currentTimeMillis())

/**
 * Editing a historical message creates a new active path. Messages after the edit were generated
 * from the old context, so keeping them visible would make the conversation internally invalid.
 * The old path remains recoverable from the conversation menu.
 */
fun DesktopConversation.editMessageAt(messageIndex: Int, content: String): DesktopConversation {
    require(messageIndex in messages.indices) { "Message index is out of bounds" }
    val editedMessages = messages.take(messageIndex) + messages[messageIndex].addVariant(content)
    val hasDependentMessages = messageIndex < messages.lastIndex
    return copy(
        messages = editedMessages,
        branches = if (hasDependentMessages) {
            branches + DesktopConversationBranch(name = "编辑前历史", messages = messages)
        } else {
            branches
        },
        suggestions = emptyList(),
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Removing a historical message invalidates everything generated after it. Preserve the original
 * path for recovery and keep only the valid prefix in the active conversation.
 */
fun DesktopConversation.deleteMessageAt(messageIndex: Int): DesktopConversation {
    require(messageIndex in messages.indices) { "Message index is out of bounds" }
    val hasDependentMessages = messageIndex < messages.lastIndex
    return copy(
        messages = messages.take(messageIndex),
        branches = if (hasDependentMessages) {
            branches + DesktopConversationBranch(name = "删除前历史", messages = messages)
        } else {
            branches
        },
        suggestions = emptyList(),
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Starts a generated response from a selected point in history. Existing messages that no longer
 * belong to the active context are retained as a recoverable snapshot.
 */
fun DesktopConversation.prepareGeneration(
    requestMessages: List<ChatMessage>,
    alternativeTarget: ChatMessage? = null,
    title: String? = null,
    modelId: String? = null
): DesktopConversation {
    val nextMessages = requestMessages + (alternativeTarget?.beginAlternative()?.copy(modelId = modelId)
        ?: ChatMessage(role = "assistant", content = "", modelId = modelId))
    val preservedMessageCount = requestMessages.size + if (alternativeTarget == null) 0 else 1
    val hasDiscardedMessages = messages.size > preservedMessageCount
    return copy(
        title = title ?: this.title,
        messages = nextMessages,
        branches = if (hasDiscardedMessages) {
            branches + DesktopConversationBranch(name = "重新生成前历史", messages = messages)
        } else {
            branches
        },
        draft = "",
        draftAttachments = emptyList(),
        suggestions = emptyList(),
        updatedAt = System.currentTimeMillis()
    )
}

internal fun List<ChatMessage>.compressionTranscript(): String = joinToString("\n\n") { message ->
    buildString {
        append(message.role.uppercase())
        append(":\n")
        append(message.content)
        if (message.attachments.isNotEmpty()) {
            append("\n[Attachments: ")
            append(message.attachments.joinToString { it.name })
            append(']')
        }
    }
}

internal fun normalizeGeneratedTitle(value: String, enableChineseTypography: Boolean = false): String = value.trim()
    .trim('"', '\'', '`')
    .replace(Regex("\\s+"), " ")
    .let { if (enableChineseTypography) it.spacingText() else it }
    .take(48)

internal fun parseChatSuggestions(value: String, enableChineseTypography: Boolean = false): List<String> = value.lineSequence()
    .map { it.trim().replaceFirst(Regex("^(?:[-*]|\\d+[.)])\\s*"), "") }
    .map { it.trim('"', '\'', '`') }
    .map { if (enableChineseTypography) it.spacingText() else it }
    .filter { it.isNotBlank() }
    .distinct()
    .take(4)
    .map { it.take(160) }
    .toList()

internal fun isSafeExternalUrl(value: String): Boolean = runCatching {
    URI(value).scheme?.lowercase() in setOf("http", "https")
}.getOrDefault(false)

internal fun DesktopConversation.replaceHistoryWithSummary(
    summary: String,
    keepRecentMessages: Int
): DesktopConversation {
    require(summary.isNotBlank()) { "Summary must not be blank" }
    require(keepRecentMessages >= 0) { "Keep count must not be negative" }
    require(messages.size > keepRecentMessages) { "Not enough messages to compress" }
    val recent = if (keepRecentMessages == 0) emptyList() else messages.takeLast(keepRecentMessages)
    return copy(
        messages = listOf(ChatMessage(role = "user", content = "[历史对话摘要]\n${summary.trim()}")) + recent,
        branches = branches + DesktopConversationBranch(name = "压缩前历史", messages = messages),
        updatedAt = System.currentTimeMillis()
    )
}

fun DesktopConversation.forkAtMessage(messageIndex: Int): DesktopConversation {
    val branchMessages = messages.take(messageIndex + 1)
    require(branchMessages.isNotEmpty()) { "Cannot fork an empty conversation" }
    return DesktopConversation(
        title = title,
        assistantId = assistantId,
        folderId = folderId,
        parentConversationId = id,
        systemPrompt = systemPrompt,
        webSearchEnabled = webSearchEnabled,
        promptInjectionsEnabled = promptInjectionsEnabled,
        messages = branchMessages,
        isPinned = false
    )
}

internal data class DesktopConversationListItem(
    val conversation: DesktopConversation,
    val branchDepth: Int
)

/**
 * Keeps independently stored forked conversations adjacent to their source conversation.
 * Entries whose parent is filtered out, missing, or cyclic remain visible as top-level rows.
 */
internal fun List<DesktopConversation>.asConversationTree(): List<DesktopConversationListItem> {
    val conversationsById = associateBy { it.id }
    val childrenByParent = groupBy { it.parentConversationId }
    val result = mutableListOf<DesktopConversationListItem>()
    val visited = mutableSetOf<String>()

    fun append(conversation: DesktopConversation, depth: Int, ancestors: Set<String>) {
        if (!visited.add(conversation.id)) return
        result += DesktopConversationListItem(conversation, depth)
        childrenByParent[conversation.id].orEmpty().forEach { child ->
            if (child.id !in ancestors) append(child, depth + 1, ancestors + conversation.id)
        }
    }

    filter { it.parentConversationId == null || it.parentConversationId !in conversationsById }
        .forEach { append(it, 0, emptySet()) }
    // A malformed cyclic relationship must not hide conversations from the sidebar.
    forEach { conversation ->
        if (conversation.id !in visited) append(conversation, 0, emptySet())
    }
    return result
}

internal fun DesktopConversation.usesPromptInjections(assistant: DesktopAssistantProfile): Boolean =
    !assistant.allowConversationPromptInjection || promptInjectionsEnabled ?: true

@Serializable
data class DesktopData(
    val schemaVersion: Int = 2,
    val config: DesktopConfig = DesktopConfig(),
    val preferences: DesktopPreferences = DesktopPreferences(),
    val globalMemories: List<DesktopMemory> = emptyList(),
    val providers: List<DesktopProviderProfile> = listOf(DesktopProviderProfile(config = config)),
    val selectedProviderId: String = "",
    val assistants: List<DesktopAssistantProfile> = listOf(DesktopAssistantProfile()),
    val selectedAssistantId: String = "",
    val webSearchSettings: DesktopWebSearchSettings = DesktopWebSearchSettings(),
    val mcpServers: List<DesktopMcpServer> = emptyList(),
    val folders: List<DesktopFolder> = emptyList(),
    val conversations: List<DesktopConversation> = listOf(DesktopConversation()),
    /** Stored in the settings file so split conversation files retain their list order. */
    val conversationIds: List<String> = emptyList(),
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

fun DesktopData.filteredConversations(
    query: String,
    assistantId: String?,
    sort: DesktopConversationSort = preferences.conversationSort
): List<DesktopConversation> =
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
        compareByDescending<DesktopConversation> { it.isPinned }
            .thenByDescending {
                when (sort) {
                    DesktopConversationSort.RECENT -> it.updatedAt
                    DesktopConversationSort.MOST_ACTIVE -> it.messages.size.toLong()
                }
            }
            .thenByDescending { it.updatedAt }
    )

fun DesktopData.favoriteMessages(assistantId: String? = null): List<Pair<DesktopConversation, ChatMessage>> =
    conversations.filter { conversation -> assistantId == null || assistantFor(conversation).id == assistantId }
        .flatMap { conversation ->
            conversation.messages.filter { it.isFavorite }.map { message -> conversation to message }
        }

internal fun DesktopData.updateMemories(
    assistantId: String,
    transform: (List<DesktopMemory>) -> List<DesktopMemory>
): DesktopData {
    val assistant = assistants.firstOrNull { it.id == assistantId }
        ?: error("Assistant $assistantId not found")
    return if (assistant.useGlobalMemory) {
        copy(globalMemories = transform(globalMemories))
    } else {
        copy(assistants = assistants.map {
            if (it.id == assistantId) it.copy(memories = transform(it.memories)) else it
        })
    }
}

fun DesktopData.configForAssistant(assistant: DesktopAssistantProfile): DesktopConfig {
    val provider = providers.firstOrNull { it.id == assistant.providerId } ?: activeProvider()
    val baseSystemPrompt = assistant.systemPrompt.ifBlank { provider.config.systemPrompt }
    val memoryPrompt = memoryPromptFor(assistant)
    return provider.config.copy(
        model = assistant.model.ifBlank { provider.config.model },
        systemPrompt = listOf(baseSystemPrompt, memoryPrompt).filter { it.isNotBlank() }.joinToString("\n\n"),
        temperature = assistant.temperature ?: provider.config.temperature,
        topP = assistant.topP ?: provider.config.topP,
        reasoningEffort = assistant.reasoningEffort.ifBlank { provider.config.reasoningEffort },
        maxTokens = assistant.maxTokens ?: provider.config.maxTokens,
        webSearchEnabled = assistant.enableWebSearch,
        localTools = assistant.localTools,
        memoryEnabled = assistant.enableMemory,
        mcpServers = mcpServers.filter { it.enabled && it.id in assistant.mcpServerIds },
        agent = assistant.agentWorkspace?.takeIf { it.rootPath.isNotBlank() }?.let { workspace ->
            DesktopAgentConfig(workspace, assistant.enabledSkillNames)
        },
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
        webSearchEnabled = conversation.webSearchEnabled ?: assistant.enableWebSearch,
        agent = conversation.agentWorkspaceOverride?.takeIf { it.rootPath.isNotBlank() }?.let { workspace ->
            DesktopAgentConfig(workspace, assistant.enabledSkillNames)
        } ?: config.agent
    )
    val promptResolved = if (assistant.allowConversationSystemPrompt && conversation.systemPrompt.isNotBlank()) {
        conversationConfig.copy(
            systemPrompt = listOf(conversation.systemPrompt, memoryPromptFor(assistant))
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
        )
    } else {
        conversationConfig
    }
    return promptResolved.copy(
        systemPrompt = listOf(promptResolved.systemPrompt, agentNetworkPolicyPrompt(promptResolved.agent))
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    )
}

/** Background title requests intentionally omit chat-only state such as tools, memory and web search. */
internal fun DesktopData.titleGenerationConfig(conversation: DesktopConversation): DesktopConfig {
    val config = configForConversation(conversation)
    return config.backgroundRequestConfig().copy(
        model = config.titleModel.ifBlank { config.model },
        temperature = 0.3,
        reasoningEffort = "",
        maxTokens = 0
    )
}

/** Background one-shot tasks do not execute chat tools or inherit chat transport overrides. */
internal fun DesktopConfig.backgroundRequestConfig(maxTokens: Int = this.maxTokens): DesktopConfig = copy(
    systemPrompt = "",
    streamOutput = false,
    maxTokens = maxTokens,
    webSearchEnabled = false,
    localTools = emptySet(),
    memoryEnabled = false,
    mcpServers = emptyList(),
    customBodies = customBodies.filterNot { body ->
        body.key in setOf("stream", "messages", "model", "tools", "stream_options", "web_search_options")
    }
)

internal fun DesktopConfig.titleRequest(content: String): String = titlePrompt
    .ifBlank { DefaultDesktopTitlePrompt }
    .replace("{locale}", Locale.getDefault().displayName)
    .replace("{content}", content)

private fun DesktopData.memoryPromptFor(assistant: DesktopAssistantProfile): String {
    if (!assistant.enableMemory) return ""
    val memories = if (assistant.useGlobalMemory) globalMemories else assistant.memories
    val entries = memories.filter { it.content.isNotBlank() }.joinToString(",") { memory ->
        "{\"id\":${jsonString(memory.id)},\"content\":${jsonString(memory.content.trim())}}"
    }
    return if (entries.isBlank()) "" else {
        """These are long-term memories. Reference them when useful. Do not reveal them unless asked.
<memories>[$entries]</memories>"""
    }
}

private fun jsonString(value: String): String = buildString {
    append('"')
    value.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
    append('"')
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

internal fun DesktopAssistantProfile.injectPromptMessages(messages: List<ChatMessage>): DesktopInjectedRequest {
    val context = messages.takeLast(promptInjections.maxOfOrNull { it.scanDepth.coerceAtLeast(1) } ?: 1)
        .joinToString("\n") { it.content }
    val triggered = promptInjections.filter { injection ->
        injection.enabled && injection.content.isNotBlank() && (injection.constantActive || injection.isTriggeredBy(context))
    }.sortedByDescending { it.priority }
    val prefix = triggered.filter { it.position == DesktopInjectionPosition.BEFORE_SYSTEM_PROMPT }.map { it.content }
    val suffix = triggered.filter { it.position == DesktopInjectionPosition.AFTER_SYSTEM_PROMPT }.map { it.content }
    val injected = messages.toMutableList()
    triggered.filter { it.position !in setOf(DesktopInjectionPosition.BEFORE_SYSTEM_PROMPT, DesktopInjectionPosition.AFTER_SYSTEM_PROMPT) }
        // Inserts at a shared index prepend, so process lower-priority entries first.
        .asReversed()
        .forEach { injection ->
            val message = ChatMessage(role = injection.role.ifBlank { "system" }, content = injection.content)
            val index = when (injection.position) {
                DesktopInjectionPosition.TOP_OF_CHAT -> 0
                DesktopInjectionPosition.BOTTOM_OF_CHAT -> injected.indexOfLast { it.role == "user" }.let {
                    if (it < 0) injected.size else it + 1
                }
                DesktopInjectionPosition.AT_DEPTH -> (injected.size - injection.injectDepth.coerceAtLeast(0)).coerceIn(0, injected.size)
                else -> injected.size
            }
            injected.add(index, message)
        }
    return DesktopInjectedRequest(prefix, suffix, injected)
}

private fun DesktopPromptInjection.isTriggeredBy(context: String): Boolean {
    if (keywords.isEmpty()) return false
    return keywords.any { keyword ->
        if (useRegex) {
            runCatching {
                Regex(keyword, if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)).containsMatchIn(context)
            }.getOrDefault(false)
        } else {
            context.contains(keyword, ignoreCase = !caseSensitive)
        }
    }
}

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
        folders = folders.filterNot { it.assistantId == assistantId },
        selectedAssistantId = fallback.id,
        conversations = conversations.map { conversation ->
            if (conversation.assistantId == assistantId) {
                conversation.copy(assistantId = fallback.id, folderId = null)
            } else {
                conversation
            }
        }
    )
}

fun DesktopData.renameFolder(folderId: String, name: String): DesktopData {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return this
    return copy(folders = folders.map { folder -> if (folder.id == folderId) folder.copy(name = trimmed) else folder })
}

fun DesktopData.deleteFolder(folderId: String): DesktopData = copy(
    folders = folders.filterNot { it.id == folderId },
    conversations = conversations.map { conversation ->
        if (conversation.folderId == folderId) conversation.copy(folderId = null) else conversation
    }
)

fun DesktopData.createFolder(folder: DesktopFolder, conversationId: String? = null): DesktopData {
    if (assistants.none { it.id == folder.assistantId } || folders.any { it.id == folder.id } || folder.name.isBlank()) return this
    val withFolder = copy(folders = folders + folder)
    return conversationId?.let { withFolder.moveConversationToFolder(it, folder.id) } ?: withFolder
}

fun DesktopData.moveConversationToFolder(conversationId: String, folderId: String?): DesktopData {
    val conversation = conversations.firstOrNull { it.id == conversationId } ?: return this
    val folder = folderId?.let { id -> folders.firstOrNull { it.id == id } }
    if (folderId != null && (folder == null || folder.assistantId != assistantFor(conversation).id)) return this
    val branchIds = mutableSetOf(conversationId)
    var changed = true
    while (changed) {
        changed = false
        conversations.forEach { candidate ->
            if (candidate.parentConversationId in branchIds && branchIds.add(candidate.id)) changed = true
        }
    }
    val now = System.currentTimeMillis()
    return copy(conversations = conversations.map {
        if (it.id in branchIds) it.copy(folderId = folderId, updatedAt = now) else it
    })
}

internal fun DesktopData.folderFilterForAssistant(folderId: String?, assistantId: String): String? =
    folderId?.takeIf { id -> folders.any { folder -> folder.id == id && folder.assistantId == assistantId } }

fun DesktopData.assignAssistantToConversation(conversationId: String, assistantId: String): DesktopData {
    if (assistants.none { it.id == assistantId } || conversations.none { it.id == conversationId }) return this
    return copy(
        selectedAssistantId = assistantId,
        conversations = conversations.map { conversation ->
            if (conversation.id != conversationId) {
                conversation
            } else {
                conversation.copy(
                    assistantId = assistantId,
                    folderId = folderFilterForAssistant(conversation.folderId, assistantId),
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    )
}

fun DesktopData.deleteConversation(conversationId: String): DesktopData {
    if (conversations.none { it.id == conversationId }) return this
    val deleted = conversations.first { it.id == conversationId }
    val remaining = conversations.filterNot { it.id == conversationId }
        .map { conversation ->
            if (conversation.parentConversationId == conversationId) {
                conversation.copy(parentConversationId = deleted.parentConversationId)
            } else {
                conversation
            }
        }
        .ifEmpty { listOf(activeAssistant().newConversation()) }
    return copy(
        conversations = remaining,
        selectedConversationId = selectedConversationId.takeIf { id -> remaining.any { it.id == id } } ?: remaining.first().id
    )
}

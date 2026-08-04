package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json

internal data class SettingsEditCommit(
    val data: DesktopData,
    val modifiedSections: Set<DesktopSettingsSection>,
    val deletedProviderIds: Set<String>,
    val deletedAssistantIds: Set<String>
)

internal data class SettingsEditSession(
    private val original: DesktopData,
    val draft: DesktopData = original
) {
    val hasChanges: Boolean
        get() = draft.settingsContentDiffersFrom(original)

    val modifiedSections: Set<DesktopSettingsSection>
        get() = draft.modifiedSettingsSectionsFrom(original)

    val modifiedProviderIds: Set<String>
        get() = draft.providers.filter { provider ->
            original.providers.firstOrNull { it.id == provider.id } != provider
        }.mapTo(mutableSetOf(), DesktopProviderProfile::id)

    val modifiedAssistantIds: Set<String>
        get() = draft.assistants.filter { assistant ->
            original.assistants.firstOrNull { it.id == assistant.id } != assistant
        }.mapTo(mutableSetOf(), DesktopAssistantProfile::id)

    fun update(transform: (DesktopData) -> DesktopData): SettingsEditSession = copy(draft = transform(draft))

    fun discard(): SettingsEditSession = copy(draft = original)

    fun commitOrNull(): SettingsEditCommit? = if (isValid) commit() else null

    private val isValid: Boolean
        get() = draft.providers.isNotEmpty() && draft.assistants.isNotEmpty() &&
            draft.providers.all(DesktopProviderProfile::isValidSettingsDraft) &&
            draft.assistants.all(DesktopAssistantProfile::isValidSettingsDraft)

    private fun commit(): SettingsEditCommit = SettingsEditCommit(
        data = draft,
        modifiedSections = modifiedSections,
        deletedProviderIds = original.providers.mapTo(mutableSetOf(), DesktopProviderProfile::id) -
            draft.providers.mapTo(mutableSetOf(), DesktopProviderProfile::id),
        deletedAssistantIds = original.assistants.mapTo(mutableSetOf(), DesktopAssistantProfile::id) -
            draft.assistants.mapTo(mutableSetOf(), DesktopAssistantProfile::id)
    )
}

internal fun DesktopProviderProfile.isValidSettingsDraft(): Boolean =
    name.isNotBlank() && config.baseUrl.isNotBlank() && config.model.isNotBlank() && config.isValidSettingsDraft()

internal fun DesktopAssistantProfile.isValidSettingsDraft(): Boolean =
    name.isNotBlank() &&
        presetMessages.all { it.content.isNotBlank() } &&
        quickMessages.all { it.content.isNotBlank() } &&
        validateMessageTemplate(messageTemplate).isSuccess &&
        regexRules.all { rule -> rule.findRegex.isNotBlank() && runCatching { Regex(rule.findRegex) }.isSuccess } &&
        memories.all { it.content.isNotBlank() } &&
        promptInjections.all { injection ->
            injection.content.isNotBlank() && (injection.constantActive || injection.keywords.isNotEmpty()) &&
                (!injection.useRegex || injection.keywords.all { runCatching { Regex(it) }.isSuccess })
        } &&
        customHeaders.all { it.name.isNotBlank() } &&
        hasValidCustomBodies(customBodies)

internal fun DesktopConfig.isValidSettingsDraft(): Boolean =
    customHeaders.all { it.name.isNotBlank() } && hasValidCustomBodies(customBodies)

private fun hasValidCustomBodies(bodies: List<DesktopCustomBody>): Boolean =
    bodies.all { body -> body.key.isNotBlank() && runCatching { Json.parseToJsonElement(body.value) }.isSuccess }

internal fun DesktopData.settingsContentDiffersFrom(other: DesktopData): Boolean =
    config != other.config ||
        preferences != other.preferences ||
        globalMemories != other.globalMemories ||
        providers != other.providers ||
        selectedProviderId != other.selectedProviderId ||
        assistants != other.assistants ||
        selectedAssistantId != other.selectedAssistantId ||
        webSearchSettings != other.webSearchSettings ||
        mcpServers != other.mcpServers

internal fun DesktopData.modifiedSettingsSectionsFrom(other: DesktopData): Set<DesktopSettingsSection> = buildSet {
    val current = preferences
    val saved = other.preferences
    if (
        current.colorMode != saved.colorMode || current.themeColor != saved.themeColor ||
        current.fontFamily != saved.fontFamily || current.language != saved.language || current.fontScale != saved.fontScale
    ) add(DesktopSettingsSection.GENERAL)
    if (
        current.showUserAvatar != saved.showUserAvatar || current.userNickname != saved.userNickname ||
        current.showModelIcon != saved.showModelIcon || current.showModelName != saved.showModelName ||
        current.showAssistantBubble != saved.showAssistantBubble || current.showMessageTimestamp != saved.showMessageTimestamp ||
        current.showReasoning != saved.showReasoning || current.autoCollapseReasoning != saved.autoCollapseReasoning ||
        current.codeBlockAutoWrap != saved.codeBlockAutoWrap || current.enableChineseTypography != saved.enableChineseTypography ||
        current.enableMermaidRendering != saved.enableMermaidRendering || current.enableMermaidCli != saved.enableMermaidCli ||
        current.mermaidCliPath != saved.mermaidCliPath || current.mermaidUseSystemBrowser != saved.mermaidUseSystemBrowser
    ) add(DesktopSettingsSection.MESSAGE_DISPLAY)
    if (
        current.sendOnEnter != saved.sendOnEnter || current.enableAutoScroll != saved.enableAutoScroll ||
        current.enableSmoothScroll != saved.enableSmoothScroll || current.showMessageJumper != saved.showMessageJumper ||
        current.messageJumperOnLeft != saved.messageJumperOnLeft
    ) add(DesktopSettingsSection.INTERACTION)
    if (globalMemories != other.globalMemories || webSearchSettings != other.webSearchSettings || mcpServers != other.mcpServers) {
        add(DesktopSettingsSection.DATA)
    }
    if (assistants != other.assistants || selectedAssistantId != other.selectedAssistantId) add(DesktopSettingsSection.ASSISTANTS)
    if (providers != other.providers || selectedProviderId != other.selectedProviderId || config != other.config) {
        add(DesktopSettingsSection.PROVIDERS)
    }
}

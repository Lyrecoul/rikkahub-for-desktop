package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopAssistantProfileTest {
    private val firstProvider = DesktopProviderProfile(
        id = "first-provider",
        config = DesktopConfig(model = "provider-model", systemPrompt = "provider prompt")
    )
    private val secondProvider = DesktopProviderProfile(
        id = "second-provider",
        config = DesktopConfig(model = "second-model", systemPrompt = "second prompt")
    )
    private val firstAssistant = DesktopAssistantProfile(id = "first-assistant", name = "First")
    private val secondAssistant = DesktopAssistantProfile(
        id = "second-assistant",
        name = "Second",
        providerId = secondProvider.id,
        model = "assistant-model",
        systemPrompt = "assistant prompt",
        temperature = 0.4,
        topP = 0.8,
        reasoningEffort = "high",
        maxTokens = 768
    )

    @Test
    fun assistantOverridesProviderModelAndPrompt() {
        val config = data().configForAssistant(secondAssistant)

        assertEquals("assistant-model", config.model)
        assertEquals("assistant prompt", config.systemPrompt)
        assertEquals(0.4, config.temperature)
        assertEquals(0.8, config.topP)
        assertEquals("high", config.reasoningEffort)
        assertEquals(768, config.maxTokens)
        assertEquals(secondProvider.config.baseUrl, config.baseUrl)
    }

    @Test
    fun blankAssistantFieldsInheritActiveProvider() {
        val config = data().configForAssistant(firstAssistant)

        assertEquals("provider-model", config.model)
        assertEquals("provider prompt", config.systemPrompt)
    }

    @Test
    fun deletingAssistantRebindsItsConversations() {
        val conversation = DesktopConversation(assistantId = secondAssistant.id)
        val result = data(conversation).deleteAssistantProfile(secondAssistant.id)

        assertEquals(listOf(firstAssistant.id), result.assistants.map { it.id })
        assertEquals(firstAssistant.id, result.conversations.single().assistantId)
        assertEquals(firstAssistant.id, result.selectedAssistantId)
    }

    @Test
    fun deletingProviderRebindsAssistantsUsingIt() {
        val result = data().deleteProviderProfile(secondProvider.id)

        assertEquals(firstProvider.id, result.assistants.last().providerId)
        assertEquals("assistant-model", result.assistants.last().model)
    }

    @Test
    fun savingAssistantUpdatesItsConfiguration() {
        val updated = secondAssistant.copy(name = "Updated", model = "new-model")
        val result = data().saveAssistantProfile(updated)

        assertEquals(updated, result.assistants.last())
    }

    @Test
    fun contextLimitKeepsOnlyMostRecentMessages() {
        val messages = listOf(
            ChatMessage("user", "one"),
            ChatMessage("assistant", "two"),
            ChatMessage("user", "three")
        )

        assertEquals(
            listOf("two", "three"),
            secondAssistant.copy(contextMessageSize = 2).limitContext(messages).map { it.content }
        )
        assertEquals(messages, secondAssistant.limitContext(messages))
    }

    @Test
    fun savingAssistantPersistsQuickMessages() {
        val quickMessage = DesktopQuickMessage(
            id = "quick-message",
            title = "Summarize",
            content = "Summarize the following text"
        )
        val updated = secondAssistant.copy(quickMessages = listOf(quickMessage))

        val result = data().saveAssistantProfile(updated)

        assertEquals(listOf(quickMessage), result.assistants.last().quickMessages)
    }

    @Test
    fun conversationPromptOverridesAssistantOnlyWhenAllowed() {
        val conversation = DesktopConversation(
            assistantId = secondAssistant.id,
            systemPrompt = "conversation prompt"
        )
        val disabled = data(conversation)
        val enabled = disabled.copy(
            assistants = disabled.assistants.map {
                if (it.id == secondAssistant.id) it.copy(allowConversationSystemPrompt = true) else it
            }
        )

        assertEquals("assistant prompt", disabled.configForConversation(conversation).systemPrompt)
        assertEquals("conversation prompt", enabled.configForConversation(conversation).systemPrompt)
    }

    @Test
    fun newConversationCopiesPresetMessagesWithFreshIds() {
        val assistant = secondAssistant.copy(
            presetMessages = listOf(
                DesktopPresetMessage("assistant", "Welcome"),
                DesktopPresetMessage("user", "Example question")
            )
        )

        val firstConversation = assistant.newConversation()
        val secondConversation = assistant.newConversation()

        assertEquals(assistant.id, firstConversation.assistantId)
        assertEquals(listOf("assistant", "user"), firstConversation.messages.map { it.role })
        assertEquals(listOf("Welcome", "Example question"), firstConversation.messages.map { it.content })
        kotlin.test.assertNotEquals(
            firstConversation.messages.map { it.id },
            secondConversation.messages.map { it.id }
        )
    }

    @Test
    fun requestOverridesCombineAssistantThenProvider() {
        val provider = secondProvider.copy(
            config = secondProvider.config.copy(
                customHeaders = listOf(DesktopCustomHeader("X-Provider", "provider")),
                customBodies = listOf(DesktopCustomBody("metadata", "{\"provider\":true}"))
            )
        )
        val assistant = secondAssistant.copy(
            customHeaders = listOf(DesktopCustomHeader("X-Assistant", "assistant")),
            customBodies = listOf(DesktopCustomBody("metadata", "{\"assistant\":true}"))
        )
        val source = data().copy(
            providers = listOf(firstProvider, provider),
            assistants = listOf(firstAssistant, assistant)
        )

        val config = source.configForAssistant(assistant)

        assertEquals(listOf("X-Assistant", "X-Provider"), config.customHeaders.map { it.name })
        assertEquals(2, config.customBodies.size)
    }

    @Test
    fun conversationCanOverrideAssistantWebSearchDefault() {
        val assistant = secondAssistant.copy(enableWebSearch = true)
        val source = data().copy(
            assistants = listOf(firstAssistant, assistant)
        )
        val inherited = DesktopConversation(assistantId = assistant.id)
        val disabled = inherited.copy(webSearchEnabled = false)

        assertEquals(true, source.configForConversation(inherited).webSearchEnabled)
        assertEquals(false, source.configForConversation(disabled).webSearchEnabled)
    }

    @Test
    fun assistantControlsStreamingMode() {
        val assistant = secondAssistant.copy(streamOutput = false)

        assertEquals(false, data().configForAssistant(assistant).streamOutput)
    }

    @Test
    fun enabledMemoriesAreAddedToAssistantAndConversationPrompts() {
        val assistant = secondAssistant.copy(
            enableMemory = true,
            allowConversationSystemPrompt = true,
            memories = listOf(
                DesktopMemory(id = "one", content = "User prefers concise answers"),
                DesktopMemory(id = "two", content = "Project uses Kotlin")
            )
        )
        val conversation = DesktopConversation(
            assistantId = assistant.id,
            systemPrompt = "Act as a reviewer"
        )
        val source = data(conversation).copy(
            assistants = listOf(firstAssistant, assistant)
        )

        assertEquals(
            "assistant prompt\n\nMemory:\n- User prefers concise answers\n- Project uses Kotlin",
            source.configForAssistant(assistant).systemPrompt
        )
        assertEquals(
            "Act as a reviewer\n\nMemory:\n- User prefers concise answers\n- Project uses Kotlin",
            source.configForConversation(conversation).systemPrompt
        )
    }

    @Test
    fun disabledMemoryDoesNotAffectPrompt() {
        val assistant = secondAssistant.copy(
            enableMemory = false,
            memories = listOf(DesktopMemory(content = "Hidden memory"))
        )

        assertEquals("assistant prompt", data().configForAssistant(assistant).systemPrompt)
    }

    private fun data(conversation: DesktopConversation = DesktopConversation()) = DesktopData(
        config = firstProvider.config,
        providers = listOf(firstProvider, secondProvider),
        selectedProviderId = firstProvider.id,
        assistants = listOf(firstAssistant, secondAssistant),
        selectedAssistantId = secondAssistant.id,
        conversations = listOf(conversation),
        selectedConversationId = conversation.id
    )
}

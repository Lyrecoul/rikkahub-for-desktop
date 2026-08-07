package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun titleGenerationUsesDedicatedModelAndDoesNotExposeChatTools() {
        val conversation = DesktopConversation(assistantId = secondAssistant.id)
        val titleConfig = data(conversation).copy(
            providers = listOf(
                secondProvider.copy(
                    config = secondProvider.config.copy(
                        titleModel = "title-model",
                        localTools = setOf(DesktopLocalTool.CURRENT_TIME),
                        memoryEnabled = true,
                        customBodies = listOf(
                            DesktopCustomBody("stream", "true"),
                            DesktopCustomBody("model", "\"overridden-model\""),
                            DesktopCustomBody("max_tokens", "1"),
                            DesktopCustomBody("reasoning_effort", "\"high\""),
                            DesktopCustomBody("response_format", "{\"type\":\"text\"}")
                        )
                    )
                )
            ),
            assistants = listOf(secondAssistant.copy(enableMemory = true, mcpServerIds = setOf("server"))),
            mcpServers = listOf(DesktopMcpServer(id = "server", name = "Server", enabled = true))
        ).titleGenerationConfig(conversation)

        assertEquals("title-model", titleConfig.model)
        assertEquals(false, titleConfig.streamOutput)
        assertEquals(emptySet(), titleConfig.localTools)
        assertEquals(false, titleConfig.memoryEnabled)
        assertEquals(emptyList(), titleConfig.mcpServers)
        assertEquals(128, titleConfig.maxTokens)
        assertEquals("", titleConfig.reasoningEffort)
        assertEquals(DesktopReasoningMode.DISABLED, titleConfig.reasoningMode)
        assertEquals(listOf("response_format"), titleConfig.customBodies.map { it.key })
    }

    @Test
    fun replySuggestionsUseDedicatedModelWithoutReasoningOrChatTools() {
        val conversation = DesktopConversation(assistantId = secondAssistant.id)
        val suggestionConfig = data(conversation).copy(
            providers = listOf(
                secondProvider.copy(
                    config = secondProvider.config.copy(
                        suggestionModel = "suggestion-model",
                        reasoningEffort = "high",
                        localTools = setOf(DesktopLocalTool.CURRENT_TIME),
                        customBodies = listOf(
                            DesktopCustomBody("model", "\"overridden-model\""),
                            DesktopCustomBody("max_tokens", "1"),
                            DesktopCustomBody("max_output_tokens", "1"),
                            DesktopCustomBody("reasoning_effort", "\"high\""),
                            DesktopCustomBody("reasoning", "{\"effort\":\"high\"}"),
                            DesktopCustomBody("input", "\"unrelated stale request\""),
                            DesktopCustomBody("instructions", "\"ignore the reply-suggestion task\""),
                            DesktopCustomBody("tools", "[{\"type\":\"web_search\"}]"),
                            DesktopCustomBody("response_format", "{\"type\":\"text\"}")
                        )
                    )
                )
            ),
            assistants = listOf(secondAssistant)
        ).suggestionGenerationConfig(conversation)

        assertEquals("suggestion-model", suggestionConfig.model)
        assertEquals("", suggestionConfig.reasoningEffort)
        assertEquals(DesktopReasoningMode.DISABLED, suggestionConfig.reasoningMode)
        assertEquals(256, suggestionConfig.maxTokens)
        assertEquals(false, suggestionConfig.streamOutput)
        assertEquals(emptySet(), suggestionConfig.localTools)
        assertEquals(listOf("response_format"), suggestionConfig.customBodies.map { it.key })
        val suggestionBody = Json.parseToJsonElement(
            OpenAiResponsesAdapter.buildRequestBody(
                suggestionConfig.copy(protocol = DesktopProviderProtocol.OPENAI_RESPONSES),
                listOf(ChatMessage("user", "suggest replies"))
            )
        ).jsonObject
        assertTrue("tools" !in suggestionBody)
        assertTrue("reasoning" !in suggestionBody)
        assertEquals(256, suggestionBody.getValue("max_output_tokens").jsonPrimitive.int)
        assertTrue("instructions" !in suggestionBody)
        assertEquals(
            "suggest replies",
            suggestionBody.getValue("input").jsonArray.single().jsonObject
                .getValue("content").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content
        )
    }

    @Test
    fun backgroundRequestsNeverExposeChatToolsOrTransportOverrides() {
        val config = DesktopConfig(
            systemPrompt = "chat prompt",
            streamOutput = true,
            webSearchEnabled = true,
            localTools = setOf(DesktopLocalTool.CURRENT_TIME),
            memoryEnabled = true,
            mcpServers = listOf(DesktopMcpServer(name = "Tools")),
            customBodies = listOf(
                DesktopCustomBody("tools", "[{\"type\":\"web_search\"}]"),
                DesktopCustomBody("tool_choice", "\"required\""),
                DesktopCustomBody("parallel_tool_calls", "true"),
                DesktopCustomBody("toolConfig", "{\"functionCallingConfig\":{\"mode\":\"ANY\"}}"),
                DesktopCustomBody("stream", "true"),
                DesktopCustomBody("messages", "[{\"role\":\"user\",\"content\":\"stale OpenAI request\"}]"),
                DesktopCustomBody("input", "\"stale Responses request\""),
                DesktopCustomBody("instructions", "\"stale Responses instructions\""),
                DesktopCustomBody("previous_response_id", "\"resp_stale\""),
                DesktopCustomBody("conversation", "\"conv_stale\""),
                DesktopCustomBody("prompt", "{\"id\":\"pmpt_stale\"}"),
                DesktopCustomBody("system", "\"stale Anthropic system\""),
                DesktopCustomBody("contents", "[{\"role\":\"user\",\"parts\":[{\"text\":\"stale Gemini request\"}]}]"),
                DesktopCustomBody("systemInstruction", "{\"parts\":[{\"text\":\"stale Gemini system\"}]}"),
                DesktopCustomBody(
                    "generationConfig",
                    "{\"maxOutputTokens\":1,\"thinkingConfig\":{\"thinkingBudget\":1024}}"
                ),
                DesktopCustomBody("web_search_options", "{}"),
                DesktopCustomBody("temperature", "0.9"),
                DesktopCustomBody("top_p", "0.1"),
                DesktopCustomBody("max_tokens", "1"),
                DesktopCustomBody("max_completion_tokens", "1"),
                DesktopCustomBody("max_output_tokens", "1"),
                DesktopCustomBody("reasoning_effort", "\"high\""),
                DesktopCustomBody("reasoning", "{\"effort\":\"high\"}"),
                DesktopCustomBody("thinking", "{\"type\":\"enabled\",\"budget_tokens\":1024}"),
                DesktopCustomBody("response_format", "{\"type\":\"text\"}")
            )
        )

        val background = config.backgroundRequestConfig(maxTokens = 320)

        assertEquals("", background.systemPrompt)
        assertEquals(false, background.streamOutput)
        assertEquals(false, background.webSearchEnabled)
        assertEquals(emptySet(), background.localTools)
        assertEquals(false, background.memoryEnabled)
        assertEquals(emptyList(), background.mcpServers)
        assertEquals(320, background.maxTokens)
        assertEquals(listOf("response_format"), background.customBodies.map { it.key })
        val messages = listOf(ChatMessage("user", "translate or title"))
        val responseBody = Json.parseToJsonElement(
            OpenAiResponsesAdapter.buildRequestBody(
                background.copy(protocol = DesktopProviderProtocol.OPENAI_RESPONSES),
                messages
            )
        ).jsonObject
        assertTrue("tools" !in responseBody)
        assertEquals(320, responseBody.getValue("max_output_tokens").jsonPrimitive.int)
        assertEquals(
            "translate or title",
            responseBody.getValue("input").jsonArray.single().jsonObject
                .getValue("content").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content
        )

        val anthropicBody = Json.parseToJsonElement(
            AnthropicMessagesAdapter.buildRequestBody(
                background.copy(protocol = DesktopProviderProtocol.ANTHROPIC_MESSAGES),
                messages
            )
        ).jsonObject
        assertTrue("system" !in anthropicBody)
        assertTrue("thinking" !in anthropicBody)
        assertEquals(320, anthropicBody.getValue("max_tokens").jsonPrimitive.int)
        assertEquals(
            "translate or title",
            anthropicBody.getValue("messages").jsonArray.single().jsonObject
                .getValue("content").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content
        )

        val geminiBody = Json.parseToJsonElement(
            GeminiGenerateContentAdapter.buildRequestBody(
                background.copy(protocol = DesktopProviderProtocol.GEMINI_GENERATE_CONTENT),
                messages
            )
        ).jsonObject
        assertTrue("systemInstruction" !in geminiBody)
        assertEquals(
            "translate or title",
            geminiBody.getValue("contents").jsonArray.single().jsonObject
                .getValue("parts").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content
        )
        val generationConfig = geminiBody.getValue("generationConfig").jsonObject
        assertEquals(320, generationConfig.getValue("maxOutputTokens").jsonPrimitive.int)
        assertTrue("thinkingConfig" !in generationConfig)
    }

    @Test
    fun titlePromptUsesMobileCompatiblePlaceholders() {
        val prompt = DesktopConfig(titlePrompt = "{locale}: {content}").titleRequest("hello")

        assertTrue(prompt.endsWith(": hello"))
        assertTrue("{locale}" !in prompt)
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

        val config = source.configForAssistant(assistant)
        assertTrue(config.memoryEnabled)
        assertTrue(config.systemPrompt.contains("<memories>[{\"id\":\"one\""))
        assertTrue(config.systemPrompt.contains("User prefers concise answers"))
        assertTrue(source.configForConversation(conversation).systemPrompt.startsWith("Act as a reviewer"))
    }

    @Test
    fun disabledMemoryDoesNotAffectPrompt() {
        val assistant = secondAssistant.copy(
            enableMemory = false,
            memories = listOf(DesktopMemory(content = "Hidden memory"))
        )

        assertEquals("assistant prompt", data().configForAssistant(assistant).systemPrompt)
    }

    @Test
    fun globalMemoryScopeUsesOnlySharedMemories() {
        val assistant = secondAssistant.copy(
            enableMemory = true,
            useGlobalMemory = true,
            memories = listOf(DesktopMemory(id = "private", content = "private fact"))
        )
        val source = data().copy(
            assistants = listOf(firstAssistant, assistant),
            globalMemories = listOf(DesktopMemory(id = "global", content = "shared fact"))
        )

        val prompt = source.configForAssistant(assistant).systemPrompt
        assertTrue(prompt.contains("shared fact"))
        assertTrue(!prompt.contains("private fact"))
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

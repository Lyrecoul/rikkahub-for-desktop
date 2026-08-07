package me.rerere.rikkahub.desktop

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenAiResponsesAdapterTest {
    @Test
    fun protocolDefaultsRemainBackwardCompatibleAndClientDelegatesResponses() {
        val legacy = Json.decodeFromString<DesktopConfig>("{}")
        val config = DesktopConfig(protocol = DesktopProviderProtocol.OPENAI_RESPONSES, model = "gpt-4.1-mini")
        val body = Json.parseToJsonElement(
            OpenAiClient().buildRequestBody(config, listOf(ChatMessage("user", "hello")))
        ).jsonObject

        assertEquals(DesktopProviderProtocol.OPENAI_CHAT_COMPLETIONS, legacy.protocol)
        assertTrue("input" in body)
        assertTrue("messages" !in body)
    }

    @Test
    fun disablesReasoningOnlyWhenProviderStrategySupportsIt() {
        fun requestBody(strategy: DesktopReasoningDisableStrategy) = Json.parseToJsonElement(
            OpenAiResponsesAdapter.buildRequestBody(
                DesktopConfig(
                    protocol = DesktopProviderProtocol.OPENAI_RESPONSES,
                    baseUrl = "https://relay.example.com/v1",
                    model = "test-model",
                    reasoningDisableStrategy = strategy,
                    reasoningMode = DesktopReasoningMode.DISABLED
                ),
                listOf(ChatMessage("user", "hello"))
            )
        ).jsonObject

        val supported = requestBody(DesktopReasoningDisableStrategy.RESPONSES_EFFORT_NONE)
        val omitted = requestBody(DesktopReasoningDisableStrategy.OMIT)

        assertEquals(
            "none",
            supported.getValue("reasoning").jsonObject.getValue("effort").jsonPrimitive.content
        )
        assertTrue("reasoning" !in omitted)
    }

    @Test
    fun disableIntentTakesPrecedenceOverExplicitReasoningEffort() {
        val body = Json.parseToJsonElement(
            OpenAiResponsesAdapter.buildRequestBody(
                DesktopConfig(
                    protocol = DesktopProviderProtocol.OPENAI_RESPONSES,
                    baseUrl = "https://relay.example.com/v1",
                    model = "test-model",
                    reasoningEffort = "high",
                    reasoningDisableStrategy = DesktopReasoningDisableStrategy.RESPONSES_EFFORT_NONE,
                    reasoningMode = DesktopReasoningMode.DISABLED
                ),
                listOf(ChatMessage("user", "hello"))
            )
        ).jsonObject

        assertEquals(
            "none",
            body.getValue("reasoning").jsonObject.getValue("effort").jsonPrimitive.content
        )
    }

    @Test
    fun backgroundTaskConfigsDisableReasoningWhileChatKeepsExplicitEffort() {
        val base = DesktopConfig(
            protocol = DesktopProviderProtocol.OPENAI_RESPONSES,
            model = "test-model",
            reasoningEffort = "high",
            reasoningDisableStrategy = DesktopReasoningDisableStrategy.RESPONSES_EFFORT_NONE
        )
        val conversation = DesktopConversation()
        val data = DesktopData(
            config = base,
            providers = listOf(DesktopProviderProfile(config = base)),
            conversations = listOf(conversation)
        )
        fun effort(config: DesktopConfig): String = Json.parseToJsonElement(
            OpenAiResponsesAdapter.buildRequestBody(config, listOf(ChatMessage("user", "hello")))
        ).jsonObject.getValue("reasoning").jsonObject.getValue("effort").jsonPrimitive.content

        assertEquals("high", effort(base))
        assertEquals("none", effort(data.titleGenerationConfig(conversation)))
        assertEquals("none", effort(data.suggestionGenerationConfig(conversation)))
        assertEquals("none", effort(base.translationRequestConfig()))
        assertEquals("none", effort(base.compressionRequestConfig(maxTokens = 512)))
    }

    @Test
    fun requestReasoningModeIsNotPersisted() {
        val encoded = Json.encodeToString(
            DesktopConfig.serializer(),
            DesktopConfig(reasoningMode = DesktopReasoningMode.DISABLED)
        )
        val decoded = Json.decodeFromString<DesktopConfig>(encoded)

        assertTrue("reasoningMode" !in encoded)
        assertEquals(DesktopReasoningMode.INHERIT, decoded.reasoningMode)
    }

    @Test
    fun serializesResponsesMessagesImagesAndToolHistory() {
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.OPENAI_RESPONSES,
            model = "gpt-4.1-mini",
            systemPrompt = "system",
            localTools = setOf(DesktopLocalTool.CURRENT_TIME)
        )
        val messages = listOf(
            ChatMessage(
                role = "user",
                content = "inspect",
                attachments = listOf(
                    DesktopAttachment("photo.png", "image/png", "AQID", isImage = true),
                    DesktopAttachment("notes.txt", "text/plain", "hello")
                )
            ),
            ChatMessage(
                role = "assistant",
                content = "",
                toolCalls = listOf(DesktopToolCall("call_1", DesktopCurrentTimeToolName, "{}"))
            ),
            ChatMessage(role = "tool", content = "12:00", toolCallId = "call_1")
        )

        val body = Json.parseToJsonElement(OpenAiResponsesAdapter.buildRequestBody(config, messages)).jsonObject
        val input = body.getValue("input").jsonArray
        val userContent = input[0].jsonObject.getValue("content").jsonArray

        assertEquals("system", body.getValue("instructions").jsonPrimitive.content)
        assertEquals(listOf("input_text", "input_image", "input_text"), userContent.map {
            it.jsonObject.getValue("type").jsonPrimitive.content
        })
        assertEquals(
            "data:image/png;base64,AQID",
            userContent[1].jsonObject.getValue("image_url").jsonPrimitive.content
        )
        assertEquals(
            listOf("message", "function_call", "function_call_output"),
            input.map { it.jsonObject.getValue("type").jsonPrimitive.content }
        )
        assertEquals(
            DesktopCurrentTimeToolName,
            body.getValue("tools").jsonArray.single().jsonObject.getValue("name").jsonPrimitive.content
        )
    }

    @Test
    fun serializesOriginalDocumentAsNativeResponsesFile() {
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.OPENAI_RESPONSES,
            model = "gpt-4.1-mini"
        )
        val document = DesktopAttachment(
            name = "report.pdf",
            mimeType = "text/plain",
            data = "extracted text",
            rawData = "JVBERg==",
            rawMimeType = "application/pdf",
            sizeBytes = 4
        )

        val body = Json.parseToJsonElement(
            OpenAiResponsesAdapter.buildRequestBody(
                config,
                listOf(ChatMessage("user", "inspect", attachments = listOf(document)))
            )
        ).jsonObject
        val content = body.getValue("input").jsonArray.single().jsonObject.getValue("content").jsonArray
        val file = content[1].jsonObject

        assertEquals("input_file", file.getValue("type").jsonPrimitive.content)
        assertEquals("report.pdf", file.getValue("filename").jsonPrimitive.content)
        assertEquals(
            "data:application/pdf;base64,JVBERg==",
            file.getValue("file_data").jsonPrimitive.content
        )
    }

    @Test
    fun fallsBackToExtractedDocumentTextWithoutNativeCapability() {
        val model = "private-text-model"
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.OPENAI_RESPONSES,
            model = model,
            modelCapabilityOverrides = mapOf(model to DesktopModelCapabilities())
        )
        val document = DesktopAttachment(
            name = "report.pdf",
            mimeType = "text/plain",
            data = "extracted text",
            rawData = "JVBERg==",
            rawMimeType = "application/pdf"
        )

        val body = Json.parseToJsonElement(
            OpenAiResponsesAdapter.buildRequestBody(
                config,
                listOf(ChatMessage("user", "", attachments = listOf(document)))
            )
        ).jsonObject
        val file = body.getValue("input").jsonArray.single().jsonObject
            .getValue("content").jsonArray.single().jsonObject

        assertEquals("input_text", file.getValue("type").jsonPrimitive.content)
        assertTrue(file.getValue("text").jsonPrimitive.content.contains("extracted text"))
    }

    @Test
    fun serializesNativeWebSearchAsResponsesTool() {
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.OPENAI_RESPONSES,
            model = "gpt-4.1-mini",
            webSearchEnabled = true
        )

        val body = Json.parseToJsonElement(OpenAiResponsesAdapter.buildRequestBody(config, emptyList())).jsonObject

        assertEquals(
            "web_search_preview",
            body.getValue("tools").jsonArray.single().jsonObject.getValue("type").jsonPrimitive.content
        )
        assertTrue("web_search_options" !in body)
    }

    @Test
    fun parsesResponsesStreamingEvents() {
        val text = OpenAiResponsesAdapter.parseStreamEvent(
            """{"type":"response.output_text.delta","delta":"hello"}"""
        )
        val call = OpenAiResponsesAdapter.parseStreamEvent(
            """{"type":"response.output_item.added","output_index":2,"item":{"type":"function_call","call_id":"call_1","name":"current_time","arguments":""}}"""
        )
        val arguments = OpenAiResponsesAdapter.parseStreamEvent(
            """{"type":"response.function_call_arguments.delta","output_index":2,"item_id":"fc_1","delta":"{}"}"""
        )
        val completed = OpenAiResponsesAdapter.parseStreamEvent(
            """{"type":"response.completed","response":{"model":"gpt-4.1-mini","usage":{"input_tokens":12,"output_tokens":4,"input_tokens_details":{"cached_tokens":8}}}}"""
        )

        assertEquals("hello", text?.content)
        assertEquals(2, call?.toolCallDeltas?.single()?.index)
        assertEquals("call_1", call?.toolCallDeltas?.single()?.id)
        assertEquals(null, arguments?.toolCallDeltas?.single()?.id)
        assertEquals("{}", arguments?.toolCallDeltas?.single()?.arguments)
        val indexes = mutableMapOf<Int, Int>()
        val calls = emptyList<DesktopToolCall>()
            .merge(checkNotNull(call).normalizeProviderToolCallIndexes(indexes).toolCallDeltas)
            .merge(checkNotNull(arguments).normalizeProviderToolCallIndexes(indexes).toolCallDeltas)
        assertEquals(listOf(DesktopToolCall("call_1", "current_time", "{}")), calls)
        assertEquals(12, completed?.promptTokens)
        assertEquals(4, completed?.completionTokens)
        assertEquals(8, completed?.cachedTokens)
    }

    @Test
    fun keepsDeepSeekReasoningContentOutOfVisibleResponseText() {
        val payload = """
            {
              "model":"deepseek-v4-flash",
              "output":[
                {"type":"reasoning","content":[
                  {"type":"reasoning_text","text":"We need translate this precisely."}
                ]},
                {"type":"message","role":"assistant","content":[
                  {"type":"output_text","text":"精简后的正文"}
                ]}
              ]
            }
        """.trimIndent()

        val result = OpenAiResponsesAdapter.parseCompleteResponse(payload)

        assertEquals("精简后的正文", result.content)
        assertEquals("We need translate this precisely.", result.reasoning)
    }

    @Test
    fun parsesCompleteResponsesPayloadWithReasoningToolsAndCitations() {
        val payload = """
            {
              "model": "gpt-4.1-mini",
              "output": [
                {"type":"reasoning","summary":[{"type":"summary_text","text":"checked"}]},
                {"type":"message","role":"assistant","content":[{
                  "type":"output_text",
                  "text":"answer",
                  "annotations":[{"type":"url_citation","title":"Source","url":"https://example.com"}]
                }]},
                {"type":"function_call","call_id":"call_1","name":"current_time","arguments":"{}"}
              ],
              "usage":{"input_tokens":10,"output_tokens":3,"input_tokens_details":{"cached_tokens":5}}
            }
        """.trimIndent()

        val result = OpenAiResponsesAdapter.parseCompleteResponse(payload)

        assertEquals("answer", result.content)
        assertEquals("checked", result.reasoning)
        assertEquals("gpt-4.1-mini", result.modelId)
        assertEquals(10, result.promptTokens)
        assertEquals(3, result.completionTokens)
        assertEquals(5, result.cachedTokens)
        assertEquals(listOf(DesktopCitation("https://example.com", "Source")), result.citations)
        val call = result.toolCallDeltas.single()
        assertEquals("call_1", call.id)
        assertEquals("current_time", call.name)
        assertEquals("{}", call.arguments)
    }

    @Test
    fun parsesImageGenerationResultsFromCompletedResponses() {
        val payload = """
            {
              "output":[
                {"type":"image_generation_call","id":"ig_1","result":"AQID"},
                {"type":"message","role":"assistant","content":[{"type":"output_text","text":"done"}]}
              ]
            }
        """.trimIndent()

        val result = OpenAiResponsesAdapter.parseCompleteResponse(payload)
        val completed = OpenAiResponsesAdapter.parseStreamEvent(
            """{"type":"response.image_generation_call.completed","item":{"type":"image_generation_call","result":"AQID"}}"""
        )

        assertEquals("done", result.content)
        assertEquals("AQID", result.attachments.single().data)
        assertEquals("image/png", result.attachments.single().mimeType)
        assertEquals("AQID", completed?.attachments?.single()?.data)
    }

    @Test
    fun parsesRefusalAndIncompleteResponses() {
        val refusal = OpenAiResponsesAdapter.parseStreamEvent(
            """{"type":"response.refusal.delta","delta":"cannot comply"}"""
        )
        val incomplete = OpenAiResponsesAdapter.parseError(
            """{"type":"response.incomplete","response":{"status":"incomplete","incomplete_details":{"reason":"max_output_tokens"}}}"""
        )

        assertEquals("cannot comply", refusal?.content)
        assertEquals("Response incomplete: max_output_tokens", incomplete)
    }

    @Test
    fun rejectsAudioBeforeBuildingResponsesRequest() {
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.OPENAI_RESPONSES,
            model = "gpt-4o-audio-preview"
        )
        val message = ChatMessage(
            role = "user",
            content = "listen",
            attachments = listOf(
                DesktopAttachment("voice.wav", "audio/wav", "BAUG", kind = DesktopAttachmentKind.AUDIO)
            )
        )

        val issue = config.validateAttachments(listOf(message)).singleOrNull()

        assertNotNull(issue)
        assertTrue(issue.reason.contains("OPENAI_RESPONSES"))
    }
}

package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnthropicMessagesAdapterTest {
    @Test
    fun configuresAnthropicEndpointAndAuthenticationHeaders() {
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.ANTHROPIC_MESSAGES,
            baseUrl = "https://api.anthropic.com/v1/",
            apiKey = "secret"
        )
        val builder = Request.Builder().url(AnthropicMessagesAdapter.chatEndpoint(config))

        AnthropicMessagesAdapter.configureRequest(builder, config)
        val request = builder.build()

        assertEquals("https://api.anthropic.com/v1/messages", request.url.toString())
        assertEquals("secret", request.header("x-api-key"))
        assertEquals("2023-06-01", request.header("anthropic-version"))
        assertEquals(null, request.header("Authorization"))
    }

    @Test
    fun serializesImagesPdfAndToolHistoryAsAnthropicBlocks() {
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.ANTHROPIC_MESSAGES,
            model = "claude-sonnet-4-5",
            systemPrompt = "base system",
            localTools = setOf(DesktopLocalTool.CURRENT_TIME)
        )
        val image = DesktopAttachment("photo.png", "image/png", "AQID", isImage = true)
        val pdf = DesktopAttachment(
            "report.pdf",
            "text/plain",
            "extracted",
            rawData = "JVBERg==",
            rawMimeType = "application/pdf"
        )
        val messages = listOf(
            ChatMessage("system", "injected system"),
            ChatMessage("user", "inspect", attachments = listOf(image, pdf)),
            ChatMessage(
                "assistant",
                "checking",
                reasoning = "need time",
                reasoningSignature = "signed-thinking",
                toolCalls = listOf(DesktopToolCall("toolu_1", DesktopCurrentTimeToolName, "{}"))
            ),
            ChatMessage("tool", "12:00", toolCallId = "toolu_1")
        )

        val body = Json.parseToJsonElement(AnthropicMessagesAdapter.buildRequestBody(config, messages)).jsonObject
        val requestMessages = body.getValue("messages").jsonArray
        val userBlocks = requestMessages[0].jsonObject.getValue("content").jsonArray
        val assistantBlocks = requestMessages[1].jsonObject.getValue("content").jsonArray
        val resultBlock = requestMessages[2].jsonObject.getValue("content").jsonArray.single().jsonObject

        assertEquals("base system\n\ninjected system", body.getValue("system").jsonPrimitive.content)
        assertEquals(listOf("user", "assistant", "user"), requestMessages.map {
            it.jsonObject.getValue("role").jsonPrimitive.content
        })
        assertEquals(listOf("text", "image", "document"), userBlocks.map {
            it.jsonObject.getValue("type").jsonPrimitive.content
        })
        assertEquals(
            "AQID",
            userBlocks[1].jsonObject.getValue("source").jsonObject.getValue("data").jsonPrimitive.content
        )
        assertEquals(
            "JVBERg==",
            userBlocks[2].jsonObject.getValue("source").jsonObject.getValue("data").jsonPrimitive.content
        )
        assertEquals(listOf("thinking", "text", "tool_use"), assistantBlocks.map {
            it.jsonObject.getValue("type").jsonPrimitive.content
        })
        assertEquals(
            "signed-thinking",
            assistantBlocks[0].jsonObject.getValue("signature").jsonPrimitive.content
        )
        assertEquals("toolu_1", resultBlock.getValue("tool_use_id").jsonPrimitive.content)
        assertEquals(
            DesktopCurrentTimeToolName,
            body.getValue("tools").jsonArray.single().jsonObject.getValue("name").jsonPrimitive.content
        )
    }

    @Test
    fun mergesAdjacentUserContentAndEnablesExtendedThinking() {
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.ANTHROPIC_MESSAGES,
            model = "claude-sonnet-4-5",
            reasoningEffort = "medium",
            maxTokens = 2_000
        )
        val body = Json.parseToJsonElement(
            AnthropicMessagesAdapter.buildRequestBody(
                config,
                listOf(ChatMessage("user", "first"), ChatMessage("user", "second"))
            )
        ).jsonObject

        assertEquals(1, body.getValue("messages").jsonArray.size)
        assertEquals(2, body.getValue("messages").jsonArray.single().jsonObject.getValue("content").jsonArray.size)
        assertEquals(4_096, body.getValue("thinking").jsonObject.getValue("budget_tokens").jsonPrimitive.int)
        assertEquals(5_120, body.getValue("max_tokens").jsonPrimitive.int)
        assertEquals(1.0, body.getValue("temperature").jsonPrimitive.content.toDouble())
        assertFalse("top_p" in body)
    }

    @Test
    fun serializesNativeAnthropicWebSearchTool() {
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.ANTHROPIC_MESSAGES,
            model = "claude-sonnet-4-5",
            webSearchEnabled = true,
            webSearchSettings = DesktopWebSearchSettings(resultCount = 3)
        )

        val body = Json.parseToJsonElement(AnthropicMessagesAdapter.buildRequestBody(config, emptyList())).jsonObject
        val tool = body.getValue("tools").jsonArray.single().jsonObject

        assertEquals("web_search_20250305", tool.getValue("type").jsonPrimitive.content)
        assertEquals(3, tool.getValue("max_uses").jsonPrimitive.int)
    }

    @Test
    fun parsesAnthropicStreamingTextThinkingToolsAndUsage() {
        val start = AnthropicMessagesAdapter.parseStreamEvent(
            """{"type":"message_start","message":{"model":"claude-sonnet-4-5","usage":{"input_tokens":12,"cache_read_input_tokens":8}}}"""
        )
        val text = AnthropicMessagesAdapter.parseStreamEvent(
            """{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"hello"}}"""
        )
        val thinking = AnthropicMessagesAdapter.parseStreamEvent(
            """{"type":"content_block_delta","index":1,"delta":{"type":"thinking_delta","thinking":"checking"}}"""
        )
        val signature = AnthropicMessagesAdapter.parseStreamEvent(
            """{"type":"content_block_delta","index":1,"delta":{"type":"signature_delta","signature":"signed-thinking"}}"""
        )
        val tool = AnthropicMessagesAdapter.parseStreamEvent(
            """{"type":"content_block_start","index":2,"content_block":{"type":"tool_use","id":"toolu_1","name":"current_time","input":{}}}"""
        )
        val arguments = AnthropicMessagesAdapter.parseStreamEvent(
            """{"type":"content_block_delta","index":2,"delta":{"type":"input_json_delta","partial_json":"{}"}}"""
        )
        val citation = AnthropicMessagesAdapter.parseStreamEvent(
            """{"type":"content_block_delta","index":0,"delta":{"type":"citations_delta","citation":{"type":"web_search_result_location","url":"https://example.com","title":"Source"}}}"""
        )
        val usage = AnthropicMessagesAdapter.parseStreamEvent(
            """{"type":"message_delta","usage":{"output_tokens":4}}"""
        )

        assertEquals("claude-sonnet-4-5", start?.modelId)
        assertEquals(12, start?.promptTokens)
        assertEquals(8, start?.cachedTokens)
        assertEquals("hello", text?.content)
        assertEquals("checking", thinking?.reasoning)
        assertEquals("signed-thinking", signature?.reasoningSignature)
        val indexes = mutableMapOf<Int, Int>()
        val calls = emptyList<DesktopToolCall>()
            .merge(checkNotNull(tool).normalizeProviderToolCallIndexes(indexes).toolCallDeltas)
            .merge(checkNotNull(arguments).normalizeProviderToolCallIndexes(indexes).toolCallDeltas)
        assertEquals(listOf(DesktopToolCall("toolu_1", "current_time", "{}")), calls)
        assertEquals(listOf(DesktopCitation("https://example.com", "Source")), citation?.citations)
        assertEquals(4, usage?.completionTokens)
    }

    @Test
    fun parsesCompleteAnthropicResponseWithCitationAndToolCall() {
        val payload = """
            {
              "model":"claude-sonnet-4-5",
              "content":[
                {"type":"thinking","thinking":"checked","signature":"signed-thinking"},
                {"type":"text","text":"answer","citations":[
                  {"type":"web_search_result_location","url":"https://example.com","title":"Source"}
                ]},
                {"type":"tool_use","id":"toolu_1","name":"current_time","input":{}}
              ],
              "usage":{"input_tokens":10,"output_tokens":3,"cache_read_input_tokens":5}
            }
        """.trimIndent()

        val result = AnthropicMessagesAdapter.parseCompleteResponse(payload)

        assertEquals("answer", result.content)
        assertEquals("checked", result.reasoning)
        assertEquals("signed-thinking", result.reasoningSignature)
        assertEquals("claude-sonnet-4-5", result.modelId)
        assertEquals(10, result.promptTokens)
        assertEquals(3, result.completionTokens)
        assertEquals(5, result.cachedTokens)
        assertEquals(listOf(DesktopCitation("https://example.com", "Source")), result.citations)
        assertEquals("toolu_1", result.toolCallDeltas.single().id)
        assertEquals("{}", result.toolCallDeltas.single().arguments)
    }

    @Test
    fun rejectsAudioAndParsesAnthropicErrors() {
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.ANTHROPIC_MESSAGES,
            model = "claude-sonnet-4-5"
        )
        val message = ChatMessage(
            "user",
            "listen",
            attachments = listOf(
                DesktopAttachment("voice.wav", "audio/wav", "BAUG", kind = DesktopAttachmentKind.AUDIO)
            )
        )

        val issue = config.validateAttachments(listOf(message)).singleOrNull()

        assertNotNull(issue)
        assertTrue(issue.reason.contains("ANTHROPIC_MESSAGES"))
        assertEquals(
            "invalid request",
            AnthropicMessagesAdapter.parseError(
                """{"type":"error","error":{"type":"invalid_request_error","message":"invalid request"}}"""
            )
        )
    }
}

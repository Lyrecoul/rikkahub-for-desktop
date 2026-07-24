package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenAiClientTest {
    private val client = OpenAiClient()

    @Test
    fun parsesTextDelta() {
        val payload = """{"choices":[{"delta":{"content":"hello"}}]}"""

        assertEquals(StreamDelta(content = "hello"), client.parseDelta(payload))
    }

    @Test
    fun ignoresInvalidAndEmptyEvents() {
        assertNull(client.parseDelta("not-json"))
        assertEquals(StreamDelta(), client.parseDelta("""{"choices":[{"delta":{}}]}"""))
    }

    @Test
    fun parsesReasoningDeltaFromCompatibleProviders() {
        val payload = """{"choices":[{"delta":{"reasoning_content":"checking"}}]}"""

        assertEquals(StreamDelta(reasoning = "checking"), client.parseDelta(payload))
    }

    @Test
    fun parsesFinalStreamingUsageChunk() {
        val payload = """{"choices":[],"usage":{"prompt_tokens":120,"completion_tokens":45,"total_tokens":165}}"""

        assertEquals(
            StreamDelta(promptTokens = 120, completionTokens = 45),
            client.parseDelta(payload)
        )
    }

    @Test
    fun parsesStreamingUrlCitations() {
        val payload = """
            {
              "choices": [{
                "delta": {
                  "annotations": [{
                    "type": "url_citation",
                    "url_citation": {"title": "RikkaHub", "url": "https://example.com/source"}
                  }]
                }
              }]
            }
        """.trimIndent()

        assertEquals(
            listOf(DesktopCitation("https://example.com/source", "RikkaHub")),
            client.parseDelta(payload)?.citations
        )
    }

    @Test
    fun parsesStreamingToolCallFragments() {
        val first = client.parseDelta(
            """{"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_1","function":{"name":"web_search","arguments":"{\"query\":\""}}]}}]}"""
        )
        val second = client.parseDelta(
            """{"choices":[{"delta":{"tool_calls":[{"index":0,"function":{"arguments":"weather\"}"}}]}}]}"""
        )

        val calls = emptyList<DesktopToolCall>().merge(first!!.toolCallDeltas).merge(second!!.toolCallDeltas)

        assertEquals(listOf(DesktopToolCall("call_1", "web_search", "{\"query\":\"weather\"}")), calls)
    }

    @Test
    fun parsesCompleteNonStreamingResponse() {
        val payload = """
            {
              "choices": [{"message": {
                "content": "answer",
                "reasoning_content": "thinking",
                "annotations": [{
                  "url_citation": {"title": "Source", "url": "https://example.com"}
                }]
              }}],
              "usage": {"prompt_tokens": 10, "completion_tokens": 4}
            }
        """.trimIndent()

        assertEquals(
            StreamDelta(
                content = "answer",
                reasoning = "thinking",
                promptTokens = 10,
                completionTokens = 4,
                citations = listOf(DesktopCitation("https://example.com", "Source"))
            ),
            client.parseCompleteResponse(payload)
        )
    }

    @Test
    fun nonStreamingRequestOmitsStreamingOptions() {
        val body = Json.parseToJsonElement(
            client.buildRequestBody(
                DesktopConfig(model = "test", streamOutput = false, requestTokenUsage = true),
                emptyList()
            )
        ).jsonObject

        assertEquals(false, body.getValue("stream").jsonPrimitive.boolean)
        assertTrue("stream_options" !in body)
    }

    @Test
    fun parsesErrorEvent() {
        val payload = """{"error":{"message":"rate limited"}}"""

        assertEquals("rate limited", client.parseError(payload))
    }

    @Test
    fun includesGenerationSettingsInRequestBody() {
        val config = DesktopConfig(
            model = "test-model",
            systemPrompt = "system",
            temperature = 0.7,
            topP = 0.85,
            reasoningEffort = "medium",
            maxTokens = 2048,
            requestTokenUsage = true
        )

        val body = Json.parseToJsonElement(
            client.buildRequestBody(config, listOf(ChatMessage("user", "hello")))
        ).jsonObject

        assertEquals(0.7, body.getValue("temperature").jsonPrimitive.double)
        assertEquals(0.85, body.getValue("top_p").jsonPrimitive.double)
        assertEquals("medium", body.getValue("reasoning_effort").jsonPrimitive.content)
        assertEquals(2048, body.getValue("max_tokens").jsonPrimitive.int)
        assertEquals(
            true,
            body.getValue("stream_options").jsonObject.getValue("include_usage").jsonPrimitive.boolean
        )
    }

    @Test
    fun parsesAndSortsProviderModels() {
        val payload = """{"data":[{"id":"model-z"},{"id":"model-a"}]}"""

        assertEquals(listOf("model-a", "model-z"), client.parseModels(payload))
    }

    @Test
    fun recursivelyMergesCustomJsonBodies() {
        val config = DesktopConfig(
            model = "test-model",
            customBodies = listOf(
                DesktopCustomBody("metadata", """{"source":"desktop","nested":{"first":true}}"""),
                DesktopCustomBody("metadata", """{"nested":{"second":true}}""")
            )
        )

        val body = Json.parseToJsonElement(client.buildRequestBody(config, emptyList())).jsonObject
        val metadata = body.getValue("metadata").jsonObject
        val nested = metadata.getValue("nested").jsonObject

        assertEquals("desktop", metadata.getValue("source").jsonPrimitive.content)
        assertEquals(true, nested.getValue("first").jsonPrimitive.boolean)
        assertEquals(true, nested.getValue("second").jsonPrimitive.boolean)
    }

    @Test
    fun serializesImageAndTextAttachmentsAsMultimodalContent() {
        val message = ChatMessage(
            role = "user",
            content = "Inspect these files",
            attachments = listOf(
                DesktopAttachment("image.png", "image/png", "AQID", isImage = true),
                DesktopAttachment("notes.txt", "text/plain", "hello")
            )
        )

        val body = Json.parseToJsonElement(
            client.buildRequestBody(DesktopConfig(model = "test-model"), listOf(message))
        ).jsonObject
        val content = body.getValue("messages").jsonArray.last().jsonObject
            .getValue("content").jsonArray

        assertEquals(listOf("text", "image_url", "text"), content.map {
            it.jsonObject.getValue("type").jsonPrimitive.content
        })
        assertEquals(
            "data:image/png;base64,AQID",
            content[1].jsonObject.getValue("image_url").jsonObject.getValue("url").jsonPrimitive.content
        )
        assertTrue(content[2].jsonObject.getValue("text").jsonPrimitive.content.contains("notes.txt"))
    }

    @Test
    fun includesWebSearchOptionsOnlyWhenEnabled() {
        val enabled = Json.parseToJsonElement(
            client.buildRequestBody(DesktopConfig(model = "test", webSearchEnabled = true), emptyList())
        ).jsonObject
        val disabled = Json.parseToJsonElement(
            client.buildRequestBody(DesktopConfig(model = "test"), emptyList())
        ).jsonObject

        assertTrue("web_search_options" in enabled)
        assertTrue("web_search_options" !in disabled)
    }

    @Test
    fun configuredExternalSearchUsesAFunctionToolInsteadOfTailSystemMessages() {
        val body = Json.parseToJsonElement(
            client.buildRequestBody(
                DesktopConfig(
                    model = "test",
                    webSearchEnabled = true,
                    webSearchSettings = DesktopWebSearchSettings(searxngUrl = "https://search.example.com")
                ),
                listOf(ChatMessage("user", "latest news"))
            )
        ).jsonObject

        assertEquals(
            DesktopWebSearchToolName,
            body.getValue("tools").jsonArray[0].jsonObject.getValue("function").jsonObject
                .getValue("name").jsonPrimitive.content
        )
        assertEquals(listOf("system", "user"), body.getValue("messages").jsonArray.map {
            it.jsonObject.getValue("role").jsonPrimitive.content
        })
        assertTrue("web_search_options" !in body)
    }

    @Test
    fun declaresOnlyEnabledLocalTools() {
        val body = Json.parseToJsonElement(
            client.buildRequestBody(
                DesktopConfig(model = "test", localTools = setOf(DesktopLocalTool.CURRENT_TIME)),
                emptyList()
            )
        ).jsonObject

        assertEquals(
            listOf(DesktopCurrentTimeToolName),
            body.getValue("tools").jsonArray.map {
                it.jsonObject.getValue("function").jsonObject.getValue("name").jsonPrimitive.content
            }
        )
    }

    @Test
    fun serializesToolCallsAndResultsUsingChatCompletionsFields() {
        val body = Json.parseToJsonElement(
            client.buildRequestBody(
                DesktopConfig(model = "test", systemPrompt = ""),
                listOf(
                    ChatMessage(
                        role = "assistant",
                        content = "",
                        toolCalls = listOf(DesktopToolCall("call_1", DesktopCurrentTimeToolName))
                    ),
                    ChatMessage(role = "tool", content = "2026-07-24T12:00:00Z", toolCallId = "call_1")
                )
            )
        ).jsonObject
        val messages = body.getValue("messages").jsonArray

        assertTrue("content" !in messages[0].jsonObject)
        assertEquals("call_1", messages[0].jsonObject.getValue("tool_calls").jsonArray[0].jsonObject
            .getValue("id").jsonPrimitive.content)
        assertEquals("call_1", messages[1].jsonObject.getValue("tool_call_id").jsonPrimitive.content)
    }

    @Test
    fun omitsNativeWebSearchOptionsWhenSearxngIsConfigured() {
        val body = Json.parseToJsonElement(
            client.buildRequestBody(
                DesktopConfig(
                    model = "test",
                    webSearchEnabled = true,
                    webSearchSettings = DesktopWebSearchSettings(searxngUrl = "https://search.example.com")
                ),
                emptyList()
            )
        ).jsonObject

        assertTrue("web_search_options" !in body)
    }

    @Test
    fun evaluatesProviderBalanceExpressions() {
        val response = Json.parseToJsonElement(
            """{"data":{"total_credits":12.5,"total_usage":2.25},"balance_infos":[{"total_balance":8}],"currency":"USD"}"""
        ).jsonObject

        assertEquals("10.25", evaluateDesktopJsonExpression("data.total_credits - data.total_usage", response))
        assertEquals("20.5", evaluateDesktopJsonExpression("(data.total_credits - data.total_usage) * 2", response))
        assertEquals("8", evaluateDesktopJsonExpression("balance_infos[0].total_balance", response))
        assertEquals("USD 10.25", evaluateDesktopJsonExpression("currency ++ \" \" ++ (data.total_credits - data.total_usage)", response))
    }
}

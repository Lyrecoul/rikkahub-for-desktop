package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeminiGenerateContentAdapterTest {
    @Test
    fun configuresGeminiEndpointsAndAuthentication() {
        val streaming = DesktopConfig(
            protocol = DesktopProviderProtocol.GEMINI_GENERATE_CONTENT,
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/",
            apiKey = "secret",
            model = "models/gemini-2.5-flash",
            streamOutput = true
        )
        val builder = Request.Builder().url(GeminiGenerateContentAdapter.chatEndpoint(streaming))
        GeminiGenerateContentAdapter.configureRequest(builder, streaming)

        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse",
            builder.build().url.toString()
        )
        assertEquals("secret", builder.build().header("x-goog-api-key"))
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models",
            GeminiGenerateContentAdapter.modelsEndpoint(streaming)
        )
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
            GeminiGenerateContentAdapter.chatEndpoint(streaming.copy(streamOutput = false))
        )
        assertFails { GeminiGenerateContentAdapter.chatEndpoint(streaming.copy(model = "../bad")) }
    }

    @Test
    fun serializesNativeAttachmentsToolsAndFunctionResults() {
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.GEMINI_GENERATE_CONTENT,
            model = "gemini-2.5-flash",
            systemPrompt = "base system",
            localTools = setOf(DesktopLocalTool.CURRENT_TIME)
        )
        val image = DesktopAttachment("photo.png", "image/png", "AQID", isImage = true)
        val audio = DesktopAttachment(
            "voice.m4a",
            "audio/mp4",
            "BAUG",
            kind = DesktopAttachmentKind.AUDIO,
            audioFormat = "m4a"
        )
        val pdf = DesktopAttachment(
            "report.pdf",
            "text/plain",
            "extracted",
            rawData = "JVBERg==",
            rawMimeType = "application/pdf"
        )
        val messages = listOf(
            ChatMessage("system", "injected system"),
            ChatMessage("user", "inspect", attachments = listOf(image, audio, pdf)),
            ChatMessage(
                "assistant",
                "checking",
                reasoningSignature = "signed-thought",
                toolCalls = listOf(DesktopToolCall("call_1", DesktopCurrentTimeToolName, "{}"))
            ),
            ChatMessage("tool", "12:00", toolCallId = "call_1")
        )

        val body = Json.parseToJsonElement(GeminiGenerateContentAdapter.buildRequestBody(config, messages)).jsonObject
        val contents = body.getValue("contents").jsonArray
        val userParts = contents[0].jsonObject.getValue("parts").jsonArray
        val modelParts = contents[1].jsonObject.getValue("parts").jsonArray
        val response = contents[2].jsonObject.getValue("parts").jsonArray.single().jsonObject

        assertEquals(
            listOf("base system", "injected system"),
            body.getValue("systemInstruction").jsonObject.getValue("parts").jsonArray.map {
                it.jsonObject.getValue("text").jsonPrimitive.content
            }
        )
        assertEquals(listOf("user", "model", "user"), contents.map {
            it.jsonObject.getValue("role").jsonPrimitive.content
        })
        assertEquals(4, userParts.size)
        assertEquals("image/png", userParts[1].jsonObject.getValue("inlineData").jsonObject
            .getValue("mimeType").jsonPrimitive.content)
        assertEquals("audio/mp4", userParts[2].jsonObject.getValue("inlineData").jsonObject
            .getValue("mimeType").jsonPrimitive.content)
        assertEquals("application/pdf", userParts[3].jsonObject.getValue("inlineData").jsonObject
            .getValue("mimeType").jsonPrimitive.content)
        assertEquals("signed-thought", modelParts[1].jsonObject.getValue("thoughtSignature").jsonPrimitive.content)
        assertEquals("12:00", response.getValue("functionResponse").jsonObject
            .getValue("response").jsonObject.getValue("result").jsonPrimitive.content)
        val declaration = body.getValue("tools").jsonArray.single().jsonObject
            .getValue("functionDeclarations").jsonArray.single().jsonObject
        assertEquals(DesktopCurrentTimeToolName, declaration.getValue("name").jsonPrimitive.content)
        assertFalse(declaration.getValue("parameters").jsonObject.containsKey("additionalProperties"))
    }

    @Test
    fun fallsBackToExtractedTextForNonPdfDocuments() {
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.GEMINI_GENERATE_CONTENT,
            model = "gemini-2.5-flash"
        )
        val document = DesktopAttachment(
            "report.docx",
            "text/plain",
            "extracted document",
            rawData = "UEsDBA==",
            rawMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )

        val body = Json.parseToJsonElement(
            GeminiGenerateContentAdapter.buildRequestBody(
                config,
                listOf(ChatMessage("user", "", attachments = listOf(document)))
            )
        ).jsonObject
        val part = body.getValue("contents").jsonArray.single().jsonObject
            .getValue("parts").jsonArray.single().jsonObject

        assertTrue(part.getValue("text").jsonPrimitive.content.contains("extracted document"))
        assertFalse("inlineData" in part)
    }

    @Test
    fun serializesGoogleSearchAndThinkingConfiguration() {
        val config = DesktopConfig(
            protocol = DesktopProviderProtocol.GEMINI_GENERATE_CONTENT,
            model = "gemini-2.5-flash",
            reasoningEffort = "medium",
            maxTokens = 2_048,
            webSearchEnabled = true
        )

        val body = Json.parseToJsonElement(GeminiGenerateContentAdapter.buildRequestBody(config, emptyList())).jsonObject

        assertTrue("googleSearch" in body.getValue("tools").jsonArray.single().jsonObject)
        val generation = body.getValue("generationConfig").jsonObject
        assertEquals(5_120, generation.getValue("maxOutputTokens").jsonPrimitive.int)
        assertEquals(4_096, generation.getValue("thinkingConfig").jsonObject
            .getValue("thinkingBudget").jsonPrimitive.int)
    }

    @Test
    fun parsesGeminiTextThoughtToolsUsageAndGrounding() {
        val payload = """
            {
              "modelVersion":"gemini-2.5-flash-001",
              "candidates":[{
                "content":{"role":"model","parts":[
                  {"thought":true,"text":"checked","thoughtSignature":"signed-thought"},
                  {"text":"answer"},
                  {"functionCall":{"id":"call_1","name":"current_time","args":{}}}
                ]},
                "groundingMetadata":{"groundingChunks":[
                  {"web":{"uri":"https://example.com","title":"Source"}}
                ]}
              }],
              "usageMetadata":{
                "promptTokenCount":10,
                "candidatesTokenCount":3,
                "cachedContentTokenCount":5
              }
            }
        """.trimIndent()

        val result = GeminiGenerateContentAdapter.parseResponse(payload)

        assertEquals("answer", result.content)
        assertEquals("checked", result.reasoning)
        assertEquals("signed-thought", result.reasoningSignature)
        assertEquals("gemini-2.5-flash-001", result.modelId)
        assertEquals(10, result.promptTokens)
        assertEquals(3, result.completionTokens)
        assertEquals(5, result.cachedTokens)
        assertEquals(listOf(DesktopCitation("https://example.com", "Source")), result.citations)
        assertEquals("call_1", result.toolCallDeltas.single().id)
        assertEquals("{}", result.toolCallDeltas.single().arguments)
    }

    @Test
    fun parsesGeminiInlineGeneratedImages() {
        val result = GeminiGenerateContentAdapter.parseResponse(
            """{"candidates":[{"content":{"parts":[
                {"text":"done"},
                {"inlineData":{"mimeType":"image/webp","data":"AQID"}}
            ]}}]}"""
        )

        assertEquals("done", result.content)
        assertEquals("image/webp", result.attachments.single().mimeType)
        assertEquals("AQID", result.attachments.single().data)
    }

    @Test
    fun createsStableToolIdWhenGeminiOmitsFunctionCallId() {
        val result = GeminiGenerateContentAdapter.parseResponse(
            """{"candidates":[{"content":{"parts":[{"text":"before"},{"functionCall":{"name":"current_time","args":{}}}]}}]}"""
        )

        assertEquals("gemini-function-1", result.toolCallDeltas.single().id)
    }

    @Test
    fun parsesOnlyGenerateContentModelsAndGeminiErrors() {
        val payload = """
            {"models":[
              {"name":"models/gemini-2.5-flash","supportedGenerationMethods":["generateContent"]},
              {"name":"models/text-embedding-004","supportedGenerationMethods":["embedContent"]}
            ]}
        """.trimIndent()

        assertEquals(listOf("gemini-2.5-flash"), GeminiGenerateContentAdapter.parseModels(payload))
        assertEquals(
            "invalid request",
            GeminiGenerateContentAdapter.parseError(
                """{"error":{"code":400,"message":"invalid request","status":"INVALID_ARGUMENT"}}"""
            )
        )
    }
}

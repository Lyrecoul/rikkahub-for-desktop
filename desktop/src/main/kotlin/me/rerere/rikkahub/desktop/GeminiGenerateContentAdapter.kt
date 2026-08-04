package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Request

internal object GeminiGenerateContentAdapter : DesktopChatProviderAdapter {
    private val json = Json { ignoreUnknownKeys = true }
    private val modelIdPattern = Regex("[A-Za-z0-9._-]+")

    override fun chatEndpoint(config: DesktopConfig): String {
        val model = config.model.removePrefix("models/")
        require(model.matches(modelIdPattern)) { "Invalid Gemini model id" }
        val action = if (config.streamOutput) "streamGenerateContent?alt=sse" else "generateContent"
        return "${config.baseUrl.trimEnd('/')}/models/$model:$action"
    }

    override fun configureRequest(builder: Request.Builder, config: DesktopConfig) {
        builder.header("x-goog-api-key", config.apiKey)
    }

    override fun buildRequestBody(config: DesktopConfig, messages: List<ChatMessage>): String {
        val systemParts = buildList {
            if (config.systemPrompt.isNotBlank()) add(config.systemPrompt)
            messages.filter { it.role == "system" && it.content.isNotBlank() }.forEach { add(it.content) }
        }
        val contents = buildGeminiContents(config, messages.filterNot { it.role == "system" })
        val thinkingBudget = thinkingBudget(config.reasoningEffort)
        val maxOutputTokens = config.maxTokens.takeIf { it > 0 }?.let { configured ->
            maxOf(configured, (thinkingBudget ?: 0) + 1_024)
        }
        val base = buildJsonObject {
            if (systemParts.isNotEmpty()) {
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        systemParts.forEach { text -> add(buildJsonObject { put("text", text) }) }
                    }
                }
            }
            putJsonArray("contents") { contents.forEach(::add) }
            putJsonObject("generationConfig") {
                put("temperature", config.temperature)
                put("topP", config.topP)
                maxOutputTokens?.let { put("maxOutputTokens", it) }
                thinkingBudget?.let { budget ->
                    putJsonObject("thinkingConfig") {
                        put("thinkingBudget", budget)
                        put("includeThoughts", true)
                    }
                }
            }
            val tools = buildGeminiTools(config)
            if (tools.isNotEmpty()) put("tools", tools)
        }
        return mergeDesktopCustomBodies(base, config.customBodies, json).toString()
    }

    private fun buildGeminiContents(config: DesktopConfig, messages: List<ChatMessage>): List<JsonObject> {
        val result = mutableListOf<GeminiRequestContent>()
        messages.forEach { message ->
            val role = if (message.role == "assistant") "model" else "user"
            val parts = buildList {
                if (message.role == "tool") {
                    val callName = findToolCallName(messages, message.toolCallId)
                    add(buildJsonObject {
                        putJsonObject("functionResponse") {
                            message.toolCallId?.let { put("id", it) }
                            put("name", callName.ifBlank { "tool" })
                            putJsonObject("response") { put("result", message.content) }
                        }
                    })
                } else {
                    if (message.content.isNotBlank()) add(buildJsonObject { put("text", message.content) })
                    message.attachments.forEach { attachment ->
                        when (attachment.kind) {
                            DesktopAttachmentKind.IMAGE, DesktopAttachmentKind.AUDIO -> add(buildJsonObject {
                                putJsonObject("inlineData") {
                                    put("mimeType", attachment.mimeType)
                                    put("data", attachment.data)
                                }
                            })

                            DesktopAttachmentKind.FILE -> if (config.canSendNativeDocument(attachment)) {
                                add(buildJsonObject {
                                    putJsonObject("inlineData") {
                                        put("mimeType", attachment.rawMimeType.orEmpty())
                                        put("data", attachment.rawData.orEmpty())
                                    }
                                })
                            } else {
                                add(buildJsonObject { put("text", "File: ${attachment.name}\n${attachment.data}") })
                            }
                        }
                    }
                    message.toolCalls.forEachIndexed { index, call ->
                        add(buildJsonObject {
                            putJsonObject("functionCall") {
                                put("id", call.id)
                                put("name", call.name)
                                put("args", parseArguments(call.arguments))
                            }
                            if (index == 0 && message.reasoningSignature.isNotBlank()) {
                                put("thoughtSignature", message.reasoningSignature)
                            }
                        })
                    }
                }
            }
            if (parts.isEmpty()) return@forEach
            val previous = result.lastOrNull()
            if (previous?.role == role) {
                previous.parts += parts
            } else {
                result += GeminiRequestContent(role, parts.toMutableList())
            }
        }
        return result.map { content ->
            buildJsonObject {
                put("role", content.role)
                put("parts", JsonArray(content.parts))
            }
        }
    }

    private fun findToolCallName(messages: List<ChatMessage>, callId: String?): String = messages.asReversed()
        .asSequence()
        .flatMap { it.toolCalls.asSequence() }
        .firstOrNull { it.id == callId }
        ?.name.orEmpty()

    private fun parseArguments(arguments: String): JsonObject = runCatching {
        json.parseToJsonElement(arguments) as? JsonObject
    }.getOrNull() ?: JsonObject(emptyMap())

    private fun buildGeminiTools(config: DesktopConfig) = buildJsonArray {
        if (config.webSearchEnabled && !config.webSearchSettings.isConfigured) {
            add(buildJsonObject { putJsonObject("googleSearch") {} })
        }
        val declarations = buildDesktopToolDefinitions(config).mapNotNull { chatTool ->
            val function = chatTool.jsonObject["function"]?.jsonObject ?: return@mapNotNull null
            buildJsonObject {
                function["name"]?.let { put("name", it) }
                function["description"]?.let { put("description", it) }
                function["parameters"]?.let { put("parameters", sanitizeGeminiSchema(it)) }
            }
        }
        if (declarations.isNotEmpty()) {
            add(buildJsonObject { put("functionDeclarations", JsonArray(declarations)) })
        }
    }

    private fun sanitizeGeminiSchema(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.mapNotNull { (key, value) ->
            if (key in setOf("additionalProperties", "strict", "${'$'}schema")) null
            else key to sanitizeGeminiSchema(value)
        }.toMap())

        is JsonArray -> JsonArray(element.map(::sanitizeGeminiSchema))
        else -> element
    }

    internal fun parseResponse(data: String): StreamDelta {
        val response = json.parseToJsonElement(data).jsonObject
        parseError(data)?.let { error(it) }
        val candidate = response["candidates"]?.jsonArray.orEmpty().firstOrNull()?.jsonObject
        val parts = candidate?.get("content")?.jsonObject?.get("parts")?.jsonArray.orEmpty()
        val content = StringBuilder()
        val reasoning = StringBuilder()
        val signature = StringBuilder()
        val calls = mutableListOf<DesktopToolCallDelta>()
        parts.forEachIndexed { partIndex, element ->
            val part = element.jsonObject
            val text = part["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (part["thought"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() == true) {
                reasoning.append(text)
            } else {
                content.append(text)
            }
            signature.append(part["thoughtSignature"]?.jsonPrimitive?.contentOrNull.orEmpty())
            part["functionCall"]?.jsonObject?.let { call ->
                calls += DesktopToolCallDelta(
                    index = partIndex,
                    id = call["id"]?.jsonPrimitive?.contentOrNull ?: "gemini-function-$partIndex",
                    name = call["name"]?.jsonPrimitive?.contentOrNull,
                    arguments = call["args"]?.toString() ?: "{}"
                )
            }
        }
        val usage = response["usageMetadata"]?.jsonObject
        return StreamDelta(
            content = content.toString(),
            reasoning = reasoning.toString(),
            reasoningSignature = signature.toString(),
            modelId = response["modelVersion"]?.jsonPrimitive?.contentOrNull,
            promptTokens = usage?.get("promptTokenCount")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            completionTokens = usage?.get("candidatesTokenCount")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            cachedTokens = usage?.get("cachedContentTokenCount")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            citations = parseGroundingCitations(candidate),
            toolCallDeltas = calls
        )
    }

    internal fun parseError(data: String): String? = runCatching {
        json.parseToJsonElement(data).jsonObject["error"]?.jsonObject
            ?.get("message")?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    internal fun parseModels(data: String): List<String> = runCatching {
        json.parseToJsonElement(data).jsonObject["models"]?.jsonArray.orEmpty().mapNotNull { element ->
            val model = element.jsonObject
            val methods = model["supportedGenerationMethods"]?.jsonArray.orEmpty()
                .mapNotNull { it.jsonPrimitive.contentOrNull }
            model["name"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { "generateContent" in methods }
                ?.removePrefix("models/")
        }.distinct().sorted()
    }.getOrDefault(emptyList())

    private fun parseGroundingCitations(candidate: JsonObject?): List<DesktopCitation> {
        val chunks = candidate?.get("groundingMetadata")?.jsonObject
            ?.get("groundingChunks")?.jsonArray.orEmpty()
        return chunks.mapNotNull { element ->
            val web = element.jsonObject["web"]?.jsonObject ?: return@mapNotNull null
            val url = web["uri"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            DesktopCitation(url, web["title"]?.jsonPrimitive?.contentOrNull.orEmpty())
        }.distinctBy { it.url }
    }

    private fun thinkingBudget(effort: String): Int? = when (effort) {
        "low" -> 1_024
        "medium" -> 4_096
        "high" -> 8_192
        else -> null
    }

    private data class GeminiRequestContent(
        val role: String,
        val parts: MutableList<JsonObject>
    )
}

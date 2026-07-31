package me.rerere.rikkahub.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resumeWithException

internal data class StreamDelta(
    val content: String = "",
    val reasoning: String = "",
    val modelId: String? = null,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val cachedTokens: Int? = null,
    val citations: List<DesktopCitation> = emptyList(),
    val toolCallDeltas: List<DesktopToolCallDelta> = emptyList()
)

class OpenAiClient(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val balanceCache = mutableMapOf<String, DesktopBalanceCacheEntry>()

    internal fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> = flow {
        require(config.apiKey.isNotBlank()) { "Configure an API key first" }
        require(config.model.isNotBlank()) { "Configure a model first" }

        val body = buildRequestBody(config, messages)
        val endpoint = "${config.baseUrl.trimEnd('/')}/chat/completions"
        val requestBuilder = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Accept", if (config.streamOutput) "text/event-stream" else "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
        config.customHeaders.filter { it.name.isNotBlank() }.forEach { header ->
            requestBuilder.addHeader(header.name, header.value)
        }
        val request = requestBuilder.build()
        val call = httpClient.newCall(request)

        call.awaitResponse().use { response ->
            if (!response.isSuccessful) {
                val detail = response.body.string().take(1000)
                error("Request failed (${response.code}): $detail")
            }
            if (!config.streamOutput) {
                emit(parseCompleteResponse(response.body.string()))
                return@use
            }
            val source = response.body.source()
            while (!source.exhausted()) {
                currentCoroutineContext().ensureActive()
                val line = source.readUtf8Line() ?: break
                val data = line.removePrefix("data:").trim()
                if (!line.startsWith("data:") || data.isBlank()) continue
                if (data == "[DONE]") break
                parseError(data)?.let { error(it) }
                parseDelta(data)?.takeUnless {
                    it.content.isEmpty() && it.reasoning.isEmpty() &&
                        it.promptTokens == null && it.completionTokens == null
                        && it.cachedTokens == null && it.modelId == null
                        && it.citations.isEmpty() && it.toolCallDeltas.isEmpty()
                }?.let { emit(it) }
            }
        }
    }.flowOn(Dispatchers.IO)

    internal fun buildRequestBody(config: DesktopConfig, messages: List<ChatMessage>): String {
        val requestMessages = buildList {
            if (config.systemPrompt.isNotBlank()) add(ChatMessage("system", config.systemPrompt))
            addAll(messages)
        }
        val base = buildJsonObject {
            put("model", config.model)
            put("stream", config.streamOutput)
            put("temperature", config.temperature)
            put("top_p", config.topP)
            if (config.reasoningEffort.isNotBlank()) put("reasoning_effort", config.reasoningEffort)
            if (config.maxTokens > 0) put("max_tokens", config.maxTokens)
            if (config.requestTokenUsage && config.streamOutput) {
                putJsonObject("stream_options") { put("include_usage", true) }
            }
            if (config.webSearchEnabled && !config.webSearchSettings.isConfigured) {
                putJsonObject("web_search_options") {}
            }
            buildDesktopToolDefinitions(config).takeIf { it.isNotEmpty() }?.let { put("tools", it) }
            putJsonArray("messages") {
                requestMessages.forEach { message ->
                    add(buildJsonObject {
                        put("role", message.role)
                        message.toolCallId?.let { put("tool_call_id", it) }
                        if (message.toolCalls.isNotEmpty()) {
                            putJsonArray("tool_calls") {
                                message.toolCalls.forEach { toolCall ->
                                    add(buildJsonObject {
                                        put("id", toolCall.id)
                                        put("type", "function")
                                        putJsonObject("function") {
                                            put("name", toolCall.name)
                                            put("arguments", toolCall.arguments)
                                        }
                                    })
                                }
                            }
                        }
                        if (message.attachments.isEmpty() &&
                            (message.toolCalls.isEmpty() || message.content.isNotBlank())
                        ) {
                            put("content", message.content)
                        } else if (message.attachments.isNotEmpty()) {
                            putJsonArray("content") {
                                if (message.content.isNotBlank()) {
                                    add(buildJsonObject {
                                        put("type", "text")
                                        put("text", message.content)
                                    })
                                }
                                message.attachments.forEach { attachment ->
                                    when (attachment.kind) {
                                        DesktopAttachmentKind.IMAGE -> add(buildJsonObject {
                                            put("type", "image_url")
                                            putJsonObject("image_url") {
                                                put("url", "data:${attachment.mimeType};base64,${attachment.data}")
                                            }
                                        })

                                        DesktopAttachmentKind.AUDIO -> add(buildJsonObject {
                                            put("type", "input_audio")
                                            putJsonObject("input_audio") {
                                                put("data", attachment.data)
                                                put("format", if (attachment.mimeType == "audio/wav") "wav" else "mp3")
                                            }
                                        })

                                        DesktopAttachmentKind.FILE -> add(buildJsonObject {
                                            put("type", "text")
                                            put("text", "File: ${attachment.name}\n${attachment.data}")
                                        })
                                    }
                                }
                            }
                        }
                    })
                }
            }
        }
        return mergeCustomBodies(base, config.customBodies).toString()
    }

    internal fun mergeCustomBodies(base: JsonObject, bodies: List<DesktopCustomBody>): JsonObject {
        val result = base.toMutableMap()
        bodies.filter { it.key.isNotBlank() }.forEach { body ->
            val value = json.parseToJsonElement(body.value)
            val existing = result[body.key]
            result[body.key] = if (existing is JsonObject && value is JsonObject) {
                mergeJsonObjects(existing, value)
            } else {
                value
            }
        }
        return JsonObject(result)
    }

    private fun mergeJsonObjects(base: JsonObject, overlay: JsonObject): JsonObject = JsonObject(
        base.toMutableMap().apply {
            overlay.forEach { (key, value) ->
                val existing = this[key]
                this[key] = if (existing is JsonObject && value is JsonObject) {
                    mergeJsonObjects(existing, value)
                } else {
                    value
                }
            }
        }
    )

    internal fun parseDelta(data: String): StreamDelta? = runCatching {
        val event = json.parseToJsonElement(data).jsonObject
        val delta = event["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("delta")
            ?.jsonObject
        val usage = event["usage"]?.jsonObject
        val citations = delta?.get("annotations")?.jsonArray.orEmpty().mapNotNull { annotation ->
            runCatching {
                val citation = annotation.jsonObject["url_citation"]?.jsonObject ?: return@runCatching null
                val url = citation["url"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
                DesktopCitation(
                    url = url,
                    title = citation["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )
            }.getOrNull()
        }
        val toolCalls = delta?.get("tool_calls")?.jsonArray.orEmpty().mapIndexedNotNull { index, item ->
            runCatching {
                val call = item.jsonObject
                val function = call["function"]?.jsonObject ?: return@runCatching null
                DesktopToolCallDelta(
                    index = call["index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: index,
                    id = call["id"]?.jsonPrimitive?.contentOrNull,
                    name = function["name"]?.jsonPrimitive?.contentOrNull,
                    arguments = function["arguments"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )
            }.getOrNull()
        }
        StreamDelta(
            content = delta?.get("content").textContent(),
            reasoning = (
                delta?.get("reasoning_content")?.jsonPrimitive?.contentOrNull
                    ?: delta?.get("reasoning")?.jsonPrimitive?.contentOrNull
                ).orEmpty(),
            modelId = event["model"]?.jsonPrimitive?.contentOrNull,
            promptTokens = usage?.get("prompt_tokens")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            completionTokens = usage?.get("completion_tokens")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            cachedTokens = usage?.cachedTokens(),
            citations = citations,
            toolCallDeltas = toolCalls
        )
    }.getOrNull()

    internal fun parseCompleteResponse(data: String): StreamDelta {
        val event = json.parseToJsonElement(data).jsonObject
        parseError(data)?.let { error(it) }
        val choice = event["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: error("Response did not contain a choice")
        val message = choice["message"]?.jsonObject ?: error("Response did not contain a message")
        val usage = event["usage"]?.jsonObject
        val citations = message["annotations"]?.jsonArray.orEmpty().mapNotNull { annotation ->
            runCatching {
                val citation = annotation.jsonObject["url_citation"]?.jsonObject ?: return@runCatching null
                val url = citation["url"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
                DesktopCitation(url, citation["title"]?.jsonPrimitive?.contentOrNull.orEmpty())
            }.getOrNull()
        }
        val toolCalls = message["tool_calls"]?.jsonArray.orEmpty().mapNotNull { item ->
            runCatching {
                val call = item.jsonObject
                val function = call["function"]?.jsonObject ?: return@runCatching null
                val id = call["id"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
                val name = function["name"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
                DesktopToolCall(id, name, function["arguments"]?.jsonPrimitive?.contentOrNull.orEmpty())
            }.getOrNull()
        }
        return StreamDelta(
            content = message["content"].textContent().ifBlank { choice["text"].textContent() },
            reasoning = (
                message["reasoning_content"]?.jsonPrimitive?.contentOrNull
                    ?: message["reasoning"]?.jsonPrimitive?.contentOrNull
                ).orEmpty(),
            modelId = event["model"]?.jsonPrimitive?.contentOrNull,
            promptTokens = usage?.get("prompt_tokens")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            completionTokens = usage?.get("completion_tokens")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            cachedTokens = usage?.cachedTokens(),
            citations = citations,
            toolCallDeltas = toolCalls.mapIndexed { index, call ->
                DesktopToolCallDelta(index, call.id, call.name, call.arguments)
            }
        )
    }

    /** OpenAI-compatible services may return message content as text parts instead of a string. */
    private fun JsonElement?.textContent(): String = when (this) {
        is JsonPrimitive -> contentOrNull.orEmpty()
        is JsonArray -> joinToString(separator = "") { part -> part.textContent() }
        is JsonObject -> listOf(this["text"], this["content"], this["output_text"])
            .joinToString(separator = "") { part -> part.textContent() }

        null -> ""
    }

    private fun JsonObject.cachedTokens(): Int? = tokenCount("cached_tokens", "cache_read_tokens")
        ?: listOf("prompt_tokens_details", "input_tokens_details").firstNotNullOfOrNull { field ->
            (this[field] as? JsonObject)?.tokenCount("cached_tokens", "cache_read_tokens")
        }

    private fun JsonObject.tokenCount(vararg keys: String): Int? = keys.firstNotNullOfOrNull { key ->
        this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
    }

    internal fun parseError(data: String): String? = runCatching {
        json.parseToJsonElement(data)
            .jsonObject["error"]
            ?.jsonObject
            ?.get("message")
            ?.jsonPrimitive
            ?.contentOrNull
    }.getOrNull()

    internal suspend fun listModels(config: DesktopConfig): List<String> = withContext(Dispatchers.IO) {
        require(config.apiKey.isNotBlank()) { "Configure an API key first" }
        val endpoint = "${config.baseUrl.trimEnd('/')}/models"
        val requestBuilder = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer ${config.apiKey}")
            .get()
        config.customHeaders.filter { it.name.isNotBlank() }.forEach { header ->
            requestBuilder.addHeader(header.name, header.value)
        }
        val request = requestBuilder.build()
        httpClient.newCall(request).awaitResponse().use { response ->
            if (!response.isSuccessful) {
                val detail = response.body.string().take(1000)
                error("Connection failed (${response.code}): $detail")
            }
            parseModels(response.body.string())
        }
    }

    internal suspend fun getBalance(config: DesktopConfig): String = withContext(Dispatchers.IO) {
        require(config.apiKey.isNotBlank()) { "请先配置 API 密钥" }
        val options = config.balanceOptions
        require(options.enabled) { "未启用余额查询" }
        require(options.resultPath.isNotBlank()) { "请配置余额结果 JSON 路径" }
        val endpoint = if (options.apiPath.startsWith("http")) options.apiPath else {
            "${config.baseUrl.trimEnd('/')}/${options.apiPath.trimStart('/')}"
        }
        val request = Request.Builder().url(endpoint)
            .header("Authorization", "Bearer ${config.apiKey}")
            .get().build()
        httpClient.newCall(request).awaitResponse().use { response ->
            if (!response.isSuccessful) error("余额查询失败（${response.code}）：${response.body.string().take(500)}")
            val result = evaluateDesktopJsonExpression(
                input = options.resultPath,
                root = json.parseToJsonElement(response.body.string()).jsonObject
            )
            result.toDoubleOrNull()?.let { "%.2f".format(Locale.ROOT, it) } ?: result
        }
    }

    internal suspend fun getCachedBalance(config: DesktopConfig, forceRefresh: Boolean = false): String {
        val options = config.balanceOptions
        val key =
            listOf(config.baseUrl, config.apiKey.hashCode(), options.apiPath, options.resultPath).joinToString("|")
        val now = System.currentTimeMillis()
        synchronized(balanceCache) {
            balanceCache[key]?.takeIf { !forceRefresh && now - it.fetchedAt < BalanceCacheTtlMillis }
                ?.let { return it.value }
        }
        val value = getBalance(config)
        synchronized(balanceCache) {
            balanceCache[key] = DesktopBalanceCacheEntry(value, now)
            balanceCache.entries.removeIf { now - it.value.fetchedAt >= BalanceCacheTtlMillis }
        }
        return value
    }

    internal fun parseModels(payload: String): List<String> = runCatching {
        val body = json.parseToJsonElement(payload).jsonObject
        body["data"]?.jsonArray.orEmpty().mapNotNull { model ->
            runCatching { model.jsonObject["id"]?.jsonPrimitive?.contentOrNull }.getOrNull()
        }.sorted()
    }.getOrDefault(emptyList())

    internal suspend fun executeToolCalls(
        config: DesktopConfig,
        calls: List<DesktopToolCall>,
        memoryToolHandler: DesktopMemoryToolHandler? = null,
        mcpClient: DesktopMcpClient? = null,
        askUserHandler: (suspend (DesktopToolCall) -> String)? = null,
        agentRuntime: DesktopAgentRuntime = DesktopAgentRuntime(),
        approvalHandler: (suspend (DesktopToolCall, DesktopAgentApprovalRequest) -> Boolean)? = null
    ): List<ChatMessage> = executeDesktopToolCalls(
        httpClient,
        config,
        calls,
        memoryToolHandler,
        mcpClient,
        askUserHandler,
        agentRuntime,
        approvalHandler
    )

    internal suspend fun testWebSearch(settings: DesktopWebSearchSettings, query: String): ChatMessage =
        searchWeb(httpClient, settings, query)
}

private data class DesktopBalanceCacheEntry(val value: String, val fetchedAt: Long)

private const val BalanceCacheTtlMillis = 2 * 60 * 1000L

internal const val DesktopWebSearchToolName = "web_search"
internal const val DesktopCurrentTimeToolName = "current_time"
internal const val DesktopAskUserToolName = "ask_user"

private fun buildDesktopToolDefinitions(config: DesktopConfig) = buildJsonArray {
    if (config.webSearchEnabled && config.webSearchSettings.isConfigured) {
        add(buildJsonObject {
            put("type", "function")
            putJsonObject("function") {
                put("name", DesktopWebSearchToolName)
                put("description", "Search the web for current information and return sources.")
                putJsonObject("parameters") {
                    put("type", "object")
                    putJsonObject("properties") { putJsonObject("query") { put("type", "string") } }
                    putJsonArray("required") { add(JsonPrimitive("query")) }
                    put("additionalProperties", false)
                }
            }
        })
    }
    if (DesktopLocalTool.CURRENT_TIME in config.localTools) {
        add(buildJsonObject {
            put("type", "function")
            putJsonObject("function") {
                put("name", DesktopCurrentTimeToolName)
                put("description", "Get the current local date, time, and time zone on this computer.")
                putJsonObject("parameters") {
                    put("type", "object")
                    putJsonObject("properties") {}
                    put("additionalProperties", false)
                }
            }
        })
    }
    if (DesktopLocalTool.ASK_USER in config.localTools) {
        add(buildJsonObject {
            put("type", "function")
            putJsonObject("function") {
                put("name", DesktopAskUserToolName)
                put(
                    "description",
                    "Ask the user one or more questions when clarification, additional information, or confirmation is needed. " +
                        "Each question may provide suggested options. Answers are returned as a JSON object keyed by question id."
                )
                putJsonObject("parameters") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("questions") {
                            put("type", "array")
                            putJsonObject("items") {
                                put("type", "object")
                                putJsonObject("properties") {
                                    putJsonObject("id") { put("type", "string") }
                                    putJsonObject("question") { put("type", "string") }
                                    putJsonObject("options") {
                                        put("type", "array")
                                        putJsonObject("items") { put("type", "string") }
                                    }
                                    putJsonObject("selection_type") {
                                        put("type", "string")
                                        putJsonArray("enum") {
                                            add(JsonPrimitive("text"))
                                            add(JsonPrimitive("single"))
                                            add(JsonPrimitive("multi"))
                                        }
                                    }
                                }
                                putJsonArray("required") {
                                    add(JsonPrimitive("id"))
                                    add(JsonPrimitive("question"))
                                }
                            }
                        }
                    }
                    putJsonArray("required") { add(JsonPrimitive("questions")) }
                    put("additionalProperties", false)
                }
            }
        })
    }
    if (config.memoryEnabled) add(memoryToolDefinition())
    config.mcpServers.forEach { server ->
        server.tools.filter { it.enabled }.forEach { tool ->
            add(buildJsonObject {
                put("type", "function")
                putJsonObject("function") {
                    put("name", server.toolCallName(tool))
                    put("description", tool.description)
                    put("parameters", tool.openAiParameters())
                }
            })
        }
    }
    agentToolDefinitions(config.agent).forEach(::add)
}

private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) {
                continuation.resume(response) { _, value, _ -> value.close() }
            } else {
                response.close()
            }
        }
    })
}

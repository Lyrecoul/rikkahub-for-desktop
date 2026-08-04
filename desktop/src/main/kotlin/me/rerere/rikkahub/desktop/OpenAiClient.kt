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
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.Locale
import java.util.Base64
import kotlin.coroutines.resumeWithException

internal data class StreamDelta(
    val content: String = "",
    val reasoning: String = "",
    val reasoningSignature: String = "",
    val modelId: String? = null,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val cachedTokens: Int? = null,
    val citations: List<DesktopCitation> = emptyList(),
    val attachments: List<DesktopAttachment> = emptyList(),
    val toolCallDeltas: List<DesktopToolCallDelta> = emptyList()
)

class OpenAiClient(
    private val httpClient: OkHttpClient = desktopHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val balanceCache = mutableMapOf<String, DesktopBalanceCacheEntry>()

    internal fun stream(config: DesktopConfig, messages: List<ChatMessage>): Flow<StreamDelta> = flow {
        require(config.apiKey.isNotBlank()) { "Configure an API key first" }
        require(config.model.isNotBlank()) { "Configure a model first" }

        val adapter = desktopChatProviderAdapter(config.protocol)
        val body = adapter.buildRequestBody(config, messages)
        val requestBuilder = Request.Builder()
            .url(adapter.chatEndpoint(config))
            .header("Accept", if (config.streamOutput) "text/event-stream" else "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
        adapter.configureRequest(requestBuilder, config)
        config.customHeaders.filter { it.name.isNotBlank() }.forEach { header ->
            requestBuilder.header(header.name, header.value)
        }
        val request = requestBuilder.build()
        // A long-running reasoning stream is valid as long as it continues to make progress.
        // Only the idle read timeout is relaxed; connection and write timeouts remain bounded.
        val call = (if (config.streamOutput) {
            httpClient.newBuilder().readTimeout(StreamReadTimeoutMillis, TimeUnit.MILLISECONDS).build()
        } else {
            httpClient
        }).newCall(request)

        call.awaitResponse().use { response ->
            if (!response.isSuccessful) {
                val detail = response.body.string().take(1000)
                error("Request failed (${response.code}): $detail")
            }
            if (!config.streamOutput) {
                val payload = response.body.string()
                emit(
                    when (config.protocol) {
                        DesktopProviderProtocol.OPENAI_CHAT_COMPLETIONS -> parseCompleteResponse(payload)
                        DesktopProviderProtocol.OPENAI_RESPONSES -> OpenAiResponsesAdapter.parseCompleteResponse(payload)
                        DesktopProviderProtocol.ANTHROPIC_MESSAGES ->
                            AnthropicMessagesAdapter.parseCompleteResponse(payload)

                        DesktopProviderProtocol.GEMINI_GENERATE_CONTENT ->
                            GeminiGenerateContentAdapter.parseResponse(payload)
                    }
                )
                return@use
            }
            val source = response.body.source()
            val providerToolIndexes = mutableMapOf<Int, Int>()
            while (!source.exhausted()) {
                currentCoroutineContext().ensureActive()
                val line = source.readUtf8Line() ?: break
                val data = line.removePrefix("data:").trim()
                if (!line.startsWith("data:") || data.isBlank()) continue
                if (data == "[DONE]") break
                val responseError = when (config.protocol) {
                    DesktopProviderProtocol.OPENAI_CHAT_COMPLETIONS -> parseError(data)
                    DesktopProviderProtocol.OPENAI_RESPONSES -> OpenAiResponsesAdapter.parseError(data)
                    DesktopProviderProtocol.ANTHROPIC_MESSAGES -> AnthropicMessagesAdapter.parseError(data)
                    DesktopProviderProtocol.GEMINI_GENERATE_CONTENT -> GeminiGenerateContentAdapter.parseError(data)
                }
                responseError?.let { error(it) }
                val parsedDelta = when (config.protocol) {
                    DesktopProviderProtocol.OPENAI_CHAT_COMPLETIONS -> parseDelta(data)
                    DesktopProviderProtocol.OPENAI_RESPONSES -> OpenAiResponsesAdapter.parseStreamEvent(data)
                    DesktopProviderProtocol.ANTHROPIC_MESSAGES -> AnthropicMessagesAdapter.parseStreamEvent(data)
                    DesktopProviderProtocol.GEMINI_GENERATE_CONTENT -> GeminiGenerateContentAdapter.parseResponse(data)
                }
                val delta = parsedDelta?.let { parsed ->
                    if (config.protocol == DesktopProviderProtocol.OPENAI_CHAT_COMPLETIONS) {
                        parsed
                    } else {
                        parsed.normalizeProviderToolCallIndexes(providerToolIndexes)
                    }
                }
                delta?.takeUnless {
                    it.content.isEmpty() && it.reasoning.isEmpty() && it.reasoningSignature.isEmpty() &&
                        it.promptTokens == null && it.completionTokens == null
                        && it.cachedTokens == null && it.modelId == null
                        && it.citations.isEmpty() && it.attachments.isEmpty() && it.toolCallDeltas.isEmpty()
                }?.let { emit(it) }
            }
        }
    }.flowOn(Dispatchers.IO)

    internal fun buildRequestBody(config: DesktopConfig, messages: List<ChatMessage>): String =
        desktopChatProviderAdapter(config.protocol).buildRequestBody(config, messages)

    internal fun mergeCustomBodies(base: JsonObject, bodies: List<DesktopCustomBody>): JsonObject =
        mergeDesktopCustomBodies(base, bodies, json)

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
            attachments = delta?.get("content").desktopImageAttachments(),
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
            attachments = message["content"].desktopImageAttachments(),
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
        val adapter = desktopChatProviderAdapter(config.protocol)
        val requestBuilder = Request.Builder()
            .url(adapter.modelsEndpoint(config))
            .get()
        adapter.configureRequest(requestBuilder, config)
        config.customHeaders.filter { it.name.isNotBlank() }.forEach { header ->
            requestBuilder.header(header.name, header.value)
        }
        val request = requestBuilder.build()
        retryOnceOnNetworkFailure {
            httpClient.newCall(request).awaitResponse().use { response ->
                if (!response.isSuccessful) {
                    val detail = response.body.string().take(1000)
                    error("Connection failed (${response.code}): $detail")
                }
                val payload = response.body.string()
                if (config.protocol == DesktopProviderProtocol.GEMINI_GENERATE_CONTENT) {
                    GeminiGenerateContentAdapter.parseModels(payload)
                } else {
                    parseModels(payload)
                }
            }
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
        retryOnceOnNetworkFailure {
            httpClient.newCall(request).awaitResponse().use { response ->
                if (!response.isSuccessful) error("余额查询失败（${response.code}）：${response.body.string().take(500)}")
                val result = evaluateDesktopJsonExpression(
                    input = options.resultPath,
                    root = json.parseToJsonElement(response.body.string()).jsonObject
                )
                result.toDoubleOrNull()?.let { "%.2f".format(Locale.ROOT, it) } ?: result
            }
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

internal fun JsonElement?.desktopImageAttachments(): List<DesktopAttachment> = when (this) {
    is JsonArray -> mapIndexedNotNull { index, part ->
        (part as? JsonObject)?.desktopImageAttachment(index)
    }

    is JsonObject -> listOfNotNull(desktopImageAttachment(0))
    else -> emptyList()
}

internal fun JsonObject.desktopImageAttachment(index: Int = 0): DesktopAttachment? {
    val inlineData = this["inlineData"] as? JsonObject
    val image = this["image"] as? JsonObject
    val source = this["source"] as? JsonObject
    val imageUrl = (this["image_url"] as? JsonPrimitive)?.contentOrNull
        ?: ((this["image_url"] as? JsonObject)?.get("url") as? JsonPrimitive)?.contentOrNull
    val dataUrl = imageUrl?.desktopImageDataUrl()
    val encoded = this["b64_json"]?.jsonPrimitive?.contentOrNull
        ?: image?.get("b64_json")?.jsonPrimitive?.contentOrNull
        ?: inlineData?.get("data")?.jsonPrimitive?.contentOrNull
        ?: this["result"]?.jsonPrimitive?.contentOrNull
        ?: dataUrl?.second
        ?: return null
    val mimeType = inlineData?.get("mimeType")?.jsonPrimitive?.contentOrNull
        ?: image?.get("mime_type")?.jsonPrimitive?.contentOrNull
        ?: source?.get("media_type")?.jsonPrimitive?.contentOrNull
        ?: this["mime_type"]?.jsonPrimitive?.contentOrNull
        ?: dataUrl?.first
        ?: "image/png"
    if (!mimeType.startsWith("image/", ignoreCase = true)) return null
    val bytes = runCatching { Base64.getDecoder().decode(encoded) }.getOrNull() ?: return null
    val extension = when (mimeType.lowercase()) {
        "image/jpeg" -> "jpg"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> "png"
    }
    return DesktopAttachment(
        name = "generated-image-${index + 1}.$extension",
        mimeType = mimeType.lowercase(),
        data = encoded,
        isImage = true,
        sizeBytes = bytes.size.toLong()
    )
}

private fun String.desktopImageDataUrl(): Pair<String, String>? {
    if (!startsWith("data:", ignoreCase = true)) return null
    val comma = indexOf(',')
    if (comma <= 5) return null
    val header = substring(5, comma)
    if (!header.endsWith(";base64", ignoreCase = true)) return null
    val mimeType = header.substringBefore(';').lowercase()
    return mimeType to substring(comma + 1)
}

internal fun StreamDelta.normalizeProviderToolCallIndexes(indexes: MutableMap<Int, Int>): StreamDelta {
    if (toolCallDeltas.isEmpty()) return this
    return copy(toolCallDeltas = toolCallDeltas.map { delta ->
        delta.copy(index = indexes.getOrPut(delta.index) { indexes.size })
    })
}

private data class DesktopBalanceCacheEntry(val value: String, val fetchedAt: Long)

private const val BalanceCacheTtlMillis = 2 * 60 * 1000L
internal const val ConnectTimeoutMillis = 15_000L
internal const val WriteTimeoutMillis = 30_000L
internal const val ReadTimeoutMillis = 60_000L
internal const val StreamReadTimeoutMillis = 120_000L

internal fun desktopHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(ConnectTimeoutMillis, TimeUnit.MILLISECONDS)
    .writeTimeout(WriteTimeoutMillis, TimeUnit.MILLISECONDS)
    .readTimeout(ReadTimeoutMillis, TimeUnit.MILLISECONDS)
    // Streaming requests can legitimately exceed any fixed wall-clock duration.
    .callTimeout(0, TimeUnit.MILLISECONDS)
    .build()

internal fun Throwable.userFacingMessage(): String = when (this) {
    is SocketTimeoutException -> "Network timed out. Check your connection and try again."
    is ConnectException -> "Unable to connect. Check the server address and your connection."
    is UnknownHostException -> "Unable to resolve the server address. Check your network and server URL."
    is IOException -> "Network connection was interrupted. Check your connection and try again."
    else -> message ?: "Request failed"
}

/** Retries only operations that are safe to repeat and failed before producing a response. */
internal suspend fun <T> retryOnceOnNetworkFailure(block: suspend () -> T): T {
    var failure: IOException? = null
    repeat(2) { attempt ->
        try {
            return block()
        } catch (error: IOException) {
            failure = error
            if (attempt == 1) throw error
        }
    }
    throw checkNotNull(failure)
}

internal const val DesktopWebSearchToolName = "web_search"
internal const val DesktopCurrentTimeToolName = "current_time"
internal const val DesktopAskUserToolName = "ask_user"

internal fun buildDesktopToolDefinitions(config: DesktopConfig) = buildJsonArray {
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

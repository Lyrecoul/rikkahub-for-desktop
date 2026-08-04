package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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

internal object AnthropicMessagesAdapter : DesktopChatProviderAdapter {
    private val json = Json { ignoreUnknownKeys = true }

    override fun chatEndpoint(config: DesktopConfig): String = "${config.baseUrl.trimEnd('/')}/messages"

    override fun configureRequest(builder: Request.Builder, config: DesktopConfig) {
        builder.header("x-api-key", config.apiKey)
        builder.header("anthropic-version", "2023-06-01")
    }

    override fun buildRequestBody(config: DesktopConfig, messages: List<ChatMessage>): String {
        val systemParts = buildList {
            if (config.systemPrompt.isNotBlank()) add(config.systemPrompt)
            messages.filter { it.role == "system" && it.content.isNotBlank() }.forEach { add(it.content) }
        }
        val requestMessages = buildAnthropicMessages(config, messages.filterNot { it.role == "system" })
        val thinkingBudget = when (config.reasoningEffort) {
            "low" -> 1_024
            "medium" -> 4_096
            "high" -> 8_192
            else -> null
        }
        val maxTokens = maxOf(config.maxTokens.takeIf { it > 0 } ?: 4_096, (thinkingBudget ?: 0) + 1_024)
        val base = buildJsonObject {
            put("model", config.model)
            put("max_tokens", maxTokens)
            put("stream", config.streamOutput)
            if (systemParts.isNotEmpty()) put("system", systemParts.joinToString("\n\n"))
            if (thinkingBudget != null) {
                putJsonObject("thinking") {
                    put("type", "enabled")
                    put("budget_tokens", thinkingBudget)
                }
                put("temperature", 1.0)
            } else {
                put("temperature", config.temperature)
                put("top_p", config.topP)
            }
            val tools = buildAnthropicTools(config)
            if (tools.isNotEmpty()) put("tools", tools)
            putJsonArray("messages") {
                requestMessages.forEach { message ->
                    add(buildJsonObject {
                        put("role", message.role)
                        put("content", JsonArray(message.content))
                    })
                }
            }
        }
        return mergeDesktopCustomBodies(base, config.customBodies, json).toString()
    }

    private fun buildAnthropicMessages(
        config: DesktopConfig,
        messages: List<ChatMessage>
    ): List<AnthropicRequestMessage> {
        val result = mutableListOf<AnthropicRequestMessage>()
        messages.forEach { message ->
            val role = if (message.role == "assistant") "assistant" else "user"
            val content = buildList {
                if (message.role == "tool") {
                    message.toolCallId?.let { toolCallId ->
                        add(buildJsonObject {
                            put("type", "tool_result")
                            put("tool_use_id", toolCallId)
                            put("content", message.content)
                        })
                    }
                } else {
                    if (message.role == "assistant" && message.reasoning.isNotBlank() &&
                        message.reasoningSignature.isNotBlank()
                    ) {
                        add(buildJsonObject {
                            put("type", "thinking")
                            put("thinking", message.reasoning)
                            put("signature", message.reasoningSignature)
                        })
                    }
                    if (message.content.isNotBlank()) {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", message.content)
                        })
                    }
                    message.attachments.forEach { attachment ->
                        when (attachment.kind) {
                            DesktopAttachmentKind.IMAGE -> add(buildJsonObject {
                                put("type", "image")
                                putJsonObject("source") {
                                    put("type", "base64")
                                    put("media_type", attachment.mimeType)
                                    put("data", attachment.data)
                                }
                            })

                            DesktopAttachmentKind.AUDIO -> error(
                                "Anthropic Messages does not support the desktop audio attachment format"
                            )

                            DesktopAttachmentKind.FILE -> if (config.canSendNativeDocument(attachment)) {
                                add(buildJsonObject {
                                    put("type", "document")
                                    putJsonObject("source") {
                                        put("type", "base64")
                                        put("media_type", attachment.rawMimeType ?: "application/pdf")
                                        put("data", attachment.rawData.orEmpty())
                                    }
                                    put("title", attachment.name)
                                })
                            } else {
                                add(buildJsonObject {
                                    put("type", "text")
                                    put("text", "File: ${attachment.name}\n${attachment.data}")
                                })
                            }
                        }
                    }
                    message.toolCalls.forEach { toolCall ->
                        add(buildJsonObject {
                            put("type", "tool_use")
                            put("id", toolCall.id)
                            put("name", toolCall.name)
                            put("input", parseToolInput(toolCall.arguments))
                        })
                    }
                }
            }
            if (content.isEmpty()) return@forEach
            val previous = result.lastOrNull()
            if (previous?.role == role) {
                previous.content += content
            } else {
                result += AnthropicRequestMessage(role, content.toMutableList())
            }
        }
        return result
    }

    private fun parseToolInput(arguments: String): JsonObject = runCatching {
        json.parseToJsonElement(arguments) as? JsonObject
    }.getOrNull() ?: JsonObject(emptyMap())

    private fun buildAnthropicTools(config: DesktopConfig) = buildJsonArray {
        if (config.webSearchEnabled && !config.webSearchSettings.isConfigured) {
            add(buildJsonObject {
                put("type", "web_search_20250305")
                put("name", DesktopWebSearchToolName)
                put("max_uses", config.webSearchSettings.resultCount.coerceAtLeast(1))
            })
        }
        buildDesktopToolDefinitions(config).forEach { chatTool ->
            val function = chatTool.jsonObject["function"]?.jsonObject ?: return@forEach
            add(buildJsonObject {
                function["name"]?.let { put("name", it) }
                function["description"]?.let { put("description", it) }
                function["parameters"]?.let { put("input_schema", it) }
            })
        }
    }

    internal fun parseStreamEvent(data: String): StreamDelta? = runCatching {
        val event = json.parseToJsonElement(data).jsonObject
        when (event["type"]?.jsonPrimitive?.contentOrNull) {
            "message_start" -> {
                val message = event["message"]?.jsonObject
                message?.get("usage")?.jsonObject?.usageDelta()
                    ?.copy(modelId = message["model"]?.jsonPrimitive?.contentOrNull)
            }

            "content_block_start" -> {
                val block = event["content_block"]?.jsonObject ?: return@runCatching null
                val index = event["index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                when (block["type"]?.jsonPrimitive?.contentOrNull) {
                    "text" -> StreamDelta(content = block["text"]?.jsonPrimitive?.contentOrNull.orEmpty())
                    "thinking" -> StreamDelta(reasoning = block["thinking"]?.jsonPrimitive?.contentOrNull.orEmpty())
                    "tool_use" -> StreamDelta(
                        toolCallDeltas = listOf(
                            DesktopToolCallDelta(
                                index = index,
                                id = block["id"]?.jsonPrimitive?.contentOrNull,
                                name = block["name"]?.jsonPrimitive?.contentOrNull
                            )
                        )
                    )

                    else -> null
                }
            }

            "content_block_delta" -> {
                val delta = event["delta"]?.jsonObject ?: return@runCatching null
                val index = event["index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                when (delta["type"]?.jsonPrimitive?.contentOrNull) {
                    "text_delta" -> StreamDelta(content = delta["text"]?.jsonPrimitive?.contentOrNull.orEmpty())
                    "thinking_delta" -> StreamDelta(
                        reasoning = delta["thinking"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    )

                    "signature_delta" -> StreamDelta(
                        reasoningSignature = delta["signature"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    )

                    "input_json_delta" -> StreamDelta(
                        toolCallDeltas = listOf(
                            DesktopToolCallDelta(
                                index = index,
                                arguments = delta["partial_json"]?.jsonPrimitive?.contentOrNull.orEmpty()
                            )
                        )
                    )

                    "citations_delta" -> parseCitation(delta["citation"]?.jsonObject)?.let { citation ->
                        StreamDelta(citations = listOf(citation))
                    }

                    else -> null
                }
            }

            "message_delta" -> event["usage"]?.jsonObject?.usageDelta()
            else -> null
        }
    }.getOrNull()

    internal fun parseCompleteResponse(data: String): StreamDelta {
        val response = json.parseToJsonElement(data).jsonObject
        parseError(data)?.let { error(it) }
        val blocks = response["content"]?.jsonArray.orEmpty()
        val content = blocks.filter { it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "text" }
            .joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
        val thinkingBlocks = blocks.filter {
            it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "thinking"
        }
        val reasoning = thinkingBlocks.joinToString("") {
            it.jsonObject["thinking"]?.jsonPrimitive?.contentOrNull.orEmpty()
        }
        val reasoningSignature = thinkingBlocks.joinToString("") {
            it.jsonObject["signature"]?.jsonPrimitive?.contentOrNull.orEmpty()
        }
        val calls = blocks.mapNotNull { element ->
            val block = element.jsonObject
            if (block["type"]?.jsonPrimitive?.contentOrNull != "tool_use") return@mapNotNull null
            val id = block["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = block["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            DesktopToolCall(id, name, block["input"]?.toString() ?: "{}")
        }
        val usage = response["usage"]?.jsonObject?.usageDelta() ?: StreamDelta()
        return usage.copy(
            content = content,
            reasoning = reasoning,
            reasoningSignature = reasoningSignature,
            modelId = response["model"]?.jsonPrimitive?.contentOrNull,
            citations = parseCitations(blocks),
            toolCallDeltas = calls.mapIndexed { index, call ->
                DesktopToolCallDelta(index, call.id, call.name, call.arguments)
            }
        )
    }

    internal fun parseError(data: String): String? = runCatching {
        val event = json.parseToJsonElement(data).jsonObject
        event["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    private fun JsonObject.usageDelta(): StreamDelta = StreamDelta(
        promptTokens = this["input_tokens"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
        completionTokens = this["output_tokens"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
        cachedTokens = this["cache_read_input_tokens"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
    )

    private fun parseCitations(blocks: List<JsonElement>): List<DesktopCitation> = blocks.flatMap { element ->
        element.jsonObject["citations"]?.jsonArray.orEmpty().mapNotNull { citationElement ->
            parseCitation(citationElement.jsonObject)
        }
    }.distinctBy { it.url }

    private fun parseCitation(citation: JsonObject?): DesktopCitation? {
        citation ?: return null
        val source = citation["source"] as? JsonObject
        val url = citation["url"]?.jsonPrimitive?.contentOrNull
            ?: source?.get("url")?.jsonPrimitive?.contentOrNull
            ?: return null
        val title = citation["title"]?.jsonPrimitive?.contentOrNull
            ?: source?.get("title")?.jsonPrimitive?.contentOrNull
            ?: ""
        return DesktopCitation(url, title)
    }

    private data class AnthropicRequestMessage(
        val role: String,
        val content: MutableList<JsonObject>
    )
}

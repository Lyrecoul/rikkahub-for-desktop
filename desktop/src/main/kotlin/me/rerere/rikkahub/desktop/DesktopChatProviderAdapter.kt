package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Request

internal interface DesktopChatProviderAdapter {
    fun chatEndpoint(config: DesktopConfig): String
    fun modelsEndpoint(config: DesktopConfig): String = "${config.baseUrl.trimEnd('/')}/models"
    fun buildRequestBody(config: DesktopConfig, messages: List<ChatMessage>): String

    fun configureRequest(builder: Request.Builder, config: DesktopConfig) {
        builder.header("Authorization", "Bearer ${config.apiKey}")
    }
}

internal fun desktopChatProviderAdapter(protocol: DesktopProviderProtocol): DesktopChatProviderAdapter = when (protocol) {
    DesktopProviderProtocol.OPENAI_CHAT_COMPLETIONS -> OpenAiChatCompletionsAdapter
    DesktopProviderProtocol.OPENAI_RESPONSES -> OpenAiResponsesAdapter
    DesktopProviderProtocol.ANTHROPIC_MESSAGES -> AnthropicMessagesAdapter
    DesktopProviderProtocol.GEMINI_GENERATE_CONTENT -> GeminiGenerateContentAdapter
}

internal object OpenAiChatCompletionsAdapter : DesktopChatProviderAdapter {
    private val json = Json { ignoreUnknownKeys = true }

    override fun chatEndpoint(config: DesktopConfig): String =
        "${config.baseUrl.trimEnd('/')}/chat/completions"

    override fun buildRequestBody(config: DesktopConfig, messages: List<ChatMessage>): String {
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
                                            val format = attachment.audioFormat ?: when (attachment.mimeType) {
                                                "audio/wav", "audio/x-wav" -> "wav"
                                                "audio/mpeg", "audio/mp3" -> "mp3"
                                                else -> error(
                                                    "OpenAI Chat Completions supports only MP3 and WAV audio"
                                                )
                                            }
                                            require(format in setOf("mp3", "wav")) {
                                                "OpenAI Chat Completions supports only MP3 and WAV audio"
                                            }
                                            put("type", "input_audio")
                                            putJsonObject("input_audio") {
                                                put("data", attachment.data)
                                                put("format", format)
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
        return mergeDesktopCustomBodies(base, config.customBodies, json).toString()
    }
}

internal fun mergeDesktopCustomBodies(
    base: JsonObject,
    bodies: List<DesktopCustomBody>,
    json: Json = Json { ignoreUnknownKeys = true }
): JsonObject {
    val result = base.toMutableMap()
    bodies.filter { it.key.isNotBlank() }.forEach { body ->
        val value = json.parseToJsonElement(body.value)
        val existing = result[body.key]
        result[body.key] = if (existing is JsonObject && value is JsonObject) {
            mergeDesktopJsonObjects(existing, value)
        } else {
            value
        }
    }
    return JsonObject(result)
}

private fun mergeDesktopJsonObjects(base: JsonObject, overlay: JsonObject): JsonObject = JsonObject(
    base.toMutableMap().apply {
        overlay.forEach { (key, value) ->
            val existing = this[key]
            this[key] = if (existing is JsonObject && value is JsonObject) {
                mergeDesktopJsonObjects(existing, value)
            } else {
                value
            }
        }
    }
)

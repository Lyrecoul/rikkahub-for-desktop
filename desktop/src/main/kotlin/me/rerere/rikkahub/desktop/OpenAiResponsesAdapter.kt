package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json
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

internal object OpenAiResponsesAdapter : DesktopChatProviderAdapter {
    private val json = Json { ignoreUnknownKeys = true }

    override fun chatEndpoint(config: DesktopConfig): String = "${config.baseUrl.trimEnd('/')}/responses"

    override fun buildRequestBody(config: DesktopConfig, messages: List<ChatMessage>): String {
        val base = buildJsonObject {
            put("model", config.model)
            put("stream", config.streamOutput)
            if (config.systemPrompt.isNotBlank()) put("instructions", config.systemPrompt)
            put("temperature", config.temperature)
            put("top_p", config.topP)
            if (config.maxTokens > 0) put("max_output_tokens", config.maxTokens)
            if (config.reasoningEffort.isNotBlank()) {
                putJsonObject("reasoning") { put("effort", config.reasoningEffort) }
            }
            val tools = buildResponsesTools(config)
            if (tools.isNotEmpty()) put("tools", tools)
            putJsonArray("input") {
                messages.forEach { message -> addResponsesMessage(config, message) }
            }
        }
        return mergeDesktopCustomBodies(base, config.customBodies, json).toString()
    }

    private fun kotlinx.serialization.json.JsonArrayBuilder.addResponsesMessage(
        config: DesktopConfig,
        message: ChatMessage
    ) {
        if (message.role == "tool") {
            message.toolCallId?.let { callId ->
                add(buildJsonObject {
                    put("type", "function_call_output")
                    put("call_id", callId)
                    put("output", message.content)
                })
            }
            return
        }

        if (message.content.isNotBlank() || message.attachments.isNotEmpty()) {
            add(buildJsonObject {
                put("type", "message")
                put("role", message.role)
                putJsonArray("content") {
                    if (message.content.isNotBlank()) {
                        add(buildJsonObject {
                            put("type", if (message.role == "assistant") "output_text" else "input_text")
                            put("text", message.content)
                        })
                    }
                    message.attachments.forEach { attachment ->
                        when (attachment.kind) {
                            DesktopAttachmentKind.IMAGE -> add(buildJsonObject {
                                put("type", "input_image")
                                put("image_url", "data:${attachment.mimeType};base64,${attachment.data}")
                            })

                            DesktopAttachmentKind.AUDIO -> error(
                                "OpenAI Responses does not support the desktop input_audio attachment format"
                            )

                            DesktopAttachmentKind.FILE -> add(buildJsonObject {
                                if (config.canSendNativeDocument(attachment)) {
                                    put("type", "input_file")
                                    put("filename", attachment.name)
                                    put(
                                        "file_data",
                                        "data:${attachment.rawMimeType};base64,${attachment.rawData}"
                                    )
                                } else {
                                    put("type", "input_text")
                                    put("text", "File: ${attachment.name}\n${attachment.data}")
                                }
                            })
                        }
                    }
                }
            })
        }
        message.toolCalls.forEach { toolCall ->
            add(buildJsonObject {
                put("type", "function_call")
                put("call_id", toolCall.id)
                put("name", toolCall.name)
                put("arguments", toolCall.arguments)
            })
        }
    }

    private fun buildResponsesTools(config: DesktopConfig) = buildJsonArray {
        if (config.webSearchEnabled && !config.webSearchSettings.isConfigured) {
            add(buildJsonObject { put("type", "web_search_preview") })
        }
        buildDesktopToolDefinitions(config).forEach { chatTool ->
            val function = chatTool.jsonObject["function"]?.jsonObject ?: return@forEach
            add(buildJsonObject {
                put("type", "function")
                function["name"]?.let { put("name", it) }
                function["description"]?.let { put("description", it) }
                function["parameters"]?.let { put("parameters", it) }
            })
        }
    }

    internal fun parseStreamEvent(data: String): StreamDelta? = runCatching {
        val event = json.parseToJsonElement(data).jsonObject
        when (event["type"]?.jsonPrimitive?.contentOrNull) {
            "response.output_text.delta", "response.refusal.delta" -> StreamDelta(
                content = event["delta"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )

            "response.reasoning_summary_text.delta", "response.reasoning_text.delta" -> StreamDelta(
                reasoning = event["delta"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )

            "response.output_item.added" -> {
                val item = event["item"]?.jsonObject
                if (item?.get("type")?.jsonPrimitive?.contentOrNull != "function_call") return@runCatching null
                StreamDelta(
                    toolCallDeltas = listOf(
                        DesktopToolCallDelta(
                            index = event["output_index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                            id = item["call_id"]?.jsonPrimitive?.contentOrNull
                                ?: item["id"]?.jsonPrimitive?.contentOrNull,
                            name = item["name"]?.jsonPrimitive?.contentOrNull,
                            arguments = item["arguments"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        )
                    )
                )
            }

            "response.function_call_arguments.delta" -> StreamDelta(
                toolCallDeltas = listOf(
                    DesktopToolCallDelta(
                        index = event["output_index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                        arguments = event["delta"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    )
                )
            )

            "response.image_generation_call.completed", "response.output_item.done" -> {
                val item = event["item"] as? JsonObject ?: event
                item.takeIf {
                    it["type"]?.jsonPrimitive?.contentOrNull == "image_generation_call"
                }?.desktopImageAttachment()?.let { attachment ->
                    StreamDelta(attachments = listOf(attachment))
                }
            }

            "response.completed" -> event["response"]?.jsonObject?.let { response ->
                response.usageDelta().copy(attachments = response.responseImageAttachments())
            }
            else -> null
        }
    }.getOrNull()

    internal fun parseCompleteResponse(data: String): StreamDelta {
        val response = json.parseToJsonElement(data).jsonObject
        parseError(data)?.let { error(it) }
        val output = response["output"]?.jsonArray.orEmpty()
        val content = output.filter { item ->
            item.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "message"
        }.flatMap { item ->
            item.jsonObject["content"]?.jsonArray.orEmpty().mapNotNull { part ->
                val objectPart = part.jsonObject
                objectPart["text"]?.jsonPrimitive?.contentOrNull
                    ?: objectPart["refusal"]?.jsonPrimitive?.contentOrNull
            }
        }.joinToString("")
        val reasoning = output.filter { item ->
            item.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "reasoning"
        }.flatMap { item ->
            listOf("content", "summary").flatMap { field ->
                item.jsonObject[field]?.jsonArray.orEmpty().mapNotNull { part ->
                    part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                }
            }
        }.joinToString("")
        val toolCalls = output.mapNotNull { item ->
            val call = item.jsonObject
            if (call["type"]?.jsonPrimitive?.contentOrNull != "function_call") return@mapNotNull null
            val id = call["call_id"]?.jsonPrimitive?.contentOrNull
                ?: call["id"]?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            val name = call["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            DesktopToolCall(id, name, call["arguments"]?.jsonPrimitive?.contentOrNull.orEmpty())
        }
        val usage = response.usageDelta()
        return usage.copy(
            content = content,
            reasoning = reasoning,
            modelId = response["model"]?.jsonPrimitive?.contentOrNull,
            attachments = response.responseImageAttachments(),
            toolCallDeltas = toolCalls.mapIndexed { index, call ->
                DesktopToolCallDelta(index, call.id, call.name, call.arguments)
            }
        )
    }

    internal fun parseError(data: String): String? = runCatching {
        val event = json.parseToJsonElement(data).jsonObject
        val response = event["response"]?.jsonObject ?: event
        event["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            ?: response["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            ?: event.takeIf { it["type"]?.jsonPrimitive?.contentOrNull == "error" }
                ?.get("message")?.jsonPrimitive?.contentOrNull
            ?: response.takeIf { it["status"]?.jsonPrimitive?.contentOrNull == "incomplete" }
                ?.get("incomplete_details")?.jsonObject?.get("reason")?.jsonPrimitive?.contentOrNull
                ?.let { "Response incomplete: $it" }
    }.getOrNull()

    private fun JsonObject.responseImageAttachments(): List<DesktopAttachment> = buildList {
        this@responseImageAttachments["output"]?.jsonArray.orEmpty().forEachIndexed { outputIndex, element ->
            val item = element.jsonObject
            if (item["type"]?.jsonPrimitive?.contentOrNull == "image_generation_call") {
                item.desktopImageAttachment(outputIndex)?.let(::add)
            }
            item["content"].desktopImageAttachments().forEach(::add)
        }
    }.distinctBy(DesktopAttachment::data)

    private fun JsonObject.usageDelta(): StreamDelta {
        val usage = this["usage"]?.jsonObject
        val inputDetails = usage?.get("input_tokens_details")?.jsonObject
        val citations = this["output"]?.jsonArray.orEmpty().flatMap { item ->
            item.jsonObject["content"]?.jsonArray.orEmpty().flatMap { part ->
                part.jsonObject["annotations"]?.jsonArray.orEmpty().mapNotNull { annotation ->
                    val citation = annotation.jsonObject
                    val url = citation["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    DesktopCitation(url, citation["title"]?.jsonPrimitive?.contentOrNull.orEmpty())
                }
            }
        }.distinctBy { it.url }
        return StreamDelta(
            modelId = this["model"]?.jsonPrimitive?.contentOrNull,
            promptTokens = usage?.get("input_tokens")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            completionTokens = usage?.get("output_tokens")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            cachedTokens = inputDetails?.get("cached_tokens")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            citations = citations
        )
    }
}

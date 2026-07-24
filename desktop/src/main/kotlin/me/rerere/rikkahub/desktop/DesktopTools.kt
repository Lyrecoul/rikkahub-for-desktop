package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal fun List<DesktopToolCall>.merge(deltas: List<DesktopToolCallDelta>): List<DesktopToolCall> {
    if (deltas.isEmpty()) return this
    val calls = toMutableList()
    deltas.forEach { delta ->
        val existing = calls.getOrNull(delta.index)
        val updated = DesktopToolCall(
            id = delta.id ?: existing?.id.orEmpty(),
            name = delta.name ?: existing?.name.orEmpty(),
            arguments = (existing?.arguments.orEmpty() + delta.arguments)
        )
        if (delta.index < calls.size) calls[delta.index] = updated else calls.add(updated)
    }
    return calls
}

internal suspend fun executeDesktopToolCalls(
    httpClient: OkHttpClient,
    config: DesktopConfig,
    calls: List<DesktopToolCall>
): List<ChatMessage> = calls.map { call ->
    val output = when (call.name) {
        DesktopWebSearchToolName -> runCatching {
            check(config.webSearchEnabled && config.webSearchSettings.isConfigured) {
                "web_search is not enabled for this conversation"
            }
            val query = Json.parseToJsonElement(call.arguments).jsonObject["query"]
                ?.jsonPrimitive?.content.orEmpty()
            require(query.isNotBlank()) { "web_search requires a non-empty query" }
            searchWeb(httpClient, config.webSearchSettings, query).content
        }.getOrElse { "Web search failed: ${it.message ?: "unknown error"}" }
        DesktopCurrentTimeToolName -> if (DesktopLocalTool.CURRENT_TIME in config.localTools) {
            ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
        } else {
            "current_time is not enabled for this assistant"
        }
        else -> "Unsupported desktop tool: ${call.name}"
    }
    ChatMessage(role = "tool", content = output, toolCallId = call.id)
}

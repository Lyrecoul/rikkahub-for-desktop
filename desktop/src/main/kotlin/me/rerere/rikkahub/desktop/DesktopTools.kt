package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
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
    calls: List<DesktopToolCall>,
    memoryToolHandler: DesktopMemoryToolHandler? = null,
    mcpClient: DesktopMcpClient = DesktopMcpClient(),
    askUserHandler: (suspend (DesktopToolCall) -> String)? = null,
    agentRuntime: DesktopAgentRuntime = DesktopAgentRuntime(),
    approvalHandler: (suspend (DesktopToolCall, DesktopAgentApprovalRequest) -> Boolean)? = null
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
        DesktopAskUserToolName -> if (DesktopLocalTool.ASK_USER in config.localTools && askUserHandler != null) {
            askUserHandler(call)
        } else {
            "ask_user is not enabled for this assistant"
        }
        DesktopMemoryToolName -> runCatching {
            check(config.memoryEnabled && memoryToolHandler != null) { "memory_tool is not enabled for this assistant" }
            executeMemoryToolCall(call.arguments, memoryToolHandler)
        }.getOrElse { "Memory update failed: ${it.message ?: "unknown error"}" }
        DesktopAgentListFilesToolName,
        DesktopAgentSearchFilesToolName,
        DesktopAgentReadFileToolName,
        DesktopAgentWriteFileToolName,
        DesktopAgentEditFileToolName,
        DesktopAgentShellToolName,
        DesktopUseSkillToolName -> runCatching {
            val agent = requireNotNull(config.agent) { "agent is not enabled for this assistant" }
            val approve = requireNotNull(approvalHandler) { "agent approval handler is unavailable" }
            agentRuntime.execute(agent, call) { request -> approve(call, request) }
        }.getOrElse { "Agent tool failed: ${it.message ?: "unknown error"}" }
        else -> runCatching {
            val target = config.mcpServers.asSequence()
                .flatMap { server -> server.tools.asSequence().map { tool -> server to tool } }
                .firstOrNull { (server, tool) -> tool.enabled && server.toolCallName(tool) == call.name }
                ?: error("Unsupported desktop tool: ${call.name}")
            mcpClient.callTool(target.first, target.second.name, call.arguments)
        }.getOrElse { "MCP tool failed: ${it.message ?: "unknown error"}" }
    }
    ChatMessage(role = "tool", content = output, toolCallId = call.id)
}

internal data class DesktopMemoryToolHandler(
    val create: (String) -> DesktopMemory,
    val edit: (String, String) -> DesktopMemory,
    val delete: (String) -> Unit
)

internal fun memoryToolDefinition() = buildJsonObject {
    put("type", "function")
    putJsonObject("function") {
        put("name", DesktopMemoryToolName)
        put(
            "description",
            """
            Store long-term information across conversations. Use action create, edit, or delete.
            Create needs content. Edit needs id and content. Delete needs id.
            Do not store sensitive information such as ethnicity, religion, sexual orientation, political views, sex life, or criminal records.
            You may store preferences, plans, work notes, chat style preferences, and preferred names. Similar memories should be merged.
            Do not reveal memory content unless the user explicitly asks.
            """.trimIndent()
        )
        putJsonObject("parameters") {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("action") {
                    put("type", "string")
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("create"))
                        add(JsonPrimitive("edit"))
                        add(JsonPrimitive("delete"))
                    })
                }
                putJsonObject("id") { put("type", "string") }
                putJsonObject("content") { put("type", "string") }
            }
            put("required", buildJsonArray { add(JsonPrimitive("action")) })
            put("additionalProperties", false)
        }
    }
}

private fun executeMemoryToolCall(arguments: String, handler: DesktopMemoryToolHandler): String {
    val params = Json.parseToJsonElement(arguments).jsonObject
    val action = params["action"]?.jsonPrimitive?.contentOrNull ?: error("action is required")
    return when (action) {
        "create" -> Json.encodeToString(DesktopMemory.serializer(), handler.create(params.requiredContent()))
        "edit" -> Json.encodeToString(
            DesktopMemory.serializer(),
            handler.edit(params.requiredId(), params.requiredContent())
        )
        "delete" -> {
            val id = params.requiredId()
            handler.delete(id)
            buildJsonObject { put("success", true); put("id", id) }.toString()
        }
        else -> error("unknown action: $action, must be one of [create, edit, delete]")
    }
}

private fun kotlinx.serialization.json.JsonObject.requiredId(): String =
    this["id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: error("id is required")

private fun kotlinx.serialization.json.JsonObject.requiredContent(): String =
    this["content"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() } ?: error("content is required")

internal const val DesktopMemoryToolName = "memory_tool"

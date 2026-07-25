package me.rerere.rikkahub.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@Serializable
enum class DesktopMcpTransport {
    STREAMABLE_HTTP,
    SSE
}

@Serializable
data class DesktopMcpTool(
    val name: String,
    val description: String = "",
    val inputSchema: JsonObject = JsonObject(emptyMap()),
    val required: List<String> = emptyList(),
    val enabled: Boolean = true
)

@Serializable
data class DesktopMcpServer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val url: String = "",
    val transport: DesktopMcpTransport = DesktopMcpTransport.STREAMABLE_HTTP,
    val headers: List<DesktopCustomHeader> = emptyList(),
    val enabled: Boolean = true,
    val tools: List<DesktopMcpTool> = emptyList()
)

internal fun DesktopMcpServer.toolCallName(tool: DesktopMcpTool): String = "mcp__${name}__${tool.name}"

/** Keeps one initialized MCP session per server, matching the Android client's connection model. */
internal class DesktopMcpClient {
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(SSE)
    }
    private val sessions = ConcurrentHashMap<String, McpSession>()
    private val sessionsMutex = Mutex()

    suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> = withClient(server) { client ->
        client.listTools().tools.map { tool ->
            DesktopMcpTool(
                name = tool.name,
                description = tool.description.orEmpty(),
                inputSchema = tool.inputSchema.properties ?: JsonObject(emptyMap()),
                required = tool.inputSchema.required.orEmpty()
            )
        }
    }

    suspend fun callTool(server: DesktopMcpServer, toolName: String, arguments: String): String = withClient(server) { client ->
        val args = Json.parseToJsonElement(arguments).jsonObject
        val result = client.callTool(
            request = CallToolRequest(params = CallToolRequestParams(name = toolName, arguments = args)),
            options = RequestOptions(timeout = 120.seconds)
        )
        result.content.joinToString("\n") { content ->
            when (content) {
                is TextContent -> content.text
                else -> content.toString()
            }
        }
    }

    private suspend fun <T> withClient(server: DesktopMcpServer, action: suspend (Client) -> T): T = withContext(Dispatchers.IO) {
        sessionsMutex.withLock {
            require(server.name.matches(Regex("[A-Za-z0-9]+"))) {
                "MCP server name must contain only letters and numbers: ${server.name}"
            }
            require(server.url.isNotBlank()) { "MCP server URL is required" }
            val connection = server.connectionKey()
            val existing = sessions[server.id]
            if (existing != null && existing.connection != connection) {
                existing.client.close()
                sessions.remove(server.id, existing)
            }
            val session = sessions[server.id] ?: Client(
                Implementation(name = "RikkaHub Desktop", version = "1.0")
            ).let { client ->
                client.connect(createTransport(server))
                McpSession(connection, client).also { sessions[server.id] = it }
            }
            try {
                action(session.client)
            } catch (error: Throwable) {
                // A failed transport is not reusable; initialize a fresh session for the next attempt.
                if (sessions.remove(server.id, session)) session.client.close()
                throw error
            }
        }
    }

    private data class McpSession(val connection: McpConnectionKey, val client: Client)

    private data class McpConnectionKey(
        val name: String,
        val url: String,
        val transport: DesktopMcpTransport,
        val headers: List<DesktopCustomHeader>
    )

    private fun DesktopMcpServer.connectionKey() = McpConnectionKey(name, url, transport, headers)

    private fun createTransport(server: DesktopMcpServer): AbstractTransport = when (server.transport) {
        DesktopMcpTransport.SSE -> SseClientTransport(urlString = server.url, client = httpClient) {
            appendHeaders(server)
        }
        DesktopMcpTransport.STREAMABLE_HTTP -> StreamableHttpClientTransport(url = server.url, client = httpClient) {
            appendHeaders(server)
        }
    }

    private fun HttpRequestBuilder.appendHeaders(server: DesktopMcpServer) {
        server.headers.filter { it.name.isNotBlank() }.forEach { headers.append(it.name, it.value) }
    }
}

internal fun DesktopMcpTool.openAiParameters(): JsonObject = buildJsonObject {
    put("type", "object")
    put("properties", inputSchema)
    if (required.isNotEmpty()) put("required", buildJsonArray { required.forEach { add(JsonPrimitive(it)) } })
    put("additionalProperties", false)
}

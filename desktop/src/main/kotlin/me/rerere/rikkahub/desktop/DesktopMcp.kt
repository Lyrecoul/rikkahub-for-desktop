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
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
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

private const val MCP_CONNECTION_TIMEOUT_MILLIS = 30_000L

@Serializable
enum class DesktopMcpTransport {
    STREAMABLE_HTTP,
    SSE,
    STDIO
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
    val command: String = "",
    val arguments: List<String> = emptyList(),
    val environment: List<DesktopCustomHeader> = emptyList(),
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

    suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> =
        withTimeout(MCP_CONNECTION_TIMEOUT_MILLIS) {
            withClient(server) { client ->
                client.listTools().tools.map { tool ->
                    DesktopMcpTool(
                        name = tool.name,
                        description = tool.description.orEmpty(),
                        inputSchema = tool.inputSchema.properties ?: JsonObject(emptyMap()),
                        required = tool.inputSchema.required.orEmpty()
                    )
                }
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
            require(server.transport == DesktopMcpTransport.STDIO || server.url.isNotBlank()) {
                "MCP server URL is required"
            }
            val connection = server.connectionKey()
            val existing = sessions[server.id]
            if (existing != null && existing.connection != connection) {
                existing.close()
                sessions.remove(server.id, existing)
            }

            repeat(2) { attempt ->
                val cachedSession = sessions[server.id]
                if (cachedSession?.process?.isAlive == false && sessions.remove(server.id, cachedSession)) {
                    cachedSession.close()
                }
                val session = sessions[server.id] ?: createSession(server).also { sessions[server.id] = it }
                try {
                    return@withLock action(session.client)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    // Recreate a dead transport once so transient process exits recover without user intervention.
                    if (sessions.remove(server.id, session)) session.close()
                    if (attempt == 1) throw error
                }
            }

            error("MCP client retry loop completed without a result")
        }
    }

    private suspend fun createSession(server: DesktopMcpServer): McpSession {
        val process = server.createProcess()
        return try {
            val client = Client(Implementation(name = "RikkaHub Desktop", version = "1.0"))
            client.connect(createTransport(server, process))
            McpSession(server.connectionKey(), client, process)
        } catch (error: Throwable) {
            process?.destroy()
            throw error
        }
    }

    private data class McpSession(
        val connection: McpConnectionKey,
        val client: Client,
        val process: Process?
    ) {
        suspend fun close() {
            try {
                client.close()
            } finally {
                process?.destroy()
            }
        }
    }

    private data class McpConnectionKey(
        val name: String,
        val url: String,
        val transport: DesktopMcpTransport,
        val headers: List<DesktopCustomHeader>,
        val command: String,
        val arguments: List<String>,
        val environment: List<DesktopCustomHeader>
    )

    private fun DesktopMcpServer.connectionKey() = McpConnectionKey(
        name, url, transport, headers, command.trim(), arguments.normalizedArguments(), environment
    )

    private fun createTransport(server: DesktopMcpServer, process: Process?): AbstractTransport = when (server.transport) {
        DesktopMcpTransport.SSE -> SseClientTransport(urlString = server.url, client = httpClient) {
            appendHeaders(server)
        }
        DesktopMcpTransport.STREAMABLE_HTTP -> StreamableHttpClientTransport(url = server.url, client = httpClient) {
            appendHeaders(server)
        }
        DesktopMcpTransport.STDIO -> {
            requireNotNull(process) { "A process is required for stdio MCP transport" }
            StdioClientTransport(
                input = process.inputStream.asSource().buffered(),
                output = process.outputStream.asSink().buffered(),
                error = process.errorStream.asSource().buffered(),
            )
        }
    }

    private fun DesktopMcpServer.createProcess(): Process? {
        if (transport != DesktopMcpTransport.STDIO) return null
        val executable = command.trim()
        require(executable.isNotBlank()) { "MCP server command is required" }
        return ProcessBuilder(listOf(executable) + arguments.normalizedArguments())
            .apply {
                environment.filter { it.name.isNotBlank() }
                    .forEach { (name, value) -> environment()[name] = value }
            }
            .start()
    }

    private fun List<String>.normalizedArguments(): List<String> =
        map(String::trim).filter(String::isNotBlank)

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

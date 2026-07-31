package me.rerere.rikkahub.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.modelcontextprotocol.kotlin.sdk.types.AudioContent
import io.modelcontextprotocol.kotlin.sdk.types.BlobResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.EmbeddedResource
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsRequest
import io.modelcontextprotocol.kotlin.sdk.types.PaginatedRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.ResourceLink
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.ToolListChangedNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.Closeable
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

private const val MCP_CONNECTION_TIMEOUT_MILLIS = 10_000L
private const val MCP_CLOSE_TIMEOUT_MILLIS = 2_000L

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
    val definitions: JsonObject = JsonObject(emptyMap()),
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
    val tools: List<DesktopMcpTool> = emptyList(),
    val toolsConfigurationHash: String = ""
)

internal fun DesktopMcpServer.toolCallName(tool: DesktopMcpTool): String {
    val serverPart = name.toFunctionNamePart().ifBlank { "server" }.take(16)
    val suffix = sha256("$id\u0000${tool.name}").take(12)
    val prefix = "mcp_${serverPart}_"
    val toolBudget = 64 - prefix.length - suffix.length - 1
    val toolPart = tool.name.toFunctionNamePart().ifBlank { "tool" }.take(toolBudget)
    return "${prefix}${toolPart}_$suffix"
}

internal fun DesktopMcpServer.hasCurrentTools(): Boolean =
    tools.isNotEmpty() && toolsConfigurationHash == mcpToolsConfigurationHash()

internal fun DesktopMcpServer.withSyncedTools(tools: List<DesktopMcpTool>): DesktopMcpServer = copy(
    tools = tools,
    toolsConfigurationHash = mcpToolsConfigurationHash()
)

private fun DesktopMcpServer.mcpToolsConfigurationHash(): String = sha256(
    listOf(transport.name, url, headers, command.trim(), arguments.normalizedMcpArguments(), environment)
        .joinToString("\u0000")
)

private fun String.toFunctionNamePart(): String = replace(Regex("[^A-Za-z0-9_]"), "_")

private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray())
    .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }

private fun List<String>.normalizedMcpArguments(): List<String> =
    map(String::trim).filter(String::isNotBlank)

/** Keeps one initialized MCP session per server, matching the Android client's connection model. */
internal class DesktopMcpClient : Closeable {
    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(ConnectTimeoutMillis, TimeUnit.MILLISECONDS)
                writeTimeout(WriteTimeoutMillis, TimeUnit.MILLISECONDS)
                readTimeout(StreamReadTimeoutMillis, TimeUnit.MILLISECONDS)
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = ConnectTimeoutMillis
            socketTimeoutMillis = StreamReadTimeoutMillis
            // Remote MCP SSE sessions are long-lived; per-operation limits are set by the SDK calls.
            requestTimeoutMillis = null
        }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(SSE)
    }
    private val sessions = ConcurrentHashMap<String, McpSession>()
    private val activeSessions = ConcurrentHashMap<String, McpSession>()
    private val activeProcesses = ConcurrentHashMap<String, Process>()
    private val serverMutexes = ConcurrentHashMap<String, Mutex>()
    private val invalidatedToolServers = ConcurrentHashMap.newKeySet<String>()
    private val closed = AtomicBoolean(false)

    suspend fun syncTools(server: DesktopMcpServer): List<DesktopMcpTool> =
        withTimeout(MCP_CONNECTION_TIMEOUT_MILLIS) {
            withClient(server, retryOnce = true) { client ->
                val tools = mutableListOf<DesktopMcpTool>()
                val seenCursors = mutableSetOf<String>()
                var cursor: String? = null
                do {
                    val result = client.listTools(
                        ListToolsRequest(params = PaginatedRequestParams(cursor = cursor))
                    )
                    tools += result.tools.map { tool ->
                        DesktopMcpTool(
                            name = tool.name,
                            description = tool.description.orEmpty(),
                            inputSchema = tool.inputSchema.properties ?: JsonObject(emptyMap()),
                            required = tool.inputSchema.required.orEmpty(),
                            definitions = tool.inputSchema.defs ?: JsonObject(emptyMap())
                        )
                    }
                    cursor = result.nextCursor?.takeIf { it.isNotBlank() && seenCursors.add(it) }
                } while (cursor != null)
                invalidatedToolServers.remove(server.id)
                tools
            }
        }

    suspend fun probeTools(
        server: DesktopMcpServer,
        timeoutMillis: Long = MCP_CONNECTION_TIMEOUT_MILLIS
    ): List<DesktopMcpTool> = coroutineScope {
        val watchdog = launch {
            delay(timeoutMillis)
            cancelProbe(server.id)
        }
        try {
            withTimeout(timeoutMillis) { syncTools(server) }
        } finally {
            watchdog.cancel()
            disconnect(server.id)
        }
    }

    suspend fun cancelProbe(serverId: String) {
        withContext(NonCancellable + Dispatchers.IO) {
            val process = activeProcesses.remove(serverId)
            process?.stop()
            val session = activeSessions.remove(serverId) ?: return@withContext
            sessions.remove(serverId, session)
            session.abort(processAlreadyStopped = process != null)
        }
    }

    fun toolsAreCurrent(server: DesktopMcpServer): Boolean =
        server.hasCurrentTools() && server.id !in invalidatedToolServers

    suspend fun callTool(server: DesktopMcpServer, toolName: String, arguments: String): String =
        withClient(server, retryOnce = false) { client ->
            val args = Json.parseToJsonElement(arguments).jsonObject
            client.callTool(
                request = CallToolRequest(params = CallToolRequestParams(name = toolName, arguments = args)),
                options = RequestOptions(timeout = 120.seconds)
            ).desktopOutput()
        }

    suspend fun reconcileServers(servers: List<DesktopMcpServer>) {
        withContext(Dispatchers.IO) {
            val expectedConnections = servers.filter { it.enabled }.associate { it.id to it.connectionKey() }
            sessions.keys.toList().forEach { serverId ->
                val mutex = serverMutexes.computeIfAbsent(serverId) { Mutex() }
                mutex.withLock {
                    val session = sessions[serverId] ?: return@withLock
                    if (expectedConnections[serverId] != session.connection && sessions.remove(serverId, session)) {
                        activeSessions.remove(serverId, session)
                        activeProcesses.remove(serverId, session.process)
                        runCatching { session.close() }
                    }
                }
            }
        }
    }

    private suspend fun disconnect(serverId: String) {
        withContext(NonCancellable + Dispatchers.IO) {
            serverMutexes.computeIfAbsent(serverId) { Mutex() }.withLock {
                sessions.remove(serverId)?.let { session ->
                    activeSessions.remove(serverId, session)
                    activeProcesses.remove(serverId, session.process)
                    session.close()
                }
            }
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            serverMutexes.keys.toList().forEach { serverId ->
                serverMutexes[serverId]?.withLock {
                    sessions.remove(serverId)?.let { session ->
                        activeSessions.remove(serverId, session)
                        activeProcesses.remove(serverId, session.process)
                        runCatching { session.close() }
                    }
                }
            }
            activeSessions.values.toList().forEach { session -> runCatching { session.close() } }
            activeSessions.clear()
            activeProcesses.values.toList().forEach { process -> runCatching { process.stop() } }
            activeProcesses.clear()
            sessions.values.toList().forEach { session -> runCatching { session.close() } }
            sessions.clear()
        }
        httpClient.close()
    }

    private suspend fun <T> withClient(
        server: DesktopMcpServer,
        retryOnce: Boolean,
        action: suspend (Client) -> T
    ): T = withContext(Dispatchers.IO) {
        check(!closed.get()) { "MCP client is closed" }
        serverMutexes.computeIfAbsent(server.id) { Mutex() }.withLock {
            check(!closed.get()) { "MCP client is closed" }
            require(server.name.matches(Regex("[A-Za-z0-9]+"))) {
                "MCP server name must contain only letters and numbers: ${server.name}"
            }
            require(server.transport == DesktopMcpTransport.STDIO || server.url.isNotBlank()) {
                "MCP server URL is required"
            }
            val connection = server.connectionKey()
            val existing = sessions[server.id]
            if (existing != null && existing.connection != connection && sessions.remove(server.id, existing)) {
                activeSessions.remove(server.id, existing)
                activeProcesses.remove(server.id, existing.process)
                runCatching { existing.close() }
            }

            repeat(if (retryOnce) 2 else 1) { attempt ->
                check(!closed.get()) { "MCP client is closed" }
                val cachedSession = sessions[server.id]
                if (cachedSession?.process?.isAlive == false && sessions.remove(server.id, cachedSession)) {
                    activeSessions.remove(server.id, cachedSession)
                    activeProcesses.remove(server.id, cachedSession.process)
                    runCatching { cachedSession.close() }
                }
                val session = sessions[server.id] ?: createSession(server).also { sessions[server.id] = it }
                if (closed.get()) {
                    sessions.remove(server.id, session)
                    activeSessions.remove(server.id, session)
                    activeProcesses.remove(server.id, session.process)
                    runCatching { session.close() }
                    error("MCP client is closed")
                }
                try {
                    return@withLock action(session.client)
                } catch (error: CancellationException) {
                    if (sessions.remove(server.id, session)) runCatching { session.close() }
                    activeSessions.remove(server.id, session)
                    activeProcesses.remove(server.id, session.process)
                    throw error
                } catch (error: Throwable) {
                    if (sessions.remove(server.id, session)) runCatching { session.close() }
                    activeSessions.remove(server.id, session)
                    activeProcesses.remove(server.id, session.process)
                    coroutineContext.ensureActive()
                    if (!retryOnce || attempt == 1) throw error
                }
            }

            error("MCP client retry loop completed without a result")
        }
    }

    private suspend fun createSession(server: DesktopMcpServer): McpSession {
        val process = server.createProcess()
        process?.let { activeProcesses[server.id] = it }
        val client = Client(Implementation(name = "RikkaHub Desktop", version = "1.0"))
        val session = McpSession(server.connectionKey(), client, process)
        activeSessions[server.id] = session
        return try {
            val toolListChanged = ToolListChangedNotification()
            client.setNotificationHandler<ToolListChangedNotification>(toolListChanged.method) {
                invalidatedToolServers += server.id
                CompletableDeferred(Unit)
            }
            client.connect(createTransport(server, process))
            session
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                activeSessions.remove(server.id, session)
                process?.let { activeProcesses.remove(server.id, it) }
                runCatching { session.close() }
            }
            throw error
        }
    }

    private class McpSession(
        val connection: McpConnectionKey,
        val client: Client,
        val process: Process?
    ) {
        private val closed = AtomicBoolean(false)

        suspend fun close() {
            if (!closed.compareAndSet(false, true)) return
            try {
                client.closeWithinDeadline()
            } finally {
                process?.stop()
            }
        }

        suspend fun abort(processAlreadyStopped: Boolean = false) {
            if (!closed.compareAndSet(false, true)) return
            try {
                if (!processAlreadyStopped) process?.stop()
            } finally {
                client.closeWithinDeadline()
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
        name, url, transport, headers, command.trim(), arguments.normalizedMcpArguments(), environment
    )

    private fun createTransport(server: DesktopMcpServer, process: Process?): AbstractTransport =
        when (server.transport) {
            DesktopMcpTransport.SSE -> SseClientTransport(urlString = server.url, client = httpClient) {
                appendHeaders(server)
            }

            DesktopMcpTransport.STREAMABLE_HTTP -> StreamableHttpClientTransport(
                url = server.url,
                client = httpClient
            ) {
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
        return ProcessBuilder(listOf(executable) + arguments.normalizedMcpArguments())
            .apply {
                environment.filter { it.name.isNotBlank() }
                    .forEach { (name, value) -> environment()[name] = value }
            }
            .start()
    }

    private fun HttpRequestBuilder.appendHeaders(server: DesktopMcpServer) {
        server.headers.filter { it.name.isNotBlank() }.forEach { headers.append(it.name, it.value) }
    }
}

private suspend fun Client.closeWithinDeadline() {
    withTimeoutOrNull(MCP_CLOSE_TIMEOUT_MILLIS) { close() }
}

internal fun DesktopMcpTool.openAiParameters(): JsonObject = buildJsonObject {
    put("type", "object")
    put("properties", inputSchema)
    if (required.isNotEmpty()) put("required", buildJsonArray { required.forEach { add(JsonPrimitive(it)) } })
    if (definitions.isNotEmpty()) put("\$defs", definitions)
}

internal fun Process.stop() {
    val descendants = toHandle().descendants().toList().asReversed()
    descendants.forEach { handle -> runCatching { handle.destroy() } }
    destroy()

    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
    descendants.forEach { handle -> handle.waitUntil(deadline) }
    waitUntil(deadline)

    descendants.filter { it.isAlive }.forEach { handle -> runCatching { handle.destroyForcibly() } }
    if (isAlive) destroyForcibly()

    val forceDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
    descendants.forEach { handle -> handle.waitUntil(forceDeadline) }
    waitUntil(forceDeadline)
}

private fun ProcessHandle.waitUntil(deadlineNanos: Long) {
    val remaining = deadlineNanos - System.nanoTime()
    if (isAlive && remaining > 0) runCatching { onExit().get(remaining, TimeUnit.NANOSECONDS) }
}

private fun Process.waitUntil(deadlineNanos: Long) {
    val remaining = deadlineNanos - System.nanoTime()
    if (isAlive && remaining > 0) runCatching { waitFor(remaining, TimeUnit.NANOSECONDS) }
}

internal fun CallToolResult.desktopOutput(): String {
    val parts = content.mapNotNull { block ->
        when (block) {
            is TextContent -> block.text
            is ImageContent -> "[image omitted: ${block.mimeType}, ${block.data.length} base64 characters]"
            is AudioContent -> "[audio omitted: ${block.mimeType}, ${block.data.length} base64 characters]"
            is ResourceLink -> "[resource: ${block.name}, ${block.uri}]"
            is EmbeddedResource -> when (val resource = block.resource) {
                is TextResourceContents -> "Resource ${resource.uri}:\n${resource.text}"
                is BlobResourceContents -> "[binary resource omitted: ${resource.uri}, ${resource.mimeType.orEmpty()}]"
                else -> "[unknown embedded resource omitted: ${resource.uri}]"
            }
        }.takeIf { it.isNotBlank() }
    }.toMutableList()
    structuredContent?.let { parts += it.toString() }
    val output = parts.joinToString("\n").ifBlank { "[MCP tool returned no content]" }
    return if (isError == true) "MCP tool error:\n$output" else output
}

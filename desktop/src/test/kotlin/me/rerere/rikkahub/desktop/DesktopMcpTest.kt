package me.rerere.rikkahub.desktop

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DesktopMcpTest {
    private val server = DesktopMcpServer(
        id = "server-1",
        name = "GitHub",
        url = "https://example.com/mcp",
        tools = listOf(
            DesktopMcpTool(
                name = "search",
                description = "Search repositories",
                inputSchema = buildJsonObject { put("query", buildJsonObject { put("type", "string") }) },
                required = listOf("query")
            )
        )
    )

    @Test
    fun selectedMcpServersAreIncludedInAssistantConfig() {
        val assistant = DesktopAssistantProfile(mcpServerIds = setOf(server.id))
        val data = DesktopData(assistants = listOf(assistant), mcpServers = listOf(server))

        assertEquals(listOf(server), data.configForAssistant(assistant).mcpServers)
    }

    @Test
    fun mcpToolsAreAddedToOpenAiRequest() {
        val body = Json.parseToJsonElement(
            OpenAiClient().buildRequestBody(
                DesktopConfig(model = "test", mcpServers = listOf(server)),
                listOf(ChatMessage("user", "hello"))
            )
        ).jsonObject
        val function = body["tools"]!!.jsonArray.single().jsonObject["function"]!!.jsonObject
        val parameters = function["parameters"]!!.jsonObject

        assertEquals(server.toolCallName(server.tools.single()), function["name"]!!.jsonPrimitive.content)
        assertEquals("object", parameters["type"]!!.jsonPrimitive.content)
        assertTrue(parameters["required"]!!.jsonArray.any { it.jsonPrimitive.content == "query" })
    }

    @Test
    fun generatedToolNamesAreUniqueAndProviderSafe() {
        val longTool = DesktopMcpTool("search.with-invalid-characters-and-a-name-that-is-far-too-long-for-openai")
        val first = server.toolCallName(longTool)
        val second = server.copy(id = "server-2").toolCallName(longTool)

        assertNotEquals(first, second)
        assertTrue(first.length <= 64)
        assertTrue(first.matches(Regex("[A-Za-z0-9_]+")))
    }

    @Test
    fun openAiParametersKeepSchemaDefinitionsAndDoNotForbidDynamicKeys() {
        val definitions = buildJsonObject {
            put("Query", buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject { put("text", buildJsonObject { put("type", "string") }) })
            })
        }
        val parameters = DesktopMcpTool(
            name = "search",
            inputSchema = buildJsonObject {
                put("query", buildJsonObject { put("\$ref", "#/\$defs/Query") })
            },
            definitions = definitions
        ).openAiParameters()

        assertEquals(definitions, parameters["\$defs"])
        assertFalse("additionalProperties" in parameters)
    }

    @Test
    fun toolCacheIsInvalidatedWhenConnectionSettingsChange() {
        val synced = server.withSyncedTools(server.tools)

        assertTrue(synced.hasCurrentTools())
        assertFalse(synced.copy(url = "https://other.example/mcp").hasCurrentTools())
        assertFalse(synced.copy(headers = listOf(DesktopCustomHeader("Authorization", "changed"))).hasCurrentTools())
    }

    @Test
    fun toolResultsPreserveErrorsAndStructuredContentWithoutDumpingMedia() {
        val output = CallToolResult(
            content = listOf(
                TextContent("failed"),
                ImageContent(data = "sensitive-base64", mimeType = "image/png")
            ),
            isError = true,
            structuredContent = buildJsonObject { put("code", "E_TEST") }
        ).desktopOutput()

        assertTrue(output.startsWith("MCP tool error:"))
        assertTrue(output.contains("\"code\":\"E_TEST\""))
        assertTrue(output.contains("image omitted"))
        assertFalse(output.contains("sensitive-base64"))
    }

    @Test
    fun stdioServerKeepsProcessSettings() {
        val base = DesktopMcpServer(
            id = "stdio",
            name = "Local",
            transport = DesktopMcpTransport.STDIO,
            command = "npx",
            arguments = listOf("-y", "@modelcontextprotocol/server-filesystem"),
            environment = listOf(DesktopCustomHeader("ROOT", "/tmp"))
        )

        assertEquals(DesktopMcpTransport.STDIO, base.transport)
        assertEquals("npx", base.command)
        assertEquals(listOf("ROOT"), base.environment.map { it.name })
    }

    @Test
    fun stdioArgumentsCanHaveWhitespaceFromMultilineInput() {
        val arguments = listOf(" /tmp/server.js ", "  mcp  ")

        assertEquals(listOf("/tmp/server.js", "mcp"), arguments.map(String::trim).filter(String::isNotBlank))
    }

    @Test
    fun stoppingProcessAlsoStopsDescendants() {
        val process = ProcessBuilder("sh", "-c", "sleep 30 & wait").start()
        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
            var descendants = process.toHandle().descendants().toList()
            while (descendants.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(10)
                descendants = process.toHandle().descendants().toList()
            }

            assertTrue(descendants.isNotEmpty())
            process.stop()

            assertFalse(process.isAlive)
            assertTrue(descendants.none { it.isAlive })
        } finally {
            if (process.isAlive) process.stop()
        }
    }

    @Test
    fun cancellingProbeStopsUnresponsiveStdioProcess() = runBlocking {
        val pidFile = Files.createTempFile("rikkahub-mcp-test", ".pid")
        Files.deleteIfExists(pidFile)
        val client = DesktopMcpClient()
        val stdioServer = DesktopMcpServer(
            id = "cancel-probe",
            name = "CancelProbe",
            transport = DesktopMcpTransport.STDIO,
            command = "sh",
            arguments = listOf("-c", "echo $$ > '$pidFile'; sleep 30")
        )
        val probe = launch { runCatching { client.probeTools(stdioServer) } }

        try {
            withTimeout(2_000) {
                while (!Files.exists(pidFile) || Files.readString(pidFile).isBlank()) delay(10)
            }
            delay(50)
            val process = ProcessHandle.of(Files.readString(pidFile).trim().toLong()).orElseThrow()

            probe.cancel()
            client.cancelProbe(stdioServer.id)
            probe.cancelAndJoin()

            assertFalse(process.isAlive)
        } finally {
            probe.cancelAndJoin()
            client.close()
            Files.deleteIfExists(pidFile)
        }
    }

    @Test
    fun probeTimeoutStopsUnresponsiveStdioProcess() = runBlocking {
        val pidFile = Files.createTempFile("rikkahub-mcp-timeout-test", ".pid")
        Files.deleteIfExists(pidFile)
        val client = DesktopMcpClient()
        val stdioServer = DesktopMcpServer(
            id = "timeout-probe",
            name = "TimeoutProbe",
            transport = DesktopMcpTransport.STDIO,
            command = "sh",
            arguments = listOf("-c", "echo $$ > '$pidFile'; sleep 30")
        )

        try {
            val result = runCatching { client.probeTools(stdioServer, timeoutMillis = 500) }
            assertTrue(result.isFailure)
            val pid = Files.readString(pidFile).trim().toLong()
            assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false))
        } finally {
            client.close()
            Files.deleteIfExists(pidFile)
        }
    }
}

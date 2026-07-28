package me.rerere.rikkahub.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
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

        assertEquals("mcp__GitHub__search", function["name"]!!.jsonPrimitive.content)
        assertEquals("object", parameters["type"]!!.jsonPrimitive.content)
        assertTrue(parameters["required"]!!.jsonArray.any { it.jsonPrimitive.content == "query" })
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
}

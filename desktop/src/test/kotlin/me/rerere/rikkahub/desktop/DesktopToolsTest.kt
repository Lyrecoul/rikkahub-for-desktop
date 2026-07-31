package me.rerere.rikkahub.desktop

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopToolsTest {
    @TempDir
    lateinit var workspace: Path

    @Test
    fun memoryToolMutatesTheSelectedMemoryScope() = runBlocking {
        var memories = listOf(DesktopMemory(id = "one", content = "old"))
        val handler = DesktopMemoryToolHandler(
            create = { content -> DesktopMemory(id = "two", content = content).also { memories = memories + it } },
            edit = { id, content ->
                val updated = memories.first { it.id == id }.copy(content = content)
                memories = memories.map { if (it.id == id) updated else it }
                updated
            },
            delete = { id -> memories = memories.filterNot { it.id == id } }
        )
        val results = executeDesktopToolCalls(
            OkHttpClient(),
            DesktopConfig(memoryEnabled = true),
            listOf(
                DesktopToolCall("create", DesktopMemoryToolName, "{\"action\":\"create\",\"content\":\"new\"}"),
                DesktopToolCall(
                    "edit",
                    DesktopMemoryToolName,
                    "{\"action\":\"edit\",\"id\":\"one\",\"content\":\"updated\"}"
                ),
                DesktopToolCall("delete", DesktopMemoryToolName, "{\"action\":\"delete\",\"id\":\"two\"}")
            ),
            handler
        )

        assertEquals(listOf(DesktopMemory(id = "one", content = "updated")), memories)
        assertTrue(results[0].content.contains("\"id\":\"two\""))
        assertTrue(results[2].content.contains("\"success\":true"))
    }

    @Test
    fun disabledMemoryToolDoesNotInvokeTheHandler() = runBlocking {
        var invoked = false
        val result = executeDesktopToolCalls(
            OkHttpClient(),
            DesktopConfig(),
            listOf(DesktopToolCall("call", DesktopMemoryToolName, "{\"action\":\"create\",\"content\":\"new\"}")),
            DesktopMemoryToolHandler(
                create = { invoked = true; DesktopMemory(content = it) },
                edit = { _, _ -> error("unexpected") },
                delete = { error("unexpected") }
            )
        ).single()

        assertEquals(false, invoked)
        assertTrue(result.content.contains("not enabled"))
    }

    @Test
    fun askUserToolReturnsTheSubmittedAnswer() = runBlocking {
        val result = executeDesktopToolCalls(
            OkHttpClient(),
            DesktopConfig(localTools = setOf(DesktopLocalTool.ASK_USER)),
            listOf(DesktopToolCall("call", DesktopAskUserToolName, "{}")),
            askUserHandler = { "{\"answers\":{\"goal\":\"Write tests\"}}" }
        ).single()

        assertEquals("tool", result.role)
        assertEquals("call", result.toolCallId)
        assertEquals("{\"answers\":{\"goal\":\"Write tests\"}}", result.content)
    }

    @Test
    fun deniedAgentToolReportsTheUserDecision() = runBlocking {
        val result = executeDesktopToolCalls(
            OkHttpClient(),
            DesktopConfig(
                agent = DesktopAgentConfig(
                    DesktopAgentWorkspace(
                        workspace.toString(),
                        DesktopAgentBackend.LOCAL
                    )
                )
            ),
            listOf(DesktopToolCall("call", DesktopAgentShellToolName, "{\"command\":\"pwd\",\"network\":false}")),
            approvalHandler = { _, _ -> false }
        ).single()

        assertEquals(DesktopAgentApprovalDeniedResult, result.content)
        assertTrue(result.content.contains("用户拒绝"))
    }
}

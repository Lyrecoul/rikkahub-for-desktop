package me.rerere.rikkahub.desktop

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopAgentTest {
    @TempDir
    lateinit var root: Path

    @Test
    fun readsOnlyFilesInsideTheSelectedWorkspace() = runBlocking {
        Files.writeString(root.resolve("notes.txt"), "hello")
        val runtime = DesktopAgentRuntime(skillsRoot = root.resolve("skills"))
        val config = DesktopAgentConfig(DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.LOCAL))

        val content = runtime.execute(config, DesktopToolCall("read", DesktopAgentReadFileToolName, "{\"path\":\"notes.txt\"}")) { true }

        assertEquals("hello", content)
        try {
            runtime.execute(config, DesktopToolCall("escape", DesktopAgentReadFileToolName, "{\"path\":\"../secret\"}")) { true }
            error("Expected path escape to fail")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun deniedWriteDoesNotCreateAFile() = runBlocking {
        val runtime = DesktopAgentRuntime(skillsRoot = root.resolve("skills"))
        val config = DesktopAgentConfig(DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.LOCAL))

        try {
            runtime.execute(
                config,
                DesktopToolCall("write", DesktopAgentWriteFileToolName, "{\"path\":\"new.txt\",\"text\":\"blocked\"}")
            ) { false }
            error("Expected denied write to fail")
        } catch (error: DesktopAgentApprovalDeniedException) {
            assertEquals("User denied this tool operation", error.message)
        }

        assertFalse(Files.exists(root.resolve("new.txt")))
    }

    @Test
    fun writeAndEditRequireApprovalAndStayWithinRoot() = runBlocking {
        val runtime = DesktopAgentRuntime(skillsRoot = root.resolve("skills"))
        val config = DesktopAgentConfig(DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.LOCAL))
        val approvals = mutableListOf<DesktopAgentApprovalKind>()
        val approve: suspend (DesktopAgentApprovalRequest) -> Boolean = { approvals += it.kind; true }

        runtime.execute(config, DesktopToolCall("write", DesktopAgentWriteFileToolName, "{\"path\":\"src/a.txt\",\"text\":\"old\"}"), approve)
        runtime.execute(config, DesktopToolCall("edit", DesktopAgentEditFileToolName, "{\"path\":\"src/a.txt\",\"old_text\":\"old\",\"new_text\":\"new\"}"), approve)

        assertEquals("new", Files.readString(root.resolve("src/a.txt")))
        assertEquals(listOf(DesktopAgentApprovalKind.WRITE, DesktopAgentApprovalKind.WRITE), approvals)
    }

    @Test
    fun dockerNetworkShellRequestsOneCombinedApproval() = runBlocking {
        val runtime = DesktopAgentRuntime(skillsRoot = root.resolve("skills"))
        val config = DesktopAgentConfig(DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.DOCKER))
        val requests = mutableListOf<DesktopAgentApprovalRequest>()

        try {
            runtime.execute(
                config,
                DesktopToolCall("shell", DesktopAgentShellToolName, "{\"command\":\"curl example.com\",\"network\":true}")
            ) { request ->
                requests += request
                false
            }
            error("Expected denied shell to fail")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }

        assertEquals(1, requests.size)
        assertEquals(DesktopAgentApprovalKind.SHELL, requests.single().kind)
        assertTrue(requests.single().network)
        assertEquals(DesktopAgentBackend.DOCKER, requests.single().backend)
    }

    @Test
    fun dockerNetworkGrantCoversOnlyTheSameWorkspaceShellCommands() {
        val workspace = DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.DOCKER, "ubuntu:24.04")
        val otherWorkspace = workspace.copy(rootPath = root.resolve("other").toString())
        val shell = DesktopAgentApprovalRequest(
            DesktopAgentApprovalKind.SHELL,
            "执行命令",
            "pwd",
            DesktopAgentBackend.DOCKER,
            workspace = workspace
        )
        val networkShell = shell.copy(network = true)
        val otherShell = shell.copy(workspace = otherWorkspace)
        val networkGrant = requireNotNull(networkShell.rememberedGrant())
        val shellGrant = requireNotNull(shell.rememberedGrant())

        assertTrue(networkShell.canRemember)
        assertTrue(setOf(networkGrant).approves(networkShell))
        assertTrue(setOf(networkGrant).approves(shell))
        assertFalse(setOf(shellGrant).approves(networkShell))
        assertFalse(setOf(networkGrant).approves(otherShell))
        val localShell = DesktopAgentApprovalRequest(
            DesktopAgentApprovalKind.SHELL,
            "执行命令",
            "pwd",
            DesktopAgentBackend.LOCAL
        )
        assertFalse(localShell.canRemember)
        assertNull(localShell.rememberedGrant())
    }

    @Test
    fun imagePullGrantIsBoundToTheExactWorkspaceImage() {
        val workspace = DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.DOCKER, "ubuntu:24.04")
        val request = DesktopAgentApprovalRequest(
            DesktopAgentApprovalKind.IMAGE_PULL,
            "下载容器镜像",
            workspace.dockerImage,
            workspace = workspace
        )
        val differentImage = request.copy(
            detail = "alpine:3.21",
            workspace = workspace.copy(dockerImage = "alpine:3.21")
        )
        val grant = requireNotNull(request.rememberedGrant())

        assertTrue(setOf(grant).approves(request))
        assertFalse(setOf(grant).approves(differentImage))
    }

    @Test
    fun agentDefinitionsAreExposedOnlyForBoundWorkspaces() {
        assertTrue(agentToolDefinitions(null).isEmpty())
        val definitions = agentToolDefinitions(
            DesktopAgentConfig(DesktopAgentWorkspace("/workspace"), enabledSkillNames = setOf("repo-guide"))
        )
        val names = definitions.map { it.jsonObject.getValue("function").jsonObject.getValue("name").jsonPrimitive.content }

        assertTrue(DesktopAgentShellToolName in names)
        assertTrue(DesktopUseSkillToolName in names)

        val shell = definitions.first { definition ->
            definition.jsonObject.getValue("function").jsonObject.getValue("name").jsonPrimitive.content == DesktopAgentShellToolName
        }.jsonObject.getValue("function").jsonObject
        assertTrue(shell.getValue("description").jsonPrimitive.content.contains("MUST set network=true"))
        assertEquals(
            listOf("command", "network"),
            shell.getValue("parameters").jsonObject.getValue("required").jsonArray.map { it.jsonPrimitive.content }
        )
    }
}

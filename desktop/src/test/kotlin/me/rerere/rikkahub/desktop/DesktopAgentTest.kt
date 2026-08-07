package me.rerere.rikkahub.desktop

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopAgentTest {
    @TempDir
    lateinit var root: Path

    private val managedDockerConfig =
        "rikkahub-agent-isolated|[\"SETUID\"]|{\"io.rikkahub.agent.schema\":\"2\"}|" +
            "[{\"Destination\":\"/root\"}]|{\"/tmp\":\"rw,nosuid,nodev,size=536870912\"}"

    @Test
    fun readsOnlyFilesInsideTheSelectedWorkspace() = runBlocking {
        Files.writeString(root.resolve("notes.txt"), "hello")
        val runtime = DesktopAgentRuntime(skillsRoot = root.resolve("skills"))
        val config = DesktopAgentConfig(DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.LOCAL))

        val content = runtime.execute(
            config,
            DesktopToolCall("read", DesktopAgentReadFileToolName, "{\"path\":\"notes.txt\"}")
        ) { true }

        assertEquals("hello", content)
        try {
            runtime.execute(
                config,
                DesktopToolCall("escape", DesktopAgentReadFileToolName, "{\"path\":\"../secret\"}")
            ) { true }
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
    fun cancellingShellCommandStopsTheToolCallPromptly() = runBlocking {
        val runtime = DesktopAgentRuntime(skillsRoot = root.resolve("skills"))
        val config = DesktopAgentConfig(DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.LOCAL))
        val started = root.resolve("started")
        val job = launch {
            runtime.execute(
                config,
                DesktopToolCall(
                    "shell",
                    DesktopAgentShellToolName,
                    "{\"command\":\"touch started; sleep 30\",\"network\":false}"
                )
            ) { true }
        }

        withTimeout(5_000) {
            while (!Files.exists(started)) delay(10)
        }
        withTimeout(2_000) { job.cancelAndJoin() }
    }

    @Test
    fun writeAndEditRequireApprovalAndStayWithinRoot() = runBlocking {
        val runtime = DesktopAgentRuntime(skillsRoot = root.resolve("skills"))
        val config = DesktopAgentConfig(DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.LOCAL))
        val approvals = mutableListOf<DesktopAgentApprovalKind>()
        val approve: suspend (DesktopAgentApprovalRequest) -> Boolean = { approvals += it.kind; true }

        runtime.execute(
            config,
            DesktopToolCall("write", DesktopAgentWriteFileToolName, "{\"path\":\"src/a.txt\",\"text\":\"old\"}"),
            approve
        )
        runtime.execute(
            config,
            DesktopToolCall(
                "edit",
                DesktopAgentEditFileToolName,
                "{\"path\":\"src/a.txt\",\"old_text\":\"old\",\"new_text\":\"new\"}"
            ),
            approve
        )

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
                DesktopToolCall(
                    "shell",
                    DesktopAgentShellToolName,
                    "{\"command\":\"curl example.com\",\"network\":true}"
                )
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
    fun missingDockerContainerRecoversFromItsCheckpointImage() = runBlocking {
        val commands = mutableListOf<List<String>>()
        val runtime = DesktopAgentRuntime(
            commandRunner = DesktopAgentCommandRunner { command, _, _ ->
                commands += command
                when {
                    command.take(3) == listOf("docker", "image", "inspect") &&
                        command.last().endsWith("-snapshot") -> DesktopAgentCommandResult(0, "", "")
                    command.take(3) == listOf("docker", "image", "inspect") ->
                        DesktopAgentCommandResult(1, "", "base image was pruned")
                    command.take(3) == listOf("docker", "container", "inspect") ->
                        DesktopAgentCommandResult(1, "", "not found")
                    else -> DesktopAgentCommandResult(0, "", "")
                }
            },
            skillsRoot = root.resolve("skills")
        )
        val workspace = DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.DOCKER)

        runtime.execute(
            DesktopAgentConfig(workspace),
            DesktopToolCall("shell", DesktopAgentShellToolName, "{\"command\":\"true\",\"network\":false}")
        ) { true }

        val create = commands.single { it.take(2) == listOf("docker", "create") }
        assertTrue(create[create.lastIndex - 2].endsWith("-snapshot"))
        assertFalse(commands.any { it.take(2) == listOf("docker", "pull") })
    }

    @Test
    fun legacyDockerContainerIsMigratedToPersistentHomeStorage() = runBlocking {
        val commands = mutableListOf<List<String>>()
        val runtime = DesktopAgentRuntime(
            commandRunner = DesktopAgentCommandRunner { command, _, _ ->
                commands += command
                when {
                    command.take(3) == listOf("docker", "inspect", "--format") ->
                        DesktopAgentCommandResult(0, "rikkahub-agent-isolated|[\"SETUID\"]\n", "")
                    command.take(3) == listOf("docker", "volume", "inspect") ->
                        DesktopAgentCommandResult(1, "", "not found")
                    else -> DesktopAgentCommandResult(0, "", "")
                }
            },
            skillsRoot = root.resolve("skills")
        )
        val config = DesktopAgentConfig(DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.DOCKER))

        runtime.execute(
            config,
            DesktopToolCall("shell", DesktopAgentShellToolName, "{\"command\":\"true\",\"network\":false}")
        ) { true }

        assertTrue(commands.any { it.take(2) == listOf("docker", "commit") })
        val create = commands.single { it.take(2) == listOf("docker", "create") }
        assertTrue(create.any { it.endsWith("-home,dst=/root") })
        assertTrue(create.windowed(2).any { it == listOf("--tmpfs", "/tmp:rw,nosuid,nodev,size=536870912") })
        assertTrue(create.windowed(2).any { it == listOf("--label", "io.rikkahub.agent.managed=true") })
        val volumeCreate = commands.single { it.take(3) == listOf("docker", "volume", "create") }
        assertTrue(volumeCreate.windowed(2).any { it == listOf("--label", "io.rikkahub.agent.managed=true") })
    }

    @Test
    fun dockerCommandsForTheSameWorkspaceRunSerially() = runBlocking {
        val activeCommands = AtomicInteger()
        val maximumActiveCommands = AtomicInteger()
        val runtime = DesktopAgentRuntime(
            commandRunner = DesktopAgentCommandRunner { command, _, _ ->
                if (command.take(2) == listOf("docker", "exec")) {
                    val active = activeCommands.incrementAndGet()
                    maximumActiveCommands.updateAndGet { maxOf(it, active) }
                    Thread.sleep(100)
                    activeCommands.decrementAndGet()
                }
                if (command.take(3) == listOf("docker", "inspect", "--format")) {
                    DesktopAgentCommandResult(0, managedDockerConfig, "")
                } else {
                    DesktopAgentCommandResult(0, "", "")
                }
            },
            skillsRoot = root.resolve("skills")
        )
        val config = DesktopAgentConfig(DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.DOCKER))

        List(2) { index ->
            async {
                runtime.execute(
                    config,
                    DesktopToolCall(
                        "shell-$index",
                        DesktopAgentShellToolName,
                        "{\"command\":\"true\",\"network\":false}"
                    )
                ) { true }
            }
        }.awaitAll()

        assertEquals(1, maximumActiveCommands.get())
    }

    @Test
    fun cancellingHostNetworkCommandRestoresTheIsolatedContainer() = runBlocking {
        val commandStarted = CompletableDeferred<Unit>()
        val commands = mutableListOf<List<String>>()
        var mainContainerName: String? = null
        var mainContainerExists = true
        val runtime = DesktopAgentRuntime(
            commandRunner = DesktopAgentCommandRunner { command, _, _ ->
                synchronized(commands) { commands += command }
                when {
                    command.take(3) == listOf("docker", "container", "inspect") -> {
                        mainContainerName = mainContainerName ?: command.last()
                        DesktopAgentCommandResult(
                            if (mainContainerExists) 0 else 1,
                            "",
                            if (mainContainerExists) "" else "not found"
                        )
                    }
                    command.take(3) == listOf("docker", "inspect", "--format") ->
                        DesktopAgentCommandResult(0, managedDockerConfig, "")
                    command.take(3) == listOf("docker", "rm", "-f") && command.last() == mainContainerName -> {
                        mainContainerExists = false
                        DesktopAgentCommandResult(0, "", "")
                    }
                    command.take(3) == listOf("docker", "create", "--name") &&
                        command[3] == mainContainerName -> {
                        mainContainerExists = true
                        DesktopAgentCommandResult(0, "", "")
                    }
                    command.take(2) == listOf("docker", "exec") -> {
                        commandStarted.complete(Unit)
                        Thread.sleep(30_000)
                        DesktopAgentCommandResult(0, "", "")
                    }
                    else -> DesktopAgentCommandResult(0, "", "")
                }
            },
            skillsRoot = root.resolve("skills")
        )
        val workspace = DesktopAgentWorkspace(
            root.toString(),
            DesktopAgentBackend.DOCKER,
            dockerNetworkMode = DesktopAgentDockerNetworkMode.HOST
        )
        val job = launch {
            runtime.execute(
                DesktopAgentConfig(workspace),
                DesktopToolCall("shell", DesktopAgentShellToolName, "{\"command\":\"curl example.com\",\"network\":true}")
            ) { true }
        }

        withTimeout(5_000) { commandStarted.await() }
        withTimeout(5_000) { job.cancelAndJoin() }

        assertTrue(mainContainerExists)
        assertTrue(
            synchronized(commands) {
                commands.any { command ->
                    command.take(3) == listOf("docker", "create", "--name") &&
                        command[3] == mainContainerName && command[command.lastIndex - 2].endsWith("-snapshot")
                }
            }
        )
    }

    @Test
    fun bridgeNetworkReusesTheContainerAndCreatesOneCheckpoint() = runBlocking {
        val commands = mutableListOf<List<String>>()
        val runtime = DesktopAgentRuntime(
            commandRunner = DesktopAgentCommandRunner { command, _, _ ->
                commands += command
                when {
                    command.take(3) == listOf("docker", "inspect", "--format") ->
                        DesktopAgentCommandResult(0, managedDockerConfig, "")
                    command.take(3) == listOf("docker", "image", "inspect") &&
                        "--format" in command -> DesktopAgentCommandResult(0, "sha256:checkpoint\n", "")
                    else -> DesktopAgentCommandResult(0, "", "")
                }
            },
            skillsRoot = root.resolve("skills")
        )
        val workspace = DesktopAgentWorkspace(
            root.toString(),
            DesktopAgentBackend.DOCKER,
            dockerNetworkMode = DesktopAgentDockerNetworkMode.BRIDGE
        )

        runtime.execute(
            DesktopAgentConfig(workspace),
            DesktopToolCall("shell", DesktopAgentShellToolName, "{\"command\":\"curl example.com\",\"network\":true}")
        ) { true }

        assertEquals(1, commands.count { it.take(2) == listOf("docker", "commit") })
        assertFalse(commands.any { it.take(3) == listOf("docker", "rm", "-f") })
        assertTrue(commands.any { it.take(3) == listOf("docker", "network", "connect") })
        assertTrue(commands.any { it.take(3) == listOf("docker", "network", "disconnect") })
    }

    @Test
    fun dockerWorkspaceStatusReportsManagedResources() = runBlocking {
        val runtime = DesktopAgentRuntime(
            commandRunner = DesktopAgentCommandRunner { command, _, _ ->
                when {
                    command.take(3) == listOf("docker", "container", "inspect") ->
                        DesktopAgentCommandResult(0, "running\n", "")
                    command.take(3) == listOf("docker", "image", "inspect") ->
                        DesktopAgentCommandResult(0, "", "")
                    command.take(3) == listOf("docker", "volume", "inspect") ->
                        DesktopAgentCommandResult(0, "", "")
                    else -> DesktopAgentCommandResult(1, "", "not found")
                }
            },
            skillsRoot = root.resolve("skills")
        )

        val status = runtime.dockerWorkspaceStatus(
            DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.DOCKER)
        )

        assertEquals("running", status.containerState)
        assertTrue(status.checkpointExists)
        assertTrue(status.homeVolumeExists)
    }

    @Test
    fun resettingDockerWorkspaceRemovesOnlyItsManagedResources() = runBlocking {
        val commands = mutableListOf<List<String>>()
        val runtime = DesktopAgentRuntime(
            commandRunner = DesktopAgentCommandRunner { command, _, _ ->
                commands += command
                DesktopAgentCommandResult(0, "", "")
            },
            skillsRoot = root.resolve("skills")
        )

        runtime.resetDockerWorkspace(DesktopAgentWorkspace(root.toString(), DesktopAgentBackend.DOCKER))

        val containerRemovals = commands.filter { it.take(3) == listOf("docker", "rm", "-f") }
        assertEquals(2, containerRemovals.size)
        val workspaceName = containerRemovals.first().last().removeSuffix("-network")
        assertTrue(containerRemovals.any { it.last() == workspaceName })
        assertTrue(containerRemovals.any { it.last() == "$workspaceName-network" })
        assertTrue(commands.any { it == listOf("docker", "image", "rm", "$workspaceName-snapshot") })
        assertTrue(commands.any { it == listOf("docker", "network", "rm", "$workspaceName-network") })
        assertTrue(commands.any { it == listOf("docker", "volume", "rm", "$workspaceName-home") })
    }

    @Test
    fun agentDefinitionsAreExposedOnlyForBoundWorkspaces() {
        assertTrue(agentToolDefinitions(null).isEmpty())
        val definitions = agentToolDefinitions(
            DesktopAgentConfig(DesktopAgentWorkspace("/workspace"), enabledSkillNames = setOf("repo-guide"))
        )
        val names =
            definitions.map { it.jsonObject.getValue("function").jsonObject.getValue("name").jsonPrimitive.content }

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

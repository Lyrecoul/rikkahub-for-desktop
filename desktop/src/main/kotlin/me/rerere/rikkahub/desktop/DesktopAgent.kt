package me.rerere.rikkahub.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.util.concurrent.TimeUnit

internal const val DesktopAgentListFilesToolName = "agent_list_files"
internal const val DesktopAgentSearchFilesToolName = "agent_search_files"
internal const val DesktopAgentReadFileToolName = "agent_read_file"
internal const val DesktopAgentWriteFileToolName = "agent_write_file"
internal const val DesktopAgentEditFileToolName = "agent_edit_file"
internal const val DesktopAgentShellToolName = "agent_shell"
internal const val DesktopUseSkillToolName = "use_skill"

internal enum class DesktopAgentApprovalKind { WRITE, SHELL, IMAGE_PULL, SKILL }

internal enum class DesktopAgentApprovalScope { WRITE, SKILL, DOCKER_SHELL, DOCKER_NETWORK, IMAGE_PULL }

internal data class DesktopAgentApprovalGrant(
    val scope: DesktopAgentApprovalScope,
    val workspace: DesktopAgentWorkspace? = null
)

internal data class DesktopAgentApprovalRequest(
    val kind: DesktopAgentApprovalKind,
    val title: String,
    val detail: String,
    val backend: DesktopAgentBackend? = null,
    val network: Boolean = false,
    val workspace: DesktopAgentWorkspace? = null
)

internal fun DesktopAgentApprovalRequest.rememberedGrant(): DesktopAgentApprovalGrant? = when (kind) {
    DesktopAgentApprovalKind.WRITE -> DesktopAgentApprovalGrant(DesktopAgentApprovalScope.WRITE)
    DesktopAgentApprovalKind.SKILL -> DesktopAgentApprovalGrant(DesktopAgentApprovalScope.SKILL)
    DesktopAgentApprovalKind.SHELL -> workspace?.takeIf { it.backend == DesktopAgentBackend.DOCKER }?.let {
        DesktopAgentApprovalGrant(
            if (network) DesktopAgentApprovalScope.DOCKER_NETWORK else DesktopAgentApprovalScope.DOCKER_SHELL,
            it
        )
    }

    DesktopAgentApprovalKind.IMAGE_PULL -> workspace?.let {
        DesktopAgentApprovalGrant(DesktopAgentApprovalScope.IMAGE_PULL, it)
    }
}

internal val DesktopAgentApprovalRequest.canRemember: Boolean
    get() = rememberedGrant() != null

internal fun Set<DesktopAgentApprovalGrant>.approves(request: DesktopAgentApprovalRequest): Boolean {
    val grant = request.rememberedGrant() ?: return false
    return grant in this || (
        grant.scope == DesktopAgentApprovalScope.DOCKER_SHELL &&
            DesktopAgentApprovalGrant(DesktopAgentApprovalScope.DOCKER_NETWORK, grant.workspace) in this
        )
}

internal class DesktopAgentApprovalDeniedException : IllegalArgumentException("User denied this tool operation")

internal data class DesktopAgentCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false,
    val truncated: Boolean = false
)

internal fun interface DesktopAgentCommandRunner {
    fun run(command: List<String>, workingDirectory: File?, timeoutMillis: Long): DesktopAgentCommandResult
}

internal class ProcessDesktopAgentCommandRunner : DesktopAgentCommandRunner {
    override fun run(command: List<String>, workingDirectory: File?, timeoutMillis: Long): DesktopAgentCommandResult {
        val process = ProcessBuilder(command).directory(workingDirectory).start()
        return process.readAgentResult(timeoutMillis)
    }
}

private const val AgentMaxOutputChars = 128 * 1024
private const val AgentMaxReadBytes = 512 * 1024L
private const val AgentMaxWriteBytes = 2 * 1024 * 1024
private const val DesktopAgentIsolatedNetworkName = "rikkahub-agent-isolated"

private fun Process.readAgentResult(timeoutMillis: Long): DesktopAgentCommandResult {
    val stdout = AgentStreamCollector(inputStream)
    val stderr = AgentStreamCollector(errorStream)
    val finished = try {
        waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
    } catch (error: InterruptedException) {
        destroyAgentProcessTree()
        throw error
    }
    if (!finished) destroyAgentProcessTree()
    stdout.join(1_000)
    stderr.join(1_000)
    return DesktopAgentCommandResult(
        exitCode = if (finished) exitValue() else -1,
        stdout = stdout.text(),
        stderr = stderr.text(),
        timedOut = !finished,
        truncated = stdout.truncated || stderr.truncated
    )
}

private fun Process.destroyAgentProcessTree() {
    // A shell often has a separate child process. Killing only the shell leaves that command running after cancellation.
    toHandle().descendants().use { descendants ->
        descendants.toList().asReversed().forEach { it.destroyForcibly() }
    }
    destroyForcibly()
}

private class AgentStreamCollector(stream: InputStream) {
    private val value = StringBuilder()

    @Volatile
    var truncated = false
        private set
    private val thread = Thread {
        runCatching {
            stream.bufferedReader().use { reader ->
                val buffer = CharArray(4_096)
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    synchronized(value) {
                        val remaining = AgentMaxOutputChars - value.length
                        if (remaining > 0) value.append(buffer, 0, minOf(remaining, count))
                        if (count > remaining) truncated = true
                    }
                }
            }
        }
    }.apply { isDaemon = true; start() }

    fun join(timeoutMillis: Long) = thread.join(timeoutMillis)
    fun text(): String = synchronized(value) { value.toString() }
}

internal class DesktopAgentRuntime(
    private val commandRunner: DesktopAgentCommandRunner = ProcessDesktopAgentCommandRunner(),
    private val skillsRoot: Path = defaultDesktopSkillsRoot()
) {
    suspend fun execute(
        config: DesktopAgentConfig,
        call: DesktopToolCall,
        approve: suspend (DesktopAgentApprovalRequest) -> Boolean
    ): String = withContext(Dispatchers.IO) {
        val root = requireWorkspaceRoot(config.workspace)
        val params = Json.parseToJsonElement(call.arguments).jsonObject
        when (call.name) {
            DesktopAgentListFilesToolName -> listFiles(root, params.optionalString("path").orEmpty())
            DesktopAgentSearchFilesToolName -> searchFiles(
                root,
                params.requiredString("query"),
                params.optionalString("path").orEmpty()
            )

            DesktopAgentReadFileToolName -> readFile(root, params.requiredString("path"))
            DesktopAgentWriteFileToolName -> {
                requireApproval(
                    approve(
                        DesktopAgentApprovalRequest(
                            DesktopAgentApprovalKind.WRITE,
                            "写入文件",
                            params.requiredString("path")
                        )
                    )
                )
                writeFile(root, params.requiredString("path"), params.requiredString("text"))
            }

            DesktopAgentEditFileToolName -> {
                requireApproval(
                    approve(
                        DesktopAgentApprovalRequest(
                            DesktopAgentApprovalKind.WRITE,
                            "编辑文件",
                            params.requiredString("path")
                        )
                    )
                )
                editFile(
                    root,
                    params.requiredString("path"),
                    params.requiredString("old_text"),
                    params.requiredString("new_text")
                )
            }

            DesktopAgentShellToolName -> {
                val command = params.requiredString("command")
                val network = params.optionalBoolean("network") ?: false
                requireApproval(
                    approve(
                        DesktopAgentApprovalRequest(
                            DesktopAgentApprovalKind.SHELL,
                            if (config.workspace.backend == DesktopAgentBackend.DOCKER && network) "执行联网命令" else "执行命令",
                            command.take(2_000),
                            config.workspace.backend,
                            network,
                            config.workspace
                        )
                    )
                )
                shell(config.workspace, root, command, params.optionalString("cwd").orEmpty(), network, approve)
            }

            DesktopUseSkillToolName -> {
                val name = params.requiredString("name")
                require(name in config.enabledSkillNames) { "Skill '$name' is not enabled for this assistant" }
                requireApproval(
                    approve(
                        DesktopAgentApprovalRequest(
                            DesktopAgentApprovalKind.SKILL,
                            "加载 Skill",
                            name
                        )
                    )
                )
                readSkill(name, params.optionalString("path"))
            }

            else -> error("Unsupported agent tool: ${call.name}")
        }
    }

    private fun listFiles(root: Path, relative: String): String {
        val dir = resolvePath(root, relative.ifBlank { "." }, mustExist = true)
        require(Files.isDirectory(dir)) { "Path is not a directory: $relative" }
        return Files.list(dir).use { paths ->
            paths.sorted().limit(500).iterator().asSequence().joinToString("\n") { path ->
                val suffix = if (Files.isDirectory(path)) "/" else ""
                root.relativize(path).toString() + suffix
            }
        }
    }

    private fun searchFiles(root: Path, query: String, relative: String): String {
        val start = resolvePath(root, relative.ifBlank { "." }, mustExist = true)
        val matches = mutableListOf<String>()
        Files.walk(start).use { paths ->
            paths.filter { Files.isRegularFile(it) }.limit(5_000).forEach { file ->
                if (matches.size >= 100 || Files.size(file) > AgentMaxReadBytes) return@forEach
                runCatching { Files.readAllLines(file, StandardCharsets.UTF_8) }.getOrNull()
                    ?.forEachIndexed { index, line ->
                        if (matches.size < 100 && line.contains(query, ignoreCase = true)) {
                            matches += "${root.relativize(file)}:${index + 1}:$line"
                        }
                    }
            }
        }
        return matches.joinToString("\n")
    }

    private fun readFile(root: Path, relative: String): String {
        val file = resolvePath(root, relative, mustExist = true)
        require(Files.isRegularFile(file)) { "Path is not a file: $relative" }
        require(Files.size(file) <= AgentMaxReadBytes) { "File is too large to read" }
        return Files.readString(file, StandardCharsets.UTF_8)
    }

    private fun writeFile(root: Path, relative: String, text: String): String {
        require(text.toByteArray(StandardCharsets.UTF_8).size <= AgentMaxWriteBytes) { "Content is too large" }
        val file = resolvePath(root, relative, mustExist = false)
        Files.createDirectories(file.parent)
        require(file.parent.toRealPath().startsWith(root)) { "Path escapes workspace root" }
        Files.writeString(
            file,
            text,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
        return "Wrote ${root.relativize(file)}"
    }

    private fun editFile(root: Path, relative: String, oldText: String, newText: String): String {
        require(oldText.isNotEmpty()) { "old_text must not be empty" }
        val file = resolvePath(root, relative, mustExist = true)
        val original = Files.readString(file, StandardCharsets.UTF_8)
        require(original.indexOf(oldText) >= 0) { "old_text was not found" }
        require(original.indexOf(oldText) == original.lastIndexOf(oldText)) { "old_text must occur exactly once" }
        return writeFile(root, relative, original.replace(oldText, newText))
    }

    private suspend fun shell(
        workspace: DesktopAgentWorkspace,
        root: Path,
        command: String,
        cwd: String,
        network: Boolean,
        approve: suspend (DesktopAgentApprovalRequest) -> Boolean
    ): String {
        val directoryPath = resolvePath(root, cwd.ifBlank { "." }, mustExist = true)
        val directory = directoryPath.toFile()
        require(directory.isDirectory) { "cwd is not a directory" }
        val result = when (workspace.backend) {
            DesktopAgentBackend.LOCAL -> runCommand(listOf("/bin/sh", "-lc", command), directory, 600_000)
            DesktopAgentBackend.DOCKER -> dockerShell(
                workspace,
                root,
                command,
                root.relativize(directoryPath).toString().ifBlank { "." },
                network,
                approve
            )
        }
        return buildString {
            append("exitCode: ${result.exitCode}\n")
            if (result.timedOut) append("timedOut: true\n")
            if (result.truncated) append("truncated: true\n")
            if (result.stdout.isNotBlank()) append("stdout:\n${result.stdout}\n")
            if (result.stderr.isNotBlank()) append("stderr:\n${result.stderr}")
        }.trim()
    }

    private suspend fun dockerShell(
        workspace: DesktopAgentWorkspace,
        root: Path,
        command: String,
        cwd: String,
        network: Boolean,
        approve: suspend (DesktopAgentApprovalRequest) -> Boolean
    ): DesktopAgentCommandResult {
        require(',' !in root.toString()) { "Docker workspaces cannot contain commas in their path" }
        val name = "rikkahub-agent-${
            UUID.nameUUIDFromBytes(root.toString().toByteArray()).toString().replace("-", "").take(24)
        }"
        if (runCommand(listOf("docker", "image", "inspect", workspace.dockerImage), null, 10_000).exitCode != 0) {
            requireApproval(
                approve(
                    DesktopAgentApprovalRequest(
                        DesktopAgentApprovalKind.IMAGE_PULL,
                        "下载容器镜像",
                        workspace.dockerImage,
                        workspace = workspace
                    )
                )
            )
            requireDockerSuccess(
                runCommand(listOf("docker", "pull", workspace.dockerImage), null, 600_000),
                "pull Docker image"
            )
        }
        ensureDockerNetwork(DesktopAgentIsolatedNetworkName, internal = true)
        val existing = runCommand(listOf("docker", "container", "inspect", name), null, 10_000)
        val containerImage = if (existing.exitCode == 0 && containerNeedsMigration(name)) {
            migrateLegacyContainer(name)
        } else {
            workspace.dockerImage
        }
        if (existing.exitCode != 0 || containerImage != workspace.dockerImage) {
            val create = dockerCreateCommand(name, containerImage, DesktopAgentIsolatedNetworkName, root)
            requireDockerSuccess(runCommand(create, null, 30_000), "create Docker workspace")
        }
        val start = runCommand(listOf("docker", "start", name), null, 30_000)
        if (start.exitCode != 0 && !start.stderr.contains("already started", ignoreCase = true)) {
            requireDockerSuccess(start, "start Docker workspace")
        }
        if (network) return dockerShellWithHostNetwork(name, root, cwd, command)
        return runCommand(
            listOf("docker", "exec", "-w", "/workspace/$cwd", name, "/bin/sh", "-lc", command),
            null,
            600_000
        )
    }

    /**
     * Some Linux hosts deliberately disable Docker bridge NAT. A temporary host-network container
     * is the only reliable approved-network path there. Its filesystem is committed before it is
     * returned to the normal isolated container, so package installs survive the command.
     */
    private suspend fun dockerShellWithHostNetwork(
        name: String,
        root: Path,
        cwd: String,
        command: String
    ): DesktopAgentCommandResult {
        val snapshot = "$name-snapshot"
        val networkName = "$name-network"
        requireDockerSuccess(
            runCommand(listOf("docker", "commit", name, snapshot), null, 600_000),
            "snapshot Docker workspace"
        )
        requireDockerSuccess(
            runCommand(listOf("docker", "rm", "-f", name), null, 30_000),
            "prepare approved network session"
        )
        try {
            requireDockerSuccess(
                runCommand(dockerCreateCommand(networkName, snapshot, "host", root), null, 30_000),
                "create approved network session"
            )
            requireDockerSuccess(
                runCommand(listOf("docker", "start", networkName), null, 30_000),
                "start approved network session"
            )
            val result = runCommand(
                listOf("docker", "exec", "-w", "/workspace/$cwd", networkName, "/bin/sh", "-lc", command),
                null,
                600_000
            )
            requireDockerSuccess(
                runCommand(listOf("docker", "commit", networkName, snapshot), null, 600_000),
                "persist approved network session"
            )
            return result
        } finally {
            runCommand(listOf("docker", "rm", "-f", networkName), null, 30_000)
            if (runCommand(listOf("docker", "container", "inspect", name), null, 10_000).exitCode != 0) {
                ensureDockerNetwork(DesktopAgentIsolatedNetworkName, internal = true)
                requireDockerSuccess(
                    runCommand(
                        dockerCreateCommand(name, snapshot, DesktopAgentIsolatedNetworkName, root),
                        null,
                        30_000
                    ),
                    "restore isolated Docker workspace"
                )
            }
        }
    }

    private fun dockerCreateCommand(name: String, image: String, network: String, root: Path): List<String> = listOf(
        "docker", "create", "--name", name, "--network", network, "--cap-drop", "ALL",
        "--cap-add", "SETUID", "--cap-add", "SETGID", "--cap-add", "CHOWN", "--cap-add", "FOWNER",
        "--cap-add", "DAC_OVERRIDE", "--security-opt", "no-new-privileges",
        "--mount", "type=bind,src=${root},dst=/workspace", image, "sleep", "infinity"
    )

    private suspend fun ensureDockerNetwork(name: String, internal: Boolean) {
        val inspect = runCommand(listOf("docker", "network", "inspect", name), null, 10_000)
        if (inspect.exitCode == 0) return
        val command = buildList {
            addAll(listOf("docker", "network", "create", "--driver", "bridge"))
            if (internal) add("--internal")
            add(name)
        }
        val create = runCommand(
            command,
            null,
            30_000
        )
        if (create.exitCode != 0 && !create.stderr.contains("already exists", ignoreCase = true)) {
            requireDockerSuccess(create, "create the approved Docker network")
        }
    }

    private suspend fun containerNeedsMigration(name: String): Boolean {
        val result = runCommand(
            listOf("docker", "inspect", "--format", "{{.HostConfig.NetworkMode}}|{{json .HostConfig.CapAdd}}", name),
            null,
            10_000
        )
        val config = result.stdout.trim()
        return result.exitCode == 0 && (config.startsWith("none|") || !config.contains("SETUID"))
    }

    private suspend fun migrateLegacyContainer(name: String): String {
        val snapshot = "${name}-snapshot"
        requireDockerSuccess(
            runCommand(listOf("docker", "commit", name, snapshot), null, 600_000),
            "snapshot legacy Docker workspace"
        )
        requireDockerSuccess(
            runCommand(listOf("docker", "rm", "-f", name), null, 30_000),
            "migrate legacy Docker workspace"
        )
        return snapshot
    }

    private suspend fun runCommand(
        command: List<String>,
        workingDirectory: File?,
        timeoutMillis: Long
    ): DesktopAgentCommandResult = runInterruptible {
        commandRunner.run(command, workingDirectory, timeoutMillis)
    }

    private fun requireDockerSuccess(result: DesktopAgentCommandResult, action: String) {
        require(result.exitCode == 0) { "Unable to $action: ${dockerFailureDetail(result)}" }
    }

    private fun requireApproval(approved: Boolean) {
        if (!approved) throw DesktopAgentApprovalDeniedException()
    }

    private fun dockerFailureDetail(result: DesktopAgentCommandResult): String =
        (result.stderr.ifBlank { result.stdout }).trim().take(2_000).ifBlank { "exit code ${result.exitCode}" }

    private fun readSkill(name: String, relative: String?): String {
        require(name.matches(Regex("[A-Za-z0-9._-]+"))) { "Invalid skill name" }
        val root = skillsRoot.toAbsolutePath().normalize()
        val skillDir = root.resolve(name).normalize()
        require(skillDir.parent == root && Files.isDirectory(skillDir)) { "Skill '$name' not found" }
        val target = skillDir.resolve(relative?.takeIf { it.isNotBlank() } ?: "SKILL.md").normalize()
        require(target.startsWith(skillDir) && Files.isRegularFile(target)) { "Skill file not found" }
        return Files.readString(target, StandardCharsets.UTF_8)
    }

    private fun requireWorkspaceRoot(workspace: DesktopAgentWorkspace): Path {
        require(workspace.rootPath.isNotBlank()) { "No workspace is configured" }
        return Path.of(workspace.rootPath).toRealPath()
            .also { require(Files.isDirectory(it)) { "Workspace root is not a directory" } }
    }

    private fun resolvePath(root: Path, relative: String, mustExist: Boolean): Path {
        require(relative.isNotBlank()) { "path is required" }
        val input = Path.of(relative)
        require(!input.isAbsolute) { "Only paths relative to the workspace are allowed" }
        val target = root.resolve(input).normalize()
        require(target.startsWith(root)) { "Path escapes workspace root" }
        if (mustExist) return target.toRealPath()
            .also { require(it.startsWith(root)) { "Path escapes workspace root" } }
        var existing = target.parent ?: root
        while (!Files.exists(existing)) existing = existing.parent ?: root
        require(existing.toRealPath().startsWith(root)) { "Path escapes workspace root" }
        return target
    }
}

internal fun defaultDesktopSkillsRoot(): Path {
    val configHome = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
        ?: Path.of(System.getProperty("user.home"), ".config").toString()
    return Path.of(configHome, "rikkahub", "skills")
}

internal fun agentToolDefinitions(config: DesktopAgentConfig?): List<JsonObject> {
    if (config == null) return emptyList()
    fun definition(name: String, description: String, required: List<String>, properties: JsonObject): JsonObject =
        buildJsonObject {
            put("type", "function")
            putJsonObject("function") {
                put("name", name)
                put("description", description)
                putJsonObject("parameters") {
                    put("type", "object")
                    put("properties", properties)
                    put("required", buildJsonArray { required.forEach { add(JsonPrimitive(it)) } })
                    put("additionalProperties", false)
                }
            }
        }

    fun stringProperty(description: String) = buildJsonObject { put("type", "string"); put("description", description) }
    val path = stringProperty("Path relative to the selected workspace root")
    return buildList {
        add(
            definition(
                DesktopAgentListFilesToolName,
                "List files in the selected workspace.",
                emptyList(),
                buildJsonObject { put("path", path) })
        )
        add(
            definition(
                DesktopAgentSearchFilesToolName,
                "Search UTF-8 text files in the selected workspace.",
                listOf("query"),
                buildJsonObject { put("query", stringProperty("Text to search")); put("path", path) })
        )
        add(
            definition(
                DesktopAgentReadFileToolName,
                "Read a UTF-8 text file inside the selected workspace.",
                listOf("path"),
                buildJsonObject { put("path", path) })
        )
        add(
            definition(
                DesktopAgentWriteFileToolName,
                "Write a UTF-8 text file inside the selected workspace. Requires approval.",
                listOf("path", "text"),
                buildJsonObject { put("path", path); put("text", stringProperty("New file content")) })
        )
        add(
            definition(
                DesktopAgentEditFileToolName,
                "Replace exactly one occurrence in a text file. Requires approval.",
                listOf("path", "old_text", "new_text"),
                buildJsonObject {
                    put("path", path); put("old_text", stringProperty("Existing text")); put(
                    "new_text",
                    stringProperty("Replacement text")
                )
                })
        )
        add(
            definition(
                DesktopAgentShellToolName,
                "Run a shell command in the selected workspace. Requires approval. You MUST set network=true before the " +
                    "first attempt of any command that may access the Internet, including apt/apt-get, curl, wget, git " +
                    "clone/pull, npm/pnpm/yarn, pip/uv, cargo, go, Gradle, Maven, package updates, downloads, or network " +
                    "diagnostics. network=true requests explicit user approval. Use network=false only for commands that are " +
                    "certainly local. Do not retry a failed network command without network=true.",
                listOf("command", "network"),
                buildJsonObject {
                    put("command", stringProperty("Shell command"))
                    put("cwd", path)
                    putJsonObject("network") {
                        put("type", "boolean")
                        put(
                            "description",
                            "Required. true for any Internet, package registry, download, update, git remote, or connectivity command; false only for certainly local commands."
                        )
                    }
                }
            ))
        if (config.enabledSkillNames.isNotEmpty()) add(
            definition(
                DesktopUseSkillToolName,
                "Load instructions from an enabled skill. Requires approval.",
                listOf("name"),
                buildJsonObject {
                    put("name", stringProperty("Enabled skill name")); put(
                    "path",
                    stringProperty("Linked file within the skill")
                )
                })
        )
    }
}

internal fun agentNetworkPolicyPrompt(config: DesktopAgentConfig?): String = config?.let {
    """
    **Agent shell network policy**
    The `agent_shell` tool requires an explicit `network` argument on every call.
    Set `network: true` on the FIRST call for anything that may need Internet access, including apt/apt-get,
    curl, wget, git clone/pull/fetch, npm/pnpm/yarn, pip/uv, cargo, go, Gradle, Maven, downloads,
    package or system updates, remote APIs, and connectivity/DNS tests. This asks the user for approval.
    Do not first attempt such commands with `network: false`, and do not describe a command as impossible to
    network until it has been attempted with approved `network: true`.
    Set `network: false` only for operations known to be entirely local.
    """.trimIndent()
}.orEmpty()

private fun JsonObject.requiredString(name: String): String =
    this[name]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        ?: error("$name is required")

private fun JsonObject.optionalString(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull
private fun JsonObject.optionalBoolean(name: String): Boolean? =
    this[name]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()

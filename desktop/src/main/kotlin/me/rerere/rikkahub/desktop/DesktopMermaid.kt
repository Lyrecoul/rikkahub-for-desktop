package me.rerere.rikkahub.desktop

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

internal fun String.isMermaidLanguage(): Boolean = trim().equals("mermaid", ignoreCase = true)

private val ChromiumExecutableCandidates = listOf(
    "/usr/bin/chromium",
    "/usr/bin/chromium-browser",
    "/usr/bin/google-chrome",
    "/usr/bin/google-chrome-stable",
    "/usr/bin/brave",
    "/usr/bin/brave-browser",
    "/usr/bin/microsoft-edge",
    "/usr/bin/microsoft-edge-stable",
    "/usr/bin/vivaldi"
).map(Path::of)

private val MermaidCliNames = listOf("mmdc", "mermaid-cli")

internal data class MermaidRenderResult(
    val pngBytes: ByteArray,
    val source: String,
    val dark: Boolean,
    val useSystemBrowser: Boolean,
    val cliPath: String
)

internal object DesktopMermaidRenderer {
    private const val RenderTimeoutSeconds = 45L

    private data class CacheKey(
        val source: String,
        val dark: Boolean,
        val useSystemBrowser: Boolean,
        val cliPath: String
    )

    private val cache = mutableMapOf<CacheKey, MermaidRenderResult?>()
    private val svgCache = mutableMapOf<CacheKey, ByteArray?>()

    fun render(source: String, dark: Boolean, useSystemBrowser: Boolean, cliPath: String): MermaidRenderResult? =
        synchronized(cache) {
            val key = CacheKey(source, dark, useSystemBrowser, cliPath)
            if (key in cache) cache[key] else renderUncached(
                source,
                dark,
                useSystemBrowser,
                cliPath
            ).also { cache[key] = it }
        }

    fun cachedResult(source: String, dark: Boolean, useSystemBrowser: Boolean, cliPath: String): MermaidRenderResult? =
        synchronized(cache) {
            cache[CacheKey(source, dark, useSystemBrowser, cliPath)]
        }

    fun renderSvg(result: MermaidRenderResult): ByteArray? = synchronized(svgCache) {
        val key = CacheKey(result.source, result.dark, result.useSystemBrowser, result.cliPath)
        if (key in svgCache) svgCache[key] else renderSvgUncached(result, key).also { svgCache[key] = it }
    }

    private fun renderUncached(
        source: String,
        dark: Boolean,
        useSystemBrowser: Boolean,
        cliPath: String
    ): MermaidRenderResult? {
        val chromiumExecutable = if (useSystemBrowser) findChromiumExecutable() else null
        val executable = findMermaidCli(cliPath) ?: return null
        val directory = runCatching { Files.createTempDirectory("rikkahub-mermaid-") }.getOrNull() ?: return null
        val input = directory.resolve("diagram.mmd")
        val pngOutput = directory.resolve("diagram.png")
        try {
            Files.writeString(input, source, StandardCharsets.UTF_8)
            if (!runMermaidCli(executable, input, pngOutput, dark, chromiumExecutable, scale = 3)) return null
            return MermaidRenderResult(Files.readAllBytes(pngOutput), source, dark, useSystemBrowser, cliPath)
        } catch (_: Exception) {
            return null
        } finally {
            runCatching { Files.deleteIfExists(input) }
            runCatching { Files.deleteIfExists(pngOutput) }
            runCatching { Files.deleteIfExists(directory) }
        }
    }

    private fun renderSvgUncached(result: MermaidRenderResult, key: CacheKey): ByteArray? {
        val chromiumExecutable = if (result.useSystemBrowser) findChromiumExecutable() else null
        val executable = findMermaidCli(result.cliPath) ?: return null
        val directory = runCatching { Files.createTempDirectory("rikkahub-mermaid-") }.getOrNull() ?: return null
        val input = directory.resolve("diagram.mmd")
        val output = directory.resolve("diagram.svg")
        try {
            Files.writeString(input, key.source, StandardCharsets.UTF_8)
            if (!runMermaidCli(executable, input, output, key.dark, chromiumExecutable)) return null
            return Files.readAllBytes(output).takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            return null
        } finally {
            runCatching { Files.deleteIfExists(input) }
            runCatching { Files.deleteIfExists(output) }
            runCatching { Files.deleteIfExists(directory) }
        }
    }

    private fun runMermaidCli(
        executable: Path,
        input: Path,
        output: Path,
        dark: Boolean,
        chromiumExecutable: Path?,
        scale: Int? = null
    ): Boolean {
        val command = buildList {
            add(executable.toString())
            addAll(listOf("-i", input.toString(), "-o", output.toString()))
            addAll(listOf("-t", if (dark) "dark" else "default", "-b", "transparent"))
            scale?.let { addAll(listOf("-s", it.toString())) }
            add("-q")
        }
        val process = ProcessBuilder(command).apply {
            if (chromiumExecutable != null) environment()["PUPPETEER_EXECUTABLE_PATH"] = chromiumExecutable.toString()
            else environment().remove("PUPPETEER_EXECUTABLE_PATH")
        }
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
        if (!process.waitFor(RenderTimeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return false
        }
        return process.exitValue() == 0 && Files.isRegularFile(output) && Files.size(output) > 0
    }

    private fun findMermaidCli(configuredPath: String): Path? {
        configuredPath.trim().takeIf { it.isNotEmpty() }?.let { path ->
            return runCatching { Path.of(path) }.getOrNull()?.takeIf(::isExecutable)
        }
        return mermaidCliCandidates().firstOrNull(::isExecutable)
    }

    private fun mermaidCliCandidates(): Sequence<Path> = sequence {
        val userHome = runCatching { Path.of(System.getProperty("user.home")) }.getOrNull()
        userHome?.let { home ->
            MermaidCliNames.forEach { name ->
                yield(home.resolve(".npm-global/bin").resolve(name))
                yield(home.resolve(".local/bin").resolve(name))
            }
        }
        runCatching { System.getenv("NPM_CONFIG_PREFIX")?.let(Path::of) }.getOrNull()?.let { prefix ->
            MermaidCliNames.forEach { name -> yield(prefix.resolve("bin").resolve(name)) }
        }
        System.getenv("PATH")?.split(File.pathSeparator)?.forEach { directory ->
            runCatching { Path.of(directory) }.getOrNull()?.let { path ->
                MermaidCliNames.forEach { name -> yield(path.resolve(name)) }
            }
        }
        listOf("/usr/local/bin", "/usr/bin", "/opt/homebrew/bin").forEach { directory ->
            MermaidCliNames.forEach { name -> yield(Path.of(directory, name)) }
        }
    }

    private fun isExecutable(path: Path): Boolean = Files.isRegularFile(path) && Files.isExecutable(path)

    private fun findChromiumExecutable(): Path? = sequence {
        runCatching { System.getenv("PUPPETEER_EXECUTABLE_PATH")?.let(Path::of) }
            .getOrNull()
            ?.let { path -> yield(path) }
        yieldAll(ChromiumExecutableCandidates)
    }.firstOrNull { Files.isRegularFile(it) && Files.isExecutable(it) }
}

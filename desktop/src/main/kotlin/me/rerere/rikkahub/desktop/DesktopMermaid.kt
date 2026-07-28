package me.rerere.rikkahub.desktop

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

internal object DesktopMermaidRenderer {
    private const val RenderTimeoutSeconds = 45L
    private data class CacheKey(val source: String, val dark: Boolean, val useSystemBrowser: Boolean)

    private val cache = mutableMapOf<CacheKey, ByteArray?>()

    fun render(source: String, dark: Boolean, useSystemBrowser: Boolean): ByteArray? = synchronized(cache) {
        val key = CacheKey(source, dark, useSystemBrowser)
        if (key in cache) cache[key] else renderUncached(source, dark, useSystemBrowser).also { cache[key] = it }
    }

    private fun renderUncached(source: String, dark: Boolean, useSystemBrowser: Boolean): ByteArray? {
        val chromiumExecutable = if (useSystemBrowser) findChromiumExecutable() else null
        for (executable in listOf("mmdc", "mermaid-cli")) {
            val directory = runCatching { Files.createTempDirectory("rikkahub-mermaid-") }.getOrNull() ?: return null
            val input = directory.resolve("diagram.mmd")
            val output = directory.resolve("diagram.png")
            try {
                Files.writeString(input, source, StandardCharsets.UTF_8)
                val process = try {
                    ProcessBuilder(
                        executable, "-i", input.toString(), "-o", output.toString(),
                        "-t", if (dark) "dark" else "default",
                        "-b", "transparent", "-s", "3", "-q"
                    ).apply {
                        chromiumExecutable?.let { environment()["PUPPETEER_EXECUTABLE_PATH"] = it.toString() }
                    }
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .start()
                } catch (_: java.io.IOException) {
                    continue
                }
                if (!process.waitFor(RenderTimeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    return null
                }
                if (process.exitValue() == 0 && Files.isRegularFile(output)) {
                    return Files.readAllBytes(output).takeIf { it.isNotEmpty() }
                }
                return null
            } catch (_: Exception) {
                return null
            } finally {
                runCatching { Files.deleteIfExists(input) }
                runCatching { Files.deleteIfExists(output) }
                runCatching { Files.deleteIfExists(directory) }
            }
        }
        return null
    }

    private fun findChromiumExecutable(): Path? = sequence {
        System.getenv("PUPPETEER_EXECUTABLE_PATH")?.let { yield(Path.of(it)) }
        yieldAll(ChromiumExecutableCandidates)
    }.firstOrNull { Files.isRegularFile(it) && Files.isExecutable(it) }
}

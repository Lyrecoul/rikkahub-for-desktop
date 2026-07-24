package me.rerere.rikkahub.desktop

import java.util.concurrent.TimeUnit

internal interface DesktopSecretStore {
    fun read(id: String): String?
    fun write(id: String, value: String): Boolean
    fun delete(id: String): Boolean
}

/** Linux Secret Service adapter. Callers must refuse to persist sensitive data when it is unavailable. */
internal class SecretToolStore : DesktopSecretStore {
    override fun read(id: String): String? = runCatching {
        val process = ProcessBuilder("secret-tool", "lookup", "application", "rikkahub-desktop", "id", id)
            .redirectErrorStream(true).start()
        if (!process.waitFor(2, TimeUnit.SECONDS) || process.exitValue() != 0) {
            process.destroyForcibly()
            null
        } else {
            process.inputStream.bufferedReader().readText().trim().ifBlank { null }
        }
    }.getOrNull()

    override fun write(id: String, value: String): Boolean = runCatching {
        val process = ProcessBuilder(
            "secret-tool", "store", "--label=RikkaHub Desktop", "application", "rikkahub-desktop", "id", id
        ).redirectErrorStream(true).start()
        process.outputStream.bufferedWriter().use { it.write(value) }
        process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0
    }.getOrDefault(false)

    override fun delete(id: String): Boolean = runCatching {
        val process = ProcessBuilder("secret-tool", "clear", "application", "rikkahub-desktop", "id", id)
            .redirectErrorStream(true).start()
        // secret-tool returns 1 when no matching item exists, which is already the desired state.
        process.waitFor(2, TimeUnit.SECONDS).also { completed ->
            if (!completed) process.destroyForcibly()
        }
    }.getOrDefault(false)
}

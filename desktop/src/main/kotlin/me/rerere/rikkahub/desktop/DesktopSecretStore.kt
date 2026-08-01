package me.rerere.rikkahub.desktop

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.concurrent.TimeUnit

internal interface DesktopSecretStore {
    fun read(id: String): String?
    fun write(id: String, value: String): Boolean
    fun delete(id: String): Boolean
}

internal fun defaultDesktopSecretStore(dataFile: Path): DesktopSecretStore = if (DesktopPlatform.isWindows) {
    WindowsDpapiSecretStore(dataFile.resolveSibling("secrets.dat"))
} else {
    SecretToolStore()
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

/** Windows DPAPI adapter. Ciphertext is scoped to the current Windows user. */
internal class WindowsDpapiSecretStore(private val file: Path) : DesktopSecretStore {
    override fun read(id: String): String? = load()[id]?.let(::unprotect)

    override fun write(id: String, value: String): Boolean = runCatching {
        val encrypted = protect(value) ?: return false
        save(load() + (id to encrypted))
        true
    }.getOrDefault(false)

    override fun delete(id: String): Boolean = runCatching {
        val entries = load()
        if (id in entries) save(entries - id)
        true
    }.getOrDefault(false)

    private fun load(): Map<String, String> = runCatching {
        if (!Files.isRegularFile(file)) return emptyMap()
        Files.readAllLines(file, StandardCharsets.US_ASCII).mapNotNull { line ->
            line.split('\t', limit = 2).takeIf { it.size == 2 }?.let { (key, value) ->
                runCatching { String(Base64.getUrlDecoder().decode(key), StandardCharsets.UTF_8) }.getOrNull()?.let { it to value }
            }
        }.toMap()
    }.getOrDefault(emptyMap())

    private fun save(entries: Map<String, String>) {
        file.parent?.let(Files::createDirectories)
        val temporary = file.resolveSibling("${file.fileName}.tmp")
        val content = entries.entries.joinToString("\n") { (id, value) ->
            "${Base64.getUrlEncoder().withoutPadding().encodeToString(id.toByteArray(StandardCharsets.UTF_8))}\t$value"
        }
        Files.writeString(temporary, content, StandardCharsets.US_ASCII)
        runCatching {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        }.getOrElse {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun protect(value: String): String? = runPowerShell(
        "[Console]::Write([Convert]::ToBase64String([Security.Cryptography.ProtectedData]::Protect([Text.Encoding]::UTF8.GetBytes([Console]::In.ReadToEnd()), \$null, [Security.Cryptography.DataProtectionScope]::CurrentUser)))",
        value
    )

    private fun unprotect(value: String): String? = runPowerShell(
        "[Console]::Write([Text.Encoding]::UTF8.GetString([Security.Cryptography.ProtectedData]::Unprotect([Convert]::FromBase64String([Console]::In.ReadToEnd()), \$null, [Security.Cryptography.DataProtectionScope]::CurrentUser)))",
        value
    )

    private fun runPowerShell(script: String, input: String): String? = runCatching {
        val process = ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script)
            .redirectErrorStream(true)
            .start()
        process.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { it.write(input) }
        if (!process.waitFor(2, TimeUnit.SECONDS) || process.exitValue() != 0) {
            process.destroyForcibly()
            null
        } else {
            process.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        }
    }.getOrNull()
}

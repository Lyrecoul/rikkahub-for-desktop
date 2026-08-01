package me.rerere.rikkahub.desktop

import java.nio.file.Path

internal object DesktopPlatform {
    val isWindows: Boolean
        get() = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    fun dataDirectory(
        osName: String = System.getProperty("os.name"),
        environment: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home")
    ): Path = if (osName.startsWith("Windows", ignoreCase = true)) {
        Path.of(
            environment["APPDATA"]?.takeIf(String::isNotBlank)
                ?: Path.of(userHome, "AppData", "Roaming").toString(),
            "RikkaHub"
        )
    } else {
        val configHome = environment["XDG_CONFIG_HOME"]?.takeIf(String::isNotBlank)
            ?: Path.of(userHome, ".config").toString()
        Path.of(configHome, "rikkahub")
    }

    fun localShellCommand(command: String, osName: String = System.getProperty("os.name")): List<String> =
        if (osName.startsWith("Windows", ignoreCase = true)) {
            listOf("cmd.exe", "/d", "/s", "/c", command)
        } else {
            listOf("/bin/sh", "-lc", command)
        }

    fun isRunnableFile(path: Path): Boolean =
        java.nio.file.Files.isRegularFile(path) && (isWindows || java.nio.file.Files.isExecutable(path))
}

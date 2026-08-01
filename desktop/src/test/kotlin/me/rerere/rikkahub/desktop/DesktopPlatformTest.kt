package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopPlatformTest {
    @Test
    fun windowsUsesRoamingAppDataAndCmd() {
        val dataDirectory = DesktopPlatform.dataDirectory("Windows 11", emptyMap(), "C:\\Users\\Rikka")
        assertEquals("RikkaHub", dataDirectory.fileName.toString())
        assertEquals("Roaming", dataDirectory.parent.fileName.toString())
        assertEquals("AppData", dataDirectory.parent.parent.fileName.toString())
        assertEquals(
            listOf("cmd.exe", "/d", "/s", "/c", "dir"),
            DesktopPlatform.localShellCommand("dir", "Windows 11")
        )
    }

    @Test
    fun unixPreservesXdgDataDirectoryAndShell() {
        assertEquals(
            "/var/config/rikkahub",
            DesktopPlatform.dataDirectory("Linux", mapOf("XDG_CONFIG_HOME" to "/var/config"), "/home/rikka").toString()
        )
        assertEquals(listOf("/bin/sh", "-lc", "pwd"), DesktopPlatform.localShellCommand("pwd", "Linux"))
    }
}

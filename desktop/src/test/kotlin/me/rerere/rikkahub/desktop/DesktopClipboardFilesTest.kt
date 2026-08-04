package me.rerere.rikkahub.desktop

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopClipboardFilesTest {
    @Test
    fun readsJavaFileListClipboardFlavorAndRemovesDuplicates() {
        val first = Files.createTempFile("rikkahub-clipboard-first", ".txt").toFile()
        val second = Files.createTempFile("rikkahub-clipboard-second", ".txt").toFile()
        val directory = Files.createTempDirectory("rikkahub-clipboard-directory").toFile()
        val transferable = SingleFlavorTransferable(
            DataFlavor.javaFileListFlavor,
            listOf(first, first, directory, second)
        )

        assertEquals(listOf(first.absoluteFile, second.absoluteFile), desktopFilesFromTransferable(transferable))
    }

    @Test
    fun readsUriListClipboardFlavor() {
        val first = Files.createTempFile("rikkahub-uri-first", ".png").toFile()
        val second = Files.createTempFile("rikkahub-uri-second", ".pdf").toFile()
        val flavor = DataFlavor("text/uri-list;class=java.lang.String;charset=UTF-8")
        val transferable = SingleFlavorTransferable(
            flavor,
            "# copied files\r\n${first.toURI()}\r\n${second.toURI()}\r\n"
        )

        assertEquals(listOf(first.absoluteFile, second.absoluteFile), desktopFilesFromTransferable(transferable))
    }

    @Test
    fun readsGnomeCopiedFilesClipboardFlavor() {
        val file = Files.createTempFile("rikkahub-gnome-file", ".md").toFile()
        val flavor = DataFlavor("x-special/gnome-copied-files;class=java.lang.String;charset=UTF-8")
        val transferable = SingleFlavorTransferable(flavor, "copy\n${file.toURI()}\n")

        assertEquals(listOf(file.absoluteFile), desktopFilesFromTransferable(transferable))
    }

    @Test
    fun ignoresPlainTextAndMissingFileUris() {
        assertTrue(desktopFilesFromTransferable(StringSelection("ordinary text")).isEmpty())
        val flavor = DataFlavor("text/uri-list;class=java.lang.String;charset=UTF-8")
        val transferable = SingleFlavorTransferable(flavor, "file:///definitely/missing/rikkahub.txt")
        assertTrue(desktopFilesFromTransferable(transferable).isEmpty())
    }

    private class SingleFlavorTransferable(
        private val flavor: DataFlavor,
        private val value: Any
    ) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(flavor)
        override fun isDataFlavorSupported(candidate: DataFlavor): Boolean = candidate == flavor
        override fun getTransferData(candidate: DataFlavor): Any {
            require(isDataFlavorSupported(candidate))
            return value
        }
    }
}

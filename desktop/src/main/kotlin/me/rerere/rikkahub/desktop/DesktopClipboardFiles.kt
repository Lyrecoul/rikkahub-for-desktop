package me.rerere.rikkahub.desktop

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.URI

internal fun readDesktopClipboardFiles(): List<File> = runCatching {
    desktopFilesFromTransferable(Toolkit.getDefaultToolkit().systemClipboard.getContents(null))
}.getOrDefault(emptyList())

internal fun desktopFilesFromTransferable(transferable: Transferable?): List<File> {
    transferable ?: return emptyList()
    val fileList = runCatching {
        @Suppress("UNCHECKED_CAST")
        transferable.takeIf { it.isDataFlavorSupported(DataFlavor.javaFileListFlavor) }
            ?.getTransferData(DataFlavor.javaFileListFlavor) as? List<Any?>
    }.getOrNull().orEmpty().mapNotNull { value ->
        when (value) {
            is File -> value
            is String -> value.toDesktopClipboardFile()
            else -> null
        }
    }
    if (fileList.isNotEmpty()) return fileList.filter(File::isFile).distinctBy(File::getAbsolutePath)

    return transferable.transferDataFlavors.asSequence()
        .filter { flavor ->
            (flavor.primaryType.equals("text", ignoreCase = true) &&
                flavor.subType.equals("uri-list", ignoreCase = true)) ||
                flavor.mimeType.contains("gnome-copied-files", ignoreCase = true)
        }
        .mapNotNull { flavor -> readTransferText(transferable, flavor) }
        .flatMap { text -> text.lineSequence() }
        .map(String::trim)
        .filter { line -> line.isNotEmpty() && !line.startsWith('#') && line !in setOf("copy", "cut") }
        .mapNotNull(String::toDesktopClipboardFile)
        .filter(File::isFile)
        .distinctBy(File::getAbsolutePath)
        .toList()
}

private fun readTransferText(transferable: Transferable, flavor: DataFlavor): String? = runCatching {
    when (val value = transferable.getTransferData(flavor)) {
        is String -> value
        is Reader -> value.use(Reader::readText)
        is InputStream -> value.bufferedReader(flavor.charsetOrUtf8()).use(Reader::readText)
        else -> null
    }
}.getOrNull()

private fun DataFlavor.charsetOrUtf8(): java.nio.charset.Charset = runCatching {
    java.nio.charset.Charset.forName(getParameter("charset") ?: "UTF-8")
}.getOrDefault(Charsets.UTF_8)

private fun String.toDesktopClipboardFile(): File? = runCatching {
    val file = if (startsWith("file:", ignoreCase = true)) File(URI(this)) else File(this)
    file.takeIf(File::exists)
}.getOrNull()

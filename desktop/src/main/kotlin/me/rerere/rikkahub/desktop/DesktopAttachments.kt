package me.rerere.rikkahub.desktop

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Base64

private const val MaxAttachmentBytes = 10L * 1024L * 1024L
private val TextAttachmentExtensions = setOf(
    "txt", "md", "csv", "json", "xml", "yaml", "yml", "kt", "java", "js", "ts",
    "tsx", "jsx", "py", "rs", "go", "c", "cpp", "h", "hpp", "css", "html", "sh"
)

internal fun isDesktopAttachmentSupported(file: File): Boolean {
    if (!file.isFile || file.length() > MaxAttachmentBytes) return false
    return file.extension.lowercase() in TextAttachmentExtensions ||
        file.extension.lowercase() in setOf("png", "jpg", "jpeg", "gif", "webp") ||
        Files.probeContentType(file.toPath()).orEmpty().startsWith("text/")
}

internal fun loadDesktopAttachment(file: File): DesktopAttachment {
    require(file.isFile) { "${file.name} is not a file" }
    require(file.length() <= MaxAttachmentBytes) { "${file.name} exceeds the 10 MB limit" }
    val mimeType = Files.probeContentType(file.toPath()).orEmpty()
    val extension = file.extension.lowercase()
    val imageMime = when (extension) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        else -> null
    }
    if (imageMime != null) {
        return DesktopAttachment(
            name = file.name,
            mimeType = imageMime,
            data = Base64.getEncoder().encodeToString(file.readBytes()),
            isImage = true
        )
    }
    require(extension in TextAttachmentExtensions || mimeType.startsWith("text/")) {
        "Unsupported attachment type: ${file.name}"
    }
    return DesktopAttachment(
        name = file.name,
        mimeType = mimeType.ifBlank { "text/plain" },
        data = file.readText(StandardCharsets.UTF_8),
        isImage = false
    )
}

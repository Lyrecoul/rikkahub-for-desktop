package me.rerere.rikkahub.desktop

import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Base64

internal const val MaxAttachmentBytes = 10L * 1024L * 1024L
internal const val MaxImagePixels = 40_000_000L
private val TextAttachmentExtensions = setOf(
    "txt", "md", "csv", "json", "xml", "yaml", "yml", "kt", "java", "js", "ts",
    "tsx", "jsx", "py", "rs", "go", "c", "cpp", "h", "hpp", "css", "html", "sh"
)
private val DocumentAttachmentMimeTypes = mapOf(
    "pdf" to "application/pdf",
    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "epub" to "application/epub+zip"
)
private data class DesktopAudioType(val mimeType: String, val format: String)

private val AudioAttachmentTypes = mapOf(
    "mp3" to DesktopAudioType("audio/mpeg", "mp3"),
    "wav" to DesktopAudioType("audio/wav", "wav"),
    "m4a" to DesktopAudioType("audio/mp4", "m4a"),
    "aac" to DesktopAudioType("audio/aac", "aac"),
    "flac" to DesktopAudioType("audio/flac", "flac"),
    "ogg" to DesktopAudioType("audio/ogg", "ogg"),
    "opus" to DesktopAudioType("audio/opus", "opus")
)

internal fun isDesktopAttachmentSupported(file: File): Boolean {
    if (!file.isFile || file.length() > MaxAttachmentBytes) return false
    return file.extension.lowercase() in TextAttachmentExtensions ||
        file.extension.lowercase() in DocumentAttachmentMimeTypes ||
        file.extension.lowercase() in AudioAttachmentTypes ||
        file.extension.lowercase() in setOf("png", "jpg", "jpeg", "gif", "webp") ||
        Files.probeContentType(file.toPath()).orEmpty().startsWith("text/")
}

internal fun deduplicateDesktopAttachments(attachments: List<DesktopAttachment>): List<DesktopAttachment> =
    attachments.distinctBy { Triple(it.name, it.data, it.rawData) }

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
        val bytes = file.readBytes()
        val codec = runCatching { Codec.makeFromData(Data.makeFromBytes(bytes)) }.getOrElse {
            throw IllegalArgumentException("Invalid image attachment: ${file.name}", it)
        }
        val actualMime = when (codec.encodedImageFormat) {
            EncodedImageFormat.PNG -> "image/png"
            EncodedImageFormat.JPEG -> "image/jpeg"
            EncodedImageFormat.GIF -> "image/gif"
            EncodedImageFormat.WEBP -> "image/webp"
            else -> null
        }
        require(actualMime == imageMime) {
            "Image content does not match the file extension: ${file.name}"
        }
        val imageSize = codec.size
        require(imageSize.x > 0 && imageSize.y > 0) { "Invalid image dimensions: ${file.name}" }
        require(imageSize.x.toLong() * imageSize.y <= MaxImagePixels) {
            "${file.name} exceeds the 40 megapixel limit"
        }
        return DesktopAttachment(
            name = file.name,
            mimeType = imageMime,
            data = Base64.getEncoder().encodeToString(bytes),
            isImage = true,
            sizeBytes = file.length(),
            imageWidth = imageSize.x,
            imageHeight = imageSize.y
        )
    }
    AudioAttachmentTypes[extension]?.let { audioType ->
        return DesktopAttachment(
            name = file.name,
            mimeType = audioType.mimeType,
            data = Base64.getEncoder().encodeToString(file.readBytes()),
            kind = DesktopAttachmentKind.AUDIO,
            sizeBytes = file.length(),
            audioFormat = audioType.format
        )
    }
    val documentMimeType = DocumentAttachmentMimeTypes[extension]
    val documentText = if (documentMimeType != null) extractDesktopDocumentText(file) else null
    require(documentText != null || extension in TextAttachmentExtensions || mimeType.startsWith("text/")) {
        "Unsupported attachment type: ${file.name}"
    }
    return DesktopAttachment(
        name = file.name,
        mimeType = if (documentText != null) "text/plain" else mimeType.ifBlank { "text/plain" },
        data = documentText ?: file.readText(StandardCharsets.UTF_8),
        isImage = false,
        rawData = documentMimeType?.let { Base64.getEncoder().encodeToString(file.readBytes()) },
        rawMimeType = documentMimeType,
        sizeBytes = file.length()
    )
}

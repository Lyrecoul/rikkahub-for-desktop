package me.rerere.rikkahub.desktop

private val DesktopImageMimeTypes = setOf("image/png", "image/jpeg", "image/gif", "image/webp")
private val DesktopNativeDocumentMimeTypes = setOf(
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
)

internal fun DesktopConfig.modelCapabilities(modelId: String = model): DesktopModelCapabilities =
    modelCapabilityOverrides[modelId] ?: inferDesktopModelCapabilities(modelId)

internal fun inferDesktopModelCapabilities(modelId: String): DesktopModelCapabilities {
    val normalized = modelId.lowercase()
    val supportsImages = listOf(
        "gpt-4o", "gpt-4.1", "gpt-4.5", "gpt-5", "gemini", "claude-3", "claude-sonnet",
        "claude-opus", "qwen-vl", "qwen2.5-vl", "qwen3-vl", "vision", "pixtral", "llava"
    ).any(normalized::contains)
    val supportsAudio = listOf("gpt-4o-audio", "gpt-audio", "gemini").any(normalized::contains)
    val supportsDocuments = listOf(
        "gpt-4o", "gpt-4.1", "gpt-5", "o1", "o3", "o4", "claude-3", "claude-sonnet",
        "claude-opus", "claude-haiku", "gemini"
    ).any(normalized::contains)
    val supportsReasoning = listOf(
        "o1", "o3", "o4", "gpt-5", "r1", "reasoner", "thinking", "deepseek-r", "gemini-2.5",
        "gemini-3"
    ).any(normalized::contains)
    val supportsTools = !listOf("embedding", "image", "tts", "whisper").any(normalized::contains)
    return DesktopModelCapabilities(
        inputModalities = buildSet {
            add(DesktopModality.TEXT)
            if (supportsImages) add(DesktopModality.IMAGE)
            if (supportsAudio) add(DesktopModality.AUDIO)
            if (supportsDocuments) add(DesktopModality.DOCUMENT)
        },
        supportsReasoning = supportsReasoning,
        supportsTools = supportsTools,
        acceptedImageMimeTypes = if (supportsImages) DesktopImageMimeTypes else emptySet(),
        acceptedAudioFormats = if (supportsAudio) {
            if (normalized.contains("gemini")) setOf("mp3", "wav", "m4a", "aac", "flac", "ogg", "opus")
            else setOf("mp3", "wav")
        } else {
            emptySet()
        },
        acceptedDocumentMimeTypes = if (supportsDocuments) DesktopNativeDocumentMimeTypes else emptySet()
    )
}

internal data class DesktopAttachmentValidationIssue(
    val attachmentName: String,
    val reason: String
)

internal fun DesktopConfig.validateAttachments(messages: List<ChatMessage>): List<DesktopAttachmentValidationIssue> {
    val capabilities = modelCapabilities()
    val issues = messages.flatMap { message ->
        message.attachments.mapNotNull { attachment ->
            val unavailableReason = when (attachment.kind) {
                DesktopAttachmentKind.IMAGE, DesktopAttachmentKind.AUDIO ->
                    "attachment data is unavailable".takeIf { attachment.data.isBlank() }

                DesktopAttachmentKind.FILE -> null
            }
            val reason = unavailableReason ?: capabilities.maxAttachmentBytes?.let { limit ->
                attachment.sizeBytes?.takeIf { it > limit }?.let {
                    "attachment exceeds the ${limit / (1024 * 1024)} MB limit for model '$model'"
                }
            } ?: when (attachment.kind) {
                DesktopAttachmentKind.IMAGE -> when {
                    attachment.imageWidth != null && attachment.imageHeight != null &&
                        attachment.imageWidth.toLong() * attachment.imageHeight > MaxImagePixels ->
                        "image exceeds the 40 megapixel limit"

                    DesktopModality.IMAGE !in capabilities.inputModalities ->
                        "model '$model' does not declare image input support"

                    capabilities.acceptedImageMimeTypes.isNotEmpty() &&
                        attachment.mimeType !in capabilities.acceptedImageMimeTypes ->
                        "image type '${attachment.mimeType}' is not supported by model '$model'"

                    else -> null
                }

                DesktopAttachmentKind.AUDIO -> {
                    val format = attachment.audioFormat ?: when (attachment.mimeType) {
                        "audio/wav", "audio/x-wav" -> "wav"
                        "audio/mpeg", "audio/mp3" -> "mp3"
                        "audio/mp4", "audio/x-m4a" -> "m4a"
                        "audio/aac" -> "aac"
                        "audio/flac", "audio/x-flac" -> "flac"
                        "audio/ogg" -> "ogg"
                        "audio/opus" -> "opus"
                        else -> null
                    }
                    when {
                        protocol in setOf(
                            DesktopProviderProtocol.OPENAI_RESPONSES,
                            DesktopProviderProtocol.ANTHROPIC_MESSAGES
                        ) -> "protocol '$protocol' does not support audio attachments"

                        DesktopModality.AUDIO !in capabilities.inputModalities ->
                            "model '$model' does not declare audio input support"

                        format == null -> "audio type '${attachment.mimeType}' is not supported"

                        capabilities.acceptedAudioFormats.isNotEmpty() &&
                            format !in capabilities.acceptedAudioFormats ->
                            "audio format '$format' is not supported by model '$model'"

                        else -> null
                    }
                }

                // Unsupported native files retain their extracted-text fallback.
                DesktopAttachmentKind.FILE -> null
            }
            reason?.let { DesktopAttachmentValidationIssue(attachment.name, it) }
        }
    }
    val totalBytes = messages.flatMap(ChatMessage::attachments).sumOf { it.sizeBytes ?: 0L }
    val requestSizeIssue = capabilities.maxRequestBytes?.takeIf { totalBytes > it }?.let { limit ->
        DesktopAttachmentValidationIssue(
            attachmentName = "Request",
            reason = "attachments total $totalBytes bytes exceeds the $limit byte limit for model '$model'"
        )
    }
    return if (requestSizeIssue == null) issues else issues + requestSizeIssue
}

internal fun DesktopConfig.canSendNativeDocument(attachment: DesktopAttachment): Boolean {
    if (attachment.rawData == null) return false
    val mimeType = attachment.rawMimeType ?: return false
    val capabilities = modelCapabilities()
    if (DesktopModality.DOCUMENT !in capabilities.inputModalities) return false
    return when (protocol) {
        DesktopProviderProtocol.OPENAI_RESPONSES ->
            capabilities.acceptedDocumentMimeTypes.isEmpty() || mimeType in capabilities.acceptedDocumentMimeTypes

        DesktopProviderProtocol.ANTHROPIC_MESSAGES -> mimeType == "application/pdf"
        DesktopProviderProtocol.GEMINI_GENERATE_CONTENT -> mimeType == "application/pdf"

        DesktopProviderProtocol.OPENAI_CHAT_COMPLETIONS -> false
    }
}

internal fun List<DesktopAttachmentValidationIssue>.toUserMessage(): String = joinToString("\n") { issue ->
    "${issue.attachmentName}: ${issue.reason}"
}

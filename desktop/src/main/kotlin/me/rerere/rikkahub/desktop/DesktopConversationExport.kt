package me.rerere.rikkahub.desktop

internal fun exportConversationMarkdown(
    conversation: DesktopConversation,
    systemPrompt: String
): String = buildString {
    appendLine("# ${conversation.title.markdownHeading()}")
    appendLine()
    appendLine("_Exported from RikkaHub_")

    if (systemPrompt.isNotBlank()) {
        appendLine()
        appendLine("## System prompt")
        appendLine()
        appendLine(systemPrompt.trim())
    }

    conversation.messages.forEach { message ->
        appendLine()
        appendLine("---")
        appendLine()
        appendLine("## ${message.role.markdownRole()}")
        if (message.content.isNotBlank()) {
            appendLine()
            appendLine(message.content.trim())
        }
        if (message.attachments.isNotEmpty()) {
            appendLine()
            appendLine("### Attachments")
            message.attachments.forEach { attachment ->
                val type = if (attachment.isImage) "Image" else "File"
                appendLine("- $type: ${attachment.name} (${attachment.mimeType})")
            }
        }
        if (message.reasoning.isNotBlank()) {
            appendLine()
            appendLine("<details>")
            appendLine("<summary>Reasoning</summary>")
            appendLine()
            appendLine(message.reasoning.trim())
            appendLine()
            appendLine("</details>")
        }
        if (message.citations.isNotEmpty()) {
            appendLine()
            appendLine("### Sources")
            message.citations.forEach { citation ->
                val label = citation.title.ifBlank { citation.url }.replace("]", "\\]")
                appendLine("- [$label](${citation.url})")
            }
        }
    }
}.trimEnd() + "\n"

private fun String.markdownHeading(): String =
    lineSequence().firstOrNull().orEmpty().replace("#", "\\#").ifBlank { "Conversation" }

private fun String.markdownRole(): String = when (lowercase()) {
    "user" -> "You"
    "assistant" -> "Assistant"
    "system" -> "System"
    "tool" -> "Tool"
    else -> replaceFirstChar { it.uppercase() }
}

package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopConversationExportTest {
    @Test
    fun exportsEffectivePromptMessagesAttachmentsReasoningAndSources() {
        val conversation = DesktopConversation(
            title = "Release notes",
            messages = listOf(
                ChatMessage(
                    role = "user",
                    content = "Summarize this",
                    attachments = listOf(DesktopAttachment("notes.md", "text/markdown", "body"))
                ),
                ChatMessage(
                    role = "assistant",
                    content = "Here is the summary.",
                    reasoning = "I read the notes.",
                    citations = listOf(DesktopCitation("https://example.com", "Example"))
                )
            )
        )

        assertEquals(
            """
                # Release notes

                _Exported from RikkaHub_

                ## System prompt

                Be concise.

                ---

                ## You

                Summarize this

                ### Attachments
                - File: notes.md (text/markdown)

                ---

                ## Assistant

                Here is the summary.

                <details>
                <summary>Reasoning</summary>

                I read the notes.

                </details>

                ### Sources
                - [Example](https://example.com)
            """.trimIndent() + "\n",
            exportConversationMarkdown(conversation, "Be concise.")
        )
    }

    @Test
    fun usesCurrentMessageBranchAndSanitizesMultilineTitle() {
        val exported = exportConversationMarkdown(
            DesktopConversation(
                title = "# First line\nSecond line",
                messages = listOf(
                    ChatMessage("assistant", "Visible response").beginAlternative()
                        .copy(content = "Current response").completeAlternative()
                )
            ),
            ""
        )

        assertEquals(true, exported.startsWith("# \\# First line\n"))
        assertEquals(true, exported.contains("Current response"))
        assertEquals(false, exported.contains("Visible response"))
    }
}

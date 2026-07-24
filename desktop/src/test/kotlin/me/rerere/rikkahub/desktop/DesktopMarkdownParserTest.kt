package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopMarkdownParserTest {
    @Test
    fun parsesCommonGfmBlocks() {
        val blocks = DesktopMarkdownParser.parse(
            """
            # Heading

            A **bold** and *soft* [link](https://example.com).

            > quoted

            1. first
            2. second

            ```kotlin
            val answer = 42
            ```
            """.trimIndent()
        )

        assertIs<MarkdownBlock.Heading>(blocks[0])
        val paragraph = assertIs<MarkdownBlock.Paragraph>(blocks[1])
        assertTrue(paragraph.spans.any { it.text == "bold" && it.bold })
        assertTrue(paragraph.spans.any { it.text == "soft" && it.italic })
        assertTrue(paragraph.spans.any { it.text == "link" && it.link == "https://example.com" })
        assertIs<MarkdownBlock.Quote>(blocks[2])
        assertIs<MarkdownBlock.ListBlock>(blocks[3])
        val code = assertIs<MarkdownBlock.Code>(blocks[4])
        assertEquals("kotlin", code.language)
        assertEquals("val answer = 42", code.content)
    }

    @Test
    fun parsesGfmTable() {
        val table = assertIs<MarkdownBlock.Table>(
            DesktopMarkdownParser.parse(
                """
                | Name | Value |
                | --- | --- |
                | Rikka | 42 |
                """.trimIndent()
            ).single()
        )

        assertEquals(listOf("Name", "Value"), table.headers)
        assertEquals(listOf(listOf("Rikka", "42")), table.rows)
    }
}

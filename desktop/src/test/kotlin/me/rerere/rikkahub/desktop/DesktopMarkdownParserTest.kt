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
                | **Name** | [Value](https://example.com) |
                | --- | --- |
                | *Rikka* | `42` |
                """.trimIndent()
            ).single()
        )

        assertTrue(table.headers[0].single().text == "Name" && table.headers[0].single().bold)
        assertTrue(table.headers[1].single().text == "Value" && table.headers[1].single().link == "https://example.com")
        assertTrue(table.rows[0][0].single().text == "Rikka" && table.rows[0][0].single().italic)
        assertTrue(table.rows[0][1].single().text == "42" && table.rows[0][1].single().code)
    }

    @Test
    fun appliesChineseTypographyWithoutChangingCodeOrMath() {
        val spans = listOf(
            MarkdownSpan("中文RikkaHub"),
            MarkdownSpan("代码RikkaHub", code = true),
            MarkdownSpan("x+y", math = true)
        ).withChineseTypography(enabled = true)

        assertEquals("中文 RikkaHub", spans[0].text)
        assertEquals("代码RikkaHub", spans[1].text)
        assertEquals("x+y", spans[2].text)
    }
}

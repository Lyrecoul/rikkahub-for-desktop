package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun parsesInlineCodeWithinChineseText() {
        val paragraph = assertIs<MarkdownBlock.Paragraph>(
            DesktopMarkdownParser.parse(
                "为了摆脱\"int 到底多大\"的噩梦，`<cstdint>` 提供了固定宽度类型：" +
                    "`int8_t / int16_t / int32_t / int64_t` 和对应的 `uint` 版。"
            ).single()
        )

        assertEquals(
            listOf(
                MarkdownSpan("为了摆脱\"int 到底多大\"的噩梦，"),
                MarkdownSpan("<cstdint>", code = true),
                MarkdownSpan(" 提供了固定宽度类型："),
                MarkdownSpan("int8_t / int16_t / int32_t / int64_t", code = true),
                MarkdownSpan(" 和对应的 "),
                MarkdownSpan("uint", code = true),
                MarkdownSpan(" 版。")
            ),
            paragraph.spans
        )
    }

    @Test
    fun preservesLiteralTildesWhileParsingStrikethrough() {
        val paragraph = assertIs<MarkdownBlock.Paragraph>(
            DesktopMarkdownParser.parse("Home: ~/projects; ~~obsolete~~").single()
        )

        assertEquals("Home: ~/projects; ", paragraph.spans[0].text)
        assertEquals("obsolete", paragraph.spans[1].text)
        assertTrue(paragraph.spans[1].strikethrough)
    }

    @Test
    fun recognizesMermaidCodeBlockLanguage() {
        assertTrue("mermaid".isMermaidLanguage())
        assertTrue(" Mermaid ".isMermaidLanguage())
        assertTrue(!"mmd".isMermaidLanguage())
    }

    @Test
    fun disablesMermaidRenderingByDefault() {
        val preferences = DesktopPreferences()

        assertFalse(preferences.enableMermaidRendering)
        assertFalse(preferences.enableMermaidCli)
        assertFalse(preferences.mermaidUseSystemBrowser)
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

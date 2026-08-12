package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class DesktopCodeHighlighterTest {
    @Test
    fun tokenizesKotlinCommentsStringsAndKeywords() {
        val tokens = tokenizeCode("// note\nval greeting = \"hello\"\nprintln(greeting)", "kotlin")

        assertEquals(CodeTokenType.Comment, tokens.first().type)
        assertTrue(tokens.any { it.text == "val" && it.type == CodeTokenType.Keyword })
        assertTrue(tokens.any { it.text == "\"hello\"" && it.type == CodeTokenType.String })
        assertTrue(tokens.any { it.text == "println" && it.type == CodeTokenType.Function })
    }

    @Test
    fun usesLanguageAliasesAndPreservesInput() {
        val code = "const answer = 42"
        val tokens = tokenizeCode(code, "js")

        assertEquals(code, tokens.joinToString(separator = "") { it.text })
        assertTrue(tokens.any { it.text == "const" && it.type == CodeTokenType.Keyword })
        assertTrue(tokens.any { it.text == "42" && it.type == CodeTokenType.Number })
    }

    @Test
    fun tokenizesLongPlainCodeInLinearTime() {
        val code = ".".repeat(49_000)
        val elapsed = measureTime { tokenizeCode(code, "kotlin") }

        assertTrue(elapsed < 1.seconds, "Highlighting took $elapsed")
    }
}

package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopModelDisplayNameTest {
    @Test
    fun formatsCommonModelIdentifiersForDisplay() {
        assertEquals("GPT-5.1 Codex Max", displayModelName("gpt-5-1-codex-max"))
        assertEquals("Claude Sonnet 4.5", displayModelName("claude-sonnet-4-5"))
        assertEquals("DeepSeek R1", displayModelName("deepseek-r1"))
        assertEquals("Qwen2.5 VL 72B", displayModelName("models/qwen2-5-vl-72b"))
    }

    @Test
    fun preservesUnknownIdentifiersWhileMakingThemReadable() {
        assertEquals("Custom Model V2", displayModelName("custom_model-v2"))
    }
}

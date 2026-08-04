package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DesktopProviderIconTest {
    @Test
    fun `prepared provider icons do not fall back to text`() {
        val providersWithIcons = listOf(
            "RikkaHub",
            "OpenAI",
            "Google Gemini",
            "Anthropic",
            "AiHubMix",
            "硅基流动",
            "DeepSeek",
            "OpenRouter",
            "Vercel AI Gateway",
            "小马算力",
            "月之暗面",
            "阿里云百炼",
            "火山引擎",
            "智谱AI开放平台",
            "阶跃星辰",
            "302.AI",
            "腾讯Hunyuan",
            "xAI",
            "MIMO"
        )

        providersWithIcons.forEach { provider ->
            assertNotNull(providerIconPath(provider), "Expected a prepared icon for $provider")
        }
    }

    @Test
    fun `model families resolve to their own icons`() {
        val modelIcons = mapOf(
            "gpt-4.1" to "openai.svg",
            "o3-mini" to "openai.svg",
            "claude-sonnet-4-5" to "claude-color.svg",
            "gemini-2.5-pro" to "gemini-color.svg",
            "deepseek-v3.2" to "deepseek-color.svg",
            "qwen3-max" to "qwen-color.svg",
            "glm-4.7" to "zhipu-color.svg",
            "doubao-seed-1.8" to "doubao-color.svg",
            "moonshot-v1-128k" to "kimi-color.svg",
            "meta-llama/llama-4-maverick" to "meta-color.svg",
            "mistral-large-latest" to "mistral-color.svg"
        )

        modelIcons.forEach { (model, expectedIcon) ->
            assertEquals(expectedIcon, providerIconPath(model), "Unexpected icon for $model")
        }
    }
}

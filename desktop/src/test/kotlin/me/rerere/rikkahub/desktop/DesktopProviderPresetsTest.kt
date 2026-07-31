package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopProviderPresetsTest {
    @Test
    fun includesAndroidCompatibleOpenAiPresets() {
        assertTrue(
            DesktopProviderPresets.map(DesktopProviderPreset::name).containsAll(
                listOf("RikkaHub", "OpenAI", "AiHubMix", "硅基流动", "DeepSeek", "OpenRouter", "月之暗面", "AckAI")
            )
        )
    }

    @Test
    fun keepsOpenRouterBalanceSemantics() {
        val openRouter = DesktopProviderPresets.first { it.name == "OpenRouter" }

        assertEquals("/credits", openRouter.balanceOptions.apiPath)
        assertEquals("data.total_credits - data.total_usage", openRouter.balanceOptions.resultPath)
    }
}

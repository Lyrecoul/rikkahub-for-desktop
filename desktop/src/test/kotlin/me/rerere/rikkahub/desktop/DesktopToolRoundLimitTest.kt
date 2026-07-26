package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopToolRoundLimitTest {
    @Test
    fun assistantDefaultsToEightToolRounds() {
        assertEquals(8, DesktopAssistantProfile().maxToolRounds)
    }

    @Test
    fun zeroToolRoundsRepresentsUnlimited() {
        assertEquals(0, DesktopAssistantProfile(maxToolRounds = 0).maxToolRounds)
    }
}

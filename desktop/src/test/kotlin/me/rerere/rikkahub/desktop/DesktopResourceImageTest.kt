package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertNotNull

class DesktopResourceImageTest {
    @Test
    fun decodesPackagedPngAndSvgResources() {
        assertNotNull(loadDesktopResourceImage("icon.png"))
        assertNotNull(loadDesktopResourceImage("icons/openai.svg"))
    }
}

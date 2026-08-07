package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopLocalizationTest {
    @Test
    fun translationTargetFallsBackToLocalizedChinese() {
        assertEquals(
            "简体中文",
            defaultTranslationTargetLanguage(DesktopLanguage.CHINESE_SIMPLIFIED)
        )
    }

    @Test
    fun translationTargetOptionsIncludeEverySupportedLanguageExceptSystem() {
        assertEquals(
            DesktopLanguage.entries.size - 1,
            translationTargetLanguageOptions(DesktopLanguage.ENGLISH).size
        )
        assertEquals(
            "English",
            translationTargetLanguageOptions(DesktopLanguage.ENGLISH).first()
        )
    }
}

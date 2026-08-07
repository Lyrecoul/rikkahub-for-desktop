package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

private val formatPlaceholder = Regex("""\{[^{}]+}|%(?:\d+\$)?[sd]""")

class DesktopLocalizationTest {
    @Test
    fun everyLanguageDictionaryMatchesChineseAndEnglishBaselines() {
        assertEquals(
            EnglishDesktopTranslations.keys,
            SimplifiedChineseDesktopTranslations.keys,
            "English and Simplified Chinese must define the same keys"
        )

        val baselineKeys = EnglishDesktopTranslations.keys
        val baselinePlaceholders = baselineKeys.associateWith { key ->
            placeholders(EnglishDesktopTranslations.getValue(key))
        }

        localizedDictionaries().forEach { (language, translations) ->
            assertEquals(baselineKeys, translations.keys, "$language has missing or extra translation keys")
            baselinePlaceholders.forEach { (key, expected) ->
                assertEquals(
                    expected,
                    placeholders(translations.getValue(key)),
                    "$language has incompatible placeholders for $key"
                )
            }
        }
    }

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

    private fun localizedDictionaries(): Map<DesktopLanguage, Map<String, String>> =
        DesktopLanguage.entries
            .filterNot { it == DesktopLanguage.SYSTEM }
            .associateWith { language ->
                val primary = when (language) {
                    DesktopLanguage.ENGLISH -> EnglishDesktopTranslations
                    DesktopLanguage.CHINESE_SIMPLIFIED -> SimplifiedChineseDesktopTranslations
                    DesktopLanguage.CHINESE_TRADITIONAL -> TraditionalChineseDesktopTranslations
                    DesktopLanguage.JAPANESE -> JapaneseDesktopTranslations
                    DesktopLanguage.KOREAN -> KoreanDesktopTranslations
                    DesktopLanguage.RUSSIAN -> RussianDesktopTranslations
                    DesktopLanguage.SPANISH -> SpanishDesktopTranslations
                    DesktopLanguage.FRENCH -> FrenchDesktopTranslations
                    DesktopLanguage.GERMAN -> GermanDesktopTranslations
                    DesktopLanguage.PORTUGUESE_BRAZIL -> PortugueseBrazilDesktopTranslations
                    DesktopLanguage.SYSTEM -> error("System language has no dictionary")
                }
                primary +
                    (DesktopNavigationTranslations[language] ?: emptyMap()) +
                    (DesktopCommonTranslations[language] ?: emptyMap()) +
                    (DesktopMessageJumperTranslations[language] ?: emptyMap())
            }

    private fun placeholders(value: String): List<String> =
        formatPlaceholder.findAll(value).map { it.value }.sorted().toList()
}

package me.rerere.rikkahub.desktop

import java.util.Locale

internal fun DesktopLanguage.resolved(): DesktopLanguage = when (this) {
    DesktopLanguage.SYSTEM -> when (Locale.getDefault().language) {
        "zh" -> if (Locale.getDefault().country.equals("TW", ignoreCase = true)) {
            DesktopLanguage.CHINESE_TRADITIONAL
        } else {
            DesktopLanguage.CHINESE_SIMPLIFIED
        }
        "ja" -> DesktopLanguage.JAPANESE
        "ko" -> DesktopLanguage.KOREAN
        "ru" -> DesktopLanguage.RUSSIAN
        "es" -> DesktopLanguage.SPANISH
        "fr" -> DesktopLanguage.FRENCH
        "de" -> DesktopLanguage.GERMAN
        "pt" -> DesktopLanguage.PORTUGUESE_BRAZIL
        else -> DesktopLanguage.ENGLISH
    }
    else -> this
}

internal fun DesktopLanguage.displayName(language: DesktopLanguage): String = when (this) {
        DesktopLanguage.SYSTEM -> desktopText(language, "language.system_default")
        DesktopLanguage.ENGLISH -> "English"
        DesktopLanguage.CHINESE_SIMPLIFIED -> "简体中文"
        DesktopLanguage.CHINESE_TRADITIONAL -> "繁體中文"
        DesktopLanguage.JAPANESE -> "日本語"
        DesktopLanguage.KOREAN -> "한국어"
        DesktopLanguage.RUSSIAN -> "Русский"
        DesktopLanguage.SPANISH -> "Español"
        DesktopLanguage.FRENCH -> "Français"
        DesktopLanguage.GERMAN -> "Deutsch"
        DesktopLanguage.PORTUGUESE_BRAZIL -> "Português (Brasil)"
    }

/** Returns the translation for [key], falling back to English and finally the key itself. */
internal fun desktopText(language: DesktopLanguage, key: String): String {
    val resolvedLanguage = language.resolved()
    val texts = DesktopTranslations[resolvedLanguage] ?: EnglishDesktopTranslations
    return texts[key]
        ?: DesktopNavigationTranslations[resolvedLanguage]?.get(key)
        ?: DesktopCommonTranslations[resolvedLanguage]?.get(key)
        ?: DesktopMessageJumperTranslations[resolvedLanguage]?.get(key)
        ?: EnglishDesktopTranslations[key]
        ?: key
}

private val DesktopTranslations = mapOf(
    DesktopLanguage.ENGLISH to EnglishDesktopTranslations,
    DesktopLanguage.CHINESE_SIMPLIFIED to SimplifiedChineseDesktopTranslations,
    DesktopLanguage.CHINESE_TRADITIONAL to TraditionalChineseDesktopTranslations,
    DesktopLanguage.JAPANESE to JapaneseDesktopTranslations,
    DesktopLanguage.KOREAN to KoreanDesktopTranslations,
    DesktopLanguage.RUSSIAN to RussianDesktopTranslations,
    DesktopLanguage.SPANISH to SpanishDesktopTranslations,
    DesktopLanguage.FRENCH to FrenchDesktopTranslations,
    DesktopLanguage.GERMAN to GermanDesktopTranslations,
    DesktopLanguage.PORTUGUESE_BRAZIL to PortugueseBrazilDesktopTranslations
)

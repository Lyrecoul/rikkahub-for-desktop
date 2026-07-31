package me.rerere.rikkahub.desktop

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private data class ThemeColors(
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color
)

internal fun desktopColorScheme(theme: DesktopThemeColor, dark: Boolean): ColorScheme {
    val colors = if (dark) darkThemeColors(theme) else lightThemeColors(theme)
    return if (dark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.background,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.primary,
            secondary = colors.secondary,
            onSecondary = colors.background,
            secondaryContainer = colors.secondaryContainer,
            onSecondaryContainer = colors.secondary,
            tertiary = colors.tertiary,
            onTertiary = colors.background,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.onSurfaceVariant,
            outline = colors.outline
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.primary,
            secondary = colors.secondary,
            onSecondary = Color.White,
            secondaryContainer = colors.secondaryContainer,
            onSecondaryContainer = colors.secondary,
            tertiary = colors.tertiary,
            onTertiary = Color.White,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.onSurfaceVariant,
            outline = colors.outline
        )
    }
}

internal fun DesktopThemeColor.previewColors(dark: Boolean): List<Color> {
    val colors = if (dark) darkThemeColors(this) else lightThemeColors(this)
    return listOf(colors.primary, colors.secondary, colors.tertiary)
}

internal fun desktopTypography(fontFamily: DesktopFontFamily): Typography {
    val family = when (fontFamily) {
        DesktopFontFamily.SYSTEM -> FontFamily.Default
        DesktopFontFamily.SANS_SERIF -> FontFamily.SansSerif
        DesktopFontFamily.SERIF -> FontFamily.Serif
        DesktopFontFamily.MONOSPACE -> FontFamily.Monospace
    }
    val defaults = Typography()
    return defaults.copy(
        displayLarge = defaults.displayLarge.copy(fontFamily = family),
        displayMedium = defaults.displayMedium.copy(fontFamily = family),
        displaySmall = defaults.displaySmall.copy(fontFamily = family),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = family),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = family),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = family),
        titleLarge = defaults.titleLarge.copy(fontFamily = family),
        titleMedium = defaults.titleMedium.copy(fontFamily = family),
        titleSmall = defaults.titleSmall.copy(fontFamily = family),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = family),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = family),
        bodySmall = defaults.bodySmall.copy(fontFamily = family),
        labelLarge = defaults.labelLarge.copy(fontFamily = family),
        labelMedium = defaults.labelMedium.copy(fontFamily = family),
        labelSmall = defaults.labelSmall.copy(fontFamily = family)
    )
}

private fun lightThemeColors(theme: DesktopThemeColor): ThemeColors = when (theme) {
    DesktopThemeColor.SAKURA -> ThemeColors(
        Color(0xFF8E4955),
        Color(0xFFFFD9DD),
        Color(0xFF76565A),
        Color(0xFFFFD9DD),
        Color(0xFF785831),
        Color(0xFFFFF8F7),
        Color(0xFFFFF8F7),
        Color(0xFFF3DDDF),
        Color(0xFF22191A),
        Color(0xFF22191A),
        Color(0xFF524345),
        Color(0xFF847374)
    )

    DesktopThemeColor.OCEAN -> ThemeColors(
        Color(0xFF006A6A),
        Color(0xFF9CF1F0),
        Color(0xFF4A6363),
        Color(0xFFCCE8E7),
        Color(0xFF4C5F91),
        Color(0xFFF4FBFA),
        Color(0xFFF4FBFA),
        Color(0xFFDAE5E4),
        Color(0xFF161D1D),
        Color(0xFF161D1D),
        Color(0xFF3F4948),
        Color(0xFF6F7978)
    )

    DesktopThemeColor.FOREST -> ThemeColors(
        Color(0xFF386A20),
        Color(0xFFB8F397),
        Color(0xFF52634A),
        Color(0xFFD5E8CA),
        Color(0xFF386568),
        Color(0xFFF8FBF1),
        Color(0xFFF8FBF1),
        Color(0xFFE0E4D8),
        Color(0xFF191D17),
        Color(0xFF191D17),
        Color(0xFF44483F),
        Color(0xFF74796E)
    )

    DesktopThemeColor.SUNSET -> ThemeColors(
        Color(0xFF9C4100),
        Color(0xFFFFDBCA),
        Color(0xFF765744),
        Color(0xFFFFDCC9),
        Color(0xFF745A00),
        Color(0xFFFFF8F5),
        Color(0xFFFFF8F5),
        Color(0xFFF3DFD5),
        Color(0xFF241A15),
        Color(0xFF241A15),
        Color(0xFF554238),
        Color(0xFF887367)
    )

    DesktopThemeColor.LAVENDER -> ThemeColors(
        Color(0xFF6750A4),
        Color(0xFFE9DDFF),
        Color(0xFF625B71),
        Color(0xFFE8DEF8),
        Color(0xFF7D5260),
        Color(0xFFFFF7FF),
        Color(0xFFFFF7FF),
        Color(0xFFE7E0EC),
        Color(0xFF1D1B20),
        Color(0xFF1D1B20),
        Color(0xFF49454F),
        Color(0xFF79747E)
    )

    DesktopThemeColor.SLATE -> ThemeColors(
        Color(0xFF365F91),
        Color(0xFFD4E3FF),
        Color(0xFF535F70),
        Color(0xFFD7E3F7),
        Color(0xFF695779),
        Color(0xFFF9F9FF),
        Color(0xFFF9F9FF),
        Color(0xFFE0E2EC),
        Color(0xFF191C20),
        Color(0xFF191C20),
        Color(0xFF44474E),
        Color(0xFF74777F)
    )
}

private fun darkThemeColors(theme: DesktopThemeColor): ThemeColors = when (theme) {
    DesktopThemeColor.SAKURA -> ThemeColors(
        Color(0xFFFFB2BC),
        Color(0xFF72333E),
        Color(0xFFE5BDC1),
        Color(0xFF5C3F43),
        Color(0xFFEABF8F),
        Color(0xFF1A1112),
        Color(0xFF1A1112),
        Color(0xFF524345),
        Color(0xFFF0DEDF),
        Color(0xFFF0DEDF),
        Color(0xFFD7C1C3),
        Color(0xFF9F8C8E)
    )

    DesktopThemeColor.OCEAN -> ThemeColors(
        Color(0xFF4CDADA),
        Color(0xFF004F50),
        Color(0xFFB0CCCB),
        Color(0xFF324B4B),
        Color(0xFFB5C5FF),
        Color(0xFF0E1515),
        Color(0xFF0E1515),
        Color(0xFF3F4948),
        Color(0xFFDDE4E3),
        Color(0xFFDDE4E3),
        Color(0xFFBEC9C8),
        Color(0xFF899392)
    )

    DesktopThemeColor.FOREST -> ThemeColors(
        Color(0xFF9CD67C),
        Color(0xFF205107),
        Color(0xFFB9CDB0),
        Color(0xFF3B4B35),
        Color(0xFFA0CFD2),
        Color(0xFF11150F),
        Color(0xFF11150F),
        Color(0xFF44483F),
        Color(0xFFE1E5D9),
        Color(0xFFE1E5D9),
        Color(0xFFC4C8BC),
        Color(0xFF8E9388)
    )

    DesktopThemeColor.SUNSET -> ThemeColors(
        Color(0xFFFFB68C),
        Color(0xFF773100),
        Color(0xFFE7BDA5),
        Color(0xFF5C4030),
        Color(0xFFE7C350),
        Color(0xFF1B120D),
        Color(0xFF1B120D),
        Color(0xFF554238),
        Color(0xFFEDE0D9),
        Color(0xFFEDE0D9),
        Color(0xFFD7C3B8),
        Color(0xFFA18C80)
    )

    DesktopThemeColor.LAVENDER -> ThemeColors(
        Color(0xFFD0BCFF),
        Color(0xFF4F378B),
        Color(0xFFCCC2DC),
        Color(0xFF4A4458),
        Color(0xFFEFB8C8),
        Color(0xFF141218),
        Color(0xFF141218),
        Color(0xFF49454F),
        Color(0xFFE6E0E9),
        Color(0xFFE6E0E9),
        Color(0xFFCAC4D0),
        Color(0xFF938F99)
    )

    DesktopThemeColor.SLATE -> ThemeColors(
        Color(0xFFA6C8FF),
        Color(0xFF1D4F7C),
        Color(0xFFBBC7DB),
        Color(0xFF3C4758),
        Color(0xFFD6BEE8),
        Color(0xFF111318),
        Color(0xFF111318),
        Color(0xFF44474E),
        Color(0xFFE1E2E9),
        Color(0xFFE1E2E9),
        Color(0xFFC4C6D0),
        Color(0xFF8E9099)
    )
}

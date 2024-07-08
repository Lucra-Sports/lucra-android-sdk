package com.lucrasports.sdk.app.theming

import com.lucrasports.sdk.core.style_guide.ColorStyle

internal class ApplyThemeHelper(
    private val onColorSelected: (dialogId: Int, color: Int) -> Unit
) {

    private val defaultTheme = listOf(
        "#09E35F", // primary
        "#5E5BD0", // secondary
        "#9C99FC", // tertiary
        "#1C2575", // surface
        "#001448", // background
        "#001448", // onPrimary
        "#FFFFFF", // onSecondary
        "#FFFFFF", // onTertiary
        "#FFFFFF", // onSurface
        "#FFFFFF"  // onBackground
    )

    private val duperTheme = listOf(
        "#3A79E0", // primary
        "#EBECF2", // secondary
        "#CDD0DF", // tertiary
        "#F5F6F9", // surface
        "#FFFFFF", // background
        "#FFFFFF", // onPrimary
        "#05155E", // onSecondary
        "#05155E", // onTertiary
        "#05155E", // onSurface
        "#05155E"  // onBackground
    )

    private val chaosTheme = listOf(
        "#A3D16E", // primary
        "#285FF5", // secondary
        "#CDD0DF", // tertiary
        "#541107", // surface
        "#FFFFFF", // background
        "#000000", // onPrimary
        "#FFFFFF", // onSecondary
        "#05155E", // onTertiary
        "#FFFFFF", // onSurface
        "#000000"  // onBackground
    )

    internal fun hexToIntColor(hex: String): Int {
        return hex.removePrefix("#").toInt(16) or 0xFF000000.toInt()
    }

    internal fun intToColorHex(color: Int): String {
        return String.format("#%06X", (color and 0xFFFFFF))
    }

    private fun applyTheme(theme: List<String>) {
        theme.forEachIndexed { index, colorHex ->
            onColorSelected(index, hexToIntColor(colorHex))
        }
    }

    internal fun applyDefaultTheme() { applyTheme(defaultTheme) }

    internal fun applyDuprTheme() { applyTheme(duperTheme) }

    internal fun applyChaosTheme() { applyTheme(chaosTheme) }

    internal fun getLightColorStyle() = ColorStyle(
        primary = ThemeColors.getColorHexById(LightThemeColorOption.PRIMARY.id),
        secondary = ThemeColors.getColorHexById(LightThemeColorOption.SECONDARY.id),
        tertiary = ThemeColors.getColorHexById(LightThemeColorOption.TERTIARY.id),
        surface = ThemeColors.getColorHexById(LightThemeColorOption.SURFACE.id),
        background = ThemeColors.getColorHexById(LightThemeColorOption.BACKGROUND.id),
        onPrimary = ThemeColors.getColorHexById(LightThemeColorOption.ON_PRIMARY.id),
        onSecondary = ThemeColors.getColorHexById(LightThemeColorOption.ON_SECONDARY.id),
        onTertiary = ThemeColors.getColorHexById(LightThemeColorOption.ON_TERTIARY.id),
        onSurface = ThemeColors.getColorHexById(LightThemeColorOption.ON_SURFACE.id),
        onBackground = ThemeColors.getColorHexById(LightThemeColorOption.ON_BACKGROUND.id)
    )

    internal fun getDarkColorStyle() = ColorStyle(
        primary = ThemeColors.getColorHexById(DarkThemeColorOption.PRIMARY.id),
        secondary = ThemeColors.getColorHexById(DarkThemeColorOption.SECONDARY.id),
        tertiary = ThemeColors.getColorHexById(DarkThemeColorOption.TERTIARY.id),
        surface = ThemeColors.getColorHexById(DarkThemeColorOption.SURFACE.id),
        background = ThemeColors.getColorHexById(DarkThemeColorOption.BACKGROUND.id),
        onPrimary = ThemeColors.getColorHexById(DarkThemeColorOption.ON_PRIMARY.id),
        onSecondary = ThemeColors.getColorHexById(DarkThemeColorOption.ON_SECONDARY.id),
        onTertiary = ThemeColors.getColorHexById(DarkThemeColorOption.ON_TERTIARY.id),
        onSurface = ThemeColors.getColorHexById(DarkThemeColorOption.ON_SURFACE.id),
        onBackground = ThemeColors.getColorHexById(DarkThemeColorOption.ON_BACKGROUND.id)
    )
}
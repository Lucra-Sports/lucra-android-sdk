package com.lucrasports.sdk.app.theming

internal object ThemeColors {
    private val colors = mutableMapOf(
        LightThemeColorOption.PRIMARY.id to LightThemeColorOption.PRIMARY.colorHex,
        LightThemeColorOption.SECONDARY.id to LightThemeColorOption.SECONDARY.colorHex,
        LightThemeColorOption.TERTIARY.id to LightThemeColorOption.TERTIARY.colorHex,
        LightThemeColorOption.SURFACE.id to LightThemeColorOption.SURFACE.colorHex,
        LightThemeColorOption.BACKGROUND.id to LightThemeColorOption.BACKGROUND.colorHex,
        LightThemeColorOption.ON_PRIMARY.id to LightThemeColorOption.ON_PRIMARY.colorHex,
        LightThemeColorOption.ON_SECONDARY.id to LightThemeColorOption.ON_SECONDARY.colorHex,
        LightThemeColorOption.ON_TERTIARY.id to LightThemeColorOption.ON_TERTIARY.colorHex,
        LightThemeColorOption.ON_SURFACE.id to LightThemeColorOption.ON_SURFACE.colorHex,
        LightThemeColorOption.ON_BACKGROUND.id to LightThemeColorOption.ON_BACKGROUND.colorHex,
        DarkThemeColorOption.PRIMARY.id to DarkThemeColorOption.PRIMARY.colorHex,
        DarkThemeColorOption.SECONDARY.id to DarkThemeColorOption.SECONDARY.colorHex,
        DarkThemeColorOption.TERTIARY.id to DarkThemeColorOption.TERTIARY.colorHex,
        DarkThemeColorOption.SURFACE.id to DarkThemeColorOption.SURFACE.colorHex,
        DarkThemeColorOption.BACKGROUND.id to DarkThemeColorOption.BACKGROUND.colorHex,
        DarkThemeColorOption.ON_PRIMARY.id to DarkThemeColorOption.ON_PRIMARY.colorHex,
        DarkThemeColorOption.ON_SECONDARY.id to DarkThemeColorOption.ON_SECONDARY.colorHex,
        DarkThemeColorOption.ON_TERTIARY.id to DarkThemeColorOption.ON_TERTIARY.colorHex,
        DarkThemeColorOption.ON_SURFACE.id to DarkThemeColorOption.ON_SURFACE.colorHex,
        DarkThemeColorOption.ON_BACKGROUND.id to DarkThemeColorOption.ON_BACKGROUND.colorHex
    )

    fun getColorHexById(id: Int): String = colors[id] ?: throw IllegalArgumentException("Invalid color ID: $id")

    fun setNewColor(
        forId: Int,
        colorHex: String
    ) {
        colors[forId] = colorHex
    }
}

internal enum class LightThemeColorOption(
    val descriptor: String,
    val id: Int,
    val colorHex: String
) {
    PRIMARY("Primary", 0, "#3A79E0"),
    SECONDARY("Secondary", 1, "#EBECF2"),
    TERTIARY("Tertiary", 2, "#CDD0DF"),
    SURFACE("Surface", 3, "#F5F6F9"),
    BACKGROUND("Background", 4, "#FFFFFF"),
    ON_PRIMARY("On Primary", 5, "#FFFFFF"),
    ON_SECONDARY("On Secondary", 6, "#05155E"),
    ON_TERTIARY("On Tertiary", 7, "#05155E"),
    ON_SURFACE("On Surface", 8, "#05155E"),
    ON_BACKGROUND("On Background", 9, "#05155E");
}

internal enum class DarkThemeColorOption(
    val descriptor: String,
    val id: Int,
    val colorHex: String
) {
    PRIMARY("Primary", 10, "#09E35F"),
    SECONDARY("Secondary", 11, "#5E5BD0"),
    TERTIARY("Tertiary", 12, "#9C99FC"),
    SURFACE("Surface", 13, "#1C2575"),
    BACKGROUND("Background", 14, "#001448"),
    ON_PRIMARY("On Primary", 15, "#001448"),
    ON_SECONDARY("On Secondary", 16, "#FFFFFF"),
    ON_TERTIARY("On Tertiary", 17, "#FFFFFF"),
    ON_SURFACE("On Surface", 18, "#FFFFFF"),
    ON_BACKGROUND("On Background", 19, "#FFFFFF");
}
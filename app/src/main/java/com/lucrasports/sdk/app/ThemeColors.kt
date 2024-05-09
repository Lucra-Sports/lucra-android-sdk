package com.lucrasports.sdk.app

internal object ThemeColors {
    private val colors = mutableMapOf(
        ThemeColorOption.PRIMARY.id to ThemeColorOption.PRIMARY.colorHex,
        ThemeColorOption.SECONDARY.id to ThemeColorOption.SECONDARY.colorHex,
        ThemeColorOption.TERTIARY.id to ThemeColorOption.TERTIARY.colorHex,
        ThemeColorOption.SURFACE.id to ThemeColorOption.SURFACE.colorHex,
        ThemeColorOption.BACKGROUND.id to ThemeColorOption.BACKGROUND.colorHex,
        ThemeColorOption.ON_PRIMARY.id to ThemeColorOption.ON_PRIMARY.colorHex,
        ThemeColorOption.ON_SECONDARY.id to ThemeColorOption.ON_SECONDARY.colorHex,
        ThemeColorOption.ON_TERTIARY.id to ThemeColorOption.ON_TERTIARY.colorHex,
        ThemeColorOption.ON_SURFACE.id to ThemeColorOption.ON_SURFACE.colorHex,
        ThemeColorOption.ON_BACKGROUND.id to ThemeColorOption.ON_BACKGROUND.colorHex
    )

    fun getColorHexById(id: Int): String = colors[id] ?: throw IllegalArgumentException("Invalid color ID: $id")

    fun setNewColor(
        forId: Int,
        colorHex: String
    ) {
        colors[forId] = colorHex
    }
}

internal enum class ThemeColorOption(
    val descriptor: String,
    val id: Int,
    val colorHex: String
) {
    PRIMARY("Primary", 0, "#1976D2"),
    SECONDARY("Secondary", 1, "#F57C00"),
    TERTIARY("Tertiary", 2, "#388E3C"),
    SURFACE("Surface", 3, "#FFFFFF"),
    BACKGROUND("Background", 4, "#F5F5F5"),
    ON_PRIMARY("On Primary", 5, "#FFFFFF"),
    ON_SECONDARY("On Secondary", 6, "#FFFFFF"),
    ON_TERTIARY("On Tertiary", 7, "#FFFFFF"),
    ON_SURFACE("On Surface", 8, "#000000"),
    ON_BACKGROUND("On Background", 9, "#000000");

    companion object {
        fun hexToIntColor(hex: String): Int {
            return hex.removePrefix("#").toInt(16) or 0xFF000000.toInt()
        }

        fun intToColorHex(color: Int): String {
            return String.format("#%06X", (color and 0xFFFFFF))
        }
    }
}

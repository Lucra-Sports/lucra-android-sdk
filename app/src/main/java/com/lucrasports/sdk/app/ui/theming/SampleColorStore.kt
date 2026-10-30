package com.lucrasports.sdk.app.ui.theming

import com.lucrasports.sdk.app.BuildConfig
import com.lucrasports.sdk.core.style_guide.ColorStyle

internal object SampleColorStore {

    val dandbLightTheme = ColorStyle(
        primary         = "#0D0441",
        secondary       = "#FFFFFF",
        tertiary        = "#FFFFFF",
        onPrimary       = "#FE5B00",
        onSecondary     = "#2F26D3",
        onTertiary      = "#4FABF7",
    )

    val dandbDarkTheme = ColorStyle(
        primary         = "#FE5B00",
        secondary       = "#2F26D3",
        tertiary        = "#4FABF7",
        onPrimary       = "#0D0441",
        onSecondary     = "#FFFFFF",
        onTertiary      = "#FFFFFF"
    )

    val duprTheme = ColorStyle(
        primary = "#3A79E0",
        secondary = "#EBECF2",
        tertiary = "#CDD0DF",
        onPrimary = "#FFFFFF",
        onSecondary = "#05155E",
        onTertiary = "#05155E"
    )

    val psfTheme = ColorStyle(
        primary = "#387FD1",
        secondary = "#121212",
        tertiary = "#FFFFFF",
        onPrimary = "#FFFFFF",
        onSecondary = "#FFFFFF",
        onTertiary = "#000000"
    )

    val t1Theme = ColorStyle(
        primary = "#DEE32A",
        secondary = "#5E5BD0",
        tertiary = "#9C99FC",
        onPrimary = "#001448",
        onSecondary = "#FFFFFF",
        onTertiary = "#FFFFFF"
    )

    val chaosTheme = ColorStyle(
        primary = "#A3D16E",
        secondary = "#285FF5",
        tertiary = "#CDD0DF",
        onPrimary = "#000000",
        onSecondary = "#FFFFFF",
        onTertiary = "#05155E"
    )

    val trackmanTheme = ColorStyle(
        primary = "#EC691A",
        secondary = "#101820",
        tertiary = "#E8E9EB",
        onPrimary = "#101820",
        onSecondary = "#FFFFFF",
        onTertiary = "#101820"
    )

    // Lucra's own default theme, from the 2026 Lucra Style Guide Library.
    private const val SECONDARY = "#6654D8"
    private const val ON_SECONDARY = "#FDFEFF"
    private const val TERTIARY = "#FFB364"
    private const val ON_TERTIARY = "#FDFEFF"

    /*
     * 2026's dark-mode onPrimary, used in both modes: 2026 pairs its dark-green light primary with
     * white, which is unreadable on the bright 2025 environment primaries (the staging yellow).
     */
    private const val ON_PRIMARY = "#09090A"

    /** Per-environment primary, from the 2025 style guide's environment swatches. */
    private val environmentPrimary: String = when (BuildConfig.BUILD_TYPE) {
        "release" -> "#09E35F"
        "sandbox" -> "#C2B280"
        "staging" -> "#FDE92B"
        "dev2" -> "#3A79E0"
        // debug, and any other variant, which is important for the public sample
        else -> "#FE5B00"
    }

    val defaultBaseTheme = ColorStyle(
        primary = environmentPrimary,
        secondary = SECONDARY,
        tertiary = TERTIARY,
        onPrimary = ON_PRIMARY,
        onSecondary = ON_SECONDARY,
        onTertiary = ON_TERTIARY,
    )

    /*
     * Identical under the 2026 guide, but kept as two so `ClientTheme` still receives both modes
     * and the theme picker can re-point either one.
     */
    val defaultLightModeTheme = defaultBaseTheme
    val defaultDarkModeTheme = defaultBaseTheme

    private var activeLightModeTheme: ColorStyle = defaultLightModeTheme
    private var activeDarkModeTheme: ColorStyle = defaultDarkModeTheme

    private fun String.hexToIntColor(): Int {
        return removePrefix("#").toInt(16) or 0xFF000000.toInt()
    }

    internal fun Int.intToColorHex(): String {
        return String.format("#%06X", (this and 0xFFFFFF))
    }

    internal fun applyTheme(lightMode: ColorStyle, darkMode: ColorStyle = lightMode) {
        activeLightModeTheme = lightMode
        activeDarkModeTheme = darkMode
    }

    internal fun updateLightProperty(update: ColorStyle.() -> ColorStyle) {
        activeLightModeTheme = activeLightModeTheme.update()
    }

    internal fun updateDarkProperty(update: ColorStyle.() -> ColorStyle) {
        activeDarkModeTheme = activeDarkModeTheme.update()
    }

    internal fun getLightColorStyle(): ColorStyle = activeLightModeTheme

    internal fun getDarkColorStyle(): ColorStyle = activeDarkModeTheme


    // This is required for the color mapping logic we have
    enum class ColorIdMap(
        val id: Int,
        val update: (Int) -> Unit
    ) {
        LIGHT_BACKGROUND(0, {
            updateLightProperty {
                copy(background = it.intToColorHex())
            }
        }),
        LIGHT_SURFACE(1, {
            updateLightProperty {
                copy(surface = it.intToColorHex())
            }
        }),
        LIGHT_PRIMARY(2, {
            updateLightProperty {
                copy(primary = it.intToColorHex())
            }
        }),
        LIGHT_SECONDARY(3, {
            updateLightProperty {
                copy(secondary = it.intToColorHex())
            }
        }),
        LIGHT_TERTIARY(4, {
            updateLightProperty {
                copy(tertiary = it.intToColorHex())
            }
        }),
        LIGHT_ON_PRIMARY(7, {
            updateLightProperty {
                copy(onPrimary = it.intToColorHex())
            }
        }),
        LIGHT_ON_SECONDARY(8, {
            updateLightProperty {
                copy(onSecondary = it.intToColorHex())
            }
        }),
        LIGHT_ON_TERTIARY(9, {
            updateLightProperty {
                copy(onTertiary = it.intToColorHex())
            }
        }),
        DARK_PRIMARY(12, {
            updateDarkProperty {
                copy(primary = it.intToColorHex())
            }
        }),
        DARK_SECONDARY(13, {
            updateDarkProperty {
                copy(secondary = it.intToColorHex())
            }
        }),
        DARK_TERTIARY(14, {
            updateDarkProperty {
                copy(tertiary = it.intToColorHex())
            }
        }),
        DARK_ON_PRIMARY(17, {
            updateDarkProperty {
                copy(onPrimary = it.intToColorHex())
            }
        }),
        DARK_ON_SECONDARY(18, {
            updateDarkProperty {
                copy(onSecondary = it.intToColorHex())
            }
        }),
        DARK_ON_TERTIARY(19, {
            updateDarkProperty {
                copy(onTertiary = it.intToColorHex())
            }
        });

        companion object {
            fun updateColorBasedOnId(id: Int, color: Int) =
                entries.first { it.id == id }.update(color)
        }
    }

    internal fun getColorIdHexIntForAllLightModeProperties(details: (id: Int, title: String, colorHex: String, colorInt: Int) -> Unit) {
        val style = getLightColorStyle()
        details(
            ColorIdMap.LIGHT_PRIMARY.id,
            "Primary",
            style.primary!!,
            style.primary!!.hexToIntColor()
        )
        details(
            ColorIdMap.LIGHT_SECONDARY.id,
            "Secondary",
            style.secondary!!,
            style.secondary!!.hexToIntColor()
        )
        details(
            ColorIdMap.LIGHT_TERTIARY.id,
            "Tertiary",
            style.tertiary!!,
            style.tertiary!!.hexToIntColor()
        )
        //on
        details(
            ColorIdMap.LIGHT_ON_PRIMARY.id,
            "On Primary",
            style.onPrimary!!,
            style.onPrimary!!.hexToIntColor()
        )
        details(
            ColorIdMap.LIGHT_ON_SECONDARY.id,
            "On Secondary",
            style.onSecondary!!,
            style.onSecondary!!.hexToIntColor()
        )
        details(
            ColorIdMap.LIGHT_ON_TERTIARY.id,
            "On Tertiary",
            style.onTertiary!!,
            style.onTertiary!!.hexToIntColor()
        )
    }

    internal fun getColorIdHexIntForAllDarkModeProperties(details: (id: Int, title: String, colorHex: String, colorInt: Int) -> Unit) {
        val style = getDarkColorStyle()
        details(
            ColorIdMap.DARK_PRIMARY.id,
            "Primary",
            style.primary!!,
            style.primary!!.hexToIntColor()
        )
        details(
            ColorIdMap.DARK_SECONDARY.id,
            "Secondary",
            style.secondary!!,
            style.secondary!!.hexToIntColor()
        )
        details(
            ColorIdMap.DARK_TERTIARY.id,
            "Tertiary",
            style.tertiary!!,
            style.tertiary!!.hexToIntColor()
        )
        //on
        details(
            ColorIdMap.DARK_ON_PRIMARY.id,
            "On Primary",
            style.onPrimary!!,
            style.onPrimary!!.hexToIntColor()
        )
        details(
            ColorIdMap.DARK_ON_SECONDARY.id,
            "On Secondary",
            style.onSecondary!!,
            style.onSecondary!!.hexToIntColor()
        )
        details(
            ColorIdMap.DARK_ON_TERTIARY.id,
            "On Tertiary",
            style.onTertiary!!,
            style.onTertiary!!.hexToIntColor()
        )
    }
}
package com.lucrasports.sdk.app.theming

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

    val defaultBaseTheme = ColorStyle(
        primary = "#FA5455",
        secondary = "#5E5BD0",
        tertiary = "#9C99FC",
        onPrimary = "#001448",
        onSecondary = "#FFFFFF",
        onTertiary = "#FFFFFF"
    )

    val defaultLightModeTheme = when (BuildConfig.BUILD_TYPE) {
        "release" -> ColorStyle(
            primary = "#09E35F",
            secondary = "#5E5BD0",
            tertiary = "#9C99FC",
            onPrimary = "#001448",
            onSecondary = "#FFFFFF",
            onTertiary = "#FFFFFF"
        )

        "sandbox" -> ColorStyle(
            primary = "#C2B280",
            secondary = "#5E5BD0",
            tertiary = "#9C99FC",
            onPrimary = "#001448",
            onSecondary = "#FFFFFF",
            onTertiary = "#FFFFFF"
        )

        "staging" -> ColorStyle(
            primary = "#FDE92B",
            secondary = "#5E5BD0",
            tertiary = "#9C99FC",
            onPrimary = "#001448",
            onSecondary = "#FFFFFF",
            onTertiary = "#FFFFFF"
        )
        // debug and any other variant, which is important for the public sample
        else -> ColorStyle(
            primary = "#FA5455",
            secondary = "#5E5BD0",
            tertiary = "#9C99FC",
            onPrimary = "#001448",
            onSecondary = "#FFFFFF",
            onTertiary = "#FFFFFF"
        )
    }

    val defaultDarkModeTheme = when (BuildConfig.BUILD_TYPE) {
        "release" -> defaultBaseTheme.copy(primary = "#09E35F")
        "sandbox" -> defaultBaseTheme.copy(primary = "#C2B280")
        "staging" -> defaultBaseTheme.copy(primary = "#FDE92B")
        "debug" -> defaultBaseTheme.copy(primary = "#FE5B00")
        "dev2" -> defaultBaseTheme.copy(primary = "#3A79E0")
        else -> defaultBaseTheme
    }

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
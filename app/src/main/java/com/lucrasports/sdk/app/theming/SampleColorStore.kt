package com.lucrasports.sdk.app.theming

import com.lucrasports.sdk.app.BuildConfig
import com.lucrasports.sdk.core.style_guide.ColorStyle

internal object SampleColorStore {

    val duprTheme = ColorStyle(
        background = "#FFFFFF",
        surface = "#F5F6F9",
        primary = "#3A79E0",
        secondary = "#EBECF2",
        tertiary = "#CDD0DF",
        onBackground = "#05155E",
        onSurface = "#05155E",
        onPrimary = "#FFFFFF",
        onSecondary = "#05155E",
        onTertiary = "#05155E"
    )

    val psfTheme = ColorStyle(
        background = "#1E1E29",
        surface = "#2C3042",
        primary = "#387FD1",
        secondary = "#121212",
        tertiary = "#FFFFFF",
        onBackground = "#FFFFFF",
        onSurface = "#FFFFFF",
        onPrimary = "#FFFFFF",
        onSecondary = "#FFFFFF",
        onTertiary = "#000000"
    )

    val t1Theme = ColorStyle(
        background = "#2C2F74",
        surface = "#474DD0",
        primary = "#DEE32A",
        secondary = "#5E5BD0",
        tertiary = "#9C99FC",
        onBackground = "#FFFFFF",
        onSurface = "#FFFFFF",
        onPrimary = "#001448",
        onSecondary = "#FFFFFF",
        onTertiary = "#FFFFFF"
    )

    val chaosTheme = ColorStyle(
        background = "#FFFFFF",
        surface = "#541107",
        primary = "#A3D16E",
        secondary = "#285FF5",
        tertiary = "#CDD0DF",
        onBackground = "#000000",
        onSurface = "#FFFFFF",
        onPrimary = "#000000",
        onSecondary = "#FFFFFF",
        onTertiary = "#05155E"
    )

    val defaultLightModeTheme = when (BuildConfig.BUILD_TYPE) {
        "release" -> ColorStyle(
            background = "#FDFDFD",
            surface = "#1C2575",
            primary = "#09E35F",
            secondary = "#5E5BD0",
            tertiary = "#9C99FC",
            onBackground = "#001448",
            onSurface = "#FFFFFF",
            onPrimary = "#001448",
            onSecondary = "#FFFFFF",
            onTertiary = "#FFFFFF"
        )

        "sandbox" -> ColorStyle(
            background = "#FDFDFD",
            surface = "#1C2575",
            primary = "#C2B280",
            secondary = "#5E5BD0",
            tertiary = "#9C99FC",
            onBackground = "#001448",
            onSurface = "#FFFFFF",
            onPrimary = "#001448",
            onSecondary = "#FFFFFF",
            onTertiary = "#FFFFFF"
        )

        "staging" -> ColorStyle(
            background = "#FDFDFD",
            surface = "#1C2575",
            primary = "#FDE92B",
            secondary = "#5E5BD0",
            tertiary = "#9C99FC",
            onBackground = "#001448",
            onSurface = "#FFFFFF",
            onPrimary = "#001448",
            onSecondary = "#FFFFFF",
            onTertiary = "#FFFFFF"
        )
        // debug and any other variant, which is important for the public sample
        else -> ColorStyle(
            background = "#FDFDFD",
            surface = "#1C2575",
            primary = "#FA5455",
            secondary = "#5E5BD0",
            tertiary = "#9C99FC",
            onBackground = "#001448",
            onSurface = "#FFFFFF",
            onPrimary = "#001448",
            onSecondary = "#FFFFFF",
            onTertiary = "#FFFFFF"
        )
    }

    val defaultDarkModeTheme = when (BuildConfig.BUILD_TYPE) {
        "release" -> ColorStyle(
            background = "#001448",
            surface = "#1C2575",
            primary = "#09E35F",
            secondary = "#5E5BD0",
            tertiary = "#9C99FC",
            onBackground = "#FFFFFF",
            onSurface = "#FFFFFF",
            onPrimary = "#001448",
            onSecondary = "#FFFFFF",
            onTertiary = "#FFFFFF"
        )

        "sandbox" -> ColorStyle(
            background = "#001448",
            surface = "#1C2575",
            primary = "#C2B280",
            secondary = "#5E5BD0",
            tertiary = "#9C99FC",
            onBackground = "#FFFFFF",
            onSurface = "#FFFFFF",
            onPrimary = "#001448",
            onSecondary = "#FFFFFF",
            onTertiary = "#FFFFFF"
        )

        "staging" -> ColorStyle(
            background = "#001448",
            surface = "#1C2575",
            primary = "#FDE92B",
            secondary = "#5E5BD0",
            tertiary = "#9C99FC",
            onBackground = "#FFFFFF",
            onSurface = "#FFFFFF",
            onPrimary = "#001448",
            onSecondary = "#FFFFFF",
            onTertiary = "#FFFFFF"
        )
        // debug and any other variant, which is important for the public sample
        else -> ColorStyle(
            background = "#001448",
            surface = "#1C2575",
            primary = "#FA5455",
            secondary = "#5E5BD0",
            tertiary = "#9C99FC",
            onBackground = "#FFFFFF",
            onSurface = "#FFFFFF",
            onPrimary = "#001448",
            onSecondary = "#FFFFFF",
            onTertiary = "#FFFFFF"
        )
    }

    private var activeLightModeTheme: ColorStyle = defaultLightModeTheme
    private var activeDarkModeTheme: ColorStyle = defaultDarkModeTheme

    internal fun String.hexToIntColor(): Int {
        return removePrefix("#").toInt(16) or 0xFF000000.toInt()
    }

    internal fun Int.intToColorHex(): String {
        return String.format("#%06X", (this and 0xFFFFFF))
    }

    internal fun applyTheme(lightMode: ColorStyle, darkMode: ColorStyle = lightMode) {
        activeLightModeTheme = lightMode
        activeDarkModeTheme = darkMode
    }

    internal fun applyDefaultTheme() {
        activeLightModeTheme = defaultLightModeTheme
        activeDarkModeTheme = defaultDarkModeTheme
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
        LIGHT_ON_BACKGROUND(5, {
            updateLightProperty {
                copy(onBackground = it.intToColorHex())
            }
        }),
        LIGHT_ON_SURFACE(6, {
            updateLightProperty {
                copy(onSurface = it.intToColorHex())
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

        DARK_BACKGROUND(10, {
            updateDarkProperty {
                copy(background = it.intToColorHex())
            }
        }),
        DARK_SURFACE(11, {
            updateDarkProperty {
                copy(surface = it.intToColorHex())
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
        DARK_ON_BACKGROUND(15, {
            updateDarkProperty {
                copy(onBackground = it.intToColorHex())
            }
        }),
        DARK_ON_SURFACE(16, {
            updateDarkProperty {
                copy(onSurface = it.intToColorHex())
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
            ColorIdMap.LIGHT_BACKGROUND.id,
            "Background",
            style.background!!,
            style.background!!.hexToIntColor()
        )
        details(
            ColorIdMap.LIGHT_SURFACE.id,
            "Surface",
            style.surface!!,
            style.surface!!.hexToIntColor()
        )
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
            ColorIdMap.LIGHT_ON_BACKGROUND.id,
            "On Background",
            style.onBackground!!,
            style.onBackground!!.hexToIntColor()
        )
        details(
            ColorIdMap.LIGHT_ON_SURFACE.id,
            "On Surface",
            style.onSurface!!,
            style.onSurface!!.hexToIntColor()
        )
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
            ColorIdMap.DARK_BACKGROUND.id,
            "Background",
            style.background!!,
            style.background!!.hexToIntColor()
        )
        details(
            ColorIdMap.DARK_SURFACE.id,
            "Surface",
            style.surface!!,
            style.surface!!.hexToIntColor()
        )
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
            ColorIdMap.DARK_ON_BACKGROUND.id,
            "On Background",
            style.onBackground!!,
            style.onBackground!!.hexToIntColor()
        )
        details(
            ColorIdMap.DARK_ON_SURFACE.id,
            "On Surface",
            style.onSurface!!,
            style.onSurface!!.hexToIntColor()
        )
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
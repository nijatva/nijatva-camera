package com.nijatva.camera.ui

import com.nijatva.camera.settings.AccentChoice
import com.nijatva.camera.settings.ThemeChoice

/**
 * The colours every view draws with, in one place.
 *
 * The chrome is built by hand in several classes (strip, pill, grid, activity), so
 * a single mutable holder is simpler than threading a theme object through every
 * constructor, and it lets an appearance change take effect on the next frame
 * instead of rebuilding the camera screen.
 */
object Palette {

    @Volatile
    var accent: Int = AccentChoice.SAFFRON.argb
        private set

    @Volatile
    var background: Int = 0xFF0B0B0C.toInt()
        private set

    @Volatile
    var surface: Int = 0xFF141416.toInt()
        private set

    @Volatile
    var onSurface: Int = 0xFFFFFFFF.toInt()
        private set

    @Volatile
    var muted: Int = 0xFFA8A8AE.toInt()
        private set

    @Volatile
    var hairline: Int = 0xFF26262A.toInt()
        private set

    @Volatile
    var isLight: Boolean = false
        private set

    /**
     * @param darkSystem true when the system is in dark mode, used by [ThemeChoice.SYSTEM]
     */
    fun apply(theme: ThemeChoice, accentChoice: AccentChoice, darkSystem: Boolean) {
        accent = accentChoice.argb

        val dark = when (theme) {
            ThemeChoice.BLACK, ThemeChoice.DARK -> true
            ThemeChoice.LIGHT -> false
            ThemeChoice.SYSTEM -> darkSystem
        }
        isLight = !dark

        if (dark) {
            // BLACK is the site's pure black; DARK is one step up from it.
            background = if (theme == ThemeChoice.BLACK) 0xFF0B0B0C.toInt() else 0xFF141416.toInt()
            surface = if (theme == ThemeChoice.BLACK) 0xFF141416.toInt() else 0xFF1D1D21.toInt()
            onSurface = 0xFFFFFFFF.toInt()
            muted = 0xFFA8A8AE.toInt()
            hairline = 0xFF26262A.toInt()
        } else {
            background = 0xFFF6F5F2.toInt()
            surface = 0xFFFFFFFF.toInt()
            onSurface = 0xFF121214.toInt()
            muted = 0xFF5B5B63.toInt()
            hairline = 0xFFDCDAD4.toInt()
        }
    }

    /**
     * Text colour that stays legible on the accent: dark on saffron, white on the
     * darker green and indigo.
     *
     * The cut-off is 0.179, which is where black and white have the same contrast
     * ratio against a colour - not 0.5, which would put white on saffron at about
     * 2.9:1, under the 4.5:1 that body text needs.
     */
    fun onAccent(): Int {
        fun channel(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        val luminance = 0.2126 * channel((accent shr 16) and 0xFF) +
            0.7152 * channel((accent shr 8) and 0xFF) +
            0.0722 * channel(accent and 0xFF)
        return if (luminance > 0.179) 0xFF101820.toInt() else 0xFFFFFFFF.toInt()
    }

    /** Accent mixed toward the surface, for pill fills and avatar discs. */
    fun accentSoft(alpha: Int = 40): Int =
        (alpha shl 24) or (accent and 0x00FFFFFF)
}

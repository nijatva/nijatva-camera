package com.nijatva.camera.camera

import com.nijatva.camera.R

/**
 * Which modes an edition shows, and how each one is labelled and drawn.
 *
 * The rule lives here as pure functions rather than inside the activity, so the
 * edition behaviour (Lite has no video, no panorama, no RAW) can be unit tested
 * without a device or a camera session.
 */
object Modes {

    /**
     * The modes on the strip, in order, with MORE appended by the UI.
     *
     * Video and astro are full-edition only: the Lite build has no video surface
     * and no astro stack, so showing the tabs would only produce dead controls.
     */
    fun stripModes(lite: Boolean): List<Mode> = stripModes(lite, emptyList())

    /**
     * The strip with the user's own arrangement applied.
     *
     * Names that are not a mode of this edition are dropped, duplicates collapse, and
     * an empty or unusable arrangement falls back to the default - so a settings file
     * from another edition cannot leave the strip blank.
     */
    fun stripModes(lite: Boolean, custom: List<String>): List<Mode> {
        val available = gridModes(lite).toSet()
        if (custom.isEmpty()) return defaultStrip(lite)
        val chosen = custom.mapNotNull { name ->
            Mode.entries.firstOrNull { it.name == name }
        }.filter { it in available }.distinct()
        return chosen.ifEmpty { defaultStrip(lite) }
    }

    private fun defaultStrip(lite: Boolean): List<Mode> =
        if (lite) {
            listOf(Mode.PHOTO, Mode.NIGHT, Mode.PRO)
        } else {
            listOf(Mode.PHOTO, Mode.VIDEO, Mode.NIGHT, Mode.PRO, Mode.ASTRO)
        }

    /** The strip with one mode added, or moved to the end if it was already there. */
    fun withMode(lite: Boolean, custom: List<String>, mode: Mode): List<String> {
        val current = stripModes(lite, custom).map { it.name }
        return (current - mode.name) + mode.name
    }

    /** Every mode the edition can actually do. The MORE grid lists exactly these. */
    fun gridModes(lite: Boolean): List<Mode> =
        if (lite) {
            listOf(Mode.PHOTO, Mode.NIGHT, Mode.PRO)
        } else {
            listOf(Mode.PHOTO, Mode.VIDEO, Mode.NIGHT, Mode.PRO, Mode.ASTRO, Mode.HDR, Mode.PANORAMA, Mode.RAW)
        }

    fun labelRes(mode: Mode): Int = when (mode) {
        Mode.PHOTO -> R.string.mode_photo
        Mode.VIDEO -> R.string.mode_video
        Mode.NIGHT -> R.string.mode_night
        Mode.PRO -> R.string.mode_pro
        Mode.ASTRO -> R.string.mode_astro
        Mode.HDR -> R.string.mode_hdr
        Mode.PANORAMA -> R.string.mode_panorama
        Mode.RAW -> R.string.mode_raw
    }

    fun iconRes(mode: Mode): Int = when (mode) {
        Mode.PHOTO -> R.drawable.ic_mode_photo
        Mode.VIDEO -> R.drawable.ic_mode_video
        Mode.NIGHT -> R.drawable.ic_mode_night
        Mode.PRO -> R.drawable.ic_mode_pro
        Mode.ASTRO -> R.drawable.ic_mode_astro
        Mode.HDR -> R.drawable.ic_mode_hdr
        Mode.PANORAMA -> R.drawable.ic_mode_pano
        Mode.RAW -> R.drawable.ic_mode_raw
    }
}

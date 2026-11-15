package com.nijatva.camera.camera

/**
 * Decides whether the scene is dark enough for an astro stack.
 *
 * Astro mode fixes a long exposure (2-8 s) and a high ISO, which is right under a
 * night sky and catastrophic in daylight: the frame blows out to white and the
 * stack is wasted. The camera's own auto-exposure already measured the scene, so
 * its answer is used instead of adding a light meter: exposure time multiplied by
 * ISO is a brightness index that does not depend on the metering mode.
 *
 * A sunlit wall lands around 10-100, a lit room around 10-15 thousand, a night
 * street in the hundreds of thousands. The threshold sits between the last two, so
 * astro refuses daylight and indoor light but never refuses a real night sky.
 */
object AstroGate {

    /** exposure (ms) times ISO, the scene brightness as the camera measured it. */
    fun brightnessIndex(exposureNs: Long, iso: Int): Double =
        (exposureNs / 1_000_000.0) * iso.coerceAtLeast(0)

    /** Below this the scene is too bright for a multi-second exposure. */
    const val MIN_INDEX = 50_000.0

    fun isDarkEnough(exposureNs: Long, iso: Int): Boolean =
        brightnessIndex(exposureNs, iso) >= MIN_INDEX

    /**
     * True only when there is a real measurement and it says the scene is bright.
     * Before the first capture result there is nothing to judge, and a gate that
     * refuses on missing data would be a gate the user cannot get past.
     */
    fun blocksCapture(exposureNs: Long, iso: Int): Boolean =
        exposureNs > 0L && iso > 0 && !isDarkEnough(exposureNs, iso)
}

package com.nijatva.camera.camera

/**
 * Decides whether a scene needs less exposure before the shutter is pressed.
 *
 * A backlit scene is the case this exists for: the sky fills a corner with pure
 * white while the subject sits in shadow, and no amount of post-processing brings
 * that white back, because the sensor recorded the same value for the sky and for
 * a white wall. The only fix is to expose for the bright part.
 *
 * The decision is a pure function of two fractions - how much of the frame is at
 * the top of the range, and how much is at the bottom - so it can be tested
 * without a camera and reasoned about without a histogram on screen.
 */
object ExposureGuard {

    /** Pixels at or above this luma are treated as clipped or about to clip. */
    const val CLIP_LUMA = 250

    /** Pixels at or below this luma are treated as black. */
    const val FLOOR_LUMA = 6

    /** Above this share of clipped pixels, a scene is losing highlights. */
    const val CLIP_WARN = 0.02f

    /** Above this share, the frame is mostly sky and needs a real pull. */
    const val CLIP_HEAVY = 0.08f

    /**
     * Exposure compensation to apply, in EV.
     *
     * Deliberately conservative: half a stop fixes most backlit scenes without
     * making the subject muddy, and a full stop is only asked for when a tenth of
     * the frame is already gone. Shadow-only scenes get a lift, but a smaller one,
     * because lifting shadows amplifies noise.
     */
    fun biasFor(clippedFraction: Float, shadowFraction: Float): Float = when {
        clippedFraction >= CLIP_HEAVY -> -1.0f
        clippedFraction >= CLIP_WARN -> -0.5f
        // Nothing is clipping and most of the frame is dark: the meter is reading
        // the bright part, so open up a little.
        shadowFraction >= 0.55f && clippedFraction < 0.005f -> 0.3f
        else -> 0f
    }

    /** Counts the two fractions the decision needs, in one pass over luma. */
    fun measure(luma: IntArray): Pair<Float, Float> {
        if (luma.isEmpty()) return 0f to 0f
        var clipped = 0
        var shadow = 0
        for (v in luma) {
            if (v >= CLIP_LUMA) clipped++
            if (v <= FLOOR_LUMA) shadow++
        }
        val n = luma.size.toFloat()
        return (clipped / n) to (shadow / n)
    }

    /**
     * Clipped share of the frame, read from a 256-bin luma histogram.
     *
     * The analysis stream already builds this histogram for the overlay, so the
     * decision to merge a burst costs nothing extra.
     */
    fun clippedFraction(histogram: IntArray): Float {
        if (histogram.size < 256) return 0f
        var total = 0L
        var clipped = 0L
        for (bin in 0 until 256) {
            val count = histogram[bin]
            total += count
            if (bin >= CLIP_LUMA) clipped += count
        }
        if (total == 0L) return 0f
        return clipped.toFloat() / total
    }

    /**
     * Whether a plain photo should be taken as a merged burst instead.
     *
     * All four have to hold: the user left Auto HDR on, the scene is losing
     * highlights, the camera can actually hold a manual exposure for the merge, and
     * a YUV stream exists to merge from. Any one of them failing means an ordinary
     * JPEG, which is never worse than a burst that cannot be taken.
     */
    fun shouldMergeBurst(
        enabled: Boolean,
        clippedFraction: Float,
        supportsManualControls: Boolean,
        hasYuvStream: Boolean,
    ): Boolean = enabled &&
        clippedFraction >= CLIP_WARN &&
        supportsManualControls &&
        hasYuvStream

    /**
     * A short line for the viewfinder, or null when the exposure is fine. Only
     * speaks up when there is something the user can act on.
     */
    fun hintFor(clippedFraction: Float): String? = when {
        clippedFraction >= CLIP_HEAVY -> "Highlights clipping. Swipe down for -1 EV"
        clippedFraction >= CLIP_WARN -> "Sky clipping. Swipe down for -0.5 EV"
        else -> null
    }
}

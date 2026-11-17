package com.nijatva.camera.ui

import kotlin.math.abs

/**
 * Which zoom steps the pill offers, and which one a pinch is nearest to.
 *
 * Kept as pure functions because the pill's label is driven from here: the bug this
 * replaces printed "2x" for every step above 1x, so a 3x step was labelled 2x.
 */
object ZoomStops {

    /**
     * The stops for a camera that can zoom to [maxZoom].
     *
     * 1x always, then 2x, 3x and 5x when the sensor can actually reach them - a stop
     * the lens cannot reach is a button that does nothing. 0.5x is not offered: the
     * ultra-wide is a separate camera, not a zoom factor of this one.
     */
    fun stops(maxZoom: Float): List<Float> {
        val limit = maxZoom.coerceAtLeast(1f)
        val out = mutableListOf(1f)
        listOf(2f, 3f, 5f).forEach { step ->
            // A stop the sensor can reach is offered, even at the very limit; a stop
            // past it would be a button that does nothing.
            if (step <= limit + 1e-3f) out.add(step)
        }
        return out
    }

    /** The stop nearest a live ratio, or the closest available when none is exact. */
    fun nearest(stops: List<Float>, ratio: Float): Float {
        if (stops.isEmpty()) return 1f
        return stops.minByOrNull { abs(it - ratio) } ?: 1f
    }

    /** "1x", "2x", "3x", "2.4x": a label that always tells the truth. */
    fun label(ratio: Float): String {
        val rounded = (ratio * 10f).toInt() / 10f
        return if (abs(rounded - rounded.toInt()) < 0.05f) {
            "${rounded.toInt()}x"
        } else {
            "${rounded}x"
        }
    }
}

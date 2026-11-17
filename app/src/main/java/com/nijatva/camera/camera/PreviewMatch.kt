package com.nijatva.camera.camera

import kotlin.math.max
import kotlin.math.min

/**
 * Keeps the saved photo looking like the preview did.
 *
 * Two different things make a phone photo disagree with its own viewfinder:
 *
 *  1. The preview has converged its auto exposure and white balance over many
 *     frames, and a still capture re-runs both from scratch. A few frames of
 *     convergence is enough to shift brightness or colour between what the user saw
 *     and what they got. Locking both for the capture keeps the converged values.
 *  2. The viewfinder fills the screen and crops the stream, while the photo keeps the
 *     whole sensor frame - so the photo contains area the preview never showed. This
 *     computes that crop, so the saved frame can be cut to what was actually seen.
 *
 * Both are pure functions, so the arithmetic is testable without a camera.
 */
object PreviewMatch {

    /**
     * Whether to lock AE and AWB for a still capture.
     *
     * Not when the user set ISO or exposure by hand: those already run with AE off,
     * and locking a manual exposure would freeze the wrong thing. Not before the
     * preview has converged either - a lock on an unconverged reading is worse than
     * letting the capture converge on its own.
     */
    fun shouldLock(
        enabled: Boolean,
        hasManualIso: Boolean,
        hasManualExposure: Boolean,
        converged: Boolean,
    ): Boolean = enabled && !hasManualIso && !hasManualExposure && converged

    /**
     * The part of a [frameWidth] x [frameHeight] capture that a cover-cropped preview
     * of [viewWidth] x [viewHeight] was actually showing.
     *
     * Returns [left, top, right, bottom] in frame pixels. A view that is wider than
     * the frame keeps full height and loses the sides; a taller one keeps full width
     * and loses the top and bottom. Identical aspects return the whole frame.
     */
    fun visibleCrop(frameWidth: Int, frameHeight: Int, viewWidth: Int, viewHeight: Int): IntArray {
        if (frameWidth <= 0 || frameHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return intArrayOf(0, 0, max(0, frameWidth), max(0, frameHeight))
        }
        val frameAspect = frameWidth.toFloat() / frameHeight
        val viewAspect = viewWidth.toFloat() / viewHeight
        return if (viewAspect > frameAspect) {
            // The view is wider: the preview filled the width and cropped the height.
            val kept = (frameWidth / viewAspect).toInt().coerceIn(1, frameHeight)
            val top = (frameHeight - kept) / 2
            intArrayOf(0, top, frameWidth, top + kept)
        } else if (viewAspect < frameAspect) {
            // The view is taller: the preview filled the height and cropped the width.
            val kept = (frameHeight * viewAspect).toInt().coerceIn(1, frameWidth)
            val left = (frameWidth - kept) / 2
            intArrayOf(left, 0, left + kept, frameHeight)
        } else {
            intArrayOf(0, 0, frameWidth, frameHeight)
        }
    }

    /** True when the crop would actually remove something. */
    fun cropIsMeaningful(frameWidth: Int, frameHeight: Int, crop: IntArray): Boolean {
        if (crop.size < 4) return false
        val kept = (crop[2] - crop[0]).toLong() * (crop[3] - crop[1])
        val whole = frameWidth.toLong() * frameHeight
        return whole > 0 && kept < whole * 0.98
    }

    /** Shrinks the crop a little, so a pixel of rounding never shows an edge. */
    fun inset(crop: IntArray, pixels: Int): IntArray {
        if (crop.size < 4) return crop
        val l = min(crop[2] - 1, crop[0] + pixels)
        val t = min(crop[3] - 1, crop[1] + pixels)
        val r = max(l + 1, crop[2] - pixels)
        val b = max(t + 1, crop[3] - pixels)
        return intArrayOf(l, t, r, b)
    }
}

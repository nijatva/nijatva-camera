package com.nijatva.camera.processor

import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult

/**
 * Turns the sensor's own noise profile into the model the pipeline uses.
 *
 * Camera2 reports, per capture, a slope and an offset per colour channel
 * (`SENSOR_NOISE_PROFILE`). In normalised units the variance of a pixel is
 *
 *     var(n) = S * n + O
 *
 * where n is the pixel value divided by the white level. The native pipeline works
 * in raw DN, where dn = n * white, so
 *
 *     var(dn) = white^2 * (S * dn / white + O) = (S * white) * dn + (O * white^2)
 *
 * which is the `A * dn + B` the merge expects. Getting this right matters: the
 * merge weights are 1/variance, so a placeholder model either over-smooths or
 * leaves grain, and the ghost threshold is derived from the same numbers.
 *
 * The profile is only present from API 24 and only on devices that publish it, so
 * every entry point returns null rather than a guess.
 */
object NoiseModel {

    /** Slope and offset in raw DN, matching `FrameMeta.noiseA` / `noiseB`. */
    data class Ab(val a: Float, val b: Float)

    /**
     * Pure conversion, so the arithmetic can be tested without a camera.
     *
     * @param channels one (S, O) pair per reported channel
     * @param whiteLevel full-scale raw value, e.g. 1023 for RAW10
     */
    fun fromChannels(channels: List<Pair<Double, Double>>, whiteLevel: Float): Ab? {
        if (channels.isEmpty() || whiteLevel <= 0f) return null
        val usable = channels.filter { (s, o) ->
            s.isFinite() && o.isFinite() && s >= 0.0 && o >= 0.0
        }
        if (usable.isEmpty()) return null

        // One model for all channels: the merge runs on a Bayer mosaic where the
        // channels interleave, so a per-channel model would only add bookkeeping.
        val meanS = usable.sumOf { it.first } / usable.size
        val meanO = usable.sumOf { it.second } / usable.size
        val a = (meanS * whiteLevel).toFloat()
        val b = (meanO * whiteLevel.toDouble() * whiteLevel).toFloat()
        if (!a.isFinite() || !b.isFinite()) return null
        return Ab(a, b)
    }

    /** Reads the profile from the capture that produced the frame. */
    fun fromResult(result: TotalCaptureResult, whiteLevel: Float): Ab? {
        val profile = result.get(CaptureResult.SENSOR_NOISE_PROFILE) ?: return null
        // Camera2 publishes this as an array of android.util.Pair, one (slope,
        // offset) per colour channel, not as a typed profile object.
        val channels = ArrayList<Pair<Double, Double>>(profile.size)
        for (entry in profile) {
            val s = entry?.first ?: continue
            val o = entry.second ?: continue
            if (s.isFinite() && o.isFinite()) channels.add(s to o)
        }
        return fromChannels(channels, whiteLevel)
    }
}

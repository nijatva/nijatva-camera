package com.nijatva.camera.grade

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Background blur for portrait shots.
 *
 * Two things, kept separate on purpose:
 *
 *  - [radialMask] builds a mask without any model or library: sharp in the middle,
 *    blurred at the edges. It is what a phone does when it has no depth sensor, and
 *    it is honest about being an effect rather than a measurement.
 *  - [apply] takes any mask and does the blurring. The mask is a plain float array,
 *    so when a real depth map arrives from the camera - `DEPTH16` or dynamic depth
 *    on a device that publishes one - it plugs in here with no other change.
 *
 * The blur is a masked blend of the original and a blurred copy built by working at a
 * quarter resolution. Working small is what makes this cheap enough for a 12 MP frame,
 * and it also hides that a box blur is not a lens: at a quarter resolution the
 * difference between a box and a disc disappears into the sampling.
 */
object PortraitBlur {

    private const val DOWN = 4

    /**
     * A radial mask: 0 at the centre, 1 at the corners, with a soft band between.
     *
     * @param focus the share of the shorter side that stays sharp, 0..1
     * @param feather width of the transition band, as a share of the shorter side
     */
    fun radialMask(width: Int, height: Int, focus: Float = 0.42f, feather: Float = 0.30f): FloatArray {
        val mask = FloatArray(max(0, width * height))
        if (width <= 0 || height <= 0) return mask
        val cx = width / 2f
        val cy = height / 2f
        val sharp = (min(width, height) * focus).coerceAtLeast(1f)
        val band = (min(width, height) * feather).coerceAtLeast(1f)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val d = hypot(x - cx, y - cy)
                val t = ((d - sharp) / band).coerceIn(0f, 1f)
                // Smoothstep, so the transition has no visible ring.
                mask[y * width + x] = t * t * (3f - 2f * t)
            }
        }
        return mask
    }

    /**
     * Blends a blurred copy of the frame into the original wherever the mask says so.
     *
     * @param mask one value per pixel: 0 keeps the pixel sharp, 1 blurs it fully
     * @param strength 0..1 scales the whole effect
     */
    fun apply(px: IntArray, width: Int, height: Int, mask: FloatArray, strength: Float = 1f): IntArray {
        if (width <= 2 || height <= 2 || mask.size < width * height) return px
        val amount = strength.coerceIn(0f, 1f)
        if (amount == 0f) return px

        val sw = (width + DOWN - 1) / DOWN
        val sh = (height + DOWN - 1) / DOWN
        val smallR = FloatArray(sw * sh)
        val smallG = FloatArray(sw * sh)
        val smallB = FloatArray(sw * sh)

        for (sy in 0 until sh) {
            for (sx in 0 until sw) {
                var r = 0f
                var g = 0f
                var b = 0f
                var count = 0
                for (y in sy * DOWN until min(height, sy * DOWN + DOWN)) {
                    for (x in sx * DOWN until min(width, sx * DOWN + DOWN)) {
                        val p = px[y * width + x]
                        r += ((p shr 16) and 0xFF).toFloat()
                        g += ((p shr 8) and 0xFF).toFloat()
                        b += (p and 0xFF).toFloat()
                        count++
                    }
                }
                val i = sy * sw + sx
                if (count > 0) {
                    smallR[i] = r / count
                    smallG[i] = g / count
                    smallB[i] = b / count
                }
            }
        }

        // Radius grows with the resolution, so the look is the same on every phone.
        val radius = max(1, (min(sw, sh) / 60f).toInt() + 1)
        val blurR = boxBlur(smallR, sw, sh, radius)
        val blurG = boxBlur(smallG, sw, sh, radius)
        val blurB = boxBlur(smallB, sw, sh, radius)

        // In place, like ColorGrade.apply: a caller that ignores the return value must
        // still get the blurred frame, not the original.
        for (y in 0 until height) {
            val sy = min(sh - 1, y / DOWN)
            for (x in 0 until width) {
                val m = mask[y * width + x].coerceIn(0f, 1f) * amount
                if (m <= 0.001f) continue
                val sx = min(sw - 1, x / DOWN)
                val i = sy * sw + sx
                val p = px[y * width + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val nr = (r + (blurR[i] - r) * m).toInt().coerceIn(0, 255)
                val ng = (g + (blurG[i] - g) * m).toInt().coerceIn(0, 255)
                val nb = (b + (blurB[i] - b) * m).toInt().coerceIn(0, 255)
                px[y * width + x] = (p and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        return px
    }

    /** Separable box blur with edge clamping, on a float plane. */
    private fun boxBlur(src: FloatArray, w: Int, h: Int, r: Int): FloatArray {
        if (r <= 0) return src.copyOf()
        val d = 2 * r + 1
        val tmp = FloatArray(src.size)
        val out = FloatArray(src.size)
        for (y in 0 until h) {
            val row = y * w
            var acc = 0f
            for (k in -r..r) acc += src[row + k.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                tmp[row + x] = acc / d
                acc += src[row + (x + r + 1).coerceAtMost(w - 1)] - src[row + (x - r).coerceAtLeast(0)]
            }
        }
        for (x in 0 until w) {
            var acc = 0f
            for (k in -r..r) acc += tmp[k.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                out[y * w + x] = acc / d
                acc += tmp[(y + r + 1).coerceAtMost(h - 1) * w + x] - tmp[(y - r).coerceAtLeast(0) * w + x]
            }
        }
        return out
    }
}

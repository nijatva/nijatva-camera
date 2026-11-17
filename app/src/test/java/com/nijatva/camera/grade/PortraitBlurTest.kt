package com.nijatva.camera.grade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The portrait blur: the centre stays sharp, the edges lose detail, and the mask
 * decides which is which - so a real depth map can replace the radial one later.
 */
class PortraitBlurTest {

    private val w = 64
    private val h = 64

    /** A checkerboard: its detail is what a blur should destroy. */
    private fun checker(): IntArray = IntArray(w * h) { i ->
        val x = i % w
        val y = i / w
        val v = if ((x + y) % 2 == 0) 230 else 25
        (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }

    private fun variance(px: IntArray, from: Int, to: Int): Double {
        val values = (from until to).map { ((px[it] shr 16) and 0xFF).toDouble() }
        val mean = values.average()
        return values.sumOf { (it - mean) * (it - mean) } / values.size
    }

    @Test
    fun theCentreIsLeftAlone() {
        val mask = FloatArray(w * h) { 0f }
        val px = checker()
        val before = px.copyOf()
        PortraitBlur.apply(px, w, h, mask, 1f)
        assertEquals(before.toList(), px.toList())
    }

    @Test
    fun aFullMaskRemovesDetail() {
        val mask = FloatArray(w * h) { 1f }
        val px = checker()
        val before = variance(px, 0, w * h)
        PortraitBlur.apply(px, w, h, mask, 1f)
        val after = variance(px, 0, w * h)
        assertTrue("detail must fall: $before -> $after", after < before * 0.6)
    }

    @Test
    fun theRadialMaskIsSharpInTheMiddleAndBlurredAtTheCorners() {
        val mask = PortraitBlur.radialMask(w, h)
        val centre = mask[(h / 2) * w + (w / 2)]
        val corner = mask[0]
        assertEquals("centre must be sharp", 0f, centre, 1e-6f)
        assertTrue("corner must be blurred: $corner", corner > 0.9f)
    }

    @Test
    fun theRadialMaskRisesMonotonicallyOutwards() {
        val mask = PortraitBlur.radialMask(w, h)
        val row = h / 2
        var previous = -1f
        for (x in w / 2 until w) {
            val v = mask[row * w + x]
            assertTrue("mask must not fall going outwards: $previous -> $v", v >= previous - 1e-6f)
            previous = v
        }
    }

    @Test
    fun aRealDepthMapDrivesItJustAsWell() {
        // Left half near (sharp), right half far (blurred): a stand-in for a depth map.
        val mask = FloatArray(w * h) { i -> if ((i % w) < w / 2) 0f else 1f }
        val px = checker()
        val leftBefore = variance(px, 0, w / 2)
        val rightBefore = variance(px, w * h - w / 2, w * h)
        PortraitBlur.apply(px, w, h, mask, 1f)
        val leftAfter = variance(px, 0, w / 2)
        val rightAfter = variance(px, w * h - w / 2, w * h)
        assertTrue("the near half must stay sharp: $leftBefore -> $leftAfter", leftAfter >= leftBefore * 0.9)
        assertTrue("the far half must blur: $rightBefore -> $rightAfter", rightAfter < rightBefore * 0.6)
    }

    @Test
    fun strengthScalesTheEffect() {
        val mask = FloatArray(w * h) { 1f }
        val full = checker().also { PortraitBlur.apply(it, w, h, mask, 1f) }
        val half = checker().also { PortraitBlur.apply(it, w, h, mask, 0.5f) }
        assertTrue("half strength must keep more detail", variance(half, 0, w * h) > variance(full, 0, w * h))
    }

    @Test
    fun degenerateInputIsRefusedRatherThanCrashing() {
        val px = checker()
        val copy = px.copyOf()
        PortraitBlur.apply(px, 2, 2, FloatArray(4), 1f)
        PortraitBlur.apply(px, w, h, FloatArray(3), 1f) // mask too small
        assertEquals(copy.toList(), px.toList())
        assertTrue(PortraitBlur.radialMask(0, 0).isEmpty())
    }

    @Test
    fun blurKeepsAlphaAndColourBalance() {
        val mask = FloatArray(w * h) { 1f }
        val px = checker()
        PortraitBlur.apply(px, w, h, mask, 1f)
        assertTrue("every pixel must stay opaque", px.all { (it shr 24) and 0xFF == 0xFF })
        // A grey checker must stay grey: the blur must not tint it.
        val first = px[0]
        val r = (first shr 16) and 0xFF
        val g = (first shr 8) and 0xFF
        val b = first and 0xFF
        assertTrue("grey must stay grey", abs(r - g) <= 1 && abs(g - b) <= 1)
    }
}

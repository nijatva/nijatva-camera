package com.nijatva.camera.grade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The stages added for clean output: chroma denoise must cut colour noise without
 * touching luma, sharpening must add edge bite without amplifying grain, and the
 * warmth shift must move colour without moving brightness.
 */
class ColorGradeStagesTest {

    private fun grey(value: Int, w: Int, h: Int) =
        IntArray(w * h) { (0xFF shl 24) or (value shl 16) or (value shl 8) or value }

    private fun meanLuma(px: IntArray): Double = px.sumOf {
        0.299 * ((it shr 16) and 0xFF) + 0.587 * ((it shr 8) and 0xFF) + 0.114 * (it and 0xFF)
    } / px.size

    @Test
    fun warmthShiftsColourNotBrightness() {
        val px = grey(128, 8, 8)
        val before = meanLuma(px)
        ColorGrade.warmthShift(px, 0.05f)
        val r = (px[0] shr 16) and 0xFF
        val b = px[0] and 0xFF
        assertTrue("red must rise: $r", r > 128)
        assertTrue("blue must fall: $b", b < 128)
        // A warm shift moves colour, not exposure: the frame must not get brighter.
        assertTrue("brightness must stay put: $before -> ${meanLuma(px)}", abs(meanLuma(px) - before) < 1.0)
    }

    @Test
    fun chromaDenoiseCutsColourNoiseAndKeepsLuma() {
        val w = 16
        val h = 16
        // Grey frame with alternating magenta/green colour noise, luma held constant.
        val px = IntArray(w * h) { i ->
            val magenta = (i % 2 == 0)
            val r = if (magenta) 140 else 116
            val g = if (magenta) 116 else 140
            (0xFF shl 24) or (r shl 16) or (g shl 8) or 128
        }
        val lumaBefore = px.map { 0.299 * ((it shr 16) and 0xFF) + 0.587 * ((it shr 8) and 0xFF) + 0.114 * (it and 0xFF) }
        val spreadBefore = (px.maxOf { (it shr 16) and 0xFF } - px.minOf { (it shr 16) and 0xFF })

        ColorGrade.chromaDenoise(px, w, h, 1f)

        val spreadAfter = (px.maxOf { (it shr 16) and 0xFF } - px.minOf { (it shr 16) and 0xFF })
        assertTrue("colour spread must shrink: $spreadBefore -> $spreadAfter", spreadAfter < spreadBefore)

        val lumaAfter = px.map { 0.299 * ((it shr 16) and 0xFF) + 0.587 * ((it shr 8) and 0xFF) + 0.114 * (it and 0xFF) }
        val maxLumaDrift = lumaBefore.indices.maxOf { abs(lumaAfter[it] - lumaBefore[it]) }
        assertTrue("luma must survive: drift $maxLumaDrift", maxLumaDrift < 2.0)
    }

    @Test
    fun sharpeningLeavesFlatAreasAlone() {
        val px = grey(120, 16, 16)
        val copy = px.copyOf()
        ColorGrade.sharpenLuma(px, 16, 16, 0.5f)
        // A flat frame has no detail above the threshold, so nothing should move.
        assertEquals(copy.toList(), px.toList())
    }

    @Test
    fun sharpeningIncreasesEdgeContrast() {
        val w = 16
        val h = 4
        val px = IntArray(w * h) { i ->
            val x = i % w
            val v = if (x < 8) 90 else 160
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        // The step runs from dark (left) to bright (right), so the signed difference
        // is negative; contrast is its magnitude.
        val edgeBefore = kotlin.math.abs(((px[7] shr 16) and 0xFF) - ((px[8] shr 16) and 0xFF))
        ColorGrade.sharpenLuma(px, w, h, 0.6f)
        val edgeAfter = kotlin.math.abs(((px[7] shr 16) and 0xFF) - ((px[8] shr 16) and 0xFF))
        assertTrue("edge must gain contrast: $edgeBefore -> $edgeAfter", edgeAfter > edgeBefore)
    }

    @Test
    fun aFullGradeOfGreyStaysGrey() {
        val px = grey(140, 12, 12)
        ColorGrade.apply(px, 12, 12, ColorGrade.Look.of(LookPreset.NATURAL))
        val r = (px[0] shr 16) and 0xFF
        val g = (px[0] shr 8) and 0xFF
        val b = px[0] and 0xFF
        assertTrue("a neutral frame must stay neutral: $r $g $b", abs(r - g) <= 2 && abs(g - b) <= 2)
    }
}

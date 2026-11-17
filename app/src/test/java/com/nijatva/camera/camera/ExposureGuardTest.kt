package com.nijatva.camera.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The exposure guard: when to pull the exposure, and when to keep quiet.
 *
 * The numbers come from a real backlit frame - a marigold garland against a white
 * sky - where 4.8% of pixels were at pure white and the sky was unrecoverable.
 */
class ExposureGuardTest {

    @Test
    fun aBacklitSkyAsksForHalfAStop() {
        assertEquals(-0.5f, ExposureGuard.biasFor(clippedFraction = 0.05f, shadowFraction = 0.01f), 1e-6f)
    }

    @Test
    fun aMostlyBlownFrameAsksForAFullStop() {
        assertEquals(-1.0f, ExposureGuard.biasFor(clippedFraction = 0.12f, shadowFraction = 0.0f), 1e-6f)
    }

    @Test
    fun aNormalFrameIsLeftAlone() {
        assertEquals(0f, ExposureGuard.biasFor(clippedFraction = 0.005f, shadowFraction = 0.10f), 1e-6f)
    }

    @Test
    fun aDarkFrameOpensUp() {
        assertEquals(0.3f, ExposureGuard.biasFor(clippedFraction = 0.0f, shadowFraction = 0.7f), 1e-6f)
    }

    @Test
    fun aDarkFrameWithClippingIsNotOpenedUp() {
        // Sun in frame against a dark room: pulling would make the sun worse, and
        // opening up would clip more, so nothing is done.
        assertEquals(-0.5f, ExposureGuard.biasFor(clippedFraction = 0.03f, shadowFraction = 0.7f), 1e-6f)
    }

    @Test
    fun measuringCountsBothEnds() {
        val luma = IntArray(100) { i ->
            when {
                i < 5 -> 255     // clipped
                i < 45 -> 100    // midtone
                else -> 3        // black
            }
        }
        val (clipped, shadow) = ExposureGuard.measure(luma)
        assertEquals(0.05f, clipped, 1e-6f)
        assertEquals(0.55f, shadow, 1e-6f)
    }

    @Test
    fun anEmptyFrameIsNeutral() {
        val (clipped, shadow) = ExposureGuard.measure(IntArray(0))
        assertEquals(0f, clipped, 1e-6f)
        assertEquals(0f, shadow, 1e-6f)
    }

    @Test
    fun aHistogramReportsTheClippedShare() {
        // 1000 pixels, 50 of them at 255 and 20 at 250: 7% clipped.
        val hist = IntArray(256) { 0 }
        hist[255] = 50
        hist[250] = 20
        hist[128] = 930
        assertEquals(0.07f, ExposureGuard.clippedFraction(hist), 1e-4f)
    }

    @Test
    fun anEmptyOrShortHistogramReportsNothing() {
        assertEquals(0f, ExposureGuard.clippedFraction(IntArray(256)), 1e-6f)
        assertEquals(0f, ExposureGuard.clippedFraction(IntArray(10)), 1e-6f)
    }

    @Test
    fun autoHdrNeedsAllFourFacts() {
        // Every condition, one at a time, must be able to veto the burst.
        assertTrue(ExposureGuard.shouldMergeBurst(true, 0.05f, true, true))
        assertFalse("user turned it off", ExposureGuard.shouldMergeBurst(false, 0.05f, true, true))
        assertFalse("nothing clipping", ExposureGuard.shouldMergeBurst(true, 0.001f, true, true))
        assertFalse("no manual control", ExposureGuard.shouldMergeBurst(true, 0.05f, false, true))
        assertFalse("no YUV stream", ExposureGuard.shouldMergeBurst(true, 0.05f, true, false))
    }

    @Test
    fun theHintOnlySpeaksUpWhenThereIsSomethingToDo() {
        assertNull(ExposureGuard.hintFor(0.001f))
        assertNotNull(ExposureGuard.hintFor(0.05f))
        assertNotNull(ExposureGuard.hintFor(0.2f))
    }
}

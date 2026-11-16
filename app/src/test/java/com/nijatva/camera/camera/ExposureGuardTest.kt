package com.nijatva.camera.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun theHintOnlySpeaksUpWhenThereIsSomethingToDo() {
        assertNull(ExposureGuard.hintFor(0.001f))
        assertNotNull(ExposureGuard.hintFor(0.05f))
        assertNotNull(ExposureGuard.hintFor(0.2f))
    }
}

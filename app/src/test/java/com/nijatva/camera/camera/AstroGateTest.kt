package com.nijatva.camera.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The astro brightness gate: daylight and indoor light are refused, real night
 * scenes are not.
 */
class AstroGateTest {

    private fun ns(millis: Double): Long = (millis * 1_000_000.0).toLong()

    @Test
    fun sunlitSceneIsRefused() {
        // 1/2000 s at ISO 50, roughly a bright afternoon.
        assertFalse(AstroGate.isDarkEnough(ns(0.5), 50))
    }

    @Test
    fun indoorLightIsRefused() {
        // 1/30 s at ISO 400: a lit room.
        assertFalse(AstroGate.isDarkEnough(ns(33.0), 400))
    }

    @Test
    fun nightStreetIsAllowed() {
        // 1/8 s at ISO 1600.
        assertTrue(AstroGate.isDarkEnough(ns(125.0), 1600))
    }

    @Test
    fun aVeryDarkSkyIsAllowed() {
        assertTrue(AstroGate.isDarkEnough(ns(500.0), 3200))
    }

    @Test
    fun indexIsExposureTimesIso() {
        assertEquals(2000.0, AstroGate.brightnessIndex(ns(1.0), 2000), 0.001)
        assertEquals(0.0, AstroGate.brightnessIndex(0L, 3200), 0.001)
    }

    @Test
    fun aMissingMeasurementNeverBlocks() {
        // Before the first capture result there is no measurement; refusing then
        // would be a gate the user cannot get past.
        assertFalse(AstroGate.blocksCapture(0L, 0))
        assertFalse(AstroGate.blocksCapture(ns(0.5), 0))
    }

    @Test
    fun aBrightMeasurementBlocks() {
        assertTrue(AstroGate.blocksCapture(ns(0.5), 50))
        assertFalse(AstroGate.blocksCapture(ns(125.0), 1600))
    }
}

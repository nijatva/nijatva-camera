package com.nijatva.camera.grade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The four sliders: each one moves what it says it moves, nothing at zero, and the
 * ends of the travel still produce a usable photograph.
 */
class LookTuningTest {

    private val look = ColorGrade.Look.of(LookPreset.NATURAL)

    @Test
    fun neutralChangesNothing() {
        val tuning = LookTuning()
        assertTrue(tuning.isNeutral)
        assertEquals(look, tuning.applyTo(look))
    }

    @Test
    fun brightnessLiftsTheFrame() {
        val up = LookTuning(brightness = 0.5f).applyTo(look)
        val down = LookTuning(brightness = -0.5f).applyTo(look)
        assertTrue(up.brightness > look.brightness)
        assertTrue(down.brightness < look.brightness)
    }

    @Test
    fun contrastSeparatesTheMidtones() {
        val more = LookTuning(contrast = 0.5f).applyTo(look)
        assertTrue(more.contrast > look.contrast)
        // The curve really does change: more contrast means a darker quarter tone
        // and a brighter three-quarter tone, with black and white pinned.
        val flat = ColorGrade.toneCurve(0.25f, look.shoulder, look.contrast)
        val punchy = ColorGrade.toneCurve(0.25f, look.shoulder, more.contrast)
        assertTrue("quarter tone must darken: $flat -> $punchy", punchy < flat)
        assertEquals(0f, ColorGrade.toneCurve(0f, look.shoulder, more.contrast), 1e-6f)
        // White is pinned by the shoulder, not by the S-curve: it rolls off to just
        // under 1 on purpose, so nothing ever hard-clips.
        val white = ColorGrade.toneCurve(1f, look.shoulder, more.contrast)
        assertTrue("white must stay at the top: $white", white in 0.95f..1f)
        assertTrue("white must not clip flat", white < 1f)
    }

    @Test
    fun saturationScalesBothWays() {
        assertTrue(LookTuning(saturation = 0.5f).applyTo(look).saturation > look.saturation)
        assertTrue(LookTuning(saturation = -0.5f).applyTo(look).saturation < look.saturation)
    }

    @Test
    fun sharpnessScalesBothWays() {
        assertTrue(LookTuning(sharpness = 0.5f).applyTo(look).sharpen > look.sharpen)
        assertTrue(LookTuning(sharpness = -0.5f).applyTo(look).sharpen < look.sharpen)
    }

    @Test
    fun theEndsOfTheTravelStayUsable() {
        val extremeUp = LookTuning(1f, 1f, 1f, 1f).applyTo(look)
        val extremeDown = LookTuning(-1f, -1f, -1f, -1f).applyTo(look)
        assertTrue("saturation must stay in range", extremeUp.saturation in 0.5f..2f)
        assertTrue("sharpening must stay in range", extremeUp.sharpen in 0f..1.5f)
        assertTrue("saturation must not vanish", extremeDown.saturation >= 0.5f)
        assertTrue("sharpening must not vanish", extremeDown.sharpen >= 0f)
        assertTrue("brightness must stay in range", extremeUp.brightness in -1f..1f)
    }

    @Test
    fun tuningStacksOnTopOfAnyPreset() {
        LookPreset.entries.forEach { preset ->
            val base = ColorGrade.Look.of(preset)
            val brighter = LookTuning(brightness = 0.3f).applyTo(base)
            assertTrue("${preset.name}: brightness must add", brighter.brightness > base.brightness)
            assertEquals("${preset.name}: saturation untouched", base.saturation, brighter.saturation, 1e-6f)
        }
    }

    @Test
    fun outOfRangeValuesAreClamped() {
        val wild = LookTuning(brightness = 5f, contrast = -9f, saturation = 4f, sharpness = -4f).applyTo(look)
        assertTrue(wild.brightness in -1f..1f)
        assertTrue(wild.contrast in -1f..1f)
        assertTrue(wild.saturation in 0.5f..2f)
        assertTrue(wild.sharpen in 0f..1.5f)
    }
}

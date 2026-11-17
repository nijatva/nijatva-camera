package com.nijatva.camera.grade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The looks are taste, but their relationships are not: a vivid look must be more
 * saturated than a neutral one, a soft look must lift shadows, and the mapping from
 * preset to Look must be exact. Those are the parts a regression could silently
 * break.
 */
class LookPresetTest {

    @Test
    fun everyPresetMapsToItsOwnLook() {
        LookPreset.entries.forEach { preset ->
            val look = ColorGrade.Look.of(preset)
            assertEquals(preset.clarity, look.clarity, 1e-6f)
            assertEquals(preset.saturation, look.saturation, 1e-6f)
            assertEquals(preset.warmth, look.warmth, 1e-6f)
            assertEquals(preset.shadowLift, look.shadowLift, 1e-6f)
            assertEquals(preset.sharpen, look.sharpen, 1e-6f)
            assertEquals(preset.chromaNr, look.chromaNr, 1e-6f)
            assertEquals(preset.chromaCeiling, look.chromaCeiling, 1e-6f)
        }
    }

    @Test
    fun punchIsTheMostSaturatedAndHasTheWidestColourRange() {
        val punch = ColorGrade.Look.of(LookPreset.PUNCH)
        LookPreset.entries.filter { it != LookPreset.PUNCH }.forEach {
            val other = ColorGrade.Look.of(it)
            assertTrue("punch must beat ${it.name} on saturation", punch.saturation > other.saturation)
            assertTrue("punch must beat ${it.name} on chroma range", punch.chromaCeiling > other.chromaCeiling)
        }
    }

    @Test
    fun punchLeadsOnLocalContrast() {
        val punch = ColorGrade.Look.of(LookPreset.PUNCH)
        LookPreset.entries.filter { it != LookPreset.PUNCH }.forEach {
            assertTrue("punch must lead on clarity", punch.clarity > ColorGrade.Look.of(it).clarity)
        }
    }

    @Test
    fun everyLookHasACeilingInRange() {
        LookPreset.entries.forEach {
            assertTrue("${it.name} ceiling", it.chromaCeiling in 0.2f..0.8f)
        }
    }

    @Test
    fun softLiftsShadowsAndWarms() {
        val soft = ColorGrade.Look.of(LookPreset.SOFT)
        assertTrue("soft must lift shadows", soft.shadowLift > 0f)
        assertTrue("soft must be warm", soft.warmth > 0f)
        assertTrue("soft must be the gentlest on clarity", soft.clarity < ColorGrade.Look.of(LookPreset.NATURAL).clarity)
    }

    @Test
    fun cinematicDeepensShadows() {
        val cinematic = ColorGrade.Look.of(LookPreset.CINEMATIC)
        assertTrue("cinematic must deepen shadows", cinematic.shadowLift < 0f)
        assertTrue("cinematic must be the punchiest on clarity", cinematic.clarity > ColorGrade.Look.of(LookPreset.NATURAL).clarity)
    }

    @Test
    fun naturalIsNeutral() {
        val natural = ColorGrade.Look.of(LookPreset.NATURAL)
        assertEquals(0f, natural.warmth, 1e-6f)
        assertEquals(0f, natural.shadowLift, 1e-6f)
    }

    @Test
    fun everyPresetStaysInRange() {
        LookPreset.entries.forEach { preset ->
            assertTrue(preset.saturation in 0.8f..1.4f)
            assertTrue(preset.clarity in 0f..0.5f)
            assertTrue(preset.shoulder in 0.7f..1f)
            assertTrue(preset.chromaNr in 0f..1f)
            assertTrue(preset.warmth in -0.1f..0.1f)
        }
    }
}

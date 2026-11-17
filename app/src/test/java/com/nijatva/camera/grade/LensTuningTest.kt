package com.nijatva.camera.grade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Per-lens tuning: the main camera is untouched, the ultra-wide is punchier, the
 * telephoto is gentler, and a face gets the softest treatment of all.
 */
class LensTuningTest {

    private val look = ColorGrade.Look.of(LookPreset.NATURAL)

    @Test
    fun theMainCameraKeepsTheChosenLook() {
        assertEquals(look, LensTuning.adjust(look, LensTuning.Lens.MAIN))
    }

    @Test
    fun theUltraWideIsPunchier() {
        val tuned = LensTuning.adjust(look, LensTuning.Lens.ULTRA_WIDE)
        assertTrue("more saturation", tuned.saturation > look.saturation)
        assertTrue("more clarity", tuned.clarity > look.clarity)
    }

    @Test
    fun theTelephotoIsGentler() {
        val tuned = LensTuning.adjust(look, LensTuning.Lens.TELE)
        assertTrue("less sharpening", tuned.sharpen < look.sharpen)
        assertTrue("less saturation", tuned.saturation < look.saturation)
    }

    @Test
    fun theFrontCameraIsTheSoftest() {
        val front = LensTuning.adjust(look, LensTuning.Lens.FRONT)
        val tele = LensTuning.adjust(look, LensTuning.Lens.TELE)
        assertTrue("softest sharpening of all", front.sharpen < tele.sharpen)
        assertTrue("gentler colour than the main camera", front.saturation < look.saturation)
        assertTrue("a little warmth for skin", front.warmth > look.warmth)
    }

    @Test
    fun tuningComposesWithEveryLook() {
        LookPreset.entries.forEach { preset ->
            val base = ColorGrade.Look.of(preset)
            val wide = LensTuning.adjust(base, LensTuning.Lens.ULTRA_WIDE)
            val tele = LensTuning.adjust(base, LensTuning.Lens.TELE)
            assertTrue("${preset.name}: wide must beat tele on saturation", wide.saturation > tele.saturation)
        }
    }

    @Test
    fun focalLengthsClassifyTheWayPhonesAreBuilt() {
        // 24mm-equivalent main camera on a 1/1.56" sensor (7.6mm wide)
        assertEquals(LensTuning.Lens.MAIN, LensTuning.classify(0, 5.4f, 7.6f))
        // 13mm-equivalent ultra-wide
        assertEquals(LensTuning.Lens.ULTRA_WIDE, LensTuning.classify(0, 2.2f, 6.0f))
        // 70mm-equivalent telephoto
        assertEquals(LensTuning.Lens.TELE, LensTuning.classify(0, 7.0f, 3.6f))
        // Any front camera is a front camera
        assertEquals(LensTuning.Lens.FRONT, LensTuning.classify(1, 3.3f, 4.0f))
    }

    @Test
    fun missingNumbersFallBackToAMainLens() {
        assertEquals(26f, LensTuning.equivalent35mm(0f, 0f), 1e-6f)
        assertEquals(LensTuning.Lens.MAIN, LensTuning.classify(0, 0f, 0f))
    }
}

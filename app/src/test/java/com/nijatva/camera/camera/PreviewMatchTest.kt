package com.nijatva.camera.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeping the photo looking like the preview: when to lock the exposure, and how much
 * of the frame the viewfinder was actually showing.
 */
class PreviewMatchTest {

    @Test
    fun theExposureIsLockedOnlyWhenItMakesSense() {
        assertTrue(PreviewMatch.shouldLock(true, false, false, true))
        assertFalse("user turned it off", PreviewMatch.shouldLock(false, false, false, true))
        assertFalse("manual ISO already runs with AE off", PreviewMatch.shouldLock(true, true, false, true))
        assertFalse("manual exposure already runs with AE off", PreviewMatch.shouldLock(true, false, true, true))
        assertFalse("a lock before convergence freezes the wrong reading", PreviewMatch.shouldLock(true, false, false, false))
    }

    @Test
    fun aTallViewCropsTheSidesOfAFourByThreeFrame() {
        // 4:3 frame, 9:19.5 view: the preview filled the height and lost the sides.
        val crop = PreviewMatch.visibleCrop(4000, 3000, 1080, 2340)
        val keptWidth = crop[2] - crop[0]
        assertEquals("full height kept", 0, crop[1])
        assertEquals("full height kept", 3000, crop[3])
        assertTrue("width must shrink: $keptWidth", keptWidth < 4000)
        assertEquals("centred", (4000 - keptWidth) / 2, crop[0])
    }

    @Test
    fun aWideViewCropsTheTopAndBottom() {
        val crop = PreviewMatch.visibleCrop(3000, 4000, 2000, 1000)
        assertEquals("full width kept", 0, crop[0])
        assertEquals("full width kept", 3000, crop[2])
        assertTrue("height must shrink", crop[3] - crop[1] < 4000)
        assertEquals("centred", (4000 - (crop[3] - crop[1])) / 2, crop[1])
    }

    @Test
    fun theSameAspectKeepsEverything() {
        val crop = PreviewMatch.visibleCrop(4000, 3000, 1600, 1200)
        assertEquals(listOf(0, 0, 4000, 3000), crop.toList())
        assertFalse(PreviewMatch.cropIsMeaningful(4000, 3000, crop))
    }

    @Test
    fun aRealCropIsReportedAsMeaningful() {
        val crop = PreviewMatch.visibleCrop(4000, 3000, 1080, 2340)
        assertTrue(PreviewMatch.cropIsMeaningful(4000, 3000, crop))
    }

    @Test
    fun theInsetNeverInvertsTheRectangle() {
        val tiny = PreviewMatch.inset(intArrayOf(10, 10, 12, 12), 50)
        assertTrue("left must stay left of right", tiny[0] < tiny[2])
        assertTrue("top must stay above bottom", tiny[1] < tiny[3])
    }

    @Test
    fun nonsenseInputReturnsTheWholeFrame() {
        assertEquals(listOf(0, 0, 0, 0), PreviewMatch.visibleCrop(0, 0, 0, 0).toList())
    }
}

package com.nijatva.camera.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The user's own mode-strip arrangement: what survives a settings file from another
 * edition, what happens on a drop, and that the strip can never end up empty.
 */
class StripOrderTest {

    @Test
    fun noArrangementMeansTheDefault() {
        assertEquals(Modes.stripModes(false), Modes.stripModes(false, emptyList()))
    }

    @Test
    fun anArrangementIsHonoured() {
        val custom = listOf("RAW", "PHOTO", "NIGHT")
        assertEquals(listOf(Mode.RAW, Mode.PHOTO, Mode.NIGHT), Modes.stripModes(false, custom))
    }

    @Test
    fun namesThisEditionCannotDoAreDropped() {
        // Lite has no video, panorama or RAW.
        val custom = listOf("VIDEO", "PHOTO", "PANORAMA", "RAW", "NIGHT")
        assertEquals(listOf(Mode.PHOTO, Mode.NIGHT), Modes.stripModes(true, custom))
    }

    @Test
    fun nonsenseFallsBackToTheDefault() {
        assertEquals(Modes.stripModes(false), Modes.stripModes(false, listOf("NOT_A_MODE", "ALSO_NOT")))
    }

    @Test
    fun duplicatesCollapse() {
        assertEquals(listOf(Mode.PHOTO, Mode.NIGHT), Modes.stripModes(false, listOf("PHOTO", "PHOTO", "NIGHT")))
    }

    @Test
    fun aDropAddsToTheEndAndKeepsWhatWasThere() {
        val custom = listOf("PHOTO", "NIGHT")
        val next = Modes.withMode(false, custom, Mode.RAW)
        assertEquals(listOf("PHOTO", "NIGHT", "RAW"), next)
    }

    @Test
    fun droppingAModeAlreadyOnTheStripMovesItToTheEnd() {
        val custom = listOf("PHOTO", "NIGHT", "PRO")
        val next = Modes.withMode(false, custom, Mode.NIGHT)
        assertEquals(listOf("PHOTO", "PRO", "NIGHT"), next)
    }

    @Test
    fun aDropWithNoArrangementYetStartsFromTheDefault() {
        // No arrangement means the default strip, so the drop moves that mode to the
        // end rather than wiping the strip down to one entry.
        val next = Modes.withMode(true, emptyList(), Mode.NIGHT)
        assertEquals(listOf("PHOTO", "PRO", "NIGHT"), next)
        assertEquals(listOf(Mode.PHOTO, Mode.PRO, Mode.NIGHT), Modes.stripModes(true, next))
    }
}

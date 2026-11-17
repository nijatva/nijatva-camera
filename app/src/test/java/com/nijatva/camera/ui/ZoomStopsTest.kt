package com.nijatva.camera.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The zoom steps and their labels. The bug this guards against: every step above 1x
 * was labelled "2x", so a 3x stop read as 2x.
 */
class ZoomStopsTest {

    @Test
    fun labelsFollowTheFactor() {
        assertEquals("1x", ZoomStops.label(1f))
        assertEquals("2x", ZoomStops.label(2f))
        assertEquals("3x", ZoomStops.label(3f))
        assertEquals("5x", ZoomStops.label(5f))
    }

    @Test
    fun aFractionalZoomSaysSo() {
        assertEquals("2.4x", ZoomStops.label(2.4f))
        assertEquals("2.9x", ZoomStops.label(2.94f))
        assertEquals("1.5x", ZoomStops.label(1.5f))
    }

    @Test
    fun stopsStopWhereTheLensStops() {
        // A camera that cannot zoom gets one stop, not four dead buttons.
        assertEquals(listOf(1f), ZoomStops.stops(1f))
        assertEquals(listOf(1f, 2f), ZoomStops.stops(2f))
        assertEquals(listOf(1f, 2f, 3f), ZoomStops.stops(3.5f))
        assertEquals(listOf(1f, 2f, 3f, 5f), ZoomStops.stops(8f))
    }

    @Test
    fun stopsNeverExceedTheLimit() {
        listOf(1f, 1.5f, 2f, 4f, 8f, 30f).forEach { max ->
            ZoomStops.stops(max).forEach { step ->
                assertTrue("stop $step must fit $max", step <= max + 1e-4f)
            }
        }
    }

    @Test
    fun theNearestStopIsTheOneHighlighted() {
        val stops = listOf(1f, 2f, 3f)
        assertEquals(1f, ZoomStops.nearest(stops, 1.2f), 1e-6f)
        assertEquals(2f, ZoomStops.nearest(stops, 2.4f), 1e-6f)
        assertEquals(3f, ZoomStops.nearest(stops, 2.8f), 1e-6f)
    }

    @Test
    fun anEmptyListStillAnswers() {
        assertEquals(1f, ZoomStops.nearest(emptyList(), 3f), 1e-6f)
    }
}

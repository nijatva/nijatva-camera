package com.nijatva.camera.processor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The noise-model conversion: normalised units to raw DN, and the cases where it
 * has to refuse to answer.
 */
class NoiseModelTest {

    @Test
    fun convertsNormalisedUnitsToDn() {
        // S = 1e-4, O = 4e-7 at white 1023 gives A = 0.1023 and B = 0.4187.
        val ab = NoiseModel.fromChannels(listOf(1e-4 to 4e-7), whiteLevel = 1023f)
        assertNotNull(ab)
        assertEquals(0.1023f, ab!!.a, 1e-6f)
        assertEquals(0.4187f, ab.b, 1e-3f)
    }

    @Test
    fun channelsAreAveraged() {
        val ab = NoiseModel.fromChannels(
            listOf(1e-4 to 4e-7, 3e-4 to 4e-7),
            whiteLevel = 1000f,
        )
        assertNotNull(ab)
        // mean S = 2e-4 gives A = 0.2, mean O = 4e-7 gives B = 0.4
        assertEquals(0.2f, ab!!.a, 1e-6f)
        assertEquals(0.4f, ab.b, 1e-4f)
    }

    @Test
    fun varianceRisesWithSignal() {
        val ab = NoiseModel.fromChannels(listOf(1e-4 to 1e-6), whiteLevel = 1023f)!!
        val low = ab.a * 100f + ab.b
        val high = ab.a * 800f + ab.b
        assertTrue("variance must grow with signal: $low vs $high", high > low)
    }

    @Test
    fun aMissingProfileIsRefused() {
        assertNull(NoiseModel.fromChannels(emptyList(), whiteLevel = 1023f))
        assertNull(NoiseModel.fromChannels(listOf(1e-4 to 4e-7), whiteLevel = 0f))
    }

    @Test
    fun nonsenseValuesAreRefused() {
        assertNull(NoiseModel.fromChannels(listOf(Double.NaN to 1e-7), whiteLevel = 1023f))
        assertNull(NoiseModel.fromChannels(listOf(-1e-4 to 1e-7), whiteLevel = 1023f))
    }

    @Test
    fun oneBadChannelDoesNotDiscardTheRest() {
        val ab = NoiseModel.fromChannels(
            listOf(1e-4 to 4e-7, Double.NaN to 0.0),
            whiteLevel = 1023f,
        )
        assertNotNull(ab)
        assertEquals(0.1023f, ab!!.a, 1e-6f)
    }
}

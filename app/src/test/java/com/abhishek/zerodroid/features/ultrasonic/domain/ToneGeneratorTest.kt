package com.abhishek.zerodroid.features.ultrasonic.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneGeneratorTest {

    @Test
    fun `is idle before start`() {
        assertFalse(ToneGenerator().playing)
    }

    @Test
    fun `preview draws two full cycles of a unit sine`() {
        val samples = ToneGenerator().generatePreviewSamples(20_000, sampleCount = 200)

        assertEquals(200, samples.size)
        assertEquals(0f, samples[0], 1e-6f)
        assertEquals(1f, samples[25], 1e-3f)   // quarter of first cycle
        assertEquals(-1f, samples[75], 1e-3f)  // three quarters
        assertEquals(0f, samples[100], 1e-3f)  // second cycle starts
        assertTrue(samples.all { it in -1f..1f })
    }

    @Test
    fun `preview shape is independent of frequency`() {
        val g = ToneGenerator()
        assertTrue(g.generatePreviewSamples(18_000).contentEquals(g.generatePreviewSamples(24_000)))
    }

    @Test
    fun `stop is safe when nothing is playing`() {
        val g = ToneGenerator()
        g.stop()
        assertFalse(g.playing)
    }

    @Test
    fun `exposes the supported ultrasonic range`() {
        assertEquals(18_000, ToneGenerator.MIN_FREQUENCY)
        assertEquals(24_000, ToneGenerator.MAX_FREQUENCY)
        assertTrue(ToneGenerator.SAMPLE_RATE >= 2 * ToneGenerator.MAX_FREQUENCY)
    }
}

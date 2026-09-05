package com.abhishek.zerodroid.features.emf_mapper.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmfDataProcessorTest {

    private val processor = EmfDataProcessor()

    @Test
    fun `computes magnitude components and deviation`() {
        val reading = processor.processReading(floatArrayOf(3f, 4f, 12f), baseline = 10f)

        assertEquals(13f, reading.magnitude, 1e-5f)
        assertEquals(3f, reading.x, 0f)
        assertEquals(4f, reading.y, 0f)
        assertEquals(12f, reading.z, 0f)
        assertEquals(3f, reading.deviation, 1e-5f)
    }

    @Test
    fun `missing axes are treated as zero`() {
        val reading = processor.processReading(floatArrayOf(5f), baseline = 0f)
        assertEquals(5f, reading.magnitude, 0f)
        assertEquals(0f, reading.y, 0f)
    }

    @Test
    fun `classifies levels by absolute deviation`() {
        fun level(mag: Float, baseline: Float) = processor.processReading(floatArrayOf(mag, 0f, 0f), baseline).level

        assertEquals(EmfLevel.NORMAL, level(60f, 50f))
        assertEquals(EmfLevel.ELEVATED, level(66f, 50f))
        assertEquals(EmfLevel.HIGH, level(91f, 50f))
        assertEquals(EmfLevel.EXTREME, level(151f, 50f))
        assertEquals(EmfLevel.HIGH, level(5f, 50f)) // negative deviation counts too
    }

    @Test
    fun `hotspots are HIGH or EXTREME readings`() {
        assertTrue(processor.detectHotspot(processor.processReading(floatArrayOf(100f, 0f, 0f), 50f)))
        assertFalse(processor.detectHotspot(processor.processReading(floatArrayOf(60f, 0f, 0f), 50f)))
    }

    @Test
    fun `statistics return min max and average`() {
        val history = listOf(10f, 20f, 60f).map { processor.processReading(floatArrayOf(it, 0f, 0f), 0f) }

        val (min, max, avg) = processor.getStatistics(history)

        assertEquals(10f, min, 0f)
        assertEquals(60f, max, 0f)
        assertEquals(30f, avg, 1e-5f)
        assertEquals(Triple(0f, 0f, 0f), processor.getStatistics(emptyList()))
    }
}

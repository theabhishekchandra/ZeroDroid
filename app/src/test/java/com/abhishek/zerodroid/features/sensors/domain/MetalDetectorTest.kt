package com.abhishek.zerodroid.features.sensors.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetalDetectorTest {

    @Test
    fun `returns inactive state when fewer than three axes are supplied`() {
        val state = MetalDetector().update(floatArrayOf(1f, 2f))

        assertFalse(state.isActive)
        assertEquals(0f, state.currentMagnitude, 0f)
    }

    @Test
    fun `computes vector magnitude`() {
        val state = MetalDetector().update(floatArrayOf(3f, 4f, 0f))

        assertTrue(state.isActive)
        assertEquals(5f, state.currentMagnitude, 1e-5f)
    }

    @Test
    fun `baseline is the running average during warm up`() {
        val detector = MetalDetector()
        detector.update(floatArrayOf(10f, 0f, 0f))
        val second = detector.update(floatArrayOf(20f, 0f, 0f))

        assertEquals(15f, second.baseline, 1e-5f)
        assertEquals(5f, second.deviation, 1e-5f)
    }

    @Test
    fun `baseline locks after ten samples and deviation tracks changes`() {
        val detector = MetalDetector()
        repeat(10) { detector.update(floatArrayOf(50f, 0f, 0f)) }

        val spike = detector.update(floatArrayOf(90f, 0f, 0f))

        assertEquals(50f, spike.baseline, 1e-5f)
        assertEquals(40f, spike.deviation, 1e-5f)
    }

    @Test
    fun `reset clears calibration`() {
        val detector = MetalDetector()
        repeat(10) { detector.update(floatArrayOf(50f, 0f, 0f)) }
        detector.reset()

        val fresh = detector.update(floatArrayOf(10f, 0f, 0f))

        assertEquals(10f, fresh.baseline, 1e-5f)
        assertEquals(0f, fresh.deviation, 1e-5f)
    }
}

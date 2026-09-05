package com.abhishek.zerodroid.features.ultrasonic.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos

class FftProcessorTest {

    @Test
    fun `rejects sizes that are not a power of two`() {
        assertThrows(IllegalArgumentException::class.java) { FftProcessor.fft(FloatArray(6)) }
        assertThrows(IllegalArgumentException::class.java) { FftProcessor.fft(FloatArray(0)) }
    }

    @Test
    fun `constant input concentrates energy in the DC bin`() {
        val mags = FftProcessor.fft(FloatArray(64) { 2f })

        assertEquals(32, mags.size)
        assertEquals(2f, mags[0], 1e-4f)
        assertTrue((1 until 32).all { mags[it] < 1e-4f })
    }

    @Test
    fun `pure cosine peaks at its bin with half amplitude`() {
        val n = 128
        val k = 10
        val signal = FloatArray(n) { i -> cos(2.0 * PI * k * i / n).toFloat() }

        val mags = FftProcessor.fft(signal)

        val peak = mags.indices.maxByOrNull { mags[it] }!!
        assertEquals(k, peak)
        assertEquals(0.5f, mags[k], 1e-3f)
        assertTrue(mags[k + 1] < 1e-3f && mags[k - 1] < 1e-3f)
    }

    @Test
    fun `two tones produce two peaks`() {
        val n = 256
        val signal = FloatArray(n) { i ->
            (cos(2.0 * PI * 5 * i / n) + 0.5 * cos(2.0 * PI * 40 * i / n)).toFloat()
        }

        val mags = FftProcessor.fft(signal)

        assertEquals(0.5f, mags[5], 1e-3f)
        assertEquals(0.25f, mags[40], 1e-3f)
    }

    @Test
    fun `fft does not mutate its input`() {
        val input = FloatArray(16) { it.toFloat() }
        val copy = input.copyOf()
        FftProcessor.fft(input)
        assertTrue(input.contentEquals(copy))
    }

    @Test
    fun `hanning window is zero at the edges and one in the middle`() {
        val windowed = FftProcessor.applyHanningWindow(FloatArray(101) { 1f })

        assertEquals(0f, windowed[0], 1e-6f)
        assertEquals(0f, windowed[100], 1e-6f)
        assertEquals(1f, windowed[50], 1e-6f)
        assertEquals(0.5f, windowed[25], 1e-3f)
    }
}

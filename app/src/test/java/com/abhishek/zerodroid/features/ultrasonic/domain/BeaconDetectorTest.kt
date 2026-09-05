package com.abhishek.zerodroid.features.ultrasonic.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeaconDetectorTest {

    /** Bins every 100 Hz from 17 kHz to 24.9 kHz at a flat noise floor. */
    private fun spectrum(noise: Float = 1f, peaks: Map<Float, Float> = emptyMap()): List<FrequencyBin> =
        (0 until 80).map { i ->
            val f = 17_000f + i * 100f
            FrequencyBin(f, peaks[f] ?: noise)
        }

    @Test
    fun `empty spectrum and flat noise yield no beacons`() {
        assertTrue(BeaconDetector.detect(emptyList()).isEmpty())
        assertTrue(BeaconDetector.detect(spectrum()).isEmpty())
    }

    @Test
    fun `sustained peak above threshold is reported with centre and bandwidth`() {
        val beacons = BeaconDetector.detect(
            spectrum(peaks = mapOf(20_000f to 50f, 20_100f to 90f, 20_200f to 60f))
        )

        assertEquals(1, beacons.size)
        assertEquals(20_100f, beacons[0].centerFrequencyHz, 0f)
        assertEquals(200f, beacons[0].bandwidth, 0f)
        assertEquals(90f, beacons[0].magnitude, 0f)
    }

    @Test
    fun `single bin spikes are ignored`() {
        assertTrue(BeaconDetector.detect(spectrum(peaks = mapOf(21_000f to 500f))).isEmpty())
    }

    @Test
    fun `peaks below the ultrasonic band are ignored`() {
        assertTrue(BeaconDetector.detect(spectrum(peaks = mapOf(17_000f to 500f, 17_100f to 500f))).isEmpty())
    }

    @Test
    fun `peak must exceed twenty times the median floor`() {
        assertTrue(BeaconDetector.detect(spectrum(peaks = mapOf(20_000f to 19f, 20_100f to 19f))).isEmpty())
        assertEquals(1, BeaconDetector.detect(spectrum(peaks = mapOf(20_000f to 21f, 20_100f to 21f))).size)
    }

    @Test
    fun `separate peaks produce separate beacons`() {
        val beacons = BeaconDetector.detect(
            spectrum(peaks = mapOf(19_000f to 40f, 19_100f to 40f, 23_000f to 40f, 23_100f to 40f))
        )

        assertEquals(listOf(19_000f, 23_000f), beacons.map { it.centerFrequencyHz })
    }
}

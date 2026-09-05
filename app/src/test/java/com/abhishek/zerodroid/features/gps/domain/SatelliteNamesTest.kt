package com.abhishek.zerodroid.features.gps.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SatelliteNamesTest {

    private fun sat(constellation: Int, svid: Int, cn0: Float = 30f, freqHz: Float? = null) =
        SatelliteInfo(svid, constellation, cn0, 45f, 90f, usedInFix = true, carrierFrequencyHz = freqHz)

    @Test
    fun `looks up names for supported constellations`() {
        assertEquals("NAVSTAR 80", SatelliteNames.lookup(1, 1))
        assertEquals("Michibiki-2 (QZS-2)", SatelliteNames.lookup(4, 194))
        assertEquals("Thijs (GSAT0101)", SatelliteNames.lookup(6, 11))
        assertEquals("NVS-01", SatelliteNames.lookup(7, 10))
    }

    @Test
    fun `returns null for unknown svids and unsupported constellations`() {
        assertNull(SatelliteNames.lookup(1, 99))
        assertNull(SatelliteNames.lookup(3, 1))   // GLONASS intentionally omitted
        assertNull(SatelliteNames.lookup(2, 120)) // SBAS intentionally omitted
        assertNull(SatelliteNames.lookup(0, 1))
    }

    @Test
    fun `satellite info derives constellation name and common name`() {
        assertEquals("GPS", sat(1, 1).constellationName)
        assertEquals("Galileo", sat(6, 11).constellationName)
        assertEquals("Unknown", sat(42, 1).constellationName)
        assertEquals("NAVSTAR 80", sat(1, 1).commonName)
        assertNull(sat(3, 1).commonName)
    }

    @Test
    fun `signal quality buckets by carrier to noise`() {
        assertEquals("Strong", sat(1, 1, cn0 = 35f).signalQuality)
        assertEquals("Good", sat(1, 1, cn0 = 25f).signalQuality)
        assertEquals("Weak", sat(1, 1, cn0 = 15f).signalQuality)
        assertEquals("Very Weak", sat(1, 1, cn0 = 14.9f).signalQuality)
    }

    @Test
    fun `frequency band labels known carriers and formats others`() {
        assertEquals("L1", sat(1, 1, freqHz = 1_575_420_000f).frequencyBand)
        assertEquals("L5", sat(1, 1, freqHz = 1_176_450_000f).frequencyBand)
        assertEquals("E5b", sat(6, 11, freqHz = 1_207_140_000f).frequencyBand)
        assertEquals("G1", sat(3, 1, freqHz = 1_602_000_000f).frequencyBand)
        assertEquals("1300MHz", sat(1, 1, freqHz = 1_300_000_000f).frequencyBand)
        assertNull(sat(1, 1).frequencyBand)
    }
}

package com.abhishek.zerodroid.features.signal_logger.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalLogModelsTest {

    @Test
    fun `entries get unique ids and current timestamps`() {
        val a = SignalLogEntry(type = SignalType.WIFI_NEW, source = "WiFi", address = "AA", detail = "new")
        val b = SignalLogEntry(type = SignalType.WIFI_NEW, source = "WiFi", address = "AA", detail = "new")

        assertNotEquals(a.id, b.id)
        assertTrue(a.timestamp > 0)
        assertFalse(a.isAnomaly)
    }

    @Test
    fun `stats start empty and are mutable accumulators`() {
        val stats = SignalStats()
        stats.uniqueWifiAps += "AA"
        stats.uniqueWifiAps += "AA"
        stats.previousBleRssi["BB"] = -60

        assertEquals(1, stats.uniqueWifiAps.size)
        assertEquals(-60, stats.previousBleRssi["BB"])
        assertEquals(0, stats.totalWifiSeen)
    }

    @Test
    fun `default logger state is idle`() {
        val state = SignalLoggerState()
        assertFalse(state.isLogging)
        assertEquals(0, state.totalEntries)
        assertEquals(0f, state.entriesPerMinute, 0f)
    }
}

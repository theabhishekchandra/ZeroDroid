package com.abhishek.zerodroid.features.locate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocateTrackerTest {

    @Test
    fun `first sample is taken as is`() {
        val r = LocateTracker().add(-70, now = 0)
        assertEquals(-70, r.rssi)
        assertEquals(LocateTrend.STEADY, r.trend)
    }

    @Test
    fun `smoothing damps a single jump`() {
        val t = LocateTracker(alpha = 0.3f)
        t.add(-80, 0)
        val r = t.add(-50, 100)
        assertEquals(-71, r.rssi)
        assertEquals(-50, r.raw)
    }

    @Test
    fun `walking towards the device reads warmer`() {
        val t = LocateTracker()
        var last: LocateReading? = null
        for (i in 0..40) last = t.add(-90 + i, now = i * 200L)
        assertEquals(LocateTrend.WARMER, last!!.trend)
    }

    @Test
    fun `walking away reads colder`() {
        val t = LocateTracker()
        var last: LocateReading? = null
        for (i in 0..40) last = t.add(-50 - i, now = i * 200L)
        assertEquals(LocateTrend.COLDER, last!!.trend)
    }

    @Test
    fun `standing still with noise stays steady`() {
        val t = LocateTracker()
        var last: LocateReading? = null
        for (i in 0..40) last = t.add(if (i % 2 == 0) -66 else -70, now = i * 200L)
        assertEquals(LocateTrend.STEADY, last!!.trend)
    }

    @Test
    fun `lost after silence`() {
        val t = LocateTracker(lostAfterMs = 8_000)
        assertFalse(t.isLost(100_000))
        t.add(-60, 0)
        assertFalse(t.isLost(5_000))
        assertTrue(t.isLost(9_000))
    }

    @Test
    fun `proximity and beep interval scale with signal`() {
        assertEquals(0f, LocateTracker.proximity(-110f))
        assertEquals(1f, LocateTracker.proximity(-30f))
        assertTrue(LocateTracker.beepIntervalMs(1f) < LocateTracker.beepIntervalMs(0f))
        assertEquals("Within arm’s reach", LocateTracker.distanceLabel(-45f))
        assertEquals("Far or behind walls", LocateTracker.distanceLabel(-95f))
    }
}

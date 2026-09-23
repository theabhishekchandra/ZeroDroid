package com.abhishek.zerodroid.features.watch.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchersTest {

    private val min = 60_000L
    private val tag = Heard("AA:BB", "AirTag", -60)

    @Test
    fun `follow-me needs time and movement`() {
        val w = FollowMeWatcher(minDurationMs = 15 * min, minMoveMetres = 200.0)
        val start = LatLon(52.0, 13.0)
        // Scanned every minute for 16 min without moving: nothing.
        for (m in 0..16) assertTrue(w.update(m * min, listOf(tag), start).isEmpty())
        // Moved about 1.1 km: reported once.
        val hits = w.update(17 * min, listOf(tag), LatLon(52.01, 13.0))
        assertEquals(1, hits.size)
        assertTrue(hits.single().movedMetres!! > 1000)
        assertTrue(w.update(18 * min, listOf(tag), LatLon(52.02, 13.0)).isEmpty())
    }

    @Test
    fun `follow-me without location falls back to repeated sightings`() {
        val w = FollowMeWatcher(minDurationMs = 15 * min, minSightings = 4)
        listOf(0L, 5 * min, 10 * min).forEach { assertTrue(w.update(it, listOf(tag), null).isEmpty()) }
        val hit = w.update(15 * min, listOf(tag), null).single()
        assertNull(hit.movedMetres)
        assertEquals(4, hit.sightings)
    }

    @Test
    fun `a tracker gone for a while starts over`() {
        val w = FollowMeWatcher(minDurationMs = 15 * min, minSightings = 2, gapResetMs = 10 * min)
        w.update(0, listOf(tag), null)
        // 20 min of silence resets the track, so 20 min after the first sighting isn't enough.
        assertTrue(w.update(20 * min, listOf(tag), null).isEmpty())
        // Heard every 5 min from then on: reported once 15 min into the new track.
        assertTrue(w.update(25 * min, listOf(tag), null).isEmpty())
        assertTrue(w.update(30 * min, listOf(tag), null).isEmpty())
        assertEquals(1, w.update(35 * min, listOf(tag), null).size)
    }

    @Test
    fun `dwell fires once a strong device stays long enough`() {
        val w = DwellWatcher(thresholdDbm = -65, minDurationMs = 10 * min)
        val strong = Heard("11:22", "Speaker", -50)
        val weak = Heard("33:44", "Far", -80)
        for (m in 0..9) assertTrue(w.update(m * min, listOf(strong, weak)).isEmpty())
        val hits = w.update(10 * min, listOf(strong, weak))
        assertEquals(listOf("11:22"), hits.map { it.heard.key })
        assertTrue(w.update(11 * min, listOf(strong)).isEmpty())
    }

    @Test
    fun `dwell resets after a gap`() {
        val w = DwellWatcher(thresholdDbm = -65, minDurationMs = 10 * min, gapResetMs = 3 * min)
        val strong = Heard("11:22", "Speaker", -50)
        w.update(0, listOf(strong))
        w.update(5 * min, emptyList())
        assertTrue(w.update(10 * min, listOf(strong)).isEmpty())
    }

    @Test
    fun `disconnects with good signal add up, weak ones don't`() {
        val w = DisconnectWatcher(count = 3, windowMs = 10 * min, goodRssi = -70)
        assertNull(w.onDisconnect(0, -85))
        assertNull(w.onDisconnect(1 * min, -55))
        assertNull(w.onDisconnect(2 * min, -55))
        assertEquals(3, w.onDisconnect(3 * min, -55))
        assertNull(w.onDisconnect(4 * min, -55))
    }

    @Test
    fun `old disconnects fall out of the window`() {
        val w = DisconnectWatcher(count = 3, windowMs = 10 * min)
        w.onDisconnect(0, -50)
        w.onDisconnect(1 * min, -50)
        assertNull(w.onDisconnect(20 * min, -50))
    }

    @Test
    fun `cell fires only on a step down to 2G`() {
        val w = CellDowngradeWatcher()
        assertFalse(w.update(2))
        assertFalse(w.update(4))
        assertFalse(w.update(0))
        assertTrue(w.update(2))
        assertFalse(w.update(2))
    }

    @Test
    fun `network types map to generations`() {
        assertEquals(2, networkGeneration(2)) // EDGE
        assertEquals(3, networkGeneration(15)) // HSPA+
        assertEquals(4, networkGeneration(13)) // LTE
        assertEquals(5, networkGeneration(20)) // NR
        assertEquals(0, networkGeneration(0))
    }

    @Test
    fun `throttle allows one per cooldown`() {
        val t = AlertThrottle(cooldownMs = 60 * min)
        assertTrue(t.allow("a", 0))
        assertFalse(t.allow("a", 30 * min))
        assertTrue(t.allow("b", 30 * min))
        assertTrue(t.allow("a", 61 * min))
    }

    @Test
    fun `distance and minutes read naturally`() {
        assertEquals(1112.0, LatLon(52.0, 13.0).distanceTo(LatLon(52.01, 13.0)), 5.0)
        assertEquals("46 min", formatMinutes(46 * min))
        assertEquals("1 h 5 min", formatMinutes(65 * min))
        assertEquals("2 h", formatMinutes(120 * min))
    }
}

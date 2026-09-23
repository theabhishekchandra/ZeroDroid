package com.abhishek.zerodroid.features.surfaces

import com.abhishek.zerodroid.core.sessions.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetSnapshotTest {

    private val now = 10_000_000_000L
    private fun session(minutesAgo: Long, place: String?, findings: Int, title: String = "Full room sweep") =
        Session("s", "sweep", title, place, now - minutesAgo * 60_000, now, 3, findings, "")

    @Test
    fun `status lines`() {
        val s = WidgetSnapshot(3, session(120, "Living room", 0), null, 2, "low battery", now)
        assertEquals("3 open alerts", s.alertsLine)
        assertEquals("Last sweep 2h ago · Living room", s.sweepLine)
        assertEquals("Watching · 2 rules · low battery", s.watchLine)
    }

    @Test
    fun `quiet status`() {
        val s = WidgetSnapshot(0, null, null, 0, "low battery", now)
        assertEquals("All clear", s.alertsLine)
        assertEquals("No sweeps yet", s.sweepLine)
        assertNull(s.watchLine)
        assertEquals("Tracker check", s.trackerTitle)
        assertEquals("Not run yet", s.trackerSubtitle)
    }

    @Test
    fun `tracker result`() {
        assertEquals("No trackers", WidgetSnapshot(0, null, session(5, null, 0, "Tracker check"), 0, "", now).trackerTitle)
        assertEquals("2 found", WidgetSnapshot(0, null, session(5, null, 2, "Tracker check"), 0, "", now).trackerTitle)
    }

    @Test
    fun `ago buckets`() {
        assertEquals("just now", WidgetSnapshot.ago(now, now))
        assertEquals("5m ago", WidgetSnapshot.ago(now - 5 * 60_000, now))
        assertEquals("3d ago", WidgetSnapshot.ago(now - 3 * 24 * 3_600_000, now))
    }
}

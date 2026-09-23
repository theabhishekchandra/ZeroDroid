package com.abhishek.zerodroid.features.alert_center.domain

import com.abhishek.zerodroid.core.alerts.AlertResolution
import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.alerts.AlertSource
import com.abhishek.zerodroid.core.alerts.UnifiedAlert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AlertTriageTest {

    private val now = Calendar.getInstance().apply { set(2026, 8, 23, 15, 0, 0) }.timeInMillis
    private fun alert(ts: Long, severity: AlertSeverity = AlertSeverity.HIGH, source: AlertSource = AlertSource.ROGUE_AP) =
        UnifiedAlert("id$ts", source, severity, "t", "d", ts)

    @Test
    fun `buckets split on calendar days`() {
        assertEquals(DayBucket.TODAY, AlertTriage.bucket(now - 60_000, now))
        assertEquals(DayBucket.YESTERDAY, AlertTriage.bucket(now - 20 * 3_600_000L, now))
        assertEquals(DayBucket.EARLIER, AlertTriage.bucket(now - 3 * AlertTriage.DAY_MS, now))
    }

    @Test
    fun `week counts high and medium per day, oldest first`() {
        val week = AlertTriage.week(
            listOf(alert(now - 1000), alert(now - 2000, AlertSeverity.CRITICAL), alert(now - 3000, AlertSeverity.MEDIUM), alert(now - 2 * AlertTriage.DAY_MS)),
            now
        )
        assertEquals(7, week.size)
        assertEquals(2, week.last().high)
        assertEquals(1, week.last().medium)
        assertEquals(1, week[4].high)
        assertEquals("W", week.last().dayLabel)
    }

    @Test
    fun `resolve action fits the source`() {
        assertEquals(AlertResolution.MINE, AlertTriage.resolveAction(AlertSource.BLUETOOTH_TRACKER).second)
        assertEquals(AlertResolution.SAFE, AlertTriage.resolveAction(AlertSource.ROGUE_AP).second)
        assertEquals(AlertResolution.MUTED, AlertTriage.resolveAction(AlertSource.DEAUTH).second)
    }

    @Test
    fun `filters group sources`() {
        val alerts = listOf(alert(1, source = AlertSource.DEAUTH), alert(2, source = AlertSource.BLUETOOTH_TRACKER))
        assertEquals(1, AlertTriage.filter(alerts, AlertFilter.WIFI).size)
        assertEquals(2, AlertTriage.filter(alerts, AlertFilter.ALL).size)
    }
}

package com.abhishek.zerodroid.features.alert_center.domain

import com.abhishek.zerodroid.core.alerts.AlertResolution
import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.alerts.AlertSource
import com.abhishek.zerodroid.core.alerts.UnifiedAlert
import java.util.Calendar

/** Filter chips on the Alerts screen, grouping sources by what people look for. */
enum class AlertFilter(val label: String, val sources: Set<AlertSource>) {
    ALL("All", AlertSource.entries.toSet()),
    TRACKERS("Trackers", setOf(AlertSource.BLUETOOTH_TRACKER)),
    WIFI("WiFi", setOf(AlertSource.ROGUE_AP, AlertSource.DEAUTH)),
    GPS("GPS", setOf(AlertSource.GPS_SPOOF)),
    CAMERA("Camera", setOf(AlertSource.HIDDEN_CAMERA))
}

enum class DayBucket(val label: String) { TODAY("Today"), YESTERDAY("Yesterday"), EARLIER("Earlier") }

/** Counts for one day of the "This week" strip. */
data class DayCount(val dayLabel: String, val high: Int, val medium: Int)

/** Pure helpers behind Alerts triage, kept out of the UI so they can be tested. */
object AlertTriage {

    fun bucket(timestamp: Long, now: Long): DayBucket {
        val startOfToday = startOfDay(now)
        return when {
            timestamp >= startOfToday -> DayBucket.TODAY
            timestamp >= startOfToday - DAY_MS -> DayBucket.YESTERDAY
            else -> DayBucket.EARLIER
        }
    }

    /** Last seven days, oldest first; HIGH counts CRITICAL too, MEDIUM counts MEDIUM. */
    fun week(alerts: List<UnifiedAlert>, now: Long): List<DayCount> {
        val today = startOfDay(now)
        val cal = Calendar.getInstance()
        return (6 downTo 0).map { back ->
            val start = today - back * DAY_MS
            val end = start + DAY_MS
            val day = alerts.filter { it.timestamp in start until end }
            cal.timeInMillis = start
            DayCount(
                dayLabel = "SMTWTFS"[cal.get(Calendar.DAY_OF_WEEK) - 1].toString(),
                high = day.count { it.severity == AlertSeverity.CRITICAL || it.severity == AlertSeverity.HIGH },
                medium = day.count { it.severity == AlertSeverity.MEDIUM }
            )
        }
    }

    /** The resolve action that fits each source: trackers can be yours, networks can be safe. */
    fun resolveAction(source: AlertSource): Pair<String, AlertResolution> = when (source) {
        AlertSource.BLUETOOTH_TRACKER -> "It’s mine" to AlertResolution.MINE
        AlertSource.DEAUTH -> "Mute" to AlertResolution.MUTED
        AlertSource.HIDDEN_CAMERA -> "Checked" to AlertResolution.CHECKED
        AlertSource.ROGUE_AP, AlertSource.GPS_SPOOF -> "Mark safe" to AlertResolution.SAFE
    }

    fun filter(alerts: List<UnifiedAlert>, filter: AlertFilter): List<UnifiedAlert> =
        alerts.filter { it.source in filter.sources }

    private fun startOfDay(time: Long): Long = Calendar.getInstance().apply {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    const val DAY_MS = 24L * 60 * 60 * 1000
}

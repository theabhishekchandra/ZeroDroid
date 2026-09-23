package com.abhishek.zerodroid.features.watch.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLon(val lat: Double, val lon: Double) {
    /** Great-circle distance in metres. */
    fun distanceTo(o: LatLon): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(o.lat - lat)
        val dLon = Math.toRadians(o.lon - lon)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat)) * cos(Math.toRadians(o.lat)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}

/** A device heard in one scan cycle. */
data class Heard(val key: String, val label: String, val rssi: Int)

/** A device that has been around long enough to report. */
data class Dwell(val heard: Heard, val sinceMs: Long, val durationMs: Long, val movedMetres: Double?, val sightings: Int)

/**
 * Follow-me: a tracker that keeps being heard for [minDurationMs] while the phone moves at least
 * [minMoveMetres]. Without a location fix, [minSightings] separate cycles stand in for movement.
 * A tracker unheard for [gapResetMs] starts over; each one is reported once per track.
 */
class FollowMeWatcher(
    private val minDurationMs: Long = WatchRule.FOLLOW_ME_MINUTES * 60_000L,
    private val minMoveMetres: Double = 200.0,
    private val minSightings: Int = 4,
    private val gapResetMs: Long = 10 * 60_000L
) {
    private class Track(val first: Long, var last: Long, val start: LatLon?, var maxMove: Double, var sightings: Int, var reported: Boolean)

    private val tracks = mutableMapOf<String, Track>()

    fun update(now: Long, trackers: List<Heard>, here: LatLon?): List<Dwell> {
        tracks.entries.removeAll { now - it.value.last > gapResetMs }
        return trackers.mapNotNull { h ->
            val t = tracks.getOrPut(h.key) { Track(now, now, here, 0.0, 0, false) }
            t.last = now
            t.sightings++
            if (here != null && t.start != null) t.maxMove = maxOf(t.maxMove, t.start.distanceTo(here))
            val duration = now - t.first
            val moved = if (t.start != null && here != null) t.maxMove >= minMoveMetres else t.sightings >= minSightings
            if (!t.reported && duration >= minDurationMs && moved) {
                t.reported = true
                Dwell(h, t.first, duration, t.maxMove.takeIf { t.start != null }, t.sightings)
            } else null
        }
    }
}

/** Custom rule: any device at or above [thresholdDbm] continuously for [minDurationMs]. */
class DwellWatcher(
    private val thresholdDbm: Int,
    private val minDurationMs: Long,
    private val gapResetMs: Long = 3 * 60_000L
) {
    private val since = mutableMapOf<String, Pair<Long, Long>>() // key -> (first, last)
    private val reported = mutableSetOf<String>()

    fun update(now: Long, devices: List<Heard>): List<Dwell> {
        since.entries.removeAll { (key, span) -> (now - span.second > gapResetMs).also { if (it) reported -= key } }
        return devices.filter { it.rssi >= thresholdDbm }.mapNotNull { h ->
            val (first, _) = since[h.key] ?: (now to now)
            since[h.key] = first to now
            if (h.key !in reported && now - first >= minDurationMs) {
                reported += h.key
                Dwell(h, first, now - first, null, 0)
            } else null
        }
    }
}

/**
 * Deauth: [count] WiFi drops within [windowMs] while the signal was good (≥ [goodRssi]) is the
 * pattern of a deauthentication attack; drops with a weak signal are just range.
 */
class DisconnectWatcher(
    private val count: Int = 3,
    private val windowMs: Long = 10 * 60_000L,
    private val goodRssi: Int = -70
) {
    private val drops = ArrayDeque<Long>()

    /** Returns the number of drops when the rule should fire, else null. */
    fun onDisconnect(now: Long, lastRssi: Int?): Int? {
        if (lastRssi == null || lastRssi < goodRssi) return null
        drops.addLast(now)
        while (drops.isNotEmpty() && now - drops.first() > windowMs) drops.removeFirst()
        if (drops.size < count) return null
        val n = drops.size
        drops.clear()
        return n
    }
}

/** Cell: fires when the data network steps down from 3G or better to 2G. */
class CellDowngradeWatcher {
    private var previous = 0

    /** [generation]: 2, 3, 4 or 5; 0 when unknown (ignored). */
    fun update(generation: Int): Boolean {
        if (generation == 0) return false
        val fired = generation == 2 && previous >= 3
        previous = generation
        return fired
    }
}

/** At most one notification per key per [cooldownMs], so a stuck condition doesn't spam. */
class AlertThrottle(private val cooldownMs: Long = 6 * 60 * 60_000L) {
    private val last = mutableMapOf<String, Long>()

    fun allow(key: String, now: Long): Boolean {
        val prev = last[key]
        if (prev != null && now - prev < cooldownMs) return false
        last[key] = now
        return true
    }
}

/** Mobile data generation for a `TelephonyManager.NETWORK_TYPE_*` value; 0 when unknown. */
fun networkGeneration(type: Int): Int = when (type) {
    1, 2, 4, 7, 11, 16 -> 2 // GPRS, EDGE, CDMA, 1xRTT, iDEN, GSM
    3, 5, 6, 8, 9, 10, 12, 14, 15, 17 -> 3 // UMTS, EVDO, HSxPA, eHRPD, HSPA+, TD-SCDMA
    13, 18 -> 4 // LTE, IWLAN
    20 -> 5 // NR
    else -> 0
}

/** "46 min", "1 h 5 min". */
fun formatMinutes(ms: Long): String {
    val min = (ms / 60_000).toInt()
    return if (min < 60) "$min min" else "${min / 60} h${if (min % 60 > 0) " ${min % 60} min" else ""}"
}

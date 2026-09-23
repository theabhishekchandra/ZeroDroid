package com.abhishek.zerodroid.features.locate.domain

enum class LocateTrend(val label: String) { WARMER("WARMER"), COLDER("COLDER"), STEADY("STEADY"), LOST("NO SIGNAL") }

data class LocateReading(
    /** Smoothed signal, dBm. */
    val rssi: Int,
    val raw: Int,
    /** 0 = barely heard, 1 = right next to it. */
    val proximity: Float,
    val trend: LocateTrend,
    val distance: String
)

/**
 * Turns noisy per-advertisement RSSI into a hot/cold reading: an exponential moving average
 * damps the jumps from body blocking and reflections, and the trend compares the smoothed value
 * with where it was a few seconds ago, so one lucky packet doesn't flip WARMER/COLDER.
 */
class LocateTracker(
    private val alpha: Float = 0.3f,
    private val trendWindowMs: Long = 3_000,
    private val trendThresholdDb: Float = 3f,
    private val lostAfterMs: Long = 8_000
) {
    private var smoothed: Float? = null
    private var lastSampleAt = 0L
    private val history = ArrayDeque<Pair<Long, Float>>()

    fun add(rssi: Int, now: Long): LocateReading {
        val s = smoothed?.let { it + alpha * (rssi - it) } ?: rssi.toFloat()
        smoothed = s
        lastSampleAt = now
        history.addLast(now to s)
        while (history.size > 1 && now - history.first().first > trendWindowMs * 2) history.removeFirst()

        val past = history.lastOrNull { now - it.first >= trendWindowMs }?.second
        val trend = when {
            past == null -> LocateTrend.STEADY
            s - past >= trendThresholdDb -> LocateTrend.WARMER
            past - s >= trendThresholdDb -> LocateTrend.COLDER
            else -> LocateTrend.STEADY
        }
        return LocateReading(s.toInt(), rssi, proximity(s), trend, distanceLabel(s))
    }

    /** True once nothing has been heard for a while. */
    fun isLost(now: Long): Boolean = smoothed != null && now - lastSampleAt > lostAfterMs

    fun reset() {
        smoothed = null
        history.clear()
    }

    companion object {
        private const val FAR_DBM = -100f
        private const val NEAR_DBM = -40f

        fun proximity(rssi: Float): Float = ((rssi - FAR_DBM) / (NEAR_DBM - FAR_DBM)).coerceIn(0f, 1f)

        /** Rough bands only: transmit power varies by device, so metres would be false precision. */
        fun distanceLabel(rssi: Float): String = when {
            rssi >= -50 -> "Within arm’s reach"
            rssi >= -62 -> "Very close, under 2 m"
            rssi >= -75 -> "In this room"
            rssi >= -88 -> "Nearby, maybe next room"
            else -> "Far or behind walls"
        }

        /** Beep gap: fast when close, slow when far. */
        fun beepIntervalMs(proximity: Float): Long = (1_400 - 1_250 * proximity).toLong().coerceIn(150, 1_400)
    }
}

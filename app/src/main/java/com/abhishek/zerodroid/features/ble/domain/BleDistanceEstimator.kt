package com.abhishek.zerodroid.features.ble.domain

import kotlin.math.pow
import java.util.Locale

object BleDistanceEstimator {
    private const val DEFAULT_TX_POWER = -59
    private const val PATH_LOSS_EXPONENT = 2.5

    fun estimateDistance(rssi: Int, txPower: Int = DEFAULT_TX_POWER): Double {
        if (rssi == 0) return -1.0
        val ratio = (txPower - rssi).toDouble() / (10.0 * PATH_LOSS_EXPONENT)
        return 10.0.pow(ratio)
    }

    fun getDistanceLabel(distanceM: Double): String = when {
        distanceM < 0 -> "Unknown"
        distanceM < 0.5 -> "Immediate"
        distanceM < 2.0 -> "Near (${String.format(Locale.US, "%.1f", distanceM)}m)"
        distanceM < 10.0 -> "Medium (${String.format(Locale.US, "%.1f", distanceM)}m)"
        else -> "Far (${String.format(Locale.US, "%.0f", distanceM)}m)"
    }

    fun getProximityLabel(distanceM: Double): String = when {
        distanceM < 0 -> "?"
        distanceM < 0.5 -> "●"
        distanceM < 2.0 -> "◉"
        distanceM < 10.0 -> "○"
        else -> "◌"
    }

    /**
     * Honest range for a row subtitle, e.g. "~1 m" or "~2–4 m". Indoors the estimate can be
     * off by about 2×, so anything past a metre is shown as a span rather than one number.
     */
    fun rangeLabel(rssi: Int, txPower: Int = DEFAULT_TX_POWER): String {
        val d = estimateDistance(rssi, txPower)
        if (d < 0) return "range unknown"
        if (d < 1.5) return "~1 m"
        val low = kotlin.math.max(1, kotlin.math.floor(d / 1.5).toInt())
        val high = kotlin.math.max(low + 1, kotlin.math.ceil(d * 1.5).toInt())
        return if (high > 30) "30 m+" else "~$low–$high m"
    }
}

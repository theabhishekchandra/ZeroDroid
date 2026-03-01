package com.abhishek.zerodroid.features.ble.domain

import kotlin.math.pow

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
        distanceM < 2.0 -> "Near (${String.format("%.1f", distanceM)}m)"
        distanceM < 10.0 -> "Medium (${String.format("%.1f", distanceM)}m)"
        else -> "Far (${String.format("%.0f", distanceM)}m)"
    }

    fun getProximityLabel(distanceM: Double): String = when {
        distanceM < 0 -> "?"
        distanceM < 0.5 -> "●"
        distanceM < 2.0 -> "◉"
        distanceM < 10.0 -> "○"
        else -> "◌"
    }
}

package com.abhishek.zerodroid.features.proximity_radar.domain

import kotlin.math.absoluteValue
import kotlin.math.pow

enum class DeviceCategory {
    WIFI_AP,
    BLE_DEVICE,
    BLE_BEACON,
    UNKNOWN
}

data class RadarDevice(
    val id: String,
    val name: String,
    val category: DeviceCategory,
    val rssi: Int,
    val estimatedDistanceM: Float,
    val angle: Float,
    val lastSeen: Long,
    val signalPercent: Int
)

data class RadarState(
    val isScanning: Boolean = false,
    val devices: List<RadarDevice> = emptyList(),
    val wifiCount: Int = 0,
    val bleCount: Int = 0,
    val nearestDevice: RadarDevice? = null,
    val scanRadius: Float = 30f,
    val error: String? = null
)

object ProximityCalculator {

    /**
     * Estimate distance from RSSI using the log-distance path loss model.
     *
     * d = 10 ^ ((txPower - rssi) / (10 * n))
     *
     * @param rssi The received signal strength indicator in dBm.
     * @param txPower The expected RSSI at 1 meter distance (default -59 dBm).
     * @return Estimated distance in meters, clamped to a reasonable range.
     */
    fun estimateDistance(rssi: Int, txPower: Int = -59): Float {
        val n = 2.7f
        val distance = 10f.pow((txPower - rssi) / (10f * n))
        return distance.coerceIn(0.1f, 100f)
    }

    /**
     * Assign a stable angle (0-360) to a device based on its address hash.
     * This ensures the device doesn't jump around on the radar between scans.
     */
    fun stableAngle(address: String): Float {
        return (address.hashCode().absoluteValue % 360).toFloat()
    }

    /**
     * Determine the device category for a BLE device based on its service UUIDs.
     * Devices advertising iBeacon, Eddystone, or AltBeacon UUIDs are classified as beacons.
     */
    fun classifyBleDevice(serviceUuids: List<String>): DeviceCategory {
        val beaconUuidPrefixes = listOf(
            "0000feaa", // Eddystone
            "0000feb3", // AltBeacon
            "0000180f", // Battery Service (common in beacons)
        )
        val isBeacon = serviceUuids.any { uuid ->
            beaconUuidPrefixes.any { prefix -> uuid.lowercase().startsWith(prefix) }
        }
        return if (isBeacon) DeviceCategory.BLE_BEACON else DeviceCategory.BLE_DEVICE
    }

    /**
     * Calculate the auto-adjusted scan radius based on the farthest detected device.
     * Rounds up to the nearest 10m with a minimum of 10m and maximum of 100m.
     */
    fun autoScanRadius(devices: List<RadarDevice>): Float {
        if (devices.isEmpty()) return 30f
        val maxDistance = devices.maxOf { it.estimatedDistanceM }
        val rounded = ((maxDistance / 10f).toInt() + 1) * 10f
        return rounded.coerceIn(10f, 100f)
    }
}

package com.abhishek.zerodroid.features.ble.domain

data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int,
    val serviceUuids: List<String> = emptyList(),
    val isBookmarked: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
) {
    val displayName: String get() = name ?: "Unknown Device"
    val signalPercent: Int
        get() = when {
            rssi >= -50 -> 100
            rssi <= -100 -> 0
            else -> 2 * (rssi + 100)
        }
}

data class BleScanState(
    val isScanning: Boolean = false,
    val devices: List<BleDevice> = emptyList(),
    val error: String? = null
)

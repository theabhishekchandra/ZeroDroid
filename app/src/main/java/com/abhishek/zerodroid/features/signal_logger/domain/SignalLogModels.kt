package com.abhishek.zerodroid.features.signal_logger.domain

import java.util.UUID

enum class SignalType {
    WIFI_AP,
    WIFI_NEW,
    WIFI_LOST,
    BLE_DEVICE,
    BLE_NEW,
    BLE_LOST,
    ANOMALY
}

data class SignalLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: SignalType,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String,
    val address: String,
    val rssi: Int? = null,
    val detail: String,
    val isAnomaly: Boolean = false
)

data class SignalLoggerState(
    val isLogging: Boolean = false,
    val entries: List<SignalLogEntry> = emptyList(),
    val totalEntries: Int = 0,
    val wifiApCount: Int = 0,
    val bleDeviceCount: Int = 0,
    val newDevicesCount: Int = 0,
    val lostDevicesCount: Int = 0,
    val anomalyCount: Int = 0,
    val loggingDurationMs: Long = 0,
    val entriesPerMinute: Float = 0f,
    val error: String? = null
)

data class SignalStats(
    val totalWifiSeen: Int = 0,
    val totalBleSeen: Int = 0,
    val uniqueWifiAps: MutableSet<String> = mutableSetOf(),
    val uniqueBleDevices: MutableSet<String> = mutableSetOf(),
    val previousWifiAps: MutableSet<String> = mutableSetOf(),
    val previousBleDevices: MutableSet<String> = mutableSetOf(),
    val previousWifiRssi: MutableMap<String, Int> = mutableMapOf(),
    val previousBleRssi: MutableMap<String, Int> = mutableMapOf()
)

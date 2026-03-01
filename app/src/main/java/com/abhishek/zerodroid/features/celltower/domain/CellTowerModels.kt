package com.abhishek.zerodroid.features.celltower.domain

data class CellTowerInfo(
    val type: CellType,
    val mcc: Int?,
    val mnc: Int?,
    val lac: Int?,
    val cid: Long?,
    val rssi: Int,
    val arfcn: Int?,
    val isRegistered: Boolean = false
) {
    val signalPercent: Int
        get() = when {
            rssi >= -70 -> 100
            rssi <= -120 -> 0
            else -> ((rssi + 120) * 100) / 50
        }
}

enum class CellType(val displayName: String) {
    LTE("4G LTE"),
    NR("5G NR"),
    WCDMA("3G WCDMA"),
    GSM("2G GSM"),
    CDMA("CDMA"),
    TDSCDMA("TD-SCDMA"),
    UNKNOWN("Unknown")
}

data class ImsiCatcherAlert(
    val type: AlertType,
    val description: String,
    val severity: AlertSeverity,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AlertType {
    LAC_CHANGE,
    SIGNAL_SPIKE,
    FORCED_2G_DOWNGRADE,
    UNKNOWN_CELL
}

enum class AlertSeverity { LOW, MEDIUM, HIGH }

data class SignalDataPoint(
    val rssi: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class CellTowerState(
    val currentCell: CellTowerInfo? = null,
    val neighbors: List<CellTowerInfo> = emptyList(),
    val alerts: List<ImsiCatcherAlert> = emptyList(),
    val isMonitoring: Boolean = false,
    val error: String? = null,
    val signalHistory: List<Int> = emptyList()
)

package com.abhishek.zerodroid.features.wardriving.domain

data class WardrivingRecord(
    val bssid: String,
    val ssid: String?,
    val rssi: Int,
    val frequency: Int = 0,
    val capabilities: String? = null,
    val lat: Double,
    val lng: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class WardrivingSession(
    val id: String,
    val startTime: Long = System.currentTimeMillis(),
    val recordCount: Int = 0,
    val uniqueBssids: Int = 0,
    val isActive: Boolean = false
)

data class WardrivingStats(
    val totalRecords: Int = 0,
    val uniqueSsids: Int = 0,
    val uniqueBssids: Int = 0,
    val openCount: Int = 0,
    val securedCount: Int = 0,
    val sessionDurationMs: Long = 0L
) {
    val openPercent: Float get() = if (openCount + securedCount > 0) (openCount.toFloat() / (openCount + securedCount)) * 100f else 0f
    val formattedDuration: String get() {
        val totalSeconds = sessionDurationMs / 1000
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "${minutes}m ${seconds}s"
    }
}

data class WardrivingState(
    val session: WardrivingSession? = null,
    val records: List<WardrivingRecord> = emptyList(),
    val isScanning: Boolean = false,
    val exportStatus: String? = null,
    val error: String? = null,
    val stats: WardrivingStats? = null
)

enum class ExportFormat(val extension: String) {
    WIGLE_CSV("csv")
}

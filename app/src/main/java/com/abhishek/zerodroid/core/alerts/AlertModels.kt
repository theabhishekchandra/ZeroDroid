package com.abhishek.zerodroid.core.alerts

enum class AlertSource(val label: String) {
    ROGUE_AP("Rogue AP"),
    DEAUTH("Deauth Detector"),
    GPS_SPOOF("GPS Spoof"),
    HIDDEN_CAMERA("Hidden Camera"),
    BLUETOOTH_TRACKER("Tracker Scanner")
}

enum class AlertSeverity { CRITICAL, HIGH, MEDIUM, LOW }

enum class AlertStatus { OPEN, RESOLVED }

/** How the user closed an alert; shown as the tag on resolved rows. */
enum class AlertResolution(val label: String) {
    MINE("MINE"),
    SAFE("SAFE"),
    MUTED("MUTED"),
    CHECKED("CHECKED")
}

data class UnifiedAlert(
    val id: String,
    val source: AlertSource,
    val severity: AlertSeverity,
    val title: String,
    val detail: String,
    val timestamp: Long,
    val status: AlertStatus = AlertStatus.OPEN,
    val resolution: AlertResolution? = null,
    val resolvedAt: Long? = null
) {
    val isOpen: Boolean get() = status == AlertStatus.OPEN
}

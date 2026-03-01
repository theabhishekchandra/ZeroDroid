package com.abhishek.zerodroid.features.camera.domain

data class QrScanResult(
    val rawValue: String,
    val format: String,
    val contentType: QrContentType,
    val parsedContent: String,
    val isThreat: Boolean = false,
    val threatReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class QrContentType(val displayName: String) {
    URL("URL"),
    WIFI("Wi-Fi"),
    VCARD("Contact"),
    TEXT("Text"),
    EMAIL("Email"),
    PHONE("Phone"),
    SMS("SMS"),
    GEO("Location"),
    UNKNOWN("Unknown")
}

data class QrScannerState(
    val isCameraActive: Boolean = false,
    val lastScan: QrScanResult? = null,
    val scanHistory: List<QrScanResult> = emptyList(),
    val showHistory: Boolean = false,
    val activeTab: QrScreenTab = QrScreenTab.SCAN
)

enum class QrScreenTab(val displayName: String) {
    SCAN("Scan"),
    GENERATE("Generate")
}

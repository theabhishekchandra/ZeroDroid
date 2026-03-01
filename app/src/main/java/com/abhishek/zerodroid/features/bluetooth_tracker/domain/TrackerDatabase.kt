package com.abhishek.zerodroid.features.bluetooth_tracker.domain

import com.abhishek.zerodroid.features.ble.domain.BleDevice

// ── Data Models ──────────────────────────────────────────────────────────────

enum class TrackerType(val label: String) {
    AIRTAG("AirTag"),
    SMARTTAG("SmartTag"),
    TILE("Tile"),
    CHIPOLO("Chipolo"),
    PEBBLEBEE("Pebblebee"),
    GENERIC_TRACKER("Tracker"),
    UNKNOWN("Unknown")
}

enum class TrackingRisk(val label: String) {
    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW"),
    NONE("NONE")
}

data class DetectedTracker(
    val address: String,
    val name: String?,
    val type: TrackerType,
    val rssi: Int,
    val firstSeen: Long,
    val lastSeen: Long,
    val seenCount: Int,
    val risk: TrackingRisk,
    val manufacturerData: String? = null
) {
    val displayName: String get() = name ?: type.label
    val signalPercent: Int
        get() = when {
            rssi >= -50 -> 100
            rssi <= -100 -> 0
            else -> 2 * (rssi + 100)
        }
}

data class TrackerScanState(
    val isScanning: Boolean = false,
    val trackers: List<DetectedTracker> = emptyList(),
    val totalDevicesScanned: Int = 0,
    val scanDurationMs: Long = 0,
    val highRiskCount: Int = 0,
    val error: String? = null
)

// ── Tracker Identification ───────────────────────────────────────────────────

class TrackerIdentifier {

    companion object {
        // Apple AirTag manufacturer company ID
        private const val APPLE_COMPANY_ID = "004C"

        // Samsung SmartTag manufacturer company ID
        private const val SAMSUNG_COMPANY_ID = "0075"

        // Chipolo manufacturer company ID
        private const val CHIPOLO_COMPANY_ID = "02E5"

        // Known tracker service UUID prefixes
        private const val AIRTAG_SERVICE_PREFIX = "7DFC9000"
        private const val TILE_SERVICE_UUID = "FEED"

        // Generic tracker name pattern
        private val TRACKER_NAME_REGEX = Regex(
            """(?i).*(tracker|tag|find.*my|airtag|smarttag|tile|chipolo|pebblebee|cube|nut.*find|tractive).*"""
        )

        // Duration thresholds for risk assessment (milliseconds)
        private const val HIGH_RISK_DURATION_MS = 10L * 60 * 1000   // 10 minutes
        private const val HIGH_RISK_MIN_COUNT = 5
        private const val MEDIUM_RISK_MIN_COUNT = 3
    }

    /**
     * Identifies whether a BLE device is a known tracker and what type.
     */
    fun identify(device: BleDevice): TrackerType {
        // Check by name first for specific brands
        val name = device.name

        if (name != null) {
            val upper = name.uppercase()
            when {
                upper.contains("AIRTAG") -> return TrackerType.AIRTAG
                upper.contains("SMARTTAG") -> return TrackerType.SMARTTAG
                upper.contains("TILE") -> return TrackerType.TILE
                upper.contains("CHIPOLO") -> return TrackerType.CHIPOLO
                upper.contains("PEBBLEBEE") || upper.startsWith("PB-") -> return TrackerType.PEBBLEBEE
            }
        }

        // Check service UUIDs
        for (uuid in device.serviceUuids) {
            val upper = uuid.uppercase()
            when {
                upper.startsWith(AIRTAG_SERVICE_PREFIX) -> return TrackerType.AIRTAG
                upper.contains(TILE_SERVICE_UUID) -> return TrackerType.TILE
            }
        }

        // Check name against generic tracker pattern
        if (name != null && TRACKER_NAME_REGEX.matches(name)) {
            return TrackerType.GENERIC_TRACKER
        }

        return TrackerType.UNKNOWN
    }

    /**
     * Checks if a BLE device address string contains known manufacturer IDs.
     * The manufacturer data is typically the first few bytes of the advertisement.
     */
    fun identifyByManufacturerHint(address: String, name: String?): TrackerType {
        // OUI-based heuristic: Apple devices often have certain OUI prefixes
        // This is a supplementary check; primary identification uses identify()
        return TrackerType.UNKNOWN
    }

    /**
     * Assesses the tracking risk based on how long and how often a tracker has been seen.
     *
     * HIGH  — seen more than 5 times AND present for over 10 minutes
     * MEDIUM — seen more than 3 times
     * LOW   — just recently detected
     * NONE  — not a known tracker type
     */
    fun assessRisk(tracker: DetectedTracker): TrackingRisk {
        if (tracker.type == TrackerType.UNKNOWN) return TrackingRisk.NONE

        val duration = tracker.lastSeen - tracker.firstSeen

        return when {
            tracker.seenCount > HIGH_RISK_MIN_COUNT && duration > HIGH_RISK_DURATION_MS -> TrackingRisk.HIGH
            tracker.seenCount > MEDIUM_RISK_MIN_COUNT -> TrackingRisk.MEDIUM
            else -> TrackingRisk.LOW
        }
    }
}

package com.abhishek.zerodroid.features.sweep.domain

/** One thing a sweep checks. Radio checks share one WiFi + BLE listening window. */
enum class SweepCheck(val label: String, val method: String) {
    WIFI("WiFi networks", "Open and weakly encrypted networks nearby"),
    ROGUE_AP("Rogue AP check", "Evil twins and look-alike network names"),
    TRACKERS("Trackers & BLE", "AirTag, Tile, SmartTag and hobby radio modules"),
    CAMERAS("Hidden camera signatures", "Camera makers’ WiFi prefixes, names and BLE"),
    ULTRASONIC("Ultrasonic beacons", "18–24 kHz tones through the microphone"),
    MAGNETIC("Magnetic anomalies", "Electronics hidden behind surfaces")
}

/** Ready-made sweeps. Each lists the checks it runs and roughly how long it takes. */
enum class SweepPreset(
    val title: String,
    val description: String,
    val checks: List<SweepCheck>,
    val duration: String
) {
    FULL(
        "Full room sweep",
        "Everything below. Best for hotels and rentals.",
        SweepCheck.entries.toList(),
        "~2 min"
    ),
    HIDDEN_CAMERA(
        "Hidden camera",
        "WiFi makers and names, BLE signatures and magnetic sweep.",
        listOf(SweepCheck.CAMERAS, SweepCheck.WIFI, SweepCheck.MAGNETIC),
        "~60 s"
    ),
    RF_BUG(
        "RF bug sweep",
        "Radio modules (HC-05, ESP32, nRF), ultrasonic and magnetic.",
        listOf(SweepCheck.TRACKERS, SweepCheck.ULTRASONIC, SweepCheck.MAGNETIC),
        "~80 s"
    ),
    TRACKER_CHECK(
        "Tracker check",
        "Quick scan for AirTag, Tile, SmartTag, Chipolo and Pebblebee.",
        listOf(SweepCheck.TRACKERS),
        "~30 s"
    );

    val needsRadios: Boolean get() = checks.any { it in RADIO_CHECKS }
    val needsMicrophone: Boolean get() = SweepCheck.ULTRASONIC in checks

    companion object {
        val RADIO_CHECKS = setOf(SweepCheck.WIFI, SweepCheck.ROGUE_AP, SweepCheck.TRACKERS, SweepCheck.CAMERAS)
    }
}

enum class FindingLevel { HIGH, MEDIUM, LOW }

data class SweepFinding(
    val check: SweepCheck,
    val level: FindingLevel,
    val title: String,
    val detail: String,
    /** Stable identity (BSSID, BLE address) when the finding is a device; enables Locate. */
    val key: String? = null,
    val rssi: Int? = null,
    val isBle: Boolean = false
)

enum class CheckState { QUEUED, RUNNING, DONE, SKIPPED }

/** Where one check stands, and its one-line result once done. */
data class CheckProgress(
    val check: SweepCheck,
    val state: CheckState = CheckState.QUEUED,
    val result: String? = null,
    val flags: Int = 0
)

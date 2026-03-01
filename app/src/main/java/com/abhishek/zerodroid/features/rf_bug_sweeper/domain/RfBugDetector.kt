package com.abhishek.zerodroid.features.rf_bug_sweeper.domain

import com.abhishek.zerodroid.features.ble.domain.BleDevice
import java.util.UUID

enum class BugType { RF_TRANSMITTER, ULTRASONIC_BEACON, MAGNETIC_ANOMALY, SUSPICIOUS_BLE, UNKNOWN }

enum class SweepMode { BLE, ULTRASONIC, MAGNETIC, ALL }

enum class ThreatSeverity { CRITICAL, HIGH, MEDIUM, LOW }

data class BugDetection(
    val id: String = UUID.randomUUID().toString(),
    val type: BugType,
    val severity: ThreatSeverity,
    val title: String,
    val detail: String,
    val rssi: Int? = null,
    val frequency: Float? = null,
    val fieldStrength: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class BugSweepState(
    val isSweeping: Boolean = false,
    val activeModes: Set<SweepMode> = emptySet(),
    val detections: List<BugDetection> = emptyList(),
    val bleDeviceCount: Int = 0,
    val ultrasonicDetected: Boolean = false,
    val magneticBaseline: Float = 0f,
    val magneticCurrent: Float = 0f,
    val magneticDeviation: Float = 0f,
    val sweepDurationMs: Long = 0,
    val error: String? = null
)

/**
 * Detects RF listening devices / bugs by analysing BLE scan results,
 * ultrasonic frequency data, and magnetometer readings.
 */
class RfBugDetector {

    // ── Known bug / surveillance-device BLE name patterns ──────────────
    private val bugNamePatterns = listOf(
        Regex("(?i).*(bug|listen|spy|surveil|record|wiretap|transmit|beacon).*"),
        Regex("(?i).*(HC-0[5-9]|JDY-|AT-0[0-9]|BT-|HM-1[0-9]).*"),
        Regex("(?i).*(ESP32|ESP-|CC254[0-9]|nRF5[0-9]).*"),
    )

    // ── Cheap BLE module OUI prefixes ──────────────────────────────────
    private val suspiciousOuiPrefixes = listOf(
        "00:15:83",  // RF Digital
        "20:91:48",  // Texas Instruments CC254x
        "AC:23:3F",  // Shenzhen
        "34:15:13",  // Tuya modules
    )

    // ── Magnetic anomaly threshold (μT) ────────────────────────────────
    companion object {
        const val MAGNETIC_ANOMALY_THRESHOLD = 25f
    }

    // ── BLE analysis ───────────────────────────────────────────────────

    /**
     * Analyse a list of BLE devices and return detections for any that
     * look suspicious (name pattern match, OUI match, or unnamed with
     * very strong signal).
     */
    fun analyseBleDevices(devices: List<BleDevice>): List<BugDetection> {
        val detections = mutableListOf<BugDetection>()

        for (device in devices) {
            // 1. Check name patterns
            val nameMatch = device.name?.let { name ->
                bugNamePatterns.any { it.matches(name) }
            } ?: false

            if (nameMatch) {
                detections += BugDetection(
                    id = "ble-name-${device.address}",
                    type = BugType.SUSPICIOUS_BLE,
                    severity = if (device.rssi > -50) ThreatSeverity.HIGH else ThreatSeverity.MEDIUM,
                    title = "Suspicious BLE: ${device.displayName}",
                    detail = "Name matches known bug/transmitter module pattern. " +
                            "Address: ${device.address}",
                    rssi = device.rssi
                )
                continue
            }

            // 2. Check OUI prefix
            val ouiMatch = suspiciousOuiPrefixes.any { prefix ->
                device.address.uppercase().startsWith(prefix.uppercase())
            }

            if (ouiMatch) {
                detections += BugDetection(
                    id = "ble-oui-${device.address}",
                    type = BugType.RF_TRANSMITTER,
                    severity = if (device.rssi > -50) ThreatSeverity.HIGH else ThreatSeverity.MEDIUM,
                    title = "Cheap BLE Module Detected",
                    detail = "OUI ${device.address.take(8)} belongs to a low-cost RF module " +
                            "commonly used in surveillance devices. " +
                            "Name: ${device.displayName}",
                    rssi = device.rssi
                )
                continue
            }

            // 3. Unnamed device with very strong RSSI (likely very close)
            if (device.name == null && device.rssi > -40) {
                detections += BugDetection(
                    id = "ble-unnamed-${device.address}",
                    type = BugType.SUSPICIOUS_BLE,
                    severity = ThreatSeverity.MEDIUM,
                    title = "Strong Unnamed BLE Device",
                    detail = "Unnamed device at ${device.rssi} dBm (very close). " +
                            "Address: ${device.address}. " +
                            "Hidden transmitters often broadcast without a name.",
                    rssi = device.rssi
                )
            }
        }

        return detections
    }

    // ── Ultrasonic analysis ────────────────────────────────────────────

    /**
     * Given peak ultrasonic frequency and magnitude, determine whether
     * a tracking beacon is present.
     */
    fun analyseUltrasonic(
        peakFrequencyHz: Float,
        peakMagnitude: Float,
        beaconCount: Int
    ): BugDetection? {
        if (beaconCount <= 0 && peakMagnitude < 0.01f) return null

        val severity = when {
            beaconCount >= 2 -> ThreatSeverity.CRITICAL
            beaconCount == 1 -> ThreatSeverity.HIGH
            peakMagnitude > 0.05f -> ThreatSeverity.MEDIUM
            else -> ThreatSeverity.LOW
        }

        return BugDetection(
            id = "ultra-${peakFrequencyHz.toInt()}",
            type = BugType.ULTRASONIC_BEACON,
            severity = severity,
            title = "Ultrasonic Beacon Detected",
            detail = "Peak at ${String.format("%.1f", peakFrequencyHz)} Hz " +
                    "(magnitude ${String.format("%.4f", peakMagnitude)}). " +
                    if (beaconCount > 0) "$beaconCount beacon(s) identified in 18-24 kHz range. " +
                            "Ultrasonic beacons are used for cross-device tracking."
                    else "Elevated ultrasonic energy detected — possible tracking signal.",
            frequency = peakFrequencyHz
        )
    }

    // ── Magnetic anomaly analysis ──────────────────────────────────────

    /**
     * Given current magnetic deviation from baseline, determine whether
     * hidden electronics may be nearby.
     */
    fun analyseMagnetic(
        baseline: Float,
        current: Float,
        deviation: Float
    ): BugDetection? {
        val absDeviation = kotlin.math.abs(deviation)
        if (absDeviation < MAGNETIC_ANOMALY_THRESHOLD) return null

        val severity = when {
            absDeviation > 100f -> ThreatSeverity.CRITICAL
            absDeviation > 60f -> ThreatSeverity.HIGH
            absDeviation > 40f -> ThreatSeverity.MEDIUM
            else -> ThreatSeverity.LOW
        }

        return BugDetection(
            id = "mag-anomaly",
            type = BugType.MAGNETIC_ANOMALY,
            severity = severity,
            title = "Magnetic Anomaly",
            detail = "Deviation of ${String.format("%.1f", absDeviation)} μT from baseline " +
                    "(${String.format("%.1f", baseline)} → ${String.format("%.1f", current)} μT). " +
                    "Electronic devices and wiring produce detectable magnetic fields. " +
                    "Slowly move phone to pinpoint the source.",
            fieldStrength = current
        )
    }
}

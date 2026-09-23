package com.abhishek.zerodroid.features.sweep.domain

import com.abhishek.zerodroid.core.util.SecurityType
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerIdentifier
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerType
import com.abhishek.zerodroid.features.hidden_camera.domain.HiddenCameraDetector
import com.abhishek.zerodroid.features.hidden_camera.domain.ThreatLevel
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.RfBugDetector
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.ThreatSeverity
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RiskLevel
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RogueApAnalyzer
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.features.wifi.domain.WifiAssessment

/**
 * Turns what a sweep heard into findings, one check at a time. Pure apart from the detectors it
 * is given, so each rule is testable without radios.
 */
class SweepEvaluator(
    private val cameraDetector: HiddenCameraDetector,
    private val trackerIdentifier: TrackerIdentifier = TrackerIdentifier(),
    private val rogueAnalyzer: RogueApAnalyzer = RogueApAnalyzer(),
    private val bugDetector: RfBugDetector = RfBugDetector()
) {
    data class Result(val findings: List<SweepFinding>, val summary: String)

    fun wifi(aps: List<WifiAccessPoint>): Result {
        val weak = aps.filter { it.security == SecurityType.OPEN || it.security == SecurityType.WEP }
        return Result(
            weak.map {
                SweepFinding(
                    SweepCheck.WIFI,
                    FindingLevel.LOW,
                    "${if (it.security == SecurityType.OPEN) "Open" else "WEP"} network: ${WifiAssessment.displayName(it)}",
                    "Anyone nearby can read traffic on it. Avoid logging in to anything over it.",
                    key = it.bssid,
                    rssi = it.rssi
                )
            },
            if (weak.isEmpty()) "${aps.size} networks · none open or WEP" else "${aps.size} networks · ${weak.size} open or WEP"
        )
    }

    fun rogueAp(aps: List<WifiAccessPoint>, trusted: Set<String>): Result {
        val alerts = rogueAnalyzer.analyze(aps, trusted).filter { it.riskLevel != RiskLevel.SAFE && it.riskLevel != RiskLevel.LOW }
        return Result(
            alerts.map {
                SweepFinding(
                    SweepCheck.ROGUE_AP,
                    if (it.riskLevel == RiskLevel.CRITICAL || it.riskLevel == RiskLevel.HIGH) FindingLevel.HIGH else FindingLevel.MEDIUM,
                    "${it.threatType.label}: ${it.suspiciousAp.ssid.ifBlank { "[hidden]" }}",
                    it.description,
                    key = it.suspiciousAp.bssid,
                    rssi = it.suspiciousAp.rssi
                )
            },
            if (alerts.isEmpty()) "No twins or look-alikes" else "${alerts.size} suspicious access point${if (alerts.size > 1) "s" else ""}"
        )
    }

    /**
     * Trackers are HIGH when their signal says they're probably in the room (−70 dBm or
     * stronger); weaker ones are likely next door. Hobby radio modules are flagged too.
     */
    fun trackers(devices: List<BleDevice>): Result {
        val trackerFindings = devices.mapNotNull { d ->
            val type = trackerIdentifier.identify(d)
            if (type == TrackerType.UNKNOWN) return@mapNotNull null
            val close = d.rssi >= IN_ROOM_RSSI
            SweepFinding(
                SweepCheck.TRACKERS,
                if (close) FindingLevel.HIGH else FindingLevel.MEDIUM,
                "${type.label} tracker${if (close) ", likely in this room" else " nearby"}",
                if (close) "Its signal stayed strong, so it’s probably within a few metres. Check bags, furniture and fixtures."
                else "The signal is weak, so it may be in a neighbouring room.",
                key = d.address,
                rssi = d.rssi,
                isBle = true
            )
        }
        val trackerAddresses = trackerFindings.mapNotNull { it.key }.toSet()
        val moduleFindings = bugDetector.analyseBleDevices(devices.filter { it.address !in trackerAddresses }).map { b ->
            val device = devices.firstOrNull { it.name != null && b.title.contains(it.name) }
            SweepFinding(
                SweepCheck.TRACKERS,
                if (b.severity == ThreatSeverity.CRITICAL || b.severity == ThreatSeverity.HIGH) FindingLevel.HIGH else FindingLevel.MEDIUM,
                b.title,
                b.detail,
                key = device?.address,
                rssi = b.rssi,
                isBle = device != null
            )
        }
        val all = trackerFindings + moduleFindings
        return Result(
            all,
            "${devices.size} Bluetooth devices · " + if (all.isEmpty()) "no trackers or radio modules" else "${all.size} flagged"
        )
    }

    fun cameras(aps: List<WifiAccessPoint>, devices: List<BleDevice>): Result {
        val detections = aps.flatMap { listOfNotNull(cameraDetector.matchWifiOui(it), cameraDetector.matchWifiSsid(it)) } +
            devices.flatMap { listOfNotNull(cameraDetector.matchBleDevice(it), cameraDetector.matchBleOui(it)) }
        val unique = detections.distinctBy { it.title }
        return Result(
            unique.map {
                SweepFinding(
                    SweepCheck.CAMERAS,
                    when (it.threatLevel) {
                        ThreatLevel.HIGH -> FindingLevel.HIGH
                        ThreatLevel.MEDIUM -> FindingLevel.MEDIUM
                        ThreatLevel.LOW -> FindingLevel.LOW
                    },
                    it.title,
                    it.detail,
                    rssi = it.rssi
                )
            },
            if (unique.isEmpty()) "No camera signatures matched" else "${unique.size} camera-like signal${if (unique.size > 1) "s" else ""}"
        )
    }

    fun ultrasonic(peakHz: Float, peakMagnitude: Float, beaconCount: Int): Result {
        val detection = bugDetector.analyseUltrasonic(peakHz, peakMagnitude, beaconCount)?.takeIf { beaconCount > 0 }
        return Result(
            listOfNotNull(detection?.let {
                SweepFinding(SweepCheck.ULTRASONIC, FindingLevel.MEDIUM, it.title, it.detail)
            }),
            if (detection == null) "Quiet from 18 to 24 kHz" else "Steady tone near %.1f kHz".format(peakHz / 1000f)
        )
    }

    fun magnetic(baseline: Float, peak: Float): Result {
        val deviation = peak - baseline
        val detection = bugDetector.analyseMagnetic(baseline, peak, deviation)
        return Result(
            listOfNotNull(detection?.let {
                SweepFinding(SweepCheck.MAGNETIC, FindingLevel.MEDIUM, it.title, it.detail)
            }),
            if (detection == null) "Within %.0f µT of the room’s baseline".format(kotlin.math.abs(deviation).coerceAtLeast(1f))
            else "Peak %.0f µT above baseline".format(deviation)
        )
    }

    companion object {
        const val IN_ROOM_RSSI = -70
    }
}

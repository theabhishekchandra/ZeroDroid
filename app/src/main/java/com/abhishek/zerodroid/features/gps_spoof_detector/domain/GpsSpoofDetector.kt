package com.abhishek.zerodroid.features.gps_spoof_detector.domain

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import com.abhishek.zerodroid.features.celltower.domain.CellTowerInfo
import com.abhishek.zerodroid.features.gps.domain.GpsState
import com.abhishek.zerodroid.features.sensors.domain.SensorReading
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// ── Data models ────────────────────────────────────────────────────────────────

data class SpoofCheck(
    val name: String,
    val passed: Boolean,
    val detail: String
)

data class SpoofCheckResult(
    val gpsLocation: Pair<Double, Double>?,
    val cellLocation: Pair<Double, Double>?,
    val wifiLocationEstimate: Pair<Double, Double>?,
    val gpsVsCellDistanceKm: Double?,
    val gpsVsWifiDistanceKm: Double?,
    val spoofConfidence: Float,
    val checks: List<SpoofCheck>
)

data class GpsSpoofState(
    val isMonitoring: Boolean = false,
    val spoofDetected: Boolean = false,
    val confidence: Float = 0f,
    val results: List<SpoofCheckResult> = emptyList(),
    val gpsStatus: String = "Idle",
    val cellStatus: String = "Idle",
    val wifiStatus: String = "Idle",
    val sensorStatus: String = "Idle",
    val error: String? = null
)

// ── Detection engine ───────────────────────────────────────────────────────────

class GpsSpoofDetector(private val context: Context) {

    companion object {
        private const val EARTH_RADIUS_KM = 6371.0
        private const val CELL_DISTANCE_THRESHOLD_KM = 50.0
        private const val WIFI_DISTANCE_THRESHOLD_KM = 5.0
        private const val SPEED_THRESHOLD_KMH = 1000.0
        private const val ALTITUDE_MISMATCH_M = 200.0
        private const val MIN_REALISTIC_SATELLITES = 4
        private const val MAX_REALISTIC_SATELLITES = 32
        private const val ACCEL_MOVEMENT_THRESHOLD = 1.5f
    }

    private var previousGps: GpsState? = null
    private var previousTimestamp: Long = 0L
    private var previousBssids: Set<String> = emptySet()

    fun reset() {
        previousGps = null
        previousTimestamp = 0L
        previousBssids = emptySet()
    }

    /**
     * Run all seven spoof checks and produce a result with overall confidence.
     */
    fun analyze(
        gps: GpsState,
        cell: CellTowerInfo?,
        wifiAps: List<WifiAccessPoint>,
        accelerometer: SensorReading,
        barometer: SensorReading
    ): SpoofCheckResult {
        val checks = mutableListOf<SpoofCheck>()

        // ── 1. GPS vs Cell Tower distance ──────────────────────────────────
        val cellLocation = estimateCellLocation(cell)
        val gpsVsCellKm = if (gps.latitude != 0.0 && cellLocation != null) {
            haversine(gps.latitude, gps.longitude, cellLocation.first, cellLocation.second)
        } else null

        checks += SpoofCheck(
            name = "GPS vs Cell Tower",
            passed = gpsVsCellKm == null || gpsVsCellKm < CELL_DISTANCE_THRESHOLD_KM,
            detail = if (gpsVsCellKm != null) {
                "Distance: %.1f km (threshold: %.0f km)".format(gpsVsCellKm, CELL_DISTANCE_THRESHOLD_KM)
            } else "Cell location unavailable"
        )

        // ── 2. Speed anomaly (teleportation) ───────────────────────────────
        val speedAnomaly = checkSpeedAnomaly(gps)
        checks += speedAnomaly

        // ── 3. Altitude consistency (GPS vs barometric) ────────────────────
        val altitudeCheck = checkAltitudeConsistency(gps, barometer)
        checks += altitudeCheck

        // ── 4. Satellite count ─────────────────────────────────────────────
        val satCheck = checkSatelliteCount(gps)
        checks += satCheck

        // ── 5. WiFi BSSID consistency ──────────────────────────────────────
        val wifiCheck = checkWifiConsistency(wifiAps)
        checks += wifiCheck

        // WiFi location estimate (rough centroid from strongest APs)
        val wifiLocationEstimate = estimateWifiLocation(wifiAps, gps)
        val gpsVsWifiKm = if (gps.latitude != 0.0 && wifiLocationEstimate != null) {
            haversine(gps.latitude, gps.longitude, wifiLocationEstimate.first, wifiLocationEstimate.second)
        } else null

        // ── 6. Accelerometer correlation ───────────────────────────────────
        val accelCheck = checkAccelerometerCorrelation(gps, accelerometer)
        checks += accelCheck

        // ── 7. Mock location provider ──────────────────────────────────────
        val mockCheck = checkMockLocation()
        checks += mockCheck

        // ── Calculate overall confidence ───────────────────────────────────
        val failedChecks = checks.count { !it.passed }
        val confidence = (failedChecks.toFloat() / checks.size).coerceIn(0f, 1f)

        // Save state for next iteration
        previousGps = gps
        previousTimestamp = System.currentTimeMillis()
        val currentBssids = wifiAps.map { it.bssid }.toSet()
        if (currentBssids.isNotEmpty()) previousBssids = currentBssids

        return SpoofCheckResult(
            gpsLocation = if (gps.latitude != 0.0) Pair(gps.latitude, gps.longitude) else null,
            cellLocation = cellLocation,
            wifiLocationEstimate = wifiLocationEstimate,
            gpsVsCellDistanceKm = gpsVsCellKm,
            gpsVsWifiDistanceKm = gpsVsWifiKm,
            spoofConfidence = confidence,
            checks = checks
        )
    }

    // ── Individual checks ──────────────────────────────────────────────────────

    private fun checkSpeedAnomaly(gps: GpsState): SpoofCheck {
        val prev = previousGps
        if (prev == null || prev.latitude == 0.0 || gps.latitude == 0.0) {
            return SpoofCheck("Speed Anomaly", true, "No previous fix to compare")
        }

        val dt = System.currentTimeMillis() - previousTimestamp
        if (dt <= 0) {
            return SpoofCheck("Speed Anomaly", true, "Waiting for next reading")
        }

        val distKm = haversine(prev.latitude, prev.longitude, gps.latitude, gps.longitude)
        val dtHours = dt / 3_600_000.0
        val speedKmh = if (dtHours > 0) distKm / dtHours else 0.0

        val passed = speedKmh < SPEED_THRESHOLD_KMH
        return SpoofCheck(
            name = "Speed Anomaly",
            passed = passed,
            detail = "Implied speed: %.0f km/h (threshold: %.0f km/h)".format(speedKmh, SPEED_THRESHOLD_KMH)
        )
    }

    private fun checkAltitudeConsistency(gps: GpsState, barometer: SensorReading): SpoofCheck {
        if (!barometer.isAvailable || barometer.values.isEmpty()) {
            return SpoofCheck("Altitude Consistency", true, "Barometer unavailable")
        }

        val pressureHpa = barometer.values[0]
        if (pressureHpa <= 0f) {
            return SpoofCheck("Altitude Consistency", true, "Invalid barometric reading")
        }

        // Standard barometric formula: altitude = 44330 * (1 - (P/P0)^(1/5.255))
        val baroAltitude = 44330.0 * (1.0 - (pressureHpa / 1013.25).toDouble().pow(1.0 / 5.255))
        val gpsAltitude = gps.altitude
        val diff = abs(gpsAltitude - baroAltitude)

        val passed = diff < ALTITUDE_MISMATCH_M
        return SpoofCheck(
            name = "Altitude Consistency",
            passed = passed,
            detail = "GPS: %.0fm | Baro: %.0fm | Diff: %.0fm".format(gpsAltitude, baroAltitude, diff)
        )
    }

    private fun checkSatelliteCount(gps: GpsState): SpoofCheck {
        val count = gps.satelliteCount
        if (count == 0 && gps.latitude == 0.0) {
            return SpoofCheck("Satellite Count", true, "No GPS fix yet")
        }

        val passed = count in MIN_REALISTIC_SATELLITES..MAX_REALISTIC_SATELLITES
        val detail = when {
            count < MIN_REALISTIC_SATELLITES -> "Only $count satellites used in fix (min expected: $MIN_REALISTIC_SATELLITES)"
            count > MAX_REALISTIC_SATELLITES -> "Unrealistic $count satellites reported (max expected: $MAX_REALISTIC_SATELLITES)"
            else -> "$count satellites used in fix"
        }
        return SpoofCheck("Satellite Count", passed, detail)
    }

    private fun checkWifiConsistency(wifiAps: List<WifiAccessPoint>): SpoofCheck {
        if (wifiAps.isEmpty()) {
            return SpoofCheck("WiFi BSSID Consistency", true, "No WiFi APs visible")
        }

        val currentBssids = wifiAps.map { it.bssid }.toSet()

        if (previousBssids.isEmpty()) {
            return SpoofCheck("WiFi BSSID Consistency", true, "First scan -- baseline set (${currentBssids.size} APs)")
        }

        // Check overlap between previous and current BSSIDs
        val overlap = currentBssids.intersect(previousBssids)
        val totalUnion = currentBssids.union(previousBssids)
        val overlapRatio = if (totalUnion.isNotEmpty()) overlap.size.toFloat() / totalUnion.size else 1f

        // If GPS moved significantly but WiFi APs are completely different, that's expected.
        // If GPS is static but WiFi changed completely, that's suspicious.
        // If GPS teleported but WiFi stayed the same, that's very suspicious.
        val passed = overlapRatio > 0.1f || previousBssids.size < 3
        return SpoofCheck(
            name = "WiFi BSSID Consistency",
            passed = passed,
            detail = "AP overlap: %.0f%% (%d/%d common)".format(
                overlapRatio * 100, overlap.size, totalUnion.size
            )
        )
    }

    private fun checkAccelerometerCorrelation(gps: GpsState, accelerometer: SensorReading): SpoofCheck {
        if (!accelerometer.isAvailable || accelerometer.values.size < 3) {
            return SpoofCheck("Accelerometer Correlation", true, "Accelerometer unavailable")
        }

        val ax = accelerometer.values[0]
        val ay = accelerometer.values[1]
        val az = accelerometer.values[2]
        // Remove gravity (~9.81 m/s2) to detect device movement
        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        val deviation = abs(magnitude - 9.81f)
        val phoneMoving = deviation > ACCEL_MOVEMENT_THRESHOLD

        val gpsMoving = gps.speed > 2.0f // >2 m/s ≈ 7.2 km/h (walking speed)

        // Suspicious: GPS shows fast movement but phone is stationary, or phone shaking but GPS frozen
        val mismatch = (gpsMoving && !phoneMoving) || (!gpsMoving && phoneMoving && gps.speed == 0f && gps.latitude != 0.0)
        // Only flag the GPS-moving-phone-static case as a fail
        val passed = !(gpsMoving && !phoneMoving)

        return SpoofCheck(
            name = "Accelerometer Correlation",
            passed = passed,
            detail = when {
                gpsMoving && !phoneMoving -> "GPS speed: %.1f m/s but phone is stationary (accel deviation: %.2f)".format(gps.speed, deviation)
                gpsMoving && phoneMoving -> "Movement consistent (GPS: %.1f m/s, accel deviation: %.2f)".format(gps.speed, deviation)
                else -> "GPS stationary, accel deviation: %.2f m/s\u00B2".format(deviation)
            }
        )
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun checkMockLocation(): SpoofCheck {
        var mockDetected = false
        val details = mutableListOf<String>()

        // Check 1: Developer option mock location setting (deprecated but still useful on older devices)
        @Suppress("DEPRECATION")
        try {
            val mockSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ALLOW_MOCK_LOCATION
            )
            if (mockSetting == "1") {
                mockDetected = true
                details += "Mock location setting enabled"
            }
        } catch (_: Exception) {
            // Setting may not exist on newer devices
        }

        // Check 2: Check if a mock location provider is registered
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = locationManager.allProviders
            for (provider in providers) {
                try {
                    if (provider != LocationManager.PASSIVE_PROVIDER) {
                        val isEnabled = locationManager.isProviderEnabled(provider)
                        if (provider.contains("mock", ignoreCase = true)) {
                            mockDetected = true
                            details += "Mock provider detected: $provider"
                        }
                    }
                } catch (_: Exception) { }
            }

            // Check test providers via reflection if accessible
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastKnown?.isMock == true) {
                        mockDetected = true
                        details += "Last known GPS location flagged as mock"
                    }
                } catch (_: SecurityException) { }
            }
        } catch (_: Exception) { }

        return SpoofCheck(
            name = "Mock Location Provider",
            passed = !mockDetected,
            detail = if (mockDetected) details.joinToString("; ") else "No mock location providers detected"
        )
    }

    // ── Helper methods ─────────────────────────────────────────────────────────

    /**
     * Rough cell tower location estimate using CID/LAC as a proxy.
     * Real-world implementations would use a cell tower database (OpenCellID, etc.).
     * Here we return null to indicate we can't determine real location from cell info alone,
     * but if the device has a last-known network location we use that.
     */
    private fun estimateCellLocation(cell: CellTowerInfo?): Pair<Double, Double>? {
        if (cell == null) return null
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            @SuppressLint("MissingPermission")
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (networkLocation != null) {
                return Pair(networkLocation.latitude, networkLocation.longitude)
            }
        } catch (_: SecurityException) { }
        return null
    }

    /**
     * Rough WiFi-based location estimate.
     * Uses the network provider as a proxy since real WiFi geolocation requires a BSSID database.
     * Falls back to current GPS if network location is unavailable (returns null in that case).
     */
    private fun estimateWifiLocation(wifiAps: List<WifiAccessPoint>, gps: GpsState): Pair<Double, Double>? {
        if (wifiAps.isEmpty()) return null
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            @SuppressLint("MissingPermission")
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (networkLocation != null) {
                return Pair(networkLocation.latitude, networkLocation.longitude)
            }
        } catch (_: SecurityException) { }
        return null
    }

    /**
     * Haversine distance between two lat/lon points in kilometers.
     */
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c
    }
}

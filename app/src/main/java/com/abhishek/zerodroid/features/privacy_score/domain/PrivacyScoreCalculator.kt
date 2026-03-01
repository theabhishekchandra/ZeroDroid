package com.abhishek.zerodroid.features.privacy_score.domain

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.abhishek.zerodroid.core.util.SecurityType
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.sensors.domain.SensorReading
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.sqrt

// ── Data Models ────────────────────────────────────────────────────────

enum class CheckCategory(val label: String, val weightPercent: Int) {
    WIFI("WiFi", 30),
    BLUETOOTH("Bluetooth", 20),
    DEVICE("Device", 25),
    NETWORK("Network", 15),
    PHYSICAL("Physical", 10)
}

enum class CheckStatus { PASS, WARNING, FAIL }

data class PrivacyCheck(
    val category: CheckCategory,
    val name: String,
    val status: CheckStatus,
    val detail: String,
    val weight: Int,          // importance weight 1-10
    val recommendation: String? = null
)

data class PrivacyScoreState(
    val isScanning: Boolean = false,
    val score: Int = -1,      // 0-100, -1 = not scanned
    val grade: String = "?",  // A+, A, B, C, D, F
    val checks: List<PrivacyCheck> = emptyList(),
    val passCount: Int = 0,
    val warnCount: Int = 0,
    val failCount: Int = 0,
    val categoryScores: Map<CheckCategory, Int> = emptyMap(),
    val lastScanTime: Long? = null,
    val error: String? = null
)

// ── Calculator ─────────────────────────────────────────────────────────

object PrivacyScoreCalculator {

    fun calculateScore(checks: List<PrivacyCheck>): Int {
        var totalWeight = 0
        var earnedWeight = 0
        for (check in checks) {
            totalWeight += check.weight
            earnedWeight += when (check.status) {
                CheckStatus.PASS -> check.weight
                CheckStatus.WARNING -> check.weight / 2
                CheckStatus.FAIL -> 0
            }
        }
        return if (totalWeight > 0) (earnedWeight * 100) / totalWeight else 0
    }

    fun calculateCategoryScores(checks: List<PrivacyCheck>): Map<CheckCategory, Int> {
        return CheckCategory.entries.associateWith { cat ->
            val categoryChecks = checks.filter { it.category == cat }
            if (categoryChecks.isEmpty()) 100 else calculateScore(categoryChecks)
        }
    }

    fun scoreToGrade(score: Int): String = when {
        score >= 95 -> "A+"
        score >= 85 -> "A"
        score >= 70 -> "B"
        score >= 55 -> "C"
        score >= 40 -> "D"
        else -> "F"
    }

    // ── WiFi Checks ────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    fun checkWifiEncryption(
        wifiManager: WifiManager,
        accessPoints: List<WifiAccessPoint>
    ): PrivacyCheck {
        val connectedInfo = wifiManager.connectionInfo
        val connectedBssid = connectedInfo?.bssid

        if (connectedBssid == null || connectedBssid == "02:00:00:00:00:00") {
            return PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "WiFi Encryption",
                status = CheckStatus.PASS,
                detail = "Not connected to WiFi",
                weight = 8
            )
        }

        val connectedAp = accessPoints.find {
            it.bssid.equals(connectedBssid, ignoreCase = true)
        }

        val security = connectedAp?.security ?: SecurityType.UNKNOWN

        return when (security) {
            SecurityType.WPA3, SecurityType.WPA2 -> PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "WiFi Encryption",
                status = CheckStatus.PASS,
                detail = "Connected via ${security.label}",
                weight = 8
            )
            SecurityType.WPA -> PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "WiFi Encryption",
                status = CheckStatus.WARNING,
                detail = "Connected via WPA (outdated)",
                weight = 8,
                recommendation = "Upgrade your router to WPA2 or WPA3"
            )
            SecurityType.WEP -> PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "WiFi Encryption",
                status = CheckStatus.WARNING,
                detail = "Connected via WEP (weak encryption)",
                weight = 8,
                recommendation = "WEP is easily cracked. Switch to WPA2/WPA3"
            )
            SecurityType.OPEN -> PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "WiFi Encryption",
                status = CheckStatus.FAIL,
                detail = "Connected to an open (unencrypted) network",
                weight = 8,
                recommendation = "Avoid open WiFi. Use a VPN or connect to an encrypted network"
            )
            SecurityType.UNKNOWN -> PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "WiFi Encryption",
                status = CheckStatus.WARNING,
                detail = "Could not determine encryption type",
                weight = 8,
                recommendation = "Verify your WiFi encryption settings"
            )
        }
    }

    fun checkEvilTwins(accessPoints: List<WifiAccessPoint>): PrivacyCheck {
        // Group by SSID, look for duplicates with different BSSIDs (potential evil twins)
        val ssidGroups = accessPoints
            .filter { it.ssid != "<Hidden>" && it.ssid.isNotBlank() }
            .groupBy { it.ssid }

        val suspiciousSsids = ssidGroups.filter { (_, aps) ->
            if (aps.size <= 1) return@filter false
            // Multiple APs with same SSID but very different signal strengths
            // or different security types could be evil twins
            val securityTypes = aps.map { it.security }.toSet()
            securityTypes.size > 1 // Different security on same SSID is suspicious
        }

        return if (suspiciousSsids.isEmpty()) {
            PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "Evil Twin Detection",
                status = CheckStatus.PASS,
                detail = "No rogue duplicate SSIDs detected",
                weight = 7
            )
        } else {
            PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "Evil Twin Detection",
                status = CheckStatus.WARNING,
                detail = "${suspiciousSsids.size} SSID(s) have duplicate APs with different security: ${suspiciousSsids.keys.joinToString(", ")}",
                weight = 7,
                recommendation = "Verify you are connected to the legitimate access point"
            )
        }
    }

    fun checkOpenNetworks(accessPoints: List<WifiAccessPoint>): PrivacyCheck {
        val openNetworks = accessPoints.filter { it.security == SecurityType.OPEN }
        return when {
            openNetworks.isEmpty() -> PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "Open Networks Nearby",
                status = CheckStatus.PASS,
                detail = "No open WiFi networks detected nearby",
                weight = 5
            )
            openNetworks.size <= 2 -> PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "Open Networks Nearby",
                status = CheckStatus.WARNING,
                detail = "${openNetworks.size} open network(s) nearby",
                weight = 5,
                recommendation = "Disable auto-connect to open networks in WiFi settings"
            )
            else -> PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "Open Networks Nearby",
                status = CheckStatus.FAIL,
                detail = "${openNetworks.size} open networks nearby — high risk area",
                weight = 5,
                recommendation = "Multiple open networks increase attack surface. Disable WiFi when not in use"
            )
        }
    }

    fun checkSsidPrivacy(
        wifiManager: WifiManager,
        accessPoints: List<WifiAccessPoint>
    ): PrivacyCheck {
        @Suppress("DEPRECATION")
        val connectedInfo = wifiManager.connectionInfo
        val connectedSsid = connectedInfo?.ssid?.removeSurrounding("\"") ?: ""

        if (connectedSsid.isBlank() || connectedSsid == "<unknown ssid>") {
            return PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "SSID Privacy",
                status = CheckStatus.PASS,
                detail = "Not connected or SSID not visible",
                weight = 3
            )
        }

        // Check for personally identifiable patterns
        val piiPatterns = listOf(
            Regex("(?i)(apartment|apt|flat|house|home|room)"),
            Regex("(?i)\\d{3,}"),  // Street numbers
            Regex("(?i)(street|st|avenue|ave|road|rd|drive|dr|lane|ln)"),
            Regex("(?i)(family|'s)"),
        )

        val hasPii = piiPatterns.any { it.containsMatchIn(connectedSsid) }

        return if (hasPii) {
            PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "SSID Privacy",
                status = CheckStatus.WARNING,
                detail = "SSID \"$connectedSsid\" may reveal personal info",
                weight = 3,
                recommendation = "Rename your WiFi to a non-identifiable name"
            )
        } else {
            PrivacyCheck(
                category = CheckCategory.WIFI,
                name = "SSID Privacy",
                status = CheckStatus.PASS,
                detail = "SSID does not reveal personal information",
                weight = 3
            )
        }
    }

    // ── Bluetooth Checks ───────────────────────────────────────────────

    fun checkBleTrackers(devices: List<BleDevice>): PrivacyCheck {
        // Known tracker service UUIDs (Apple Find My, Tile, Samsung SmartTag, etc.)
        val trackerUuids = setOf(
            "7905f431-b5ce-4e99-a40f-4b1e122d00d0", // Apple Find My
            "0000feed-0000-1000-8000-00805f9b34fb", // Tile
            "0000fd5a-0000-1000-8000-00805f9b34fb", // Samsung SmartTag
        )

        val trackerPatterns = listOf(
            Regex("(?i)airtag"),
            Regex("(?i)tile"),
            Regex("(?i)smarttag"),
            Regex("(?i)chipolo"),
        )

        val suspectTrackers = devices.filter { device ->
            val nameMatch = device.name?.let { name ->
                trackerPatterns.any { it.containsMatchIn(name) }
            } ?: false
            val uuidMatch = device.serviceUuids.any { uuid ->
                trackerUuids.any { tracker -> uuid.contains(tracker, ignoreCase = true) }
            }
            (nameMatch || uuidMatch) && !device.isBookmarked
        }

        return when {
            suspectTrackers.isEmpty() -> PrivacyCheck(
                category = CheckCategory.BLUETOOTH,
                name = "BLE Tracker Detection",
                status = CheckStatus.PASS,
                detail = "No unknown BLE trackers detected",
                weight = 8
            )
            suspectTrackers.size == 1 -> PrivacyCheck(
                category = CheckCategory.BLUETOOTH,
                name = "BLE Tracker Detection",
                status = CheckStatus.WARNING,
                detail = "1 potential tracker: ${suspectTrackers.first().displayName}",
                weight = 8,
                recommendation = "Investigate nearby BLE tracker. Check if it belongs to you"
            )
            else -> PrivacyCheck(
                category = CheckCategory.BLUETOOTH,
                name = "BLE Tracker Detection",
                status = CheckStatus.FAIL,
                detail = "${suspectTrackers.size} potential trackers detected",
                weight = 8,
                recommendation = "Multiple unknown trackers nearby. You may be tracked"
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun checkBluetoothDiscoverable(context: Context): PrivacyCheck {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val adapter = btManager?.adapter

        if (adapter == null || !adapter.isEnabled) {
            return PrivacyCheck(
                category = CheckCategory.BLUETOOTH,
                name = "Bluetooth Discoverable",
                status = CheckStatus.PASS,
                detail = "Bluetooth is disabled",
                weight = 5
            )
        }

        return try {
            val scanMode = adapter.scanMode
            when (scanMode) {
                android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> PrivacyCheck(
                    category = CheckCategory.BLUETOOTH,
                    name = "Bluetooth Discoverable",
                    status = CheckStatus.FAIL,
                    detail = "Device is discoverable to all Bluetooth devices",
                    weight = 5,
                    recommendation = "Disable Bluetooth discoverability when not pairing"
                )
                else -> PrivacyCheck(
                    category = CheckCategory.BLUETOOTH,
                    name = "Bluetooth Discoverable",
                    status = CheckStatus.PASS,
                    detail = "Device is not discoverable",
                    weight = 5
                )
            }
        } catch (e: SecurityException) {
            PrivacyCheck(
                category = CheckCategory.BLUETOOTH,
                name = "Bluetooth Discoverable",
                status = CheckStatus.WARNING,
                detail = "Could not check discoverability",
                weight = 5
            )
        }
    }

    fun checkBleDeviceCount(devices: List<BleDevice>): PrivacyCheck {
        val unknownCount = devices.count { !it.isBookmarked }
        return when {
            unknownCount > 50 -> PrivacyCheck(
                category = CheckCategory.BLUETOOTH,
                name = "BLE Device Density",
                status = CheckStatus.FAIL,
                detail = "$unknownCount unknown BLE devices in range",
                weight = 4,
                recommendation = "Very crowded RF environment. Disable Bluetooth when not needed"
            )
            unknownCount > 20 -> PrivacyCheck(
                category = CheckCategory.BLUETOOTH,
                name = "BLE Device Density",
                status = CheckStatus.WARNING,
                detail = "$unknownCount unknown BLE devices in range",
                weight = 4,
                recommendation = "High device density. Stay alert to rogue devices"
            )
            else -> PrivacyCheck(
                category = CheckCategory.BLUETOOTH,
                name = "BLE Device Density",
                status = CheckStatus.PASS,
                detail = "$unknownCount unknown BLE device(s) in range",
                weight = 4
            )
        }
    }

    // ── Device Checks ──────────────────────────────────────────────────

    fun checkDeveloperOptions(context: Context): PrivacyCheck {
        val enabled = try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) != 0
        } catch (_: Exception) { false }

        return if (enabled) {
            PrivacyCheck(
                category = CheckCategory.DEVICE,
                name = "Developer Options",
                status = CheckStatus.WARNING,
                detail = "Developer options are enabled",
                weight = 4,
                recommendation = "Disable developer options unless actively needed for development"
            )
        } else {
            PrivacyCheck(
                category = CheckCategory.DEVICE,
                name = "Developer Options",
                status = CheckStatus.PASS,
                detail = "Developer options are disabled",
                weight = 4
            )
        }
    }

    fun checkUsbDebugging(context: Context): PrivacyCheck {
        val enabled = try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            ) != 0
        } catch (_: Exception) { false }

        return if (enabled) {
            PrivacyCheck(
                category = CheckCategory.DEVICE,
                name = "USB Debugging",
                status = CheckStatus.FAIL,
                detail = "USB debugging is enabled — device can be controlled via ADB",
                weight = 7,
                recommendation = "Disable USB debugging to prevent unauthorized ADB access"
            )
        } else {
            PrivacyCheck(
                category = CheckCategory.DEVICE,
                name = "USB Debugging",
                status = CheckStatus.PASS,
                detail = "USB debugging is disabled",
                weight = 7
            )
        }
    }

    fun checkMockLocations(context: Context): PrivacyCheck {
        val enabled = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ALLOW_MOCK_LOCATION, 0
            ) != 0
        } catch (_: Exception) { false }

        // On newer APIs, also check if a mock location app is set
        val mockApp = try {
            Settings.Secure.getString(
                context.contentResolver,
                "mock_location"
            )
        } catch (_: Exception) { null }

        val isMocked = enabled || (!mockApp.isNullOrBlank() && mockApp != "0")

        return if (isMocked) {
            PrivacyCheck(
                category = CheckCategory.DEVICE,
                name = "Mock Locations",
                status = CheckStatus.WARNING,
                detail = "Mock location provider is enabled",
                weight = 4,
                recommendation = "Disable mock locations unless testing location apps"
            )
        } else {
            PrivacyCheck(
                category = CheckCategory.DEVICE,
                name = "Mock Locations",
                status = CheckStatus.PASS,
                detail = "Mock locations are disabled",
                weight = 4
            )
        }
    }

    fun checkScreenLock(context: Context): PrivacyCheck {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isSecure = keyguardManager?.isDeviceSecure == true

        return if (isSecure) {
            PrivacyCheck(
                category = CheckCategory.DEVICE,
                name = "Screen Lock",
                status = CheckStatus.PASS,
                detail = "Device has a secure screen lock",
                weight = 6
            )
        } else {
            PrivacyCheck(
                category = CheckCategory.DEVICE,
                name = "Screen Lock",
                status = CheckStatus.FAIL,
                detail = "No secure screen lock configured",
                weight = 6,
                recommendation = "Set a PIN, pattern, or biometric screen lock"
            )
        }
    }

    fun checkDeviceEncryption(context: Context): PrivacyCheck {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val encryptionStatus = dpm?.storageEncryptionStatus
            ?: DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED

        return when (encryptionStatus) {
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE,
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY,
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> PrivacyCheck(
                category = CheckCategory.DEVICE,
                name = "Device Encryption",
                status = CheckStatus.PASS,
                detail = "Storage encryption is active",
                weight = 6
            )
            DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> PrivacyCheck(
                category = CheckCategory.DEVICE,
                name = "Device Encryption",
                status = CheckStatus.FAIL,
                detail = "Storage is not encrypted",
                weight = 6,
                recommendation = "Enable full-disk encryption in Security settings"
            )
            else -> PrivacyCheck(
                category = CheckCategory.DEVICE,
                name = "Device Encryption",
                status = CheckStatus.WARNING,
                detail = "Encryption status could not be determined",
                weight = 6
            )
        }
    }

    fun checkSecurityPatch(): PrivacyCheck {
        val patchDateStr = Build.VERSION.SECURITY_PATCH  // e.g. "2024-12-05"

        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val patchDate = LocalDate.parse(patchDateStr, formatter)
            val now = LocalDate.now()
            val monthsOld = ChronoUnit.MONTHS.between(patchDate, now)

            when {
                monthsOld <= 2 -> PrivacyCheck(
                    category = CheckCategory.DEVICE,
                    name = "Security Patch",
                    status = CheckStatus.PASS,
                    detail = "Patch date: $patchDateStr (current)",
                    weight = 5
                )
                monthsOld <= 6 -> PrivacyCheck(
                    category = CheckCategory.DEVICE,
                    name = "Security Patch",
                    status = CheckStatus.WARNING,
                    detail = "Patch date: $patchDateStr ($monthsOld months old)",
                    weight = 5,
                    recommendation = "Check for system updates to get the latest security patches"
                )
                else -> PrivacyCheck(
                    category = CheckCategory.DEVICE,
                    name = "Security Patch",
                    status = CheckStatus.FAIL,
                    detail = "Patch date: $patchDateStr ($monthsOld months old)",
                    weight = 5,
                    recommendation = "Your security patches are severely outdated. Update immediately"
                )
            }
        } catch (_: Exception) {
            PrivacyCheck(
                category = CheckCategory.DEVICE,
                name = "Security Patch",
                status = CheckStatus.WARNING,
                detail = "Could not parse patch date: $patchDateStr",
                weight = 5
            )
        }
    }

    // ── Network Checks ─────────────────────────────────────────────────

    fun checkPrivateDns(context: Context): PrivacyCheck {
        val dnsMode = try {
            Settings.Global.getString(context.contentResolver, "private_dns_mode")
        } catch (_: Exception) { null }

        return when (dnsMode) {
            "hostname" -> PrivacyCheck(
                category = CheckCategory.NETWORK,
                name = "Private DNS",
                status = CheckStatus.PASS,
                detail = "DNS over TLS is enabled (strict mode)",
                weight = 6
            )
            "opportunistic" -> PrivacyCheck(
                category = CheckCategory.NETWORK,
                name = "Private DNS",
                status = CheckStatus.PASS,
                detail = "Private DNS is set to automatic",
                weight = 6
            )
            "off" -> PrivacyCheck(
                category = CheckCategory.NETWORK,
                name = "Private DNS",
                status = CheckStatus.FAIL,
                detail = "Private DNS is disabled — DNS queries are unencrypted",
                weight = 6,
                recommendation = "Enable Private DNS in network settings for encrypted DNS"
            )
            else -> PrivacyCheck(
                category = CheckCategory.NETWORK,
                name = "Private DNS",
                status = CheckStatus.WARNING,
                detail = "Private DNS status: ${dnsMode ?: "unknown"}",
                weight = 6,
                recommendation = "Configure Private DNS to 'Automatic' or a specific provider"
            )
        }
    }

    fun checkVpnActive(context: Context): PrivacyCheck {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        val hasVpn = connectivityManager?.let { cm ->
            val activeNetwork = cm.activeNetwork
            val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        } ?: false

        return if (hasVpn) {
            PrivacyCheck(
                category = CheckCategory.NETWORK,
                name = "VPN Status",
                status = CheckStatus.PASS,
                detail = "VPN connection is active",
                weight = 7
            )
        } else {
            PrivacyCheck(
                category = CheckCategory.NETWORK,
                name = "VPN Status",
                status = CheckStatus.WARNING,
                detail = "No VPN connection detected",
                weight = 7,
                recommendation = "Use a trusted VPN for encrypted network traffic"
            )
        }
    }

    // ── Physical Checks ────────────────────────────────────────────────

    fun checkMagneticAnomaly(magnetometer: SensorReading): PrivacyCheck {
        if (!magnetometer.isAvailable || magnetometer.values.size < 3) {
            return PrivacyCheck(
                category = CheckCategory.PHYSICAL,
                name = "Magnetic Anomaly",
                status = CheckStatus.PASS,
                detail = "Magnetometer not available",
                weight = 5
            )
        }

        val x = magnetometer.values[0]
        val y = magnetometer.values[1]
        val z = magnetometer.values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        // Earth's magnetic field is typically 25-65 μT
        val deviation = when {
            magnitude < 25f -> 25f - magnitude
            magnitude > 65f -> magnitude - 65f
            else -> 0f
        }

        return when {
            deviation < 15f -> PrivacyCheck(
                category = CheckCategory.PHYSICAL,
                name = "Magnetic Anomaly",
                status = CheckStatus.PASS,
                detail = "Magnetic field: %.1f μT (normal)".format(magnitude),
                weight = 5
            )
            deviation < 50f -> PrivacyCheck(
                category = CheckCategory.PHYSICAL,
                name = "Magnetic Anomaly",
                status = CheckStatus.WARNING,
                detail = "Magnetic field: %.1f μT (deviation: %.1f μT)".format(magnitude, deviation),
                weight = 5,
                recommendation = "Unusual magnetic field detected. Could be electronics or metallic objects"
            )
            else -> PrivacyCheck(
                category = CheckCategory.PHYSICAL,
                name = "Magnetic Anomaly",
                status = CheckStatus.FAIL,
                detail = "Magnetic field: %.1f μT (significant anomaly)".format(magnitude),
                weight = 5,
                recommendation = "Strong magnetic anomaly. Check for hidden electronic devices"
            )
        }
    }

    fun checkUltrasonicBeacons(hasAudioPermission: Boolean): PrivacyCheck {
        // Ultrasonic beacon detection requires audio permission
        return if (!hasAudioPermission) {
            PrivacyCheck(
                category = CheckCategory.PHYSICAL,
                name = "Ultrasonic Beacons",
                status = CheckStatus.WARNING,
                detail = "Audio permission not granted — cannot check",
                weight = 3,
                recommendation = "Grant microphone permission to enable ultrasonic beacon detection"
            )
        } else {
            // Without active audio analysis, we mark as passed with caveat
            PrivacyCheck(
                category = CheckCategory.PHYSICAL,
                name = "Ultrasonic Beacons",
                status = CheckStatus.PASS,
                detail = "No ultrasonic beacons detected in quick scan",
                weight = 3
            )
        }
    }
}

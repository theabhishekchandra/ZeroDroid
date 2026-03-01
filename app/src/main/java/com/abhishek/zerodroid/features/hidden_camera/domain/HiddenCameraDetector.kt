package com.abhishek.zerodroid.features.hidden_camera.domain

import android.content.Context
import android.net.wifi.WifiManager
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sqrt

// --- Data Models ---

enum class DetectionSource { IR, WIFI, BLE, MAGNETIC, NETWORK }

enum class ThreatLevel { HIGH, MEDIUM, LOW }

data class CameraDetection(
    val id: String = UUID.randomUUID().toString(),
    val source: DetectionSource,
    val threatLevel: ThreatLevel,
    val title: String,
    val detail: String,
    val rssi: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class HiddenCameraScanState(
    val isScanning: Boolean = false,
    val activeMode: DetectionSource? = null,
    val detections: List<CameraDetection> = emptyList(),
    val irActive: Boolean = false,
    val wifiSuspects: Int = 0,
    val bleSuspects: Int = 0,
    val magneticAnomaly: Boolean = false,
    val networkSuspects: Int = 0,
    val networkScanProgress: String? = null,
    val error: String? = null
)

// --- Core Detector ---

class HiddenCameraDetector(private val context: Context) {

    companion object {
        // Known camera manufacturer OUI prefixes (first 3 octets, uppercase)
        private val CAMERA_OUI_PREFIXES = setOf(
            // Hikvision
            "C0:56:E3", "44:19:B6", "C4:2F:90", "54:C4:15",
            // Dahua
            "3C:EF:8C", "A0:BD:1D", "E0:50:8B",
            // Wyze
            "2C:AA:8E", "7C:78:B2",
            // TP-Link (cameras)
            "50:C7:BF", "60:32:B1", "B0:BE:76",
            // Reolink
            "EC:71:DB",
            // Amcrest
            "9C:8E:CD",
            // Ring
            "34:3E:A4", "0C:47:2D",
            // Nest / Google
            "18:B4:30", "64:16:66",
            // Xiaomi / Yi
            "78:11:DC", "64:09:80", "28:6C:07",
            // Eufy
            "98:F4:AB",
            // Arlo
            "00:1A:07", "9C:B7:0D"
        )

        private val BLE_CAMERA_PATTERN = Regex(
            "(?i).*(cam|camera|ipcam|dvr|nvr|cctv|spy|wyze|blink|arlo|ring|nest|eufy|reolink|amcrest|hikvision|dahua|yi\\s*home).*"
        )

        private val WIFI_CAMERA_SSID_PATTERN = Regex(
            "(?i).*(cam|camera|ipcam|dvr|nvr|cctv|spy|wyze|blink|arlo|ring|nest|eufy|reolink|amcrest|hikvision|dahua|yi\\s*home|esp32|esp-cam|wificam).*"
        )

        // Camera-related network ports
        val CAMERA_PORTS = listOf(554, 80, 8080, 8554, 3702, 443)

        // Magnetic anomaly threshold in μT
        const val MAGNETIC_ANOMALY_THRESHOLD = 15f
    }

    /** Check if a WiFi AP's BSSID matches a known camera manufacturer OUI */
    fun matchWifiOui(ap: WifiAccessPoint): CameraDetection? {
        val oui = ap.bssid.uppercase().take(8) // "XX:XX:XX"
        if (oui in CAMERA_OUI_PREFIXES) {
            return CameraDetection(
                source = DetectionSource.WIFI,
                threatLevel = ThreatLevel.HIGH,
                title = "Camera Manufacturer WiFi AP",
                detail = "SSID: ${ap.ssid.ifEmpty { "(hidden)" }}  MAC: ${ap.bssid}  OUI match",
                rssi = ap.rssi
            )
        }
        return null
    }

    /** Check if a WiFi AP's SSID matches camera-related keywords */
    fun matchWifiSsid(ap: WifiAccessPoint): CameraDetection? {
        if (ap.ssid.isNotEmpty() && WIFI_CAMERA_SSID_PATTERN.matches(ap.ssid)) {
            return CameraDetection(
                source = DetectionSource.WIFI,
                threatLevel = ThreatLevel.MEDIUM,
                title = "Suspicious WiFi SSID",
                detail = "SSID: ${ap.ssid}  MAC: ${ap.bssid}  Name pattern match",
                rssi = ap.rssi
            )
        }
        return null
    }

    /** Check if a BLE device name matches camera-related patterns */
    fun matchBleDevice(device: BleDevice): CameraDetection? {
        val name = device.name ?: return null
        if (BLE_CAMERA_PATTERN.matches(name)) {
            return CameraDetection(
                source = DetectionSource.BLE,
                threatLevel = ThreatLevel.MEDIUM,
                title = "Suspicious BLE Device",
                detail = "Name: $name  Address: ${device.address}",
                rssi = device.rssi
            )
        }
        return null
    }

    /** Check if a BLE device's OUI matches camera manufacturers */
    fun matchBleOui(device: BleDevice): CameraDetection? {
        val oui = device.address.uppercase().take(8)
        if (oui in CAMERA_OUI_PREFIXES) {
            return CameraDetection(
                source = DetectionSource.BLE,
                threatLevel = ThreatLevel.HIGH,
                title = "Camera Manufacturer BLE Device",
                detail = "Name: ${device.displayName}  Address: ${device.address}  OUI match",
                rssi = device.rssi
            )
        }
        return null
    }

    /** Check magnetometer deviation against threshold */
    fun checkMagneticAnomaly(deviation: Float): CameraDetection? {
        if (abs(deviation) > MAGNETIC_ANOMALY_THRESHOLD) {
            return CameraDetection(
                source = DetectionSource.MAGNETIC,
                threatLevel = ThreatLevel.LOW,
                title = "Magnetic Anomaly Detected",
                detail = "Deviation: ${"%.1f".format(abs(deviation))} μT from baseline (threshold: ${MAGNETIC_ANOMALY_THRESHOLD} μT)"
            )
        }
        return null
    }

    /** Get the local subnet base IP (e.g. "192.168.1") */
    fun getLocalSubnet(): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val dhcp = wifiManager?.dhcpInfo ?: return null
        val ip = dhcp.ipAddress
        if (ip == 0) return null
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}"
    }

    /** Probe a single host:port for open camera-related ports */
    suspend fun probePort(ip: String, port: Int, timeoutMs: Int = 500): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), timeoutMs)
                    true
                }
            } catch (_: Exception) {
                false
            }
        }

    /** Scan a single IP for camera ports, return detection if any found */
    suspend fun scanHost(ip: String): CameraDetection? {
        val openPorts = mutableListOf<Int>()
        for (port in CAMERA_PORTS) {
            if (probePort(ip, port)) {
                openPorts.add(port)
            }
        }
        if (openPorts.isEmpty()) return null

        val threatLevel = when {
            554 in openPorts || 8554 in openPorts -> ThreatLevel.HIGH // RTSP = very likely camera
            3702 in openPorts -> ThreatLevel.HIGH // ONVIF = camera discovery
            openPorts.size >= 2 -> ThreatLevel.MEDIUM
            else -> ThreatLevel.LOW
        }

        return CameraDetection(
            source = DetectionSource.NETWORK,
            threatLevel = threatLevel,
            title = "Camera Ports Open",
            detail = "IP: $ip  Ports: ${openPorts.joinToString(", ")} ${portLabels(openPorts)}"
        )
    }

    private fun portLabels(ports: List<Int>): String {
        val labels = ports.mapNotNull { port ->
            when (port) {
                554 -> "RTSP"
                8554 -> "RTSP-Alt"
                3702 -> "ONVIF"
                80 -> "HTTP"
                8080 -> "HTTP-Alt"
                443 -> "HTTPS"
                else -> null
            }
        }
        return if (labels.isNotEmpty()) "(${labels.joinToString(", ")})" else ""
    }
}

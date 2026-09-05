package com.abhishek.zerodroid.core.debug

import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.alerts.AlertSource
import com.abhishek.zerodroid.core.alerts.UnifiedAlert
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.bluetooth_classic.domain.ClassicBluetoothDevice
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.DetectedTracker
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerType
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackingRisk
import com.abhishek.zerodroid.features.celltower.domain.AlertSeverity as CellAlertSeverity
import com.abhishek.zerodroid.features.celltower.domain.AlertType as CellAlertType
import com.abhishek.zerodroid.features.celltower.domain.CellTowerInfo
import com.abhishek.zerodroid.features.celltower.domain.CellType
import com.abhishek.zerodroid.features.celltower.domain.ImsiCatcherAlert
import com.abhishek.zerodroid.features.deauth_detector.domain.AlertLevel
import com.abhishek.zerodroid.features.deauth_detector.domain.ApSnapshot
import com.abhishek.zerodroid.features.deauth_detector.domain.AttackType
import com.abhishek.zerodroid.features.deauth_detector.domain.DeauthEvent
import com.abhishek.zerodroid.features.gps.domain.GpsState
import com.abhishek.zerodroid.features.gps.domain.SatelliteInfo
import com.abhishek.zerodroid.features.hidden_camera.domain.CameraDetection
import com.abhishek.zerodroid.features.hidden_camera.domain.DetectionSource
import com.abhishek.zerodroid.features.hidden_camera.domain.ThreatLevel as CameraThreatLevel
import com.abhishek.zerodroid.features.ir.domain.IrProtocol
import com.abhishek.zerodroid.features.ir.domain.IrSignal
import com.abhishek.zerodroid.features.network_scanner.domain.NetworkDevice
import com.abhishek.zerodroid.features.network_scanner.domain.OpenPort
import com.abhishek.zerodroid.features.network_scanner.domain.ServiceType
import com.abhishek.zerodroid.features.network_scanner.domain.Vulnerability
import com.abhishek.zerodroid.features.network_scanner.domain.VulnerabilityLevel
import com.abhishek.zerodroid.features.nfc.domain.NdefContentType
import com.abhishek.zerodroid.features.nfc.domain.NdefParsedContent
import com.abhishek.zerodroid.features.nfc.domain.NfcTagInfo
import com.abhishek.zerodroid.features.proximity_radar.domain.DeviceCategory
import com.abhishek.zerodroid.features.proximity_radar.domain.RadarDevice
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.BugDetection
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.BugType
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.ThreatSeverity
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.ApThreatType
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RiskLevel
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RogueApAlert
import com.abhishek.zerodroid.features.sdr.domain.SdrDeviceInfo
import com.abhishek.zerodroid.features.signal_logger.domain.SignalLogEntry
import com.abhishek.zerodroid.features.signal_logger.domain.SignalType
import com.abhishek.zerodroid.features.usb.domain.UsbDeviceInfo
import com.abhishek.zerodroid.features.usb.domain.UsbEndpointInfo
import com.abhishek.zerodroid.features.usb.domain.UsbInterfaceInfo
import com.abhishek.zerodroid.features.usbcamera.domain.UsbCameraInfo
import com.abhishek.zerodroid.features.usbcamera.domain.UsbVideoDevice
import com.abhishek.zerodroid.features.uwb.domain.UwbDeviceInfo
import com.abhishek.zerodroid.features.uwb.domain.UwbRangingMeasurement
import com.abhishek.zerodroid.features.uwb.domain.UwbSessionConfig
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingRecord
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingStats
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.features.wifiaware.domain.WifiAwarePeer

/**
 * Representative sample data for every hardware-dependent screen, used only
 * through [DemoDataBus] in debug builds. Values are plausible but fictional.
 */
object DemoData {

    object Routes {
        const val BLE = "ble"
        const val NFC = "nfc"
        const val IR = "ir"
        const val UWB = "uwb"
        const val WIFI_AWARE = "wifi_aware"
        const val SDR = "sdr"
        const val USB_CAMERA = "usb_camera"
        const val USB = "usb"
        const val BLUETOOTH_CLASSIC = "bluetooth_classic"
        const val BLUETOOTH_TRACKER = "bluetooth_tracker"
        const val HIDDEN_CAMERA = "hidden_camera"
        const val ROGUE_AP = "rogue_ap"
        const val NETWORK_SCANNER = "network_scanner"
        const val RF_BUG_SWEEPER = "rf_bug_sweeper"
        const val DEAUTH = "deauth_detector"
        const val SIGNAL_LOGGER = "signal_logger"
        const val WARDRIVING = "wardriving"
        const val PROXIMITY_RADAR = "proximity_radar"
        const val CELL_TOWER = "cell_tower"
        const val GPS = "gps"
        const val ALERT_CENTER = "alert_center"
    }

    val supportedRoutes: Set<String> = setOf(
        Routes.BLE, Routes.NFC, Routes.IR, Routes.UWB, Routes.WIFI_AWARE, Routes.SDR, Routes.USB_CAMERA,
        Routes.USB, Routes.BLUETOOTH_CLASSIC, Routes.BLUETOOTH_TRACKER, Routes.HIDDEN_CAMERA, Routes.ROGUE_AP,
        Routes.NETWORK_SCANNER, Routes.RF_BUG_SWEEPER, Routes.DEAUTH, Routes.SIGNAL_LOGGER, Routes.WARDRIVING,
        Routes.PROXIMITY_RADAR, Routes.CELL_TOWER, Routes.GPS, Routes.ALERT_CENTER
    )

    private val now: Long get() = System.currentTimeMillis()
    private const val MIN = 60_000L

    private fun ap(ssid: String, bssid: String, rssi: Int, freq: Int, caps: String) =
        WifiAccessPoint(ssid = ssid, bssid = bssid, rssi = rssi, frequency = freq, capabilities = caps)

    // ── BLE ─────────────────────────────────────────────────────────────────
    val bleDevices: List<BleDevice> = listOf(
        BleDevice("Galaxy Buds2 Pro", "5C:F3:70:A1:02:9B", -48, listOf("0000fe2c-0000-1000-8000-00805f9b34fb")),
        BleDevice("Mi Band 7", "C8:47:8C:12:34:56", -63, listOf("0000fee0-0000-1000-8000-00805f9b34fb")),
        BleDevice(null, "7D:1E:AA:40:9C:02", -71, listOf("7dfc9000-7d1c-4951-86aa-8d9728f8d66c")),
        BleDevice("Tile Pro", "E4:B0:21:77:0A:1F", -80, listOf("0000feed-0000-1000-8000-00805f9b34fb"), isBookmarked = true),
        BleDevice("ESP32_BT", "24:6F:28:9A:B3:C1", -55)
    )

    // ── NFC ─────────────────────────────────────────────────────────────────
    val nfcTag = NfcTagInfo(
        uid = "04:A3:2F:1B:6C:5D:80",
        techList = listOf("android.nfc.tech.NfcA", "android.nfc.tech.MifareUltralight", "android.nfc.tech.Ndef"),
        atqa = "00 44",
        sak = "00",
        tagType = "NTAG215 (MIFARE Ultralight)",
        ndefMessages = listOf(
            NdefParsedContent(NdefContentType.URI, "https://zerodroid.dev/tag/demo", "U"),
            NdefParsedContent(NdefContentType.TEXT, "Lab access badge #42", "T")
        )
    )

    // ── IR ──────────────────────────────────────────────────────────────────
    val irSignals: List<IrSignal> = listOf(
        IrSignal(IrProtocol.NEC, 38000, "20DF10EF", "LG Power"),
        IrSignal(IrProtocol.SAMSUNG32, 38000, "E0E040BF", "Samsung Power"),
        IrSignal(IrProtocol.RAW, 38000, "", "AC Cool 24C", intArrayOf(9000, 4500, 560, 560, 560, 1690, 560, 560))
    )

    // ── UWB ─────────────────────────────────────────────────────────────────
    val uwbDeviceInfo = UwbDeviceInfo(true, "NXP SR100T (demo)", listOf("Distance", "Azimuth", "Elevation", "FiRa 1.1"))
    val uwbSession = UwbSessionConfig("1A2B", 421337, "0102030405060708", 9, 10)
    val uwbMeasurement = UwbRangingMeasurement(distanceMeters = 2.34f, azimuthDegrees = -18f, elevationDegrees = 4f)

    // ── Wi-Fi Aware ─────────────────────────────────────────────────────────
    val awarePeers: List<WifiAwarePeer> = listOf(
        WifiAwarePeer("peer-1", "zerodroid", "lab-phone"),
        WifiAwarePeer("peer-2", "zerodroid", null)
    )

    // ── SDR / USB ───────────────────────────────────────────────────────────
    val sdrDevices: List<SdrDeviceInfo> = listOf(
        SdrDeviceInfo(0x0BDA, 0x2838, "RTL2838UHIDIR", "RTL2838UHIDIR (RTL-SDR v3)", isRtlSdr = true),
        SdrDeviceInfo(0x1D50, 0x604B, "HackRF One", "HackRF One", isRtlSdr = false)
    )
    val usbVideoDevices: List<UsbVideoDevice> = listOf(UsbVideoDevice(0x046D, 0x0825, "HD Webcam C270", "Logitech"))
    val usbCameraInfos: List<UsbCameraInfo> = listOf(
        UsbCameraInfo("2", true, "External Camera #2", 0x046D, 0x0825, listOf("1280x720", "640x480"))
    )
    val usbDevices: List<UsbDeviceInfo> = listOf(
        UsbDeviceInfo(
            0x1FC9, 0x0083, "/dev/bus/usb/001/004", null, null, 0, 0, 2,
            listOf(
                UsbInterfaceInfo(0, 3, 1, 1, 1, listOf(UsbEndpointInfo(0x81, "IN", "Interrupt", 8))),
                UsbInterfaceInfo(1, 8, 6, 80, 2, listOf(UsbEndpointInfo(0x82, "IN", "Bulk", 512), UsbEndpointInfo(0x02, "OUT", "Bulk", 512)))
            )
        ),
        UsbDeviceInfo(
            0x0781, 0x5567, "/dev/bus/usb/001/005", "SanDisk", "Cruzer Blade", 0, 0, 1,
            listOf(UsbInterfaceInfo(0, 8, 6, 80, 2, listOf(UsbEndpointInfo(0x81, "IN", "Bulk", 512), UsbEndpointInfo(0x01, "OUT", "Bulk", 512))))
        ),
        UsbDeviceInfo(0x046D, 0xC52B, "/dev/bus/usb/001/006", "Logitech", "USB Receiver", 0, 0, 1,
            listOf(UsbInterfaceInfo(0, 3, 1, 2, 1, listOf(UsbEndpointInfo(0x81, "IN", "Interrupt", 8)))))
    )

    // ── Bluetooth Classic ───────────────────────────────────────────────────
    val classicDevices: List<ClassicBluetoothDevice> = listOf(
        ClassicBluetoothDevice("JBL Flip 6", "F8:DF:15:22:9A:01", -52, 12, "Audio/Video", "Loudspeaker", isPaired = true),
        ClassicBluetoothDevice("HC-05", "98:D3:31:F5:B2:7C", -66, 10, "Uncategorized", ""),
        ClassicBluetoothDevice(null, "00:1A:7D:DA:71:13", -84, 10, "Computer", "Laptop")
    )

    // ── Trackers ────────────────────────────────────────────────────────────
    val trackers: List<DetectedTracker> = listOf(
        DetectedTracker("7D:1E:AA:40:9C:02", null, TrackerType.AIRTAG, -62, now - 14 * MIN, now, 9, TrackingRisk.HIGH, "4C00 1219"),
        DetectedTracker("E4:B0:21:77:0A:1F", "Tile Pro", TrackerType.TILE, -80, now - 3 * MIN, now, 4, TrackingRisk.MEDIUM),
        DetectedTracker("A0:C9:A0:11:22:33", "Galaxy SmartTag2", TrackerType.SMARTTAG, -90, now - MIN, now, 1, TrackingRisk.LOW)
    )

    // ── Hidden camera ───────────────────────────────────────────────────────
    val cameraDetections: List<CameraDetection> = listOf(
        CameraDetection(source = DetectionSource.WIFI, threatLevel = CameraThreatLevel.HIGH, title = "Camera Manufacturer WiFi AP",
            detail = "SSID: Wyze-Cam-2F1A  MAC: 2C:AA:8E:2F:1A:77  OUI match", rssi = -47),
        CameraDetection(source = DetectionSource.BLE, threatLevel = CameraThreatLevel.MEDIUM, title = "Suspicious BLE Device",
            detail = "Name: Blink Mini  Address: 88:DA:1A:5C:00:9E", rssi = -70),
        CameraDetection(source = DetectionSource.NETWORK, threatLevel = CameraThreatLevel.HIGH, title = "Camera Streaming Host",
            detail = "192.168.1.42 open ports: 554, 80 (RTSP, HTTP)"),
        CameraDetection(source = DetectionSource.MAGNETIC, threatLevel = CameraThreatLevel.LOW, title = "Magnetic Anomaly Detected",
            detail = "Deviation: 31.4 μT from baseline (threshold: 15.0 μT)")
    )

    // ── Rogue AP ────────────────────────────────────────────────────────────
    val rogueAlerts: List<RogueApAlert> = listOf(
        RogueApAlert(threatType = ApThreatType.EVIL_TWIN, riskLevel = RiskLevel.CRITICAL, title = "Evil Twin: HomeNet",
            description = "Second AP broadcasting \"HomeNet\" with open security while the original uses WPA2.",
            suspiciousAp = ap("HomeNet", "DE:AD:BE:EF:00:01", -41, 2437, "[ESS]"),
            legitimateAp = ap("HomeNet", "AA:BB:CC:11:22:33", -58, 5180, "[WPA2-PSK-CCMP][ESS]")),
        RogueApAlert(threatType = ApThreatType.KARMA_ATTACK, riskLevel = RiskLevel.HIGH, title = "Karma attack pattern",
            description = "One BSSID answers to 4 different probed SSIDs (Starbucks, airport_free, HomeNet, xfinitywifi).",
            suspiciousAp = ap("Starbucks WiFi", "02:11:22:33:44:55", -50, 2412, "[ESS]")),
        RogueApAlert(threatType = ApThreatType.WEAK_SECURITY, riskLevel = RiskLevel.MEDIUM, title = "WEP network: OldRouter",
            description = "WEP can be cracked in minutes; treat any traffic on it as public.",
            suspiciousAp = ap("OldRouter", "00:1D:7E:AB:CD:EF", -77, 2462, "[WEP][ESS]"))
    )

    // ── Network scanner ─────────────────────────────────────────────────────
    val networkDevices: List<NetworkDevice> = listOf(
        NetworkDevice("192.168.1.1", "router.lan", listOf(OpenPort(53, ServiceType.DNS), OpenPort(80, ServiceType.HTTP), OpenPort(1900, ServiceType.UPNP)),
            listOf(
                Vulnerability(VulnerabilityLevel.MEDIUM, "Unencrypted Web Interface", "HTTP-only admin page.", 80, "Enable HTTPS on the router."),
                Vulnerability(VulnerabilityLevel.MEDIUM, "UPnP Service Exposed", "UPnP can open ports automatically.", 1900, "Disable UPnP if unused.")
            ), "Router"),
        NetworkDevice("192.168.1.42", null, listOf(OpenPort(554, ServiceType.RTSP, "RTSP/1.0 200 OK"), OpenPort(80, ServiceType.HTTP)),
            listOf(Vulnerability(VulnerabilityLevel.HIGH, "Camera Stream Accessible (RTSP)", "Video feed may be viewable without auth.", 554, "Set RTSP credentials.")), "Camera"),
        NetworkDevice("192.168.1.77", "nas.lan", listOf(OpenPort(22, ServiceType.SSH, "SSH-2.0-OpenSSH_9.6"), OpenPort(445, ServiceType.SMB), OpenPort(23, ServiceType.TELNET)),
            listOf(
                Vulnerability(VulnerabilityLevel.CRITICAL, "Unencrypted Remote Access (Telnet)", "Credentials sent in plaintext.", 23, "Disable Telnet, use SSH."),
                Vulnerability(VulnerabilityLevel.HIGH, "File Sharing Exposed (SMB)", "Shares reachable from the LAN.", 445, "Restrict SMB to authenticated users."),
                Vulnerability(VulnerabilityLevel.INFO, "SSH Service Available", "Encrypted remote access running.", 22, "Use key-based auth.")
            ), "File Server")
    )

    // ── RF bug sweeper ──────────────────────────────────────────────────────
    val bugDetections: List<BugDetection> = listOf(
        BugDetection(id = "ble-name-98:D3:31:F5:B2:7C", type = BugType.SUSPICIOUS_BLE, severity = ThreatSeverity.HIGH, title = "Suspicious BLE: HC-05",
            detail = "Name matches known bug/transmitter module pattern. Address: 98:D3:31:F5:B2:7C", rssi = -44),
        BugDetection(id = "ultra-19875", type = BugType.ULTRASONIC_BEACON, severity = ThreatSeverity.CRITICAL, title = "Ultrasonic Beacon Detected",
            detail = "Peak at 19875.0 Hz (magnitude 0.0812). 2 beacon(s) identified in 18-24 kHz range.", frequency = 19875f),
        BugDetection(id = "mag-anomaly", type = BugType.MAGNETIC_ANOMALY, severity = ThreatSeverity.MEDIUM, title = "Magnetic Anomaly",
            detail = "Deviation of 45.5 μT from baseline (48.2 → 93.7 μT).", fieldStrength = 93.7f)
    )

    // ── Deauth ──────────────────────────────────────────────────────────────
    val deauthEvents: List<DeauthEvent> = listOf(
        DeauthEvent(type = AttackType.DEAUTH_FLOOD, level = AlertLevel.CRITICAL, title = "Deauth Flood Detected",
            detail = "4 disconnections in the last 60s. Your device is being forcibly disconnected from the network.",
            affectedSsid = "HomeNet-5G", affectedBssid = "AA:BB:CC:11:22:33", timestamp = now - 20_000),
        DeauthEvent(type = AttackType.SIGNAL_JAMMING, level = AlertLevel.HIGH, title = "Signal Jamming Suspected",
            detail = "Signal dropped 34dBm (from -55dBm to -89dBm) while nearby APs remain stable.",
            affectedSsid = "HomeNet-5G", affectedBssid = "AA:BB:CC:11:22:33", timestamp = now - 65_000),
        DeauthEvent(type = AttackType.CHANNEL_HOPPING, level = AlertLevel.MEDIUM, title = "Unexpected Channel Change",
            detail = "\"HomeNet-5G\" switched from channel 36 to channel 149.",
            affectedSsid = "HomeNet-5G", affectedBssid = "AA:BB:CC:11:22:33", timestamp = now - 2 * MIN)
    )
    val apHistory: Map<String, List<ApSnapshot>> = mapOf(
        "AA:BB:CC:11:22:33" to (0 until 12).map { i -> ApSnapshot("AA:BB:CC:11:22:33", "HomeNet-5G", if (i < 8) -55 - i else -89, 36, now - (12 - i) * 5_000) },
        "11:22:33:44:55:66" to (0 until 12).map { i -> ApSnapshot("11:22:33:44:55:66", "Neighbour", -70 - (i % 2), 6, now - (12 - i) * 5_000) }
    )

    // ── Signal logger ───────────────────────────────────────────────────────
    val signalEntries: List<SignalLogEntry> = listOf(
        SignalLogEntry(type = SignalType.ANOMALY, source = "WiFi", address = "AA:BB:CC:11:22:33", rssi = -89, detail = "HomeNet-5G RSSI dropped 34 dBm in one cycle", isAnomaly = true, timestamp = now - 10_000),
        SignalLogEntry(type = SignalType.BLE_NEW, source = "BLE", address = "7D:1E:AA:40:9C:02", rssi = -62, detail = "New device (unnamed, AirTag service)", timestamp = now - 40_000),
        SignalLogEntry(type = SignalType.WIFI_LOST, source = "WiFi", address = "02:11:22:33:44:55", rssi = null, detail = "Starbucks WiFi no longer visible", timestamp = now - 90_000),
        SignalLogEntry(type = SignalType.WIFI_NEW, source = "WiFi", address = "DE:AD:BE:EF:00:01", rssi = -41, detail = "HomeNet (open) appeared", timestamp = now - 2 * MIN),
        SignalLogEntry(type = SignalType.BLE_DEVICE, source = "BLE", address = "5C:F3:70:A1:02:9B", rssi = -48, detail = "Galaxy Buds2 Pro", timestamp = now - 3 * MIN),
        SignalLogEntry(type = SignalType.WIFI_AP, source = "WiFi", address = "AA:BB:CC:11:22:33", rssi = -55, detail = "HomeNet-5G ch36 WPA2", timestamp = now - 4 * MIN)
    )

    // ── Wardriving ──────────────────────────────────────────────────────────
    val wardrivingRecords: List<WardrivingRecord> = listOf(
        WardrivingRecord("AA:BB:CC:11:22:33", "HomeNet-5G", -55, 5180, "[WPA2-PSK-CCMP][ESS]", 12.97160, 77.59460, now - 9 * MIN),
        WardrivingRecord("DE:AD:BE:EF:00:01", "HomeNet", -41, 2437, "[ESS]", 12.97162, 77.59463, now - 8 * MIN),
        WardrivingRecord("00:1D:7E:AB:CD:EF", "OldRouter", -77, 2462, "[WEP][ESS]", 12.97180, 77.59490, now - 6 * MIN),
        WardrivingRecord("02:11:22:33:44:55", "Starbucks WiFi", -50, 2412, "[ESS]", 12.97210, 77.59520, now - 4 * MIN),
        WardrivingRecord("11:22:33:44:55:66", "Neighbour", -70, 2437, "[WPA3-SAE-CCMP][ESS]", 12.97240, 77.59550, now - 2 * MIN),
        WardrivingRecord("66:55:44:33:22:11", null, -83, 5745, "[WPA2-PSK-CCMP][ESS]", 12.97260, 77.59580, now - MIN)
    )
    val wardrivingStats = WardrivingStats(totalRecords = 6, uniqueSsids = 5, uniqueBssids = 6, openCount = 2, securedCount = 4, sessionDurationMs = 9 * MIN)

    // ── Proximity radar ─────────────────────────────────────────────────────
    val radarDevices: List<RadarDevice> = listOf(
        RadarDevice("AA:BB:CC:11:22:33", "HomeNet-5G", DeviceCategory.WIFI_AP, -55, 4.2f, 30f, now, 90),
        RadarDevice("5C:F3:70:A1:02:9B", "Galaxy Buds2 Pro", DeviceCategory.BLE_DEVICE, -48, 1.1f, 120f, now, 100),
        RadarDevice("7D:1E:AA:40:9C:02", "Unknown Device", DeviceCategory.BLE_BEACON, -62, 3.6f, 200f, now, 76),
        RadarDevice("DE:AD:BE:EF:00:01", "HomeNet", DeviceCategory.WIFI_AP, -41, 1.8f, 280f, now, 100),
        RadarDevice("00:1D:7E:AB:CD:EF", "OldRouter", DeviceCategory.WIFI_AP, -77, 16.5f, 330f, now, 46)
    )

    // ── Cell tower ──────────────────────────────────────────────────────────
    val servingCell = CellTowerInfo(CellType.LTE, 404, 45, 3021, 1_240_581L, -86, 1800, true, 212, -10, 14, 7, 546, 20_000, "Airtel")
    val neighborCells: List<CellTowerInfo> = listOf(
        CellTowerInfo(CellType.LTE, 404, 45, 3021, 1_240_582L, -97, 1800, false, 213, -13, 6),
        CellTowerInfo(CellType.NR, 404, 45, null, null, -101, 632628, false, 88, -12, 3),
        CellTowerInfo(CellType.GSM, 404, 45, 3021, 51_223L, -92, 82, false)
    )
    val imsiAlerts: List<ImsiCatcherAlert> = listOf(
        ImsiCatcherAlert(CellAlertType.FORCED_2G_DOWNGRADE, "Registered network dropped from LTE to GSM while LTE neighbours remain strong.", CellAlertSeverity.HIGH, now - 90_000),
        ImsiCatcherAlert(CellAlertType.LAC_CHANGE, "LAC changed 3021 → 9001 without a cell ID change.", CellAlertSeverity.MEDIUM, now - 4 * MIN)
    )
    val signalHistory: List<Int> = (0 until 60).map { i -> -80 - ((i * 7) % 13) }

    // ── GPS ─────────────────────────────────────────────────────────────────
    val gpsState = GpsState(
        isTracking = true, latitude = 12.971599, longitude = 77.594566, altitude = 921.4, speed = 1.3f, bearing = 214f,
        accuracy = 3.8f, satelliteCount = 9, provider = "gps", lastUpdateTime = now,
        satellites = listOf(
            SatelliteInfo(1, 1, 41f, 62f, 118f, true, 1_575_420_000f),
            SatelliteInfo(3, 1, 38f, 44f, 201f, true, 1_575_420_000f),
            SatelliteInfo(3, 1, 27f, 44f, 201f, true, 1_176_450_000f),
            SatelliteInfo(11, 6, 36f, 71f, 12f, true, 1_575_420_000f),
            SatelliteInfo(19, 6, 33f, 28f, 305f, true, 1_575_420_000f),
            SatelliteInfo(194, 4, 30f, 52f, 88f, true, 1_575_420_000f),
            SatelliteInfo(5, 7, 29f, 66f, 140f, true, 1_176_450_000f),
            SatelliteInfo(7, 3, 22f, 15f, 250f, false, 1_602_000_000f),
            SatelliteInfo(14, 5, 18f, 9f, 40f, false, 1_561_098_000f),
            SatelliteInfo(24, 1, 12f, 6f, 355f, false, 1_575_420_000f)
        ),
        nmeaSentences = listOf(
            "\$GPGGA,082544.00,1258.2959,N,07735.6740,E,1,09,0.9,921.4,M,-87.6,M,,*5B",
            "\$GPRMC,082544.00,A,1258.2959,N,07735.6740,E,2.5,214.0,050926,,,A*7F",
            "\$GPGSV,3,1,10,01,62,118,41,03,44,201,38,11,71,012,36,19,28,305,33*7A"
        )
    )

    // ── Alert center ────────────────────────────────────────────────────────
    val unifiedAlerts: List<UnifiedAlert> = listOf(
        UnifiedAlert("demo-1", AlertSource.ROGUE_AP, AlertSeverity.CRITICAL, "Evil Twin: HomeNet", "Open AP impersonating HomeNet at DE:AD:BE:EF:00:01", now - 30_000),
        UnifiedAlert("demo-2", AlertSource.BLUETOOTH_TRACKER, AlertSeverity.HIGH, "AirTag following you", "Unnamed AirTag seen 9 times over 14 minutes", now - 3 * MIN),
        UnifiedAlert("demo-3", AlertSource.DEAUTH, AlertSeverity.CRITICAL, "Deauth Flood Detected", "4 disconnections in 60s on HomeNet-5G", now - 5 * MIN),
        UnifiedAlert("demo-4", AlertSource.HIDDEN_CAMERA, AlertSeverity.HIGH, "Camera Manufacturer WiFi AP", "Wyze-Cam-2F1A at 2C:AA:8E:2F:1A:77", now - 8 * MIN),
        UnifiedAlert("demo-5", AlertSource.GPS_SPOOF, AlertSeverity.MEDIUM, "GPS spoof confidence 43%", "Speed anomaly and mock provider detected", now - 12 * MIN)
    )
}

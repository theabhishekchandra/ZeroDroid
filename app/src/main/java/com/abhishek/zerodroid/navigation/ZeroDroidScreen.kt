package com.abhishek.zerodroid.navigation

/**
 * Every navigation destination. Tool metadata (job, icon, hardware, group) lives in
 * [ToolCatalog]; this only names routes and titles.
 */
sealed class ZeroDroidScreen(
    val route: String,
    val title: String
) {
    // Top-level tabs
    data object Dashboard : ZeroDroidScreen("dashboard", "Home")
    data object Tools : ZeroDroidScreen("tools", "Tools")
    data object AlertCenter : ZeroDroidScreen("alert_center", "Alerts")

    // Tools
    data object Sensors : ZeroDroidScreen("sensors", "Sensor Dashboard")
    data object Wifi : ZeroDroidScreen("wifi", "WiFi Analyzer")
    data object Ble : ZeroDroidScreen("ble", "BLE Scanner")
    data object Nfc : ZeroDroidScreen("nfc", "NFC Tools")
    data object Ir : ZeroDroidScreen("ir", "IR Remote")
    data object Uwb : ZeroDroidScreen("uwb", "UWB Radar")
    data object Usb : ZeroDroidScreen("usb", "USB Devices")
    data object Sdr : ZeroDroidScreen("sdr", "SDR Radio")
    data object Camera : ZeroDroidScreen("camera", "QR Scanner")
    data object Ultrasonic : ZeroDroidScreen("ultrasonic", "Ultrasonic")
    data object Wardriving : ZeroDroidScreen("wardriving", "Wardriving")
    data object WifiAware : ZeroDroidScreen("wifi_aware", "Wi-Fi Aware")
    data object CellTower : ZeroDroidScreen("cell_tower", "Cell Tower")
    data object UsbCamera : ZeroDroidScreen("usb_camera", "USB Camera")
    data object Gps : ZeroDroidScreen("gps", "GPS Tracker")
    data object BluetoothClassic : ZeroDroidScreen("bluetooth_classic", "Bluetooth Classic")
    data object WifiDirect : ZeroDroidScreen("wifi_direct", "Wi-Fi Direct")
    data object HiddenCamera : ZeroDroidScreen("hidden_camera", "Camera Detector")
    data object GpsSpoofDetector : ZeroDroidScreen("gps_spoof_detector", "GPS Spoof Detector")
    data object BluetoothTracker : ZeroDroidScreen("bluetooth_tracker", "Tracker Scanner")
    data object RogueAp : ZeroDroidScreen("rogue_ap", "Rogue AP Detector")
    data object NetworkScanner : ZeroDroidScreen("network_scanner", "Network Scanner")
    data object RfBugSweeper : ZeroDroidScreen("rf_bug_sweeper", "RF Bug Sweeper")
    data object ProximityRadar : ZeroDroidScreen("proximity_radar", "Proximity Radar")
    data object PrivacyScore : ZeroDroidScreen("privacy_score", "Privacy Score")
    data object DeauthDetector : ZeroDroidScreen("deauth_detector", "Deauth Detector")
    data object EmfMapper : ZeroDroidScreen("emf_mapper", "EMF Mapper")
    data object SignalLogger : ZeroDroidScreen("signal_logger", "Signal Logger")

    companion object {
        // Lazy: an eager list here can capture nulls when a subclass object initializes first.
        val all: List<ZeroDroidScreen> by lazy {
            listOf(
                Dashboard, Tools, AlertCenter,
                Sensors, Wifi, Ble, Nfc, Ir, Uwb,
                Usb, Sdr, Camera, Ultrasonic, Wardriving, WifiAware,
                CellTower, UsbCamera, Gps, BluetoothClassic, WifiDirect, HiddenCamera,
                GpsSpoofDetector, BluetoothTracker, RogueAp, NetworkScanner, RfBugSweeper, ProximityRadar,
                PrivacyScore, DeauthDetector, EmfMapper, SignalLogger
            )
        }
    }
}

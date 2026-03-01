package com.abhishek.zerodroid.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.vector.ImageVector

enum class ScreenCategory(val label: String) {
    WIRELESS("Wireless"),
    RF("RF & Signals"),
    SENSORS("Sensors"),
    NETWORK("Network"),
    SECURITY("Security Tools")
}

sealed class ZeroDroidScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val category: ScreenCategory
) {
    // Home
    data object Dashboard : ZeroDroidScreen("dashboard", "Dashboard", Icons.Default.Home, ScreenCategory.SENSORS)

    // Original screens
    data object Sensors : ZeroDroidScreen("sensors", "Sensor Dashboard", Icons.Default.Sensors, ScreenCategory.SENSORS)
    data object Wifi : ZeroDroidScreen("wifi", "WiFi Analyzer", Icons.Default.Wifi, ScreenCategory.WIRELESS)
    data object Ble : ZeroDroidScreen("ble", "BLE Scanner", Icons.Default.Bluetooth, ScreenCategory.WIRELESS)
    data object Nfc : ZeroDroidScreen("nfc", "NFC Tools", Icons.Default.Nfc, ScreenCategory.WIRELESS)
    data object Ir : ZeroDroidScreen("ir", "IR Remote", Icons.Default.SettingsRemote, ScreenCategory.RF)
    data object Uwb : ZeroDroidScreen("uwb", "UWB Radar", Icons.Default.GraphicEq, ScreenCategory.RF)
    data object Usb : ZeroDroidScreen("usb", "USB Devices", Icons.Default.Usb, ScreenCategory.NETWORK)
    data object Sdr : ZeroDroidScreen("sdr", "SDR Radio", Icons.Default.Radio, ScreenCategory.RF)
    data object Camera : ZeroDroidScreen("camera", "QR Scanner", Icons.Default.CameraAlt, ScreenCategory.SENSORS)
    data object Ultrasonic : ZeroDroidScreen("ultrasonic", "Ultrasonic", Icons.Default.GraphicEq, ScreenCategory.RF)
    data object Wardriving : ZeroDroidScreen("wardriving", "Wardriving", Icons.Default.Map, ScreenCategory.NETWORK)
    data object WifiAware : ZeroDroidScreen("wifi_aware", "Wi-Fi Aware", Icons.Default.WifiFind, ScreenCategory.WIRELESS)
    data object CellTower : ZeroDroidScreen("cell_tower", "Cell Tower", Icons.Default.CellTower, ScreenCategory.NETWORK)
    data object UsbCamera : ZeroDroidScreen("usb_camera", "USB Camera", Icons.Default.Videocam, ScreenCategory.SENSORS)
    data object Gps : ZeroDroidScreen("gps", "GPS Tracker", Icons.Default.MyLocation, ScreenCategory.SENSORS)
    data object BluetoothClassic : ZeroDroidScreen("bluetooth_classic", "Bluetooth Classic", Icons.Default.BluetoothSearching, ScreenCategory.WIRELESS)
    data object WifiDirect : ZeroDroidScreen("wifi_direct", "Wi-Fi Direct", Icons.Default.Share, ScreenCategory.WIRELESS)

    // Security tools
    data object HiddenCamera : ZeroDroidScreen("hidden_camera", "Camera Detector", Icons.Default.Security, ScreenCategory.SECURITY)
    data object GpsSpoofDetector : ZeroDroidScreen("gps_spoof_detector", "GPS Spoof Detector", Icons.Default.GpsFixed, ScreenCategory.SECURITY)
    data object BluetoothTracker : ZeroDroidScreen("bluetooth_tracker", "Tracker Scanner", Icons.Default.LocationSearching, ScreenCategory.SECURITY)
    data object RogueAp : ZeroDroidScreen("rogue_ap", "Rogue AP Detector", Icons.Default.WifiOff, ScreenCategory.SECURITY)
    data object NetworkScanner : ZeroDroidScreen("network_scanner", "Network Scanner", Icons.Default.Lan, ScreenCategory.SECURITY)
    data object RfBugSweeper : ZeroDroidScreen("rf_bug_sweeper", "RF Bug Sweeper", Icons.Default.BugReport, ScreenCategory.SECURITY)
    data object ProximityRadar : ZeroDroidScreen("proximity_radar", "Proximity Radar", Icons.Default.Radar, ScreenCategory.SECURITY)
    data object PrivacyScore : ZeroDroidScreen("privacy_score", "Privacy Score", Icons.Default.Shield, ScreenCategory.SECURITY)
    data object DeauthDetector : ZeroDroidScreen("deauth_detector", "Deauth Detector", Icons.Default.Router, ScreenCategory.SECURITY)
    data object EmfMapper : ZeroDroidScreen("emf_mapper", "EMF Mapper", Icons.Default.Dashboard, ScreenCategory.SENSORS)
    data object SignalLogger : ZeroDroidScreen("signal_logger", "Signal Logger", Icons.Default.Timeline, ScreenCategory.SECURITY)

    companion object {
        val all: List<ZeroDroidScreen> = listOf(
            Dashboard, Sensors, Wifi, Ble, Nfc, Ir, Uwb, Usb, Sdr,
            Camera, Ultrasonic, Wardriving, WifiAware, CellTower, UsbCamera,
            Gps, BluetoothClassic, WifiDirect,
            HiddenCamera, GpsSpoofDetector, BluetoothTracker, RogueAp,
            NetworkScanner, RfBugSweeper, ProximityRadar, PrivacyScore,
            DeauthDetector, EmfMapper, SignalLogger
        )

        val byCategory: Map<ScreenCategory, List<ZeroDroidScreen>>
            get() = all.groupBy { it.category }
    }
}

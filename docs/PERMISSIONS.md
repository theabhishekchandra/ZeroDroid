# ZeroDroid — Permissions

ZeroDroid requests permissions **only when you open a feature that needs them** — nothing at startup.
For the project overview, screenshots, and setup, see the [main README](../README.md).

[← Back to README](../README.md)

---

| Permission | Required By | Why |
|-----------|-------------|-----|
| `BLUETOOTH_SCAN` | BLE Scanner, Tracker Scanner, RF Bug Sweeper, Proximity Radar, Hidden Camera, Privacy Score | Discover Bluetooth devices |
| `BLUETOOTH_CONNECT` | BLE Scanner (GATT), Bluetooth Classic, NFC (HCE), RF Bug Sweeper | Connect to devices |
| `ACCESS_FINE_LOCATION` | WiFi Analyzer, GPS Tracker, Wardriving, Cell Tower, Rogue AP, Proximity Radar, GPS Spoof, Hidden Camera | Android requires location for WiFi/BLE scanning |
| `ACCESS_COARSE_LOCATION` | Fallback for location-based features | Approximate location |
| `ACCESS_WIFI_STATE` | WiFi Analyzer, Network Scanner, Deauth Detector | Read WiFi scan results |
| `CHANGE_WIFI_STATE` | WiFi Analyzer | Trigger WiFi scans |
| `NEARBY_WIFI_DEVICES` | Wi-Fi Direct (API 33+) | Discover WiFi Direct peers |
| `CAMERA` | QR Scanner, Hidden Camera (port scan uses camera preview) | Camera access for scanning |
| `NFC` | NFC Tools | Read/write NFC tags |
| `TRANSMIT_IR` | IR Remote | Send infrared commands |
| `READ_PHONE_STATE` | Cell Tower, GPS Spoof Detector | Access cell tower data |
| `RECORD_AUDIO` | Ultrasonic Analyzer, RF Bug Sweeper, Privacy Score | Microphone for ultrasonic detection |
| `FOREGROUND_SERVICE` | Wardriving | Background scanning service |
| `POST_NOTIFICATIONS` | Wardriving (API 33+) | Foreground service notification |
| `UWB_RANGING` | UWB Radar | Ultra-wideband distance ranging (API 31+) |

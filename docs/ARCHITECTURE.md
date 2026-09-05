# ZeroDroid — Architecture

App structure, key design decisions, and directory layout.
For the project overview, screenshots, and setup, see the [main README](../README.md).

[← Back to README](../README.md)

---

## App Structure & Key Design Decisions

```
ZeroDroidApp (@HiltAndroidApp Application)
 └── Hilt DI (core/di/*Module.kt — services scoped per module, most unscoped/fresh-per-use)
      ├── SystemServiceModule (SensorManager, WifiManager, BluetoothManager, ...)
      ├── HardwareModule (HardwareChecker — 16 capability queries)
      ├── DatabaseModule (Room, 5 entities: BLE, NFC, Wardriving, QR, Alert)
      ├── FeatureModule (~25 domain/repository classes across every tool)
      └── PreferencesModule, NfcTagBus

Navigation: Jetpack Navigation Compose + ModalNavigationDrawer
Pattern: MVVM (Screen → @HiltViewModel → Domain → Hardware)
UI: Jetpack Compose + Material 3 (dark terminal theme)
Font: JetBrains Mono throughout
```

**Key design decisions:**

- **All services are lazy-initialized.** Nothing starts until you navigate to that feature. The app launches to a zero-cost Dashboard.
- **No auto-start scanning.** Every scanner requires a manual tap to start, and auto-stops after a timeout (sensors: 60s, WiFi/BLE: 30s).
- **No `saveState` in navigation.** When you leave a screen, its scanning stops immediately.
- **Hardware follows the app lifecycle.** Every scanning screen wraps its session in `HardwareLifecycleEffect` (`core/lifecycle`), which releases radios, sensors and the microphone on `ON_STOP` (Home button, app switch, screen lock), re-acquires them on `ON_START` if they were running, and stops them when the screen leaves composition. One-shot or peer-coordinated sessions (network scan, privacy audit, UWB ranging) pause but do not auto-resume. Wardriving is the deliberate exception: it runs a foreground service so collection continues in the background.
- **Debug builds can load demo data.** The top bar shows a flask icon on hardware-dependent screens in debug builds only. It posts the current route on `DemoDataBus` (`core/debug`), and the screen's ViewModel replaces its live state with the samples in `DemoData`. This is how populated layouts are verified on phones without NFC, IR, UWB, Wi-Fi Aware or SDR hardware. `BuildConfig.DEBUG` gates both the button and the bus, so release builds carry no demo path.
- **Sensor polling at 5Hz, not 60Hz.** `SENSOR_DELAY_NORMAL` instead of `SENSOR_DELAY_UI` — 12x fewer events with no visible difference.
- **BLE uses `SCAN_MODE_LOW_POWER`** instead of `LOW_LATENCY` — 10x less battery drain.

Each feature follows the same structure:
```
feature/
├── domain/      # Business logic, scanners, analyzers (no Android UI imports)
├── data/        # Repositories (if Room persistence needed)
├── ui/          # Composable screens and components
└── viewmodel/   # @HiltViewModel with @Inject constructor
```

---

## Project Structure

```
app/src/main/java/com/abhishek/zerodroid/
├── MainActivity.kt                  # Entry point (@AndroidEntryPoint), NFC intent handling
├── ZeroDroidApp.kt                  # Application class (@HiltAndroidApp)
├── core/
│   ├── alerts/                      # Alert Center: AlertModels, AlertCenterRepository
│   ├── database/                    # Room DB, DAOs, entities, converters
│   ├── di/                          # Hilt modules — SystemServiceModule, FeatureModule,
│   │                                  DatabaseModule, HardwareModule, PreferencesModule, NfcTagBus
│   ├── hardware/HardwareChecker.kt  # 16 hardware capability queries
│   ├── permission/                  # PermissionGate composable, PermissionUtils
│   ├── ui/                          # TerminalCard, EmptyState, ScanningIndicator,
│   │                                  StatusIndicator, EthicalUseDialog, HelpContent
│   └── util/                        # FrequencyUtils, ByteArrayExt
├── features/                        # 29 tools — one package each (see below)
│   ├── dashboard/  alert_center/  sensors/  wifi/  ble/  nfc/  bluetooth_classic/  ir/  uwb/
│   ├── sdr/  ultrasonic/  usb/  usbcamera/  gps/  celltower/  wardriving/
│   ├── wifi_direct/  wifiaware/  emf_mapper/  camera/  hidden_camera/
│   ├── gps_spoof_detector/  bluetooth_tracker/  rogue_ap_detector/
│   └── network_scanner/  rf_bug_sweeper/  proximity_radar/  privacy_score/
│       deauth_detector/  signal_logger/
├── navigation/
│   ├── ZeroDroidScreen.kt          # 29 screen definitions, 5 categories
│   ├── AppNavigation.kt            # NavHost + drawer + help system
│   └── DrawerContent.kt            # Terminal-styled navigation drawer
└── ui/theme/
    ├── Color.kt                     # Terminal green, amber, red, cyan palette
    ├── Theme.kt                     # Dark-only Material 3 theme
    └── Type.kt                      # JetBrains Mono typography
```

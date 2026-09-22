package com.abhishek.zerodroid.core.ui

/** How a result looks on screen, for the "Reading the results" legend. */
enum class LegendMark { SIGNAL_STRONG, SIGNAL_WEAK, TAG_GOOD, TAG_WARN, TAG_BAD, TAG_INFO }

data class HelpLegend(val mark: LegendMark, val label: String, val meaning: String)

/** A short fact with a one-line explanation: limits, permissions, false positives. */
data class HelpFact(val title: String, val detail: String)

/**
 * In-context help for one tool, keyed by its route. [steps], [legend] and [facts] are the
 * redesign's "How to use / Reading the results / Good to know"; tools that don't have them yet
 * fall back to [capabilities] and [tips].
 */
data class FeatureHelp(
    val title: String,
    val description: String,
    val capabilities: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val legend: List<HelpLegend> = emptyList(),
    val facts: List<HelpFact> = emptyList()
)

object HelpContent {
    val features: Map<String, FeatureHelp> = mapOf(
        "sensors" to FeatureHelp(
            title = "Sensor Dashboard",
            description = "Real-time monitoring of all hardware sensors in your device.",
            capabilities = listOf(
                "Monitor accelerometer, gyroscope, magnetometer",
                "Metal detector using magnetic field deviation",
                "Floor estimation using barometric pressure",
                "Light sensor and proximity sensor readings"
            ),
            tips = listOf(
                "Metal detector works best when you calibrate by pressing Reset first",
                "Floor estimation needs initial calibration — start from a known floor",
                "Magnetometer readings can be affected by nearby electronics"
            )
        ),
        "wifi" to FeatureHelp(
            title = "WiFi Analyzer",
            description = "Find crowded channels and weak security",
            steps = listOf(
                "Tap Start. The scan runs for 30 s by default.",
                "Open Channels to see which channel is least crowded.",
                "Tap a network for its details, or flag it as trusted."
            ),
            legend = listOf(
                HelpLegend(LegendMark.SIGNAL_STRONG, "", "Strong, about -50 dBm or better"),
                HelpLegend(LegendMark.SIGNAL_WEAK, "", "Weak, about -80 dBm"),
                HelpLegend(LegendMark.TAG_GOOD, "WPA3", "Strongest encryption"),
                HelpLegend(LegendMark.TAG_BAD, "OPEN", "No encryption, others can read traffic")
            ),
            facts = listOf(
                HelpFact("Android limits scans", "4 scans every 2 minutes per app"),
                HelpFact("Location permission", "Required by Android for WiFi results; never stored"),
                HelpFact("Hidden networks", "Shown as [hidden]; their names can’t be read")
            )
        ),
        "ble" to FeatureHelp(
            title = "BLE Scanner",
            description = "Discover and analyze Bluetooth Low Energy devices nearby.",
            capabilities = listOf(
                "Scan for all BLE devices in range",
                "View RSSI signal strength and distance estimate",
                "Identify device types (fitness trackers, speakers, etc.)",
                "Bookmark devices for tracking"
            ),
            tips = listOf(
                "Distance estimation is approximate — walls reduce accuracy",
                "Some devices hide their names for privacy",
                "Bookmark important devices to track them across scans"
            )
        ),
        "cell_tower" to FeatureHelp(
            title = "Cell Tower Analyzer",
            description = "Monitor cell tower connections and detect potential IMSI catchers.",
            capabilities = listOf(
                "View current and neighboring cell towers",
                "See MCC/MNC/LAC/CID identifiers",
                "IMSI catcher detection using heuristic analysis",
                "Signal strength monitoring"
            ),
            tips = listOf(
                "Unexpected LAC changes without moving may indicate an IMSI catcher",
                "Forced 2G-only mode is a common IMSI catcher technique",
                "Run monitoring for at least 5 minutes for reliable detection"
            )
        ),
        "nfc" to FeatureHelp(
            title = "NFC Tools",
            description = "Read and write NFC tags with full NDEF support.",
            capabilities = listOf(
                "Read any NFC tag (NTAG, Mifare, ISO 14443)",
                "Parse NDEF records: URLs, text, WiFi, contacts",
                "Write text and URL records to writable tags",
                "Tag scan history"
            ),
            tips = listOf(
                "Hold the tag steady against the back of your phone",
                "NFC reading range is only 1-4 cm",
                "Not all tags are writable — some are read-only"
            )
        ),
        "usb" to FeatureHelp(
            title = "USB Devices",
            description = "Inspect connected USB devices and detect potential BadUSB threats.",
            capabilities = listOf(
                "List all connected USB devices via OTG",
                "View VID/PID, class, interfaces, endpoints",
                "BadUSB detection (HID + mass storage combo)",
                "Live connect/disconnect monitoring"
            ),
            tips = listOf(
                "Requires USB OTG cable or adapter",
                "BadUSB warning means a device has suspicious interface combinations",
                "Always verify unknown USB devices before trusting them"
            )
        ),
        "ir" to FeatureHelp(
            title = "IR Remote",
            description = "Control TVs and appliances using infrared signals.",
            capabilities = listOf(
                "Pre-built remotes for Samsung, LG, Sony TVs",
                "Custom IR code transmission (NEC, Samsung32, RC5, RC6, Sony)",
                "Import Flipper Zero .ir files",
                "Full remote control layout with D-pad"
            ),
            tips = listOf(
                "Point your phone's IR blaster directly at the device",
                "Range is typically 3-5 meters line-of-sight",
                "Not all phones have IR blasters — check the status indicator"
            )
        ),
        "ultrasonic" to FeatureHelp(
            title = "Ultrasonic Analyzer",
            description = "Detect hidden ultrasonic beacons and generate ultrasonic tones.",
            capabilities = listOf(
                "Analyze frequencies from 18-24 kHz",
                "Detect ultrasonic tracking beacons",
                "Generate custom ultrasonic tones",
                "Real-time frequency spectrum display"
            ),
            tips = listOf(
                "Some apps and TV ads use ultrasonic beacons for cross-device tracking",
                "Frequencies above 20 kHz are inaudible to most adults",
                "Use Detect mode in different rooms to check for hidden beacons"
            )
        ),
        "camera" to FeatureHelp(
            title = "QR Scanner",
            description = "Scan QR codes and barcodes with threat analysis, or generate your own.",
            capabilities = listOf(
                "Scan QR codes, barcodes (EAN, UPC, Code128, etc.)",
                "Automatic threat analysis for malicious URLs",
                "Parse WiFi credentials, contacts, locations",
                "Generate QR codes for text, URLs, and WiFi"
            ),
            tips = listOf(
                "Always check the threat analysis before opening scanned URLs",
                "WiFi QR codes let others join your network without typing passwords",
                "Generated QR codes use terminal-green theme colors"
            )
        ),
        "wardriving" to FeatureHelp(
            title = "Wardriving",
            description = "Map WiFi and BLE networks with GPS coordinates.",
            capabilities = listOf(
                "Log WiFi networks with signal strength and GPS location",
                "Log BLE devices with coordinates",
                "Export data in WiGLE CSV format",
                "Background scanning with notification"
            ),
            tips = listOf(
                "Best results when moving slowly (walking or cycling)",
                "Export to WiGLE format for community network mapping",
                "Background scan uses minimal battery via foreground service"
            )
        ),
        "sdr" to FeatureHelp(
            title = "SDR Radio",
            description = "Detect RTL-SDR dongles connected via USB.",
            capabilities = listOf(
                "Detect RTL-SDR USB dongles by VID/PID",
                "Show device information and capabilities",
                "Identify supported RTL-SDR chipsets"
            ),
            tips = listOf(
                "Full SDR signal processing requires native libraries",
                "Connect RTL-SDR dongle via USB OTG cable",
                "This screen detects the hardware — use dedicated SDR apps for reception"
            )
        ),
        "uwb" to FeatureHelp(
            title = "UWB Radar",
            description = "Check Ultra-Wideband hardware capabilities.",
            capabilities = listOf(
                "Detect UWB hardware availability",
                "Show UWB chip capabilities",
                "Display supported ranging features"
            ),
            tips = listOf(
                "UWB is used for precise indoor positioning",
                "Few Android devices currently have UWB hardware",
                "Full ranging requires a second UWB-equipped device"
            )
        ),
        "wifi_aware" to FeatureHelp(
            title = "Wi-Fi Aware",
            description = "Discover nearby devices using Wi-Fi Aware (NAN).",
            capabilities = listOf(
                "Publish and subscribe to Wi-Fi Aware services",
                "Discover nearby Wi-Fi Aware devices",
                "Works without WiFi router or internet"
            ),
            tips = listOf(
                "Wi-Fi Aware works device-to-device without a router",
                "Both devices need Wi-Fi Aware support",
                "Useful for local file sharing and messaging"
            )
        ),
        "usb_camera" to FeatureHelp(
            title = "USB Camera",
            description = "Detect and preview USB cameras connected via OTG.",
            capabilities = listOf(
                "Detect USB Video Class (UVC) devices",
                "Show Camera2 external cameras",
                "Preview external camera feed (if supported)"
            ),
            tips = listOf(
                "Connect USB camera via OTG cable",
                "Not all USB cameras are supported — UVC class required",
                "Preview works best with Camera2 EXTERNAL type cameras"
            )
        )
    )
}

package com.abhishek.zerodroid.core.ui

/** How a result looks on screen, for the "Reading the results" legend. */
enum class LegendMark { SIGNAL_STRONG, SIGNAL_WEAK, TAG_GOOD, TAG_WARN, TAG_BAD, TAG_INFO }

data class HelpLegend(val mark: LegendMark, val label: String, val meaning: String)

/** A short fact with a one-line explanation: limits, permissions, false positives. */
data class HelpFact(val title: String, val detail: String)

/**
 * In-context help for one tool, keyed by its route: what to do ([steps]), the idea behind it
 * ([howItWorks]), how to read the screen ([legend]) and the limits worth knowing ([facts]).
 */
data class FeatureHelp(
    val title: String,
    val description: String,
    val steps: List<String> = emptyList(),
    val howItWorks: String? = null,
    val legend: List<HelpLegend> = emptyList(),
    val facts: List<HelpFact> = emptyList()
)

private val strongSignal = HelpLegend(LegendMark.SIGNAL_STRONG, "", "Strong, about -55 dBm or better")
private val weakSignal = HelpLegend(LegendMark.SIGNAL_WEAK, "", "Weak, about -85 dBm; far away or behind walls")
private val passMark = HelpLegend(LegendMark.TAG_GOOD, "PASS", "Check found nothing unusual")
private val warnMark = HelpLegend(LegendMark.TAG_WARN, "WARN", "Worth a look; often has an innocent cause")
private val failMark = HelpLegend(LegendMark.TAG_BAD, "FAIL", "Matches the pattern of an attack or risk")

object HelpContent {
    val features: Map<String, FeatureHelp> = mapOf(

        // ── Detect threats ───────────────────────────────────────────────────

        "bluetooth_tracker" to FeatureHelp(
            title = "Tracker Scanner",
            description = "Find AirTags, Tiles and SmartTags near you",
            steps = listOf(
                "Tap Start and keep the phone with you as you move.",
                "Watch for trackers that stay near you across places.",
                "Tap a tracker to see why it was flagged, or mark it as yours."
            ),
            howItWorks = "Trackers advertise over Bluetooth Low Energy with a manufacturer ID or service UUID that identifies the brand. Their address rotates every few minutes, so ZeroDroid follows them by signature and timing, not by MAC. A tracker that keeps reappearing while you move is the pattern that matters.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_BAD, "HIGH", "Seen many times over a long period"),
                HelpLegend(LegendMark.TAG_WARN, "MEDIUM", "Seen a few times; keep watching"),
                HelpLegend(LegendMark.TAG_INFO, "LOW", "Just appeared, not persistent yet")
            ),
            facts = listOf(
                HelpFact("Your own tags show up too", "Mark them as yours so they stop alerting"),
                HelpFact("Separated AirTags", "An AirTag away from its owner broadcasts differently, which is what gets flagged"),
                HelpFact("Distance is a guess", "Estimated from signal strength; can be off by 2× indoors")
            )
        ),

        "hidden_camera" to FeatureHelp(
            title = "Hidden Camera",
            description = "Check a room: WiFi OUI, BLE, magnetic, ports",
            steps = listOf(
                "Tap Start while connected to the room’s WiFi.",
                "Walk slowly past smoke detectors, clocks, chargers and vents.",
                "Review findings; strong ones combine several methods."
            ),
            howItWorks = "No single signal proves a camera, so five weak signals are combined: the vendor prefix (OUI) of nearby networks, camera-style network names, BLE signatures, magnetic anomalies from electronics, and open video ports like RTSP 554 on your network. A finding that matches several at once is much more likely to be real.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_BAD, "LIKELY", "Several methods agree"),
                HelpLegend(LegendMark.TAG_WARN, "POSSIBLE", "One method matched"),
                HelpLegend(LegendMark.TAG_GOOD, "CLEAN", "Method ran and found nothing")
            ),
            facts = listOf(
                HelpFact("Offline cameras", "A camera recording to an SD card with no radio can’t be seen by any app"),
                HelpFact("Port scan scope", "Only the network you are connected to is checked"),
                HelpFact("Magnetometer", "Speakers and metal furniture also cause spikes")
            )
        ),

        "rogue_ap" to FeatureHelp(
            title = "Rogue AP",
            description = "Spot evil twins and karma hotspots",
            steps = listOf(
                "Tap Start near the networks you want to check.",
                "Mark your home and work networks as trusted.",
                "Review findings; twins of trusted networks rank highest."
            ),
            howItWorks = "An evil twin copies a real network’s name so your phone joins it instead. The detector compares every access point sharing a name: different vendors, a sudden stronger signal, or a name one character off (H0me vs Home) all point to impersonation. Karma attacks answer to many network names from one radio.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_BAD, "EVIL TWIN", "Same name, different hardware"),
                HelpLegend(LegendMark.TAG_WARN, "SPOOF", "Look-alike name of a trusted network"),
                passMark
            ),
            facts = listOf(
                HelpFact("Mesh and multi-AP networks", "Offices often have many APs with one name; same vendor is usually fine"),
                HelpFact("Android limits scans", "4 WiFi scans every 2 minutes per app"),
                HelpFact("Safer habit", "Forget open networks you no longer use")
            )
        ),

        "deauth_detector" to FeatureHelp(
            title = "Deauth Detector",
            description = "Notice floods kicking you off WiFi",
            steps = listOf(
                "Connect to the network you want to watch.",
                "Tap Start and leave it running.",
                "Check the timeline when your WiFi keeps dropping."
            ),
            howItWorks = "WiFi lets a router tell a device to disconnect with a deauthentication frame, and older networks don’t sign those frames, so anyone can forge them. Android doesn’t expose raw frames, so the detector watches the symptoms: repeated disconnects while the signal stays strong, the network vanishing, or sudden channel changes.",
            legend = listOf(failMark, warnMark, passMark),
            facts = listOf(
                HelpFact("Bad signal looks different", "Weak-signal drops come with falling dBm; attacks don’t"),
                HelpFact("WPA3 helps", "Protected Management Frames make forged deauths ineffective"),
                HelpFact("Connected network only", "It can’t see frames aimed at other devices")
            )
        ),

        "cell_tower" to FeatureHelp(
            title = "Cell Tower",
            description = "Watch for IMSI-catcher indicators",
            steps = listOf(
                "Tap Start and leave the phone still for a few minutes.",
                "Watch the network-type timeline for drops to 2G.",
                "Review indicators; repeated events matter more than one."
            ),
            howItWorks = "An IMSI catcher (Stingray) pretends to be a cell tower so nearby phones connect to it. Many force phones down to 2G, which has weak encryption. Signs include the area code changing while you stand still, a sudden jump in signal strength, and forced 2G downgrades.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_INFO, "LTE", "4G, current encryption"),
                HelpLegend(LegendMark.TAG_BAD, "2G", "Old, weakly encrypted network"),
                warnMark
            ),
            facts = listOf(
                HelpFact("Indicators, not proof", "Handovers at coverage edges look similar"),
                HelpFact("Location permission", "Android requires it to read cell info; nothing is stored"),
                HelpFact("Disable 2G", "Android 12+ lets you turn off 2G in network settings")
            )
        ),

        "gps_spoof_detector" to FeatureHelp(
            title = "GPS Spoof",
            description = "Cross-check GPS against cell, WiFi, sensors",
            steps = listOf(
                "Tap Start outdoors or near a window.",
                "Wait for all seven checks to report.",
                "One warning is common indoors; several together are not."
            ),
            howItWorks = "GPS signals are faint and unauthenticated, so a transmitter can feed a fake position. The detector checks whether GPS agrees with everything else the phone knows: the nearest cell tower, WiFi networks, barometric altitude, the accelerometer, and whether a mock-location app is on. Spoofing rarely fools all of them.",
            legend = listOf(passMark, warnMark, failMark),
            facts = listOf(
                HelpFact("Indoor drift", "GPS indoors can wander hundreds of metres"),
                HelpFact("No barometer", "The altitude check is skipped on phones without one"),
                HelpFact("Mock locations", "Developer options can enable fake GPS apps")
            )
        ),

        "rf_bug_sweeper" to FeatureHelp(
            title = "RF Bug Sweeper",
            description = "BLE + ultrasonic + magnetic sweep",
            steps = listOf(
                "Tap Start in the room you want to check.",
                "Move slowly around outlets, furniture and fixtures.",
                "Review flagged radio modules and anomalies."
            ),
            howItWorks = "Cheap bugs are built from common radio modules (HC-05, ESP32, nRF) that advertise recognisable names. The sweep looks for those over Bluetooth, listens for ultrasonic tones above human hearing, and watches the magnetometer for electronics hidden behind surfaces.",
            legend = listOf(failMark, warnMark, passMark),
            facts = listOf(
                HelpFact("Hobby gear is common", "Smart plugs and DIY projects use the same modules"),
                HelpFact("Microphone", "Used only during the sweep; audio is never saved")
            )
        ),

        "usb" to FeatureHelp(
            title = "USB Devices",
            description = "Inspect USB, flag BadUSB combos",
            steps = listOf(
                "Connect the device through a USB-OTG adapter.",
                "Allow access when Android asks.",
                "Check the interfaces; unplug anything flagged you don’t recognise."
            ),
            howItWorks = "Every USB device declares what it is through interface classes. A BadUSB looks like a flash drive but also declares itself a keyboard, then types commands at machine speed. A keyboard and storage in one device, with blank manufacturer and serial fields, is the classic pattern.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_WARN, "INPUT", "Keyboard or mouse interface"),
                HelpLegend(LegendMark.TAG_INFO, "STORAGE", "Mass-storage interface"),
                failMark
            ),
            facts = listOf(
                HelpFact("Android accepts keyboards", "A USB keyboard can type into an unlocked phone"),
                HelpFact("Safest action", "Unplugging stops it immediately")
            )
        ),

        "camera" to FeatureHelp(
            title = "QR Scanner",
            description = "Check codes for phishing before opening",
            steps = listOf(
                "Point the camera at a QR code or barcode.",
                "Read the verdict before doing anything.",
                "Copy the link, or open it only if it looks safe."
            ),
            howItWorks = "Phishing QR codes (quishing) hide a malicious link behind an innocent-looking square. The scanner decodes the content without opening it, then checks the link for look-alike brand names, free or abused domains, missing HTTPS, and login-style paths.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_BAD, "DANGEROUS", "Several phishing signs"),
                HelpLegend(LegendMark.TAG_WARN, "SUSPICIOUS", "One sign; check carefully"),
                HelpLegend(LegendMark.TAG_GOOD, "SAFE", "No known warning signs")
            ),
            facts = listOf(
                HelpFact("Short links hide the target", "bit.ly and similar can point anywhere"),
                HelpFact("WiFi codes", "Joining shares nothing, but check the network name")
            )
        ),

        "privacy_score" to FeatureHelp(
            title = "Privacy Score",
            description = "16+ checks on your phone’s exposure",
            steps = listOf(
                "Tap Start to run all checks.",
                "Look at Fix first; it’s sorted by impact.",
                "Tap Open to jump to the right Android setting."
            ),
            howItWorks = "The score weighs five areas by how much they expose you: WiFi security, device security, Bluetooth, network and physical settings. Each check reads a setting Android already exposes, such as Private DNS, USB debugging or the security patch date.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_BAD, "HIGH", "Fix this first"),
                HelpLegend(LegendMark.TAG_WARN, "MEDIUM", "Worth fixing soon"),
                HelpLegend(LegendMark.TAG_INFO, "LOW", "Minor or situational")
            ),
            facts = listOf(
                HelpFact("Nothing leaves the phone", "Checks are read locally"),
                HelpFact("Some checks need permissions", "Skipped ones don’t lower the score")
            )
        ),

        // ── Scan & discover ──────────────────────────────────────────────────

        "wifi" to FeatureHelp(
            title = "WiFi Analyzer",
            description = "Find crowded channels and weak security",
            steps = listOf(
                "Tap Start. The scan runs for 30 s by default.",
                "Open Channels to see which channel is least crowded.",
                "Tap a network for its details, or flag it as trusted."
            ),
            howItWorks = "Routers announce themselves several times a second with beacons that carry their name, channel and security. In 2.4 GHz only channels 1, 6 and 11 don’t overlap, so networks crowded onto the same channel slow each other down. Security type decides who can read your traffic.",
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
            description = "Find Bluetooth devices around you",
            steps = listOf(
                "Tap Start. Most devices appear within 10 seconds.",
                "Filter by type, or hide weak signals.",
                "Tap a device to open its GATT services or dump everything."
            ),
            howItWorks = "Bluetooth Low Energy devices advertise small packets many times a second: a name, a manufacturer ID and the services they offer. ZeroDroid uses those to guess the device type. Connecting opens the GATT table, the device’s list of readable and writable values.",
            legend = listOf(strongSignal, weakSignal, HelpLegend(LegendMark.TAG_WARN, "TRACKER", "Matches a tracker signature")),
            facts = listOf(
                HelpFact("Distance is estimated", "Signal strength varies with walls and bodies; can be off by 2×"),
                HelpFact("Random addresses", "Phones and trackers rotate their address for privacy"),
                HelpFact("Nearby devices permission", "Needed by Android to scan; used only while scanning")
            )
        ),

        "bluetooth_classic" to FeatureHelp(
            title = "Bluetooth Classic",
            description = "Discovery, SDP services, SPP serial",
            steps = listOf(
                "Tap Start; discovery runs for about 12 seconds.",
                "Tap a device to list its services (SDP).",
                "Connect to a Serial Port service to open the terminal."
            ),
            howItWorks = "Classic Bluetooth (BR/EDR) is the older, higher-bandwidth side of Bluetooth used by speakers, cars and serial modules. Each device publishes its services through SDP. The Serial Port Profile (SPP) carries plain text, which is how OBD-II car adapters and HC-05 modules talk.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_GOOD, "PAIRED", "Already paired with this phone"),
                HelpLegend(LegendMark.TAG_INFO, "NEW", "Found in this discovery")
            ),
            facts = listOf(
                HelpFact("Discoverable only", "Devices must be in pairing mode to be found"),
                HelpFact("Only connect to your own devices", "Serial commands can change device settings")
            )
        ),

        "network_scanner" to FeatureHelp(
            title = "Network Scanner",
            description = "Hosts, open ports, banners on your LAN",
            steps = listOf(
                "Connect to a network you own or may test.",
                "Tap Start to find every device on it.",
                "Tap a device to see its open ports and name it."
            ),
            howItWorks = "Each device on your network has an IP address. The scanner checks every address in your subnet, then tries common ports. An open port is a service listening for connections; the banner it replies with often names the software and version, which is how outdated or exposed services are spotted.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_GOOD, "KNOWN", "You marked this device as known"),
                HelpLegend(LegendMark.TAG_INFO, "NEW", "Not seen before"),
                HelpLegend(LegendMark.TAG_WARN, "REVIEW", "Exposes a risky service")
            ),
            facts = listOf(
                HelpFact("Permission matters", "Only scan networks you own or have written permission to test"),
                HelpFact("Your LAN only", "It never scans the internet")
            )
        ),

        "proximity_radar" to FeatureHelp(
            title = "Proximity Radar",
            description = "Plot nearby devices by distance",
            steps = listOf(
                "Tap Start and hold the phone still.",
                "Closer devices sit nearer the centre.",
                "Tap a dot for the device’s details."
            ),
            howItWorks = "Signal strength drops predictably with distance, so RSSI gives a rough range. Direction can’t be measured with a single antenna, so each device keeps a fixed angle and only its distance moves.",
            legend = listOf(strongSignal, weakSignal),
            facts = listOf(
                HelpFact("Angles are not real", "Only the distance from the centre means anything"),
                HelpFact("Bodies block signal", "Turning around can change readings by 10 dB")
            )
        ),

        "wifi_aware" to FeatureHelp(
            title = "Wi-Fi Aware",
            description = "Router-less NAN peer discovery",
            steps = listOf(
                "Pick Publish on one phone and Subscribe on another.",
                "Use the same service name on both.",
                "Send short messages once peers appear."
            ),
            howItWorks = "Wi-Fi Aware (Neighbor Awareness Networking) lets phones form a small cluster and find each other with no router or internet. Some phones can also measure distance to a peer using round-trip time (RTT).",
            facts = listOf(
                HelpFact("Both phones need it", "Wi-Fi Aware support varies by model"),
                HelpFact("Message size", "Up to 255 bytes per message"),
                HelpFact("Range", "Typically within about 30 m")
            )
        ),

        "sdr" to FeatureHelp(
            title = "SDR Radio",
            description = "Detect RTL-SDR, HackRF, AirSpy",
            steps = listOf(
                "Plug the dongle into a USB-OTG adapter.",
                "Allow access when Android asks.",
                "Hand it to an SDR app, or stream it to a computer."
            ),
            howItWorks = "A software-defined radio samples raw radio waves and leaves demodulation to software. ZeroDroid recognises dongles by their USB vendor and product IDs and reports what each can tune; decoding signals is left to dedicated SDR apps.",
            facts = listOf(
                HelpFact("Detection only", "ZeroDroid doesn’t demodulate signals itself"),
                HelpFact("Power hungry", "Dongles draw a lot of current from the phone")
            )
        ),

        "usb_camera" to FeatureHelp(
            title = "USB Camera",
            description = "UVC camera modes and preview",
            steps = listOf(
                "Connect the camera through a USB-OTG adapter.",
                "Allow access when Android asks.",
                "Pick a resolution to preview."
            ),
            howItWorks = "Most webcams and endoscopes follow the USB Video Class (UVC) standard, so one driver can talk to all of them. The camera lists the formats it supports; MJPEG sends compressed frames, YUYV sends raw ones.",
            facts = listOf(
                HelpFact("UVC required", "Cameras with their own drivers won’t work"),
                HelpFact("Adapter power", "Some cameras need a powered OTG hub")
            )
        ),

        // ── Analyze signals ──────────────────────────────────────────────────

        "ultrasonic" to FeatureHelp(
            title = "Ultrasonic",
            description = "18–24 kHz spectrum for beacons",
            steps = listOf(
                "Tap Start and keep the room quiet.",
                "Watch the spectrum for narrow, steady peaks.",
                "Use the tone generator to test other devices."
            ),
            howItWorks = "Some ads and apps play tones above human hearing that other devices’ microphones pick up, linking your devices together. The analyzer runs a Fast Fourier Transform (FFT) on the microphone signal to show how loud each frequency is, and flags narrow, persistent tones in the 18–24 kHz band.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_WARN, "BEACON?", "Narrow, steady tone above hearing"),
                HelpLegend(LegendMark.TAG_GOOD, "QUIET", "Nothing above the noise floor")
            ),
            facts = listOf(
                HelpFact("Audio is never saved", "Only the spectrum is kept"),
                HelpFact("Mic roll-off", "Many phone mics are deaf above 20 kHz, so quiet isn’t proof"),
                HelpFact("Innocent sources", "Chargers and old screens whine at high frequencies")
            )
        ),

        "emf_mapper" to FeatureHelp(
            title = "EMF Mapper",
            description = "Magnetic field map and hotspots",
            steps = listOf(
                "Tap Start away from metal to set a baseline.",
                "Tap a grid cell, hold the phone there, and measure.",
                "Fill the grid; hot cells show where fields are strongest."
            ),
            howItWorks = "The magnetometer measures the magnetic field in microtesla (µT). Earth’s field is about 25–65 µT; wiring, motors, speakers and magnets add to it. Mapping the difference from a baseline shows where hidden electronics or cables are.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_GOOD, "<15", "µT above baseline; normal"),
                HelpLegend(LegendMark.TAG_WARN, "15–40", "Elevated; nearby electronics"),
                HelpLegend(LegendMark.TAG_BAD, ">100", "Strong; magnet or motor very close")
            ),
            facts = listOf(
                HelpFact("Sensor position", "The magnetometer is near the top edge on most phones"),
                HelpFact("Phone cases", "Magnetic cases and mounts distort readings")
            )
        ),

        "sensors" to FeatureHelp(
            title = "Sensor Dashboard",
            description = "Motion, compass, level, metal detector",
            steps = listOf(
                "Tap Start; sensors refresh 5 times a second.",
                "Open a card for its instrument: level, compass, metal detector.",
                "Press Reset on the metal detector to set a baseline first."
            ),
            howItWorks = "Your phone carries MEMS sensors: an accelerometer measures force including gravity, a gyroscope measures rotation, a magnetometer measures magnetic fields, and a barometer measures air pressure. Combining them gives tilt, heading and even which floor you’re on.",
            facts = listOf(
                HelpFact("Auto-stop", "Stops after 60 s to save battery"),
                HelpFact("Missing sensors", "Cheaper phones often lack a barometer or gyroscope"),
                HelpFact("Calibrate the compass", "Wave the phone in a figure 8")
            )
        ),

        "gps" to FeatureHelp(
            title = "GPS Tracker",
            description = "Position, satellites, raw NMEA",
            steps = listOf(
                "Tap Start near a window or outdoors.",
                "Wait for a 3D fix (4+ satellites).",
                "Open Satellites or NMEA to see the raw data."
            ),
            howItWorks = "Your position comes from timing signals from satellites in several constellations: GPS (US), Galileo (EU), GLONASS (Russia) and BeiDou (China). With four or more, the receiver solves for latitude, longitude, altitude and time. NMEA sentences are the text format receivers use to report it.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_GOOD, "USED", "Satellite used in the fix"),
                HelpLegend(LegendMark.TAG_INFO, "SEEN", "Visible but not used")
            ),
            facts = listOf(
                HelpFact("C/N0", "Signal quality in dB-Hz; above 35 is good"),
                HelpFact("Indoors", "Accuracy drops sharply without a clear sky")
            )
        ),

        "uwb" to FeatureHelp(
            title = "UWB Radar",
            description = "FiRa ranging and AoA capability",
            steps = listOf(
                "Check what this phone’s UWB chip supports.",
                "Start ranging with a second UWB phone running ZeroDroid.",
                "Watch distance and angle update live."
            ),
            howItWorks = "Ultra-wideband sends very short pulses across a wide band, so it can time their flight precisely: a nanosecond is about 30 cm. Two-way ranging gives distance to within about 10 cm, and phones with several antennas measure the Angle of Arrival too.",
            facts = listOf(
                HelpFact("Rare hardware", "Few Android phones have UWB"),
                HelpFact("AirTags", "Use Apple’s protocol, which Android can’t range")
            )
        ),

        // ── Read, write & transmit ───────────────────────────────────────────

        "nfc" to FeatureHelp(
            title = "NFC Tools",
            description = "Read, write, dump MIFARE, emulate",
            steps = listOf(
                "Hold a tag flat against the back of the phone.",
                "Keep still for a second while it reads.",
                "Use Write, MIFARE or Emulate from the tabs."
            ),
            howItWorks = "NFC works at 13.56 MHz over a few centimetres: the phone powers the tag with its field and the tag answers. Most tags store NDEF records (a URL, WiFi credentials, text). MIFARE Classic cards split memory into sectors, each protected by keys, and many still use factory-default keys.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_GOOD, "WRITABLE", "Tag can be changed"),
                HelpLegend(LegendMark.TAG_WARN, "LOCKED", "Read-only, permanently")
            ),
            facts = listOf(
                HelpFact("Range", "1–4 cm; cases can block it"),
                HelpFact("Locking is permanent", "A locked tag can never be written again"),
                HelpFact("Only clone your own cards", "Copying access cards you don’t own may be illegal")
            )
        ),

        "ir" to FeatureHelp(
            title = "IR Remote",
            description = "TV remotes, Flipper .ir import",
            steps = listOf(
                "Pick a remote, or enter a custom code.",
                "Point the top of the phone at the device.",
                "Import Flipper Zero .ir files to add more."
            ),
            howItWorks = "Infrared remotes blink an LED on and off at a carrier frequency, usually 38 kHz, in patterns that encode an address and a command. Protocols like NEC, RC5 and Sony SIRC define that pattern, which is why one blaster can speak to many brands.",
            facts = listOf(
                HelpFact("Needs an IR blaster", "Most phones since 2020 don’t have one"),
                HelpFact("Line of sight", "Range is about 3–5 m"),
                HelpFact("Carrier", "Most TVs use 38 kHz; Sony uses 40 kHz")
            )
        ),

        "wifi_direct" to FeatureHelp(
            title = "Wi-Fi Direct",
            description = "P2P groups and file transfer",
            steps = listOf(
                "Tap Start to discover nearby peers.",
                "Connect or invite a peer to form a group.",
                "Send a file once the group is formed."
            ),
            howItWorks = "Wi-Fi Direct lets devices connect without a router: one becomes the group owner and acts like a small access point, the others join as clients. Printers and screen-casting often use it.",
            facts = listOf(
                HelpFact("One group at a time", "Leave the group before joining another"),
                HelpFact("Speed", "Much faster than Bluetooth for large files")
            )
        ),

        // ── Log & map ────────────────────────────────────────────────────────

        "signal_logger" to FeatureHelp(
            title = "Signal Logger",
            description = "WiFi + BLE arrival/departure timeline",
            steps = listOf(
                "Tap Start and put the phone down.",
                "Come back later and read the timeline.",
                "Filter to anomalies to see only unusual events."
            ),
            howItWorks = "The logger records when devices appear and disappear. Patterns reveal a lot: a device that arrives whenever you do, a hidden network that suddenly gets strong, or a burst of new access points in one scan.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_INFO, "NEW", "Device appeared"),
                HelpLegend(LegendMark.TAG_WARN, "SPIKE", "Signal jumped sharply"),
                HelpLegend(LegendMark.TAG_BAD, "BURST", "Many new devices at once")
            ),
            facts = listOf(
                HelpFact("Keeps the last 500 events", "Older ones roll off"),
                HelpFact("Battery", "Uses low-power scans, but long logs still cost battery")
            )
        ),

        "wardriving" to FeatureHelp(
            title = "Wardriving",
            description = "GPS + WiFi logging, WiGLE export",
            steps = listOf(
                "Tap Start; a notification keeps it running.",
                "Walk, cycle or drive a route.",
                "Export a WiGLE CSV when you’re done."
            ),
            howItWorks = "Wardriving pairs each network your phone hears with the GPS position where it was strongest, building a map of WiFi coverage and security. WiGLE is a public database built this way.",
            legend = listOf(
                HelpLegend(LegendMark.TAG_GOOD, "SECURED", "WPA2 or WPA3"),
                HelpLegend(LegendMark.TAG_WARN, "WEP", "Broken encryption"),
                HelpLegend(LegendMark.TAG_BAD, "OPEN", "No encryption")
            ),
            facts = listOf(
                HelpFact("Runs in the background", "A foreground service keeps logging with the screen off"),
                HelpFact("Passive only", "It records what networks broadcast; it never connects")
            )
        )
    )
}

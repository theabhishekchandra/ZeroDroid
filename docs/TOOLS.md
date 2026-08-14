# ZeroDroid — Complete Tool Guide

Detailed, step-by-step documentation for every tool in ZeroDroid.
For the project overview, screenshots, and setup, see the [main README](../README.md).

[← Back to README](../README.md)

---

## All 29 Tools — Detailed Guide

### Wireless Tools

---

#### 1. WiFi Analyzer

**What it solves:** You can't see which WiFi channels are congested, which networks have weak security, or what's broadcasting around you.

**How to use:**
1. Navigate to WiFi Analyzer
2. Tap **Scan** to start discovery
3. Filter by band: All / 2.4 GHz / 5 GHz
4. Review the channel chart to find the least congested channel for your router
5. Scan auto-stops after 30 seconds

**What you see per network:**
- SSID (network name) and BSSID (hardware address)
- Signal strength in dBm and as a percentage
- Frequency, channel number, and band
- Channel width (20/40/80/160 MHz)
- Security type: OPEN, WEP, WPA, WPA2, WPA3
- Channel congestion score

**Why it matters:** If your WiFi is slow, the problem is often channel congestion, not your internet speed. This tool shows you exactly which channel to switch to.

---

#### 2. BLE Scanner

**What it solves:** Dozens of invisible Bluetooth Low Energy devices surround you at all times — fitness trackers, beacons, smart home devices, headphones, potentially tracking devices. You can't see any of them without a scanner.

**How to use:**
1. Navigate to BLE Scanner
2. Tap **Scan** to start BLE discovery
3. Tap any device to see details
4. Tap a device row to open the **GATT Explorer** for deep inspection
5. Bookmark devices you want to track
6. Scan auto-stops after 30 seconds

**What you see per device:**
- Name and MAC address
- RSSI signal strength (dBm)
- Estimated distance in meters (log-distance path loss model)
- Device type classification: Audio, Fitness, Tracker, Input, TV/Media, Phone, SmartHome, etc.
- Service UUIDs being advertised

**GATT Explorer (sub-screen):**
- Connect to any BLE device
- Browse all services and characteristics
- Read/write characteristic values
- Enable notifications on characteristics
- Negotiate MTU size
- Full device dump to JSON

**Advanced features:**
- **HCI Snoop Log Parser:** Import and analyze Bluetooth HCI snoop binary files
- **BLE Device Dumper:** Connect, read ALL readable characteristics, export complete device profile as JSON

---

#### 3. NFC Tools

**What it solves:** NFC tags are everywhere — hotel key cards, transit cards, access badges, product tags — but you can't read or understand them without tools.

**How to use:**
1. Navigate to NFC Tools
2. Hold an NFC tag against the back of your phone
3. The app automatically reads the tag type and contents

**NDEF Reading:**
- URIs (with 36 protocol prefixes: http, https, tel, mailto, etc.)
- Plain text with language codes
- Smart Posters (URI + title + icon)
- WiFi credentials (auto-parsed)
- vCard contacts
- MIME type data

**MIFARE Classic:**
- Sector-by-sector authentication using Key A/B
- Tries 10 default keys automatically (FFFFFFFFFFFF, A0A1A2A3A4A5, D3F7D3F7D3F7, etc.)
- Block-level data dump in hex
- Access bits interpretation (read/write/increment/decrement permissions)
- Individual block writing

**Host Card Emulation (HCE):**
- Emulate a Type 4 NFC tag with custom NDEF data
- Your phone becomes the tag — other devices can read it
- Responds to standard SELECT AID and READ BINARY commands

**Tag history:** All scanned tags are saved to the database for later review.

---

#### 4. Bluetooth Classic

**What it solves:** Classic Bluetooth (not BLE) devices — speakers, headphones, car kits, OBD adapters — use a different protocol. BLE Scanner won't find them.

**How to use:**
1. Navigate to Bluetooth Classic
2. See paired devices and start discovery for new ones
3. Tap a device for SDP service discovery
4. Connect via SPP for serial communication

**What you see:**
- Device name, address, bond state
- Major device class (Computer, Phone, Audio/Video, Peripheral, etc.) with minor class details
- SDP service list with UUIDs and names
- SPP serial port connection management

---

#### 5. Wi-Fi Aware

**What it solves:** Sometimes you need to discover and communicate with nearby devices without any WiFi router or internet connection.

**How to use:**
1. Navigate to Wi-Fi Aware
2. Publish or subscribe to a service
3. Discover peers on the same service

**Requires:** Wi-Fi Aware hardware (Android 8.0+, limited device support).

---

#### 6. Wi-Fi Direct

**What it solves:** Transfer files between devices without WiFi or internet — useful in field conditions, air-gapped environments, or when infrastructure is down.

**How to use:**
1. Navigate to Wi-Fi Direct
2. Discover nearby peers
3. Connect and form a group
4. Transfer files directly

**What you see:**
- Discovered peers: name, MAC, device type
- Group info: network name, passphrase, group owner, client list
- File transfer progress

---

### RF & Signal Tools

---

#### 7. IR Remote

**What it solves:** Lost your TV remote, or need to control a device you don't have the remote for.

**How to use:**
1. Navigate to IR Remote
2. Select a pre-built remote (Samsung, LG, or Sony TV)
3. Point your phone's IR blaster at the TV
4. Tap any button — power, volume, channel, navigation, etc.

**Custom protocols:**
- Select protocol: NEC, Samsung32, RC5, RC6, Sony SIRC, or Raw
- Enter command data and carrier frequency
- Import Flipper Zero `.ir` files directly

**Requires:** Consumer IR blaster (available on some Samsung, Xiaomi, Huawei devices).

---

#### 8. UWB Radar

**What it solves:** Need to know if your device supports Ultra-Wideband for precise ranging and spatial awareness.

**What you see:**
- UWB availability status
- FiRa compliance
- Supported capabilities: distance measurement, Angle of Arrival, Time of Flight, IEEE 802.15.4z

---

#### 9. SDR Radio

**What it solves:** You have an SDR dongle (RTL-SDR, HackRF, AirSpy) connected via USB OTG and want to verify detection.

**What you see:** Device name, type, VID/PID. Recognizes 8 known SDR devices.

---

#### 10. Ultrasonic Analyzer

**What it solves:** Inaudible ultrasonic beacons (18-24 kHz) embedded in TV ads, store speakers, or apps can track you across devices through your microphone. You can't hear them, but your phone can.

**How to use:**
1. Navigate to Ultrasonic Analyzer
2. Grant microphone permission
3. The analyzer records at 48 kHz and runs a 4096-point FFT
4. Watch the frequency spectrum for energy spikes in the 18-24 kHz range

**What you see:**
- Full frequency spectrum visualization
- Highlighted ultrasonic range (18-24 kHz)
- Beacon detection alerts when energy peaks are found
- Tone generator to test your own ultrasonic output

---

### Sensor Tools

---

#### 11. Sensor Dashboard

**What it solves:** Need to verify your device's sensors are working correctly, check environmental conditions, or use your phone as a measurement tool.

**How to use:**
1. Navigate to Sensor Dashboard
2. Tap **Monitor** to start reading sensors
3. Auto-stops after 60 seconds to save battery
4. Tap again to restart

**Motion section:**
- **Accelerometer:** X/Y/Z acceleration in m/s^2
- **Level Meter:** Visual pitch/roll indicator — use your phone as a spirit level
- **Vibration Detector:** Real-time vibration magnitude with peak tracking and severity classification (None/Low/Moderate/High/Extreme), history graph
- **Gyroscope:** X/Y/Z angular velocity in rad/s

**Magnetic section:**
- **Magnetometer:** X/Y/Z magnetic field in microtesla
- **Compass:** Heading in degrees with visual compass
- **Metal Detector:** Tracks deviation from magnetic baseline — move your phone near metal objects to detect them, with audio alarm

**Environment section:**
- **Barometer:** Atmospheric pressure (hPa), estimated altitude (meters), estimated floor number
- **Light Sensor:** Illuminance in lux
- **Proximity Sensor:** Distance in centimeters

---

#### 12. QR Scanner

**What it solves:** QR codes can contain malicious URLs, phishing links, or suspicious content. Scanning blindly is risky.

**How to use:**
1. Navigate to QR Scanner
2. Point camera at any barcode or QR code
3. The app scans and **analyzes the content before you open it**

**Threat analysis checks:**
- Suspicious TLDs: `.tk`, `.ml`, `.ga`, `.cf`
- Phishing patterns: `login-verify`, `secure-update`, `account-confirm`
- IP-only URLs (no domain name)
- Excessively long URLs (>100 characters)
- Threat levels: SAFE, SUSPICIOUS, DANGEROUS

**Content parsing:** URLs, WiFi credentials, vCards, email, phone, SMS, geo coordinates, plain text.

**QR Generator:** Create QR codes for text, URLs, or WiFi credentials.

**Scan history:** All scans saved to database.

---

#### 13. USB Camera

**What it solves:** Need to detect external UVC cameras connected via USB OTG.

**What you see:** Device name, VID/PID, supported resolutions, USB class information.

---

#### 14. GPS Tracker

**What it solves:** Need precise GPS data, satellite information, or raw NMEA sentences for navigation, surveying, or debugging.

**How to use:**
1. Navigate to GPS Tracker
2. Grant location permission
3. See real-time position updates at 1-second intervals

**What you see:**
- Latitude, longitude, altitude, speed, bearing, accuracy
- **Satellite list:** For each visible satellite — SVID, constellation (GPS/GLONASS/Galileo/BeiDou/QZSS/SBAS/IRNSS), signal strength (C/N0), elevation, azimuth, whether used in current fix
- **NMEA log:** Last 50 raw NMEA sentences with timestamps

---

#### 15. EMF Mapper

**What it solves:** Need to map electromagnetic field strength in a room — useful for finding hidden electronics, checking EMF exposure, or locating wiring.

**How to use:**
1. Navigate to EMF Mapper
2. Walk around slowly with your phone
3. Watch for magnetic field deviations from baseline

**What you see:**
- Current magnetic field magnitude in microtesla vs. baseline
- Deviation color coding: NORMAL (<15 uT), ELEVATED (15-40), HIGH (40-100), EXTREME (>100)
- Hotspot detection for elevated readings
- Statistics: min, max, average magnitude

---

### Network Tools

---

#### 16. USB Devices

**What it solves:** A USB device plugged into your phone via OTG could be a BadUSB attack — a device that pretends to be a keyboard and types malicious commands.

**How to use:**
1. Plug in any USB device via OTG adapter
2. Navigate to USB Devices
3. See full device inspection

**What you see:**
- VID, PID, manufacturer, product name, serial number
- USB class, subclass, interface list, endpoints
- **BadUSB detection:** Flags devices presenting as both HID (keyboard/mouse) AND Mass Storage simultaneously, or HID devices without proper identity

**Live monitoring:** Real-time USB attach/detach events.

---

#### 17. Cell Tower Analyzer

**What it solves:** IMSI catchers (Stingray devices) intercept your calls and texts by impersonating cell towers. They're invisible to normal users.

**How to use:**
1. Navigate to Cell Tower
2. Grant phone state and location permissions
3. Monitor current and neighboring cell towers

**What you see per tower:**
- Technology: LTE, GSM, WCDMA, CDMA, NR (5G), TDSCDMA
- MCC (Mobile Country Code), MNC (Mobile Network Code)
- LAC/TAC (Location/Tracking Area Code), Cell ID
- Frequency channel (ARFCN/EARFCN)
- Signal strength (RSSI/RSRP)

**IMSI catcher detection (3 algorithms):**
1. **LAC change:** Sudden Location Area Code change without user movement
2. **Signal spike:** Signal jumps >20 dBm — indicates an amplified fake tower
3. **Forced 2G downgrade:** Network forcing LTE/3G down to GSM (where encryption is breakable)

---

#### 18. Wardriving

**What it solves:** Need to map WiFi coverage over a geographic area — for site surveys, security auditing, or contributing to wireless databases.

**How to use:**
1. Navigate to Wardriving
2. Start the scan — runs as a foreground service so it continues in the background
3. Drive, walk, or bike around
4. Export collected data as WiGLE CSV

**What you see:**
- Network list with SSID, BSSID, signal, security, GPS coordinates, timestamp
- Real-time network count

**Export:** Standard WiGLE CSV format compatible with [wigle.net](https://wigle.net) for community wireless mapping.

---

### Security Tools

---

#### 19. Hidden Camera Detector

**What it solves:** Hidden cameras in hotels, Airbnbs, changing rooms, or offices. Multiple detection methods because no single method catches everything.

**How to use:**
1. Navigate to Camera Detector
2. Grant all requested permissions (WiFi, BLE, Camera, Location)
3. Start the scan
4. Walk around the room slowly

**5 detection methods:**

| Method | How It Works | What It Flags |
|--------|-------------|-----------------|
| WiFi OUI | Checks MAC address prefixes against 30+ camera manufacturers (Hikvision, Dahua, Wyze, Ring, Nest, Arlo, etc.) | Possible WiFi-connected cameras |
| WiFi SSID | Pattern matches network names for camera keywords | Cameras with default SSIDs |
| BLE Scan | Matches BLE advertisements against known camera name/OUI patterns | Possible Bluetooth-enabled cameras |
| Magnetometer | Flags magnetic anomalies >15 uT from baseline | Possible electronic devices hidden in walls/objects |
| Port Scan | Probes for RTSP (554, 8554), ONVIF (3702), HTTP (80, 8080) | Network cameras streaming video |

---

#### 20. GPS Spoof Detector

**What it solves:** GPS spoofing can make your phone report a false location — used to bypass geofencing, fake delivery locations, or mislead navigation.

**7 independent validation checks:**

| Check | What It Compares | Spoof Indicator |
|-------|-----------------|-----------------|
| GPS vs Cell Tower | GPS position vs cell tower location | Large distance discrepancy |
| Speed/Teleportation | Movement speed between fixes | Physically impossible speed |
| Altitude consistency | GPS altitude vs barometric altitude | Mismatch between sensors |
| Satellite count | Number of visible satellites | Count outside realistic range (4-32) |
| WiFi BSSID | Nearby WiFi networks | Sudden WiFi change without GPS movement |
| Accelerometer | Physical movement vs GPS movement | GPS says moving, accelerometer says still |
| Mock provider | Android mock location API | Mock location provider is enabled |

Overall confidence score aggregated from all checks.

---

#### 21. Tracker Scanner

**What it solves:** Someone may have placed a Bluetooth tracker (AirTag, SmartTag, Tile) in your bag, car, or belongings to follow your movements.

**How to use:**
1. Navigate to Tracker Scanner
2. Start BLE scanning
3. The app identifies known tracker signatures

**Detected trackers:**
- Apple AirTag
- Samsung SmartTag
- Tile (all models)
- Chipolo
- Pebblebee

**Detection method:** BLE name patterns, service UUIDs, and manufacturer-specific data bytes.

**Risk levels:**
- **HIGH:** Device seen >5 times AND tracked for >10 minutes
- **MEDIUM:** Device seen >3 times
- **LOW:** Recently detected, not yet persistent

---

#### 22. Rogue AP Detector

**What it solves:** Fake WiFi access points that steal your credentials — the most common WiFi attack.

**6 detection algorithms:**

| Attack Type | How Detected | Risk Level |
|-------------|-------------|------------|
| Evil Twin | Same SSID, different BSSID, security mismatch or different OUI | CRITICAL |
| Open Impersonator | Open network using known public WiFi names (Starbucks, airport, hotel) | HIGH |
| SSID Spoofing | Network name within edit distance <=2 of a trusted SSID | HIGH |
| Weak Security | WEP encryption or completely open | MEDIUM |
| Hidden AP | Hidden SSID with strong signal (targeted attack) | MEDIUM |
| Karma Attack | 4+ different SSIDs from the same MAC prefix | CRITICAL |

---

#### 23. Network Scanner

**What it solves:** You don't know what devices are on your network, what ports they expose, or whether they're vulnerable.

**How to use:**
1. Navigate to Network Scanner
2. The app scans all 254 addresses in your subnet
3. For each live host, it scans 22 common ports
4. Banner grabbing extracts service information
5. Vulnerability assessment flags risks

**22 ports scanned:** FTP (21), SSH (22), Telnet (23), SMTP (25), DNS (53), HTTP (80), HTTPS (443), SMB (139/445), RTSP (554), MySQL (3306), PostgreSQL (5432), Redis (6379), MongoDB (27017), SNMP (161), MQTT (1883), RDP (3389), VNC (5900), UPnP (1900), Printer/IPP (631/9100), ONVIF (3702), HTTP alt (8080).

**Vulnerability flags:**

| Severity | What | Why It's Dangerous |
|----------|------|--------------------|
| CRITICAL | Telnet exposed | Plaintext authentication, no encryption |
| CRITICAL | Database exposed (MySQL, Postgres, MongoDB, Redis) | Direct data access from network |
| HIGH | FTP, SMB, RDP, VNC open | Common attack targets |
| MEDIUM | HTTP without HTTPS, MQTT, SNMP, UPnP | Data interception, misconfig vectors |
| LOW | Printer ports | Information leakage |

**Device type inference:** Camera, Router, IoT, Server, Workstation, NAS, Printer — based on open port combinations.

---

#### 24. RF Bug Sweeper

**What it solves:** Hidden RF transmitters, wireless microphones, or surveillance devices in a room.

**3 detection methods combined:**

| Method | What It Flags | Threshold |
|--------|----------------|-----------|
| BLE Module Detection | Common RF modules: HC-05, HC-06, JDY, HM-10, ESP32, nRF5x, CC254x; suspicious OUI prefixes; strong unnamed devices | Name patterns + manufacturer data |
| Ultrasonic Beacon | Possible hidden acoustic transmitters in 18-24 kHz range | Energy spikes in FFT spectrum |
| Magnetic Anomaly | Possible electronic devices behind walls/furniture | >25 uT deviation from baseline |

---

#### 25. Proximity Radar

**What it solves:** Want a visual, radar-style view of all wireless devices around you with distance estimates.

**How to use:**
1. Navigate to Proximity Radar
2. Start scanning
3. Watch devices appear on the radar display

**How distance is estimated:**
- Log-distance path loss model
- WiFi: reference power -40 dBm at 1m, path loss exponent 3.0
- BLE: reference power -59 dBm at 1m, path loss exponent 2.7
- Each device gets a stable angle based on its address hash (positions don't jump between scans)
- Radar auto-scales from 10m to 100m based on farthest device

**Beacon classification:** Identifies Eddystone and AltBeacon format beacons.

---

#### 26. Privacy Score

**What it solves:** Is your device actually secure? Most people don't know what's misconfigured.

**How to use:**
1. Navigate to Privacy Score
2. Grant requested permissions
3. Wait for the audit to complete
4. Review your score and recommendations

**Scoring: 0-100 with letter grade** (A+ >= 95, A >= 85, B >= 70, C >= 55, D >= 40, F < 40)

**5 categories (weighted):**

| Category | Weight | What's Checked |
|----------|--------|----------------|
| WiFi Security | 30% | Encryption strength, evil twin presence, open network count, PII in SSID |
| Bluetooth Security | 20% | Tracker presence, discoverability status, BLE device density |
| Device Security | 25% | Developer options, USB debugging, mock locations, screen lock, encryption, patch age |
| Network Security | 15% | Private DNS configured, VPN active |
| Physical Security | 10% | Magnetic anomalies, ultrasonic beacons |

---

#### 27. Deauth Detector

**What it solves:** WiFi deauthentication attacks look like "bad signal" but are actually someone kicking you off the network.

**5 detection algorithms:**

| Attack Pattern | Detection Logic | Severity |
|---------------|----------------|----------|
| Deauth Flood | >3 disconnections within 60 seconds | CRITICAL |
| Signal Jamming | >30 dBm signal drop on your AP while others are stable | HIGH |
| AP Disappearance | Connected AP missing from 2+ consecutive scans | HIGH |
| Rapid Reconnect | >5 reconnections within 2 minutes | CRITICAL |
| Channel Hopping | Connected AP unexpectedly changes channel | MEDIUM |

---

#### 28. Signal Logger

**What it solves:** A live scan tells you what's around *right now*. Signal Logger tells you what changed — devices that just appeared, disappeared, or spiked in a way worth a second look.

**How to use:**
1. Navigate to Signal Logger
2. Grant WiFi + BLE permissions if prompted
3. Tap **Start** — WiFi and BLE scans run continuously in parallel
4. Watch the live feed; use the filter chips (All / WiFi / BLE / Anomalies) to narrow it down
5. Tap **Export** to copy the full log as a pipe-delimited text table to your clipboard, or the trash icon to clear it
6. Tap **Stop** when done

**What's logged:** timestamp, event type, source (SSID / BLE name), address (BSSID / MAC), RSSI, and a detail string (channel/band/security for WiFi, service-UUID count for BLE). The feed keeps the most recent 500 entries, plus a live stats row (duration, WiFi AP count, BLE count, anomaly count) and an events/min rate bar with new/lost counts.

**Anomaly detection** (flagged and filterable):
- **Signal spike** — RSSI jumps more than 20 dBm between consecutive scans
- **Hidden AP, strong signal** — blank SSID with RSSI stronger than -50 dBm (someone nearby broadcasting a hidden network)
- **New device burst** — more than 5 new WiFi APs or BLE devices in a single scan cycle, flagged as possible spoofing/flooding
- Newly-appeared devices (after the first scan cycle) are also flagged; devices that drop out are logged but not treated as anomalies

---

#### 29. Alert Center

**What it solves:** Five different detectors (Rogue AP, Deauth Detector, GPS Spoof, Hidden Camera, Tracker Scanner) can each independently spot a threat while you're focused on a different screen. Alert Center is the one place that collects everything they find, persisted, so nothing gets missed just because you weren't looking at that tool at the time.

**How to use:**
1. Open **Alert Center** from the drawer, or tap "View all →" on the Dashboard's Recent Threats card
2. Review the feed — each entry shows a color-coded severity tag, source tool, title, detail, and timestamp
3. Tap **Clear All** to wipe the history once you've addressed everything (only shown when alerts exist)

**Sources & severities:** Rogue AP, Deauth Detector, GPS Spoof, Hidden Camera, and Tracker Scanner each report into the shared feed with a severity of CRITICAL, HIGH, MEDIUM, or LOW — set by that detector's own risk/threat classification. Each source de-duplicates its own findings before recording, so a single ongoing threat doesn't spam the feed on every scan cycle.

**Persistence:** Alerts are stored in Room and survive app restarts. The feed shows up to the 300 most recent alerts, newest first, until you clear them.

---

[← Back to README](../README.md)

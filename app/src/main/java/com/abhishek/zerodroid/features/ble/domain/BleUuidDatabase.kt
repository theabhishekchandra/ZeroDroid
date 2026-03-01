package com.abhishek.zerodroid.features.ble.domain

/**
 * Maps standard Bluetooth SIG 16-bit UUIDs to human-readable names.
 * 16-bit UUIDs expand to the form: 0000XXXX-0000-1000-8000-00805f9b34fb
 */
object BleUuidDatabase {

    private const val BASE_UUID_SUFFIX = "-0000-1000-8000-00805f9b34fb"

    // ---- Service UUIDs (0x1800 - 0x18FF range) ----
    private val serviceNames = mapOf(
        0x1800 to "Generic Access",
        0x1801 to "Generic Attribute",
        0x1802 to "Immediate Alert",
        0x1803 to "Link Loss",
        0x1804 to "Tx Power",
        0x1805 to "Current Time",
        0x1806 to "Reference Time Update",
        0x1807 to "Next DST Change",
        0x1808 to "Glucose",
        0x1809 to "Health Thermometer",
        0x180A to "Device Information",
        0x180D to "Heart Rate",
        0x180E to "Phone Alert Status",
        0x180F to "Battery",
        0x1810 to "Blood Pressure",
        0x1811 to "Alert Notification",
        0x1812 to "Human Interface Device",
        0x1813 to "Scan Parameters",
        0x1814 to "Running Speed and Cadence",
        0x1815 to "Automation IO",
        0x1816 to "Cycling Speed and Cadence",
        0x1818 to "Cycling Power",
        0x1819 to "Location and Navigation",
        0x181A to "Environmental Sensing",
        0x181B to "Body Composition",
        0x181C to "User Data",
        0x181D to "Weight Scale",
        0x181E to "Bond Management",
        0x181F to "Continuous Glucose Monitoring",
        0x1820 to "Internet Protocol Support",
        0x1821 to "Indoor Positioning",
        0x1822 to "Pulse Oximeter",
        0x1823 to "HTTP Proxy",
        0x1824 to "Transport Discovery",
        0x1825 to "Object Transfer",
        0x1826 to "Fitness Machine",
        0x1827 to "Mesh Provisioning",
        0x1828 to "Mesh Proxy",
        0x1829 to "Reconnection Configuration",
        0x183A to "Insulin Delivery",
        0x183B to "Binary Sensor",
        0x183C to "Emergency Configuration",
        0x183E to "Physical Activity Monitor",
        0x1843 to "Audio Input Control",
        0x1844 to "Volume Control",
        0x1845 to "Volume Offset Control",
        0x1846 to "Coordinated Set Identification",
        0x1848 to "Media Control",
        0x1849 to "Generic Media Control",
        0x184A to "Constant Tone Extension",
        0x184B to "Telephone Bearer",
        0x184C to "Generic Telephone Bearer",
        0x184D to "Microphone Control",
        0x184E to "Audio Stream Control",
        0x184F to "Broadcast Audio Scan",
        0x1850 to "Published Audio Capabilities",
        0x1851 to "Basic Audio Announcement",
        0x1852 to "Broadcast Audio Announcement",
        0x1853 to "Common Audio",
        0x1854 to "Hearing Aid",
        0xFEF5 to "Dialog Semiconductor",
    )

    // ---- Characteristic UUIDs (0x2A00 - 0x2BFF range) ----
    private val characteristicNames = mapOf(
        0x2A00 to "Device Name",
        0x2A01 to "Appearance",
        0x2A02 to "Peripheral Privacy Flag",
        0x2A03 to "Reconnection Address",
        0x2A04 to "Preferred Connection Parameters",
        0x2A05 to "Service Changed",
        0x2A06 to "Alert Level",
        0x2A07 to "Tx Power Level",
        0x2A08 to "Date Time",
        0x2A09 to "Day of Week",
        0x2A0A to "Day Date Time",
        0x2A0D to "DST Offset",
        0x2A0F to "Local Time Information",
        0x2A11 to "Time with DST",
        0x2A12 to "Time Accuracy",
        0x2A13 to "Time Source",
        0x2A14 to "Reference Time Information",
        0x2A19 to "Battery Level",
        0x2A1C to "Temperature Measurement",
        0x2A1D to "Temperature Type",
        0x2A1E to "Intermediate Temperature",
        0x2A21 to "Measurement Interval",
        0x2A23 to "System ID",
        0x2A24 to "Model Number String",
        0x2A25 to "Serial Number String",
        0x2A26 to "Firmware Revision String",
        0x2A27 to "Hardware Revision String",
        0x2A28 to "Software Revision String",
        0x2A29 to "Manufacturer Name String",
        0x2A2A to "IEEE 11073-20601 Regulatory Cert Data List",
        0x2A2B to "Current Time",
        0x2A31 to "Scan Refresh",
        0x2A33 to "Boot Keyboard Output Report",
        0x2A34 to "Glucose Measurement Context",
        0x2A35 to "Blood Pressure Measurement",
        0x2A36 to "Intermediate Cuff Pressure",
        0x2A37 to "Heart Rate Measurement",
        0x2A38 to "Body Sensor Location",
        0x2A39 to "Heart Rate Control Point",
        0x2A3F to "Alert Status",
        0x2A40 to "Ringer Control Point",
        0x2A41 to "Ringer Setting",
        0x2A46 to "New Alert",
        0x2A49 to "Blood Pressure Feature",
        0x2A4A to "HID Information",
        0x2A4B to "Report Map",
        0x2A4C to "HID Control Point",
        0x2A4D to "Report",
        0x2A4E to "Protocol Mode",
        0x2A50 to "PnP ID",
        0x2A51 to "Glucose Feature",
        0x2A52 to "Record Access Control Point",
        0x2A53 to "RSC Measurement",
        0x2A54 to "RSC Feature",
        0x2A55 to "SC Control Point",
        0x2A5A to "Aggregate",
        0x2A5B to "CSC Measurement",
        0x2A5C to "CSC Feature",
        0x2A5D to "Sensor Location",
        0x2A63 to "Cycling Power Measurement",
        0x2A64 to "Cycling Power Vector",
        0x2A65 to "Cycling Power Feature",
        0x2A66 to "Cycling Power Control Point",
        0x2A67 to "Location and Speed",
        0x2A68 to "Navigation",
        0x2A69 to "Position Quality",
        0x2A6A to "LN Feature",
        0x2A6B to "LN Control Point",
        0x2A6C to "Elevation",
        0x2A6D to "Pressure",
        0x2A6E to "Temperature",
        0x2A6F to "Humidity",
        0x2A70 to "True Wind Speed",
        0x2A71 to "True Wind Direction",
        0x2A72 to "Apparent Wind Speed",
        0x2A73 to "Apparent Wind Direction",
        0x2A74 to "Gust Factor",
        0x2A75 to "Pollen Concentration",
        0x2A76 to "UV Index",
        0x2A77 to "Irradiance",
        0x2A78 to "Rainfall",
        0x2A79 to "Wind Chill",
        0x2A7A to "Heat Index",
        0x2A7B to "Dew Point",
        0x2A7D to "Descriptor Value Changed",
        0x2A7E to "Aerobic Heart Rate Lower Limit",
        0x2A84 to "Aerobic Heart Rate Upper Limit",
        0x2A8E to "Height",
        0x2A98 to "Weight",
        0x2A99 to "Database Change Increment",
        0x2A9A to "User Index",
        0x2A9F to "User Control Point",
        0x2AA6 to "Central Address Resolution",
    )

    // ---- Descriptor UUIDs (0x2900 - 0x290F range) ----
    private val descriptorNames = mapOf(
        0x2900 to "Characteristic Extended Properties",
        0x2901 to "Characteristic User Description",
        0x2902 to "Client Characteristic Configuration",
        0x2903 to "Server Characteristic Configuration",
        0x2904 to "Characteristic Presentation Format",
        0x2905 to "Characteristic Aggregate Format",
        0x2906 to "Valid Range",
        0x2907 to "External Report Reference",
        0x2908 to "Report Reference",
        0x2909 to "Number of Digitals",
        0x290A to "Value Trigger Setting",
        0x290B to "Environmental Sensing Configuration",
        0x290C to "Environmental Sensing Measurement",
        0x290D to "Environmental Sensing Trigger Setting",
    )

    /**
     * Extracts the 16-bit short UUID from a standard 128-bit Bluetooth SIG UUID.
     * Returns null if the UUID is not a standard SIG UUID.
     */
    private fun extract16Bit(uuid: String): Int? {
        val lower = uuid.lowercase()
        if (!lower.endsWith(BASE_UUID_SUFFIX)) return null
        val prefix = lower.removePrefix("0000").substringBefore("-")
        return try {
            prefix.toInt(16)
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * Returns the human-readable name for a GATT service UUID, or a shortened UUID string.
     */
    fun serviceDisplayName(uuid: String): String {
        val shortId = extract16Bit(uuid)
        if (shortId != null) {
            serviceNames[shortId]?.let { return it }
        }
        return shortenUuid(uuid)
    }

    /**
     * Returns the human-readable name for a GATT characteristic UUID, or a shortened UUID string.
     */
    fun characteristicDisplayName(uuid: String): String {
        val shortId = extract16Bit(uuid)
        if (shortId != null) {
            characteristicNames[shortId]?.let { return it }
        }
        return shortenUuid(uuid)
    }

    /**
     * Returns the human-readable name for a GATT descriptor UUID, or a shortened UUID string.
     */
    fun descriptorDisplayName(uuid: String): String {
        val shortId = extract16Bit(uuid)
        if (shortId != null) {
            descriptorNames[shortId]?.let { return it }
        }
        return shortenUuid(uuid)
    }

    /**
     * Converts a full 128-bit UUID to a short form if it is a standard BT SIG UUID.
     * "0000180d-0000-1000-8000-00805f9b34fb" -> "0x180D"
     * Custom UUIDs are returned with first 8 chars: "12345678-..."
     */
    fun shortenUuid(uuid: String): String {
        val lower = uuid.lowercase()
        if (lower.endsWith(BASE_UUID_SUFFIX)) {
            val hex = lower.substringBefore("-").removePrefix("0000").trimStart('0')
            val shortHex = hex.ifEmpty { "0" }
            return "0x${shortHex.uppercase()}"
        }
        // For custom UUIDs, show first segment
        return uuid.substringBefore("-").let { first ->
            if (uuid.length > first.length) "$first..." else uuid
        }
    }
}

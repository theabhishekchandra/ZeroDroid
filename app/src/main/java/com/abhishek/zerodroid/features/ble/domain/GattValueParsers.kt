package com.abhishek.zerodroid.features.ble.domain

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parses raw bytes from standard BLE characteristics into human-readable strings.
 */
object GattValueParsers {

    // Standard characteristic UUIDs (lowercase, full 128-bit form)
    private const val UUID_HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"
    private const val UUID_BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb"
    private const val UUID_BLOOD_PRESSURE = "00002a35-0000-1000-8000-00805f9b34fb"
    private const val UUID_TEMPERATURE_MEASUREMENT = "00002a1c-0000-1000-8000-00805f9b34fb"
    private const val UUID_TEMPERATURE = "00002a6e-0000-1000-8000-00805f9b34fb"
    private const val UUID_HUMIDITY = "00002a6f-0000-1000-8000-00805f9b34fb"
    private const val UUID_PRESSURE = "00002a6d-0000-1000-8000-00805f9b34fb"
    private const val UUID_BODY_SENSOR_LOCATION = "00002a38-0000-1000-8000-00805f9b34fb"
    private const val UUID_TX_POWER_LEVEL = "00002a07-0000-1000-8000-00805f9b34fb"

    // String characteristics
    private const val UUID_DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb"
    private const val UUID_MODEL_NUMBER = "00002a24-0000-1000-8000-00805f9b34fb"
    private const val UUID_SERIAL_NUMBER = "00002a25-0000-1000-8000-00805f9b34fb"
    private const val UUID_FIRMWARE_REVISION = "00002a26-0000-1000-8000-00805f9b34fb"
    private const val UUID_HARDWARE_REVISION = "00002a27-0000-1000-8000-00805f9b34fb"
    private const val UUID_SOFTWARE_REVISION = "00002a28-0000-1000-8000-00805f9b34fb"
    private const val UUID_MANUFACTURER_NAME = "00002a29-0000-1000-8000-00805f9b34fb"
    private const val UUID_SYSTEM_ID = "00002a23-0000-1000-8000-00805f9b34fb"
    private const val UUID_PNP_ID = "00002a50-0000-1000-8000-00805f9b34fb"

    private val stringCharacteristics = setOf(
        UUID_DEVICE_NAME, UUID_MODEL_NUMBER, UUID_SERIAL_NUMBER,
        UUID_FIRMWARE_REVISION, UUID_HARDWARE_REVISION, UUID_SOFTWARE_REVISION,
        UUID_MANUFACTURER_NAME
    )

    /**
     * Main entry point: attempts to parse [data] based on the [characteristicUuid].
     * Returns a human-readable string, or null if no parser is available.
     */
    fun parse(characteristicUuid: String, data: ByteArray): String? {
        if (data.isEmpty()) return null
        val uuid = characteristicUuid.lowercase()

        return when (uuid) {
            UUID_HEART_RATE_MEASUREMENT -> parseHeartRate(data)
            UUID_BATTERY_LEVEL -> parseBatteryLevel(data)
            UUID_BLOOD_PRESSURE -> parseBloodPressure(data)
            UUID_TEMPERATURE_MEASUREMENT -> parseTemperatureMeasurement(data)
            UUID_TEMPERATURE -> parseEnvironmentalTemperature(data)
            UUID_HUMIDITY -> parseEnvironmentalHumidity(data)
            UUID_PRESSURE -> parseEnvironmentalPressure(data)
            UUID_BODY_SENSOR_LOCATION -> parseBodySensorLocation(data)
            UUID_TX_POWER_LEVEL -> parseTxPowerLevel(data)
            UUID_SYSTEM_ID -> parseSystemId(data)
            UUID_PNP_ID -> parsePnpId(data)
            in stringCharacteristics -> parseUtf8String(data)
            else -> null
        }
    }

    // ---- Heart Rate Measurement (0x2A37) ----
    private fun parseHeartRate(data: ByteArray): String? {
        if (data.isEmpty()) return null
        val flags = data[0].toInt() and 0xFF
        val is16Bit = flags and 0x01 != 0
        val hasEnergyExpended = flags and 0x08 != 0
        val hasRrInterval = flags and 0x10 != 0

        val sb = StringBuilder()
        var offset = 1

        // Heart rate value
        if (is16Bit) {
            if (data.size < 3) return null
            val hr = readUInt16LE(data, offset)
            offset += 2
            sb.append("Heart Rate: $hr bpm (16-bit)")
        } else {
            if (data.size < 2) return null
            val hr = data[offset].toInt() and 0xFF
            offset += 1
            sb.append("Heart Rate: $hr bpm")
        }

        // Energy Expended
        if (hasEnergyExpended && offset + 2 <= data.size) {
            val energy = readUInt16LE(data, offset)
            offset += 2
            sb.append("\nEnergy Expended: $energy kJ")
        }

        // RR-Intervals
        if (hasRrInterval) {
            val rrIntervals = mutableListOf<Double>()
            while (offset + 2 <= data.size) {
                val rr = readUInt16LE(data, offset)
                offset += 2
                rrIntervals.add(rr / 1024.0 * 1000.0) // Convert to ms
            }
            if (rrIntervals.isNotEmpty()) {
                sb.append("\nRR-Intervals: ${rrIntervals.joinToString(", ") { "%.0f ms".format(it) }}")
            }
        }

        return sb.toString()
    }

    // ---- Battery Level (0x2A19) ----
    private fun parseBatteryLevel(data: ByteArray): String? {
        if (data.isEmpty()) return null
        val level = data[0].toInt() and 0xFF
        return "Battery: $level%"
    }

    // ---- Blood Pressure Measurement (0x2A35) ----
    private fun parseBloodPressure(data: ByteArray): String? {
        if (data.size < 7) return null
        val flags = data[0].toInt() and 0xFF
        val unit = if (flags and 0x01 != 0) "kPa" else "mmHg"

        val systolic = readSFloat(data, 1)
        val diastolic = readSFloat(data, 3)
        val map = readSFloat(data, 5)

        return buildString {
            append("Systolic: %.1f $unit".format(systolic))
            append("\nDiastolic: %.1f $unit".format(diastolic))
            append("\nMAP: %.1f $unit".format(map))
        }
    }

    // ---- Temperature Measurement (0x2A1C) - IEEE-11073 FLOAT ----
    private fun parseTemperatureMeasurement(data: ByteArray): String? {
        if (data.size < 5) return null
        val flags = data[0].toInt() and 0xFF
        val unit = if (flags and 0x01 != 0) "F" else "C"
        val temp = readFloat32(data, 1)
        return "Temperature: %.2f %s".format(temp, unit)
    }

    // ---- Environmental Temperature (0x2A6E) - sint16 * 0.01 ----
    private fun parseEnvironmentalTemperature(data: ByteArray): String? {
        if (data.size < 2) return null
        val raw = readInt16LE(data, 0)
        val temp = raw * 0.01
        return "Temperature: %.2f C".format(temp)
    }

    // ---- Environmental Humidity (0x2A6F) - uint16 * 0.01 ----
    private fun parseEnvironmentalHumidity(data: ByteArray): String? {
        if (data.size < 2) return null
        val raw = readUInt16LE(data, 0)
        val humidity = raw * 0.01
        return "Humidity: %.2f %%".format(humidity)
    }

    // ---- Environmental Pressure (0x2A6D) - uint32 * 0.1 Pa ----
    private fun parseEnvironmentalPressure(data: ByteArray): String? {
        if (data.size < 4) return null
        val raw = readUInt32LE(data, 0)
        val pressurePa = raw * 0.1
        val pressureHpa = pressurePa / 100.0
        return "Pressure: %.1f hPa (%.0f Pa)".format(pressureHpa, pressurePa)
    }

    // ---- Body Sensor Location (0x2A38) ----
    private fun parseBodySensorLocation(data: ByteArray): String? {
        if (data.isEmpty()) return null
        val location = when (data[0].toInt() and 0xFF) {
            0 -> "Other"
            1 -> "Chest"
            2 -> "Wrist"
            3 -> "Finger"
            4 -> "Hand"
            5 -> "Ear Lobe"
            6 -> "Foot"
            else -> "Unknown (${data[0].toInt() and 0xFF})"
        }
        return "Body Sensor Location: $location"
    }

    // ---- Tx Power Level (0x2A07) ----
    private fun parseTxPowerLevel(data: ByteArray): String? {
        if (data.isEmpty()) return null
        val power = data[0].toInt() // signed int8
        return "Tx Power: $power dBm"
    }

    // ---- System ID (0x2A23) ----
    private fun parseSystemId(data: ByteArray): String? {
        if (data.size < 8) return null
        val manufacturer = data.sliceArray(0..4).joinToString(":") { "%02X".format(it) }
        val orgUnique = data.sliceArray(5..7).joinToString(":") { "%02X".format(it) }
        return "Manufacturer ID: $manufacturer\nOrg Unique ID: $orgUnique"
    }

    // ---- PnP ID (0x2A50) ----
    private fun parsePnpId(data: ByteArray): String? {
        if (data.size < 7) return null
        val vendorSource = when (data[0].toInt() and 0xFF) {
            1 -> "Bluetooth SIG"
            2 -> "USB Implementer's Forum"
            else -> "Unknown"
        }
        val vendorId = readUInt16LE(data, 1)
        val productId = readUInt16LE(data, 3)
        val version = readUInt16LE(data, 5)
        return "Vendor Source: $vendorSource\nVendor ID: 0x%04X\nProduct ID: 0x%04X\nVersion: %d.%d.%d".format(
            vendorId, productId,
            (version shr 8) and 0xFF, (version shr 4) and 0x0F, version and 0x0F
        )
    }

    // ---- UTF-8 String ----
    private fun parseUtf8String(data: ByteArray): String? {
        if (data.isEmpty()) return null
        return String(data, Charsets.UTF_8).trimEnd('\u0000')
    }

    // ---- Helper functions ----

    fun readUInt16LE(data: ByteArray, offset: Int): Int {
        if (offset + 2 > data.size) return 0
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    fun readInt16LE(data: ByteArray, offset: Int): Int {
        val raw = readUInt16LE(data, offset)
        return if (raw >= 0x8000) raw - 0x10000 else raw
    }

    fun readUInt32LE(data: ByteArray, offset: Int): Long {
        if (offset + 4 > data.size) return 0L
        return (data[offset].toLong() and 0xFF) or
                ((data[offset + 1].toLong() and 0xFF) shl 8) or
                ((data[offset + 2].toLong() and 0xFF) shl 16) or
                ((data[offset + 3].toLong() and 0xFF) shl 24)
    }

    /**
     * Reads an IEEE-11073 32-bit FLOAT from [data] at [offset].
     * Format: 8-bit exponent (signed) + 24-bit mantissa (signed).
     */
    fun readFloat32(data: ByteArray, offset: Int): Double {
        if (offset + 4 > data.size) return 0.0
        val raw = readUInt32LE(data, offset).toInt()
        // Special values
        if (raw == 0x007FFFFF || raw == 0x00800000.toInt() || raw == 0x007FFFFE) return Double.NaN

        var mantissa = raw and 0x00FFFFFF
        if (mantissa >= 0x800000) {
            mantissa -= 0x1000000
        }
        val exponent = (raw shr 24).toByte().toInt()

        return mantissa * Math.pow(10.0, exponent.toDouble())
    }

    /**
     * Reads an IEEE-11073 16-bit SFLOAT from [data] at [offset].
     * Format: 4-bit exponent (signed) + 12-bit mantissa (signed).
     */
    fun readSFloat(data: ByteArray, offset: Int): Double {
        if (offset + 2 > data.size) return 0.0
        val raw = readUInt16LE(data, offset)
        // Special values
        if (raw == 0x07FF || raw == 0x0800 || raw == 0x07FE) return Double.NaN

        var mantissa = raw and 0x0FFF
        if (mantissa >= 0x0800) {
            mantissa -= 0x1000
        }
        var exponent = (raw shr 12) and 0x0F
        if (exponent >= 0x08) {
            exponent -= 0x10
        }

        return mantissa * Math.pow(10.0, exponent.toDouble())
    }
}

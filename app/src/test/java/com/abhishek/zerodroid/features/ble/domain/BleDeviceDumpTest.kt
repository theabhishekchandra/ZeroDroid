package com.abhishek.zerodroid.features.ble.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BleDeviceDumpTest {

    private val readable = DumpedCharacteristic(
        uuid = "00002a19-0000-1000-8000-00805f9b34fb", displayName = "Battery Level",
        serviceUuid = "0000180f-0000-1000-8000-00805f9b34fb", properties = 0x02,
        value = byteArrayOf(0x64), hexString = "64", readError = null
    )
    private val writable = DumpedCharacteristic(
        uuid = "6e400002-b5a3-f393-e0a9-e50e24dcca9e", displayName = "RX",
        serviceUuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e", properties = 0x0C,
        value = byteArrayOf(0x01, 0xFF.toByte()), hexString = "01ff", readError = null
    )
    private val failed = DumpedCharacteristic(
        uuid = "00002a00-0000-1000-8000-00805f9b34fb", displayName = "Device Name",
        serviceUuid = "00001800-0000-1000-8000-00805f9b34fb", properties = 0x02,
        value = null, hexString = "", readError = "GATT_READ_NOT_PERMITTED"
    )
    private val dump = BleDeviceDump(
        deviceAddress = "AA:BB:CC:DD:EE:FF", deviceName = "Sensor", timestamp = 1_700_000_000_000L, mtu = 247,
        services = listOf(
            DumpedService("0000180f-0000-1000-8000-00805f9b34fb", "Battery", listOf(readable)),
            DumpedService("6e400001-b5a3-f393-e0a9-e50e24dcca9e", "Nordic UART", listOf(writable, failed))
        )
    )

    @Test
    fun `counts characteristics reads and failures`() {
        assertEquals(3, dump.totalCharacteristics)
        assertEquals(2, dump.successfulReads)
        assertEquals(1, dump.failedReads)
    }

    @Test
    fun `replayable requires a value and a write property`() {
        assertTrue(writable.isReplayable)
        assertFalse(readable.isReplayable)
        assertFalse(failed.copy(properties = 0x08).isReplayable)
    }

    @Test
    fun `json round trip preserves every field`() {
        val restored = BleDeviceDump.fromJson(dump.toJson())

        assertEquals(dump.deviceAddress, restored.deviceAddress)
        assertEquals(dump.deviceName, restored.deviceName)
        assertEquals(dump.timestamp, restored.timestamp)
        assertEquals(dump.mtu, restored.mtu)
        assertEquals(dump.services.map { it.uuid }, restored.services.map { it.uuid })
        assertEquals(dump.services, restored.services)
        assertArrayEquals(byteArrayOf(0x01, 0xFF.toByte()), restored.services[1].characteristics[0].value)
        assertNull(restored.services[1].characteristics[1].value)
        assertEquals("GATT_READ_NOT_PERMITTED", restored.services[1].characteristics[1].readError)
    }

    @Test
    fun `null device name survives the round trip`() {
        val restored = BleDeviceDump.fromJson(dump.copy(deviceName = null).toJson())
        assertNull(restored.deviceName)
    }

    @Test
    fun `json exposes hex value and null markers`() {
        val json = dump.toJson()
        val chars = json.getJSONArray("services").getJSONObject(1).getJSONArray("characteristics")

        assertEquals("01ff", chars.getJSONObject(0).getString("rawBytesHex"))
        assertTrue(chars.getJSONObject(1).isNull("rawBytesHex"))
        assertTrue(chars.getJSONObject(1).isNull("valueHex"))
    }
}

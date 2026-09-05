package com.abhishek.zerodroid.features.ble.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BleUuidDatabaseTest {

    @Test
    fun `resolves standard service characteristic and descriptor names`() {
        assertEquals("Heart Rate", BleUuidDatabase.serviceDisplayName("0000180d-0000-1000-8000-00805f9b34fb"))
        assertEquals("Battery Level", BleUuidDatabase.characteristicDisplayName("00002a19-0000-1000-8000-00805f9b34fb"))
        assertEquals("Client Characteristic Configuration", BleUuidDatabase.descriptorDisplayName("00002902-0000-1000-8000-00805f9b34fb"))
    }

    @Test
    fun `lookup is case insensitive`() {
        assertEquals("Device Information", BleUuidDatabase.serviceDisplayName("0000180A-0000-1000-8000-00805F9B34FB"))
    }

    @Test
    fun `unknown standard uuids fall back to short hex form`() {
        assertEquals("0x1234", BleUuidDatabase.serviceDisplayName("00001234-0000-1000-8000-00805f9b34fb"))
        assertEquals("0xABCD", BleUuidDatabase.characteristicDisplayName("0000abcd-0000-1000-8000-00805f9b34fb"))
    }

    @Test
    fun `custom uuids show their first segment`() {
        assertEquals("6e400001...", BleUuidDatabase.serviceDisplayName("6e400001-b5a3-f393-e0a9-e50e24dcca9e"))
    }

    @Test
    fun `shortenUuid strips leading zeros and handles degenerate input`() {
        assertEquals("0x180D", BleUuidDatabase.shortenUuid("0000180d-0000-1000-8000-00805f9b34fb"))
        assertEquals("0x0", BleUuidDatabase.shortenUuid("00000000-0000-1000-8000-00805f9b34fb"))
        assertEquals("abc", BleUuidDatabase.shortenUuid("abc"))
    }

    @Test
    fun `service names are not used for characteristic lookups`() {
        // 0x180D is a service; as a characteristic it is unknown and shortened.
        assertEquals("0x180D", BleUuidDatabase.characteristicDisplayName("0000180d-0000-1000-8000-00805f9b34fb"))
    }
}

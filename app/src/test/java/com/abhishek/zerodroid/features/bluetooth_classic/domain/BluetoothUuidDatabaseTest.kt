package com.abhishek.zerodroid.features.bluetooth_classic.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BluetoothUuidDatabaseTest {

    @Test
    fun `resolves standard profiles from full uuids`() {
        val spp = BluetoothUuidDatabase.lookup("00001101-0000-1000-8000-00805F9B34FB")

        assertEquals("SPP", spp.profileName)
        assertEquals("Serial Port Profile", spp.description)
        assertEquals("0x1101", spp.shortUuid)
        assertEquals("00001101-0000-1000-8000-00805f9b34fb", spp.uuid)
    }

    @Test
    fun `resolves audio and input profiles`() {
        assertEquals("A2DP Sink", BluetoothUuidDatabase.lookup("0000110b-0000-1000-8000-00805f9b34fb").profileName)
        assertEquals("HFP", BluetoothUuidDatabase.lookup("0000111e-0000-1000-8000-00805f9b34fb").profileName)
        assertEquals("HID", BluetoothUuidDatabase.lookup("00001124-0000-1000-8000-00805f9b34fb").profileName)
    }

    @Test
    fun `unknown standard uuids are labelled with their short form`() {
        val info = BluetoothUuidDatabase.lookup("0000abcd-0000-1000-8000-00805f9b34fb")

        assertEquals("Unknown Profile (0xABCD)", info.profileName)
        assertEquals("0xABCD", info.shortUuid)
    }

    @Test
    fun `32 bit standard uuids keep the full prefix`() {
        val info = BluetoothUuidDatabase.lookup("12345678-0000-1000-8000-00805f9b34fb")
        assertEquals("0x12345678", info.shortUuid)
    }

    @Test
    fun `vendor uuids are reported as vendor services`() {
        val info = BluetoothUuidDatabase.lookup("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")

        assertEquals("vendor", info.shortUuid)
        assertEquals("Vendor Service", info.profileName)
    }

    @Test
    fun `classic device model labels bond state and name`() {
        val d = ClassicBluetoothDevice(name = null, address = "AA", bondState = 12)
        assertEquals("Unknown Device", d.displayName)
        assertEquals("Paired", d.bondStateLabel)
        assertEquals("Pairing...", d.copy(bondState = 11).bondStateLabel)
        assertEquals("Not Paired", d.copy(bondState = 10).bondStateLabel)
        assertEquals("Unknown", d.copy(bondState = 0).bondStateLabel)
    }
}

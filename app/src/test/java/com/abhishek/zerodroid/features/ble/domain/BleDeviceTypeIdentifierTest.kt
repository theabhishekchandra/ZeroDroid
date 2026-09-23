package com.abhishek.zerodroid.features.ble.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BleDeviceTypeIdentifierTest {

    private fun category(name: String?) = BleDeviceTypeIdentifier.identify(name).category

    @Test
    fun `classifies well known device names`() {
        assertEquals("Audio", category("Abhishek's AirPods Pro"))
        assertEquals("Fitness", category("Mi Band 7"))
        assertEquals("Tracker", category("Tile Pro"))
        assertEquals("Input", category("MX Master 3"))
        assertEquals("TV/Media", category("Living Room TV"))
        assertEquals("Phone", category("Pixel 8"))
        assertEquals("Tablet", category("iPad Air"))
        assertEquals("Printer", category("HP DeskJet"))
        assertEquals("Smart Home", category("Hue Bridge"))
        assertEquals("Automotive", category("ELM327 OBD"))
    }

    @Test
    fun `matching is case insensitive`() {
        assertEquals("Audio", category("JBL FLIP"))
        assertEquals("Audio", category("jbl flip"))
    }

    @Test
    fun `first matching pattern wins`() {
        // "Galaxy Buds" matches Audio before Phone.
        assertEquals("Audio", category("Galaxy Buds2"))
    }

    @Test
    fun `unknown and null names fall back to Unknown with bluetooth icon`() {
        val unknown = BleDeviceTypeIdentifier.identify(null)
        assertEquals("Unknown", unknown.category)
        assertEquals("bluetooth", unknown.icon)
        assertEquals("Unknown", category("XJ-9000"))
    }

    @Test
    fun `ble device model exposes display name and signal percent`() {
        assertEquals("Unknown Device", BleDevice(null, "AA", -60).displayName)
        assertEquals(100, BleDevice("x", "AA", -40).signalPercent)
        assertEquals(0, BleDevice("x", "AA", -110).signalPercent)
        assertEquals(50, BleDevice("x", "AA", -75).signalPercent)
    }

    @Test
    fun `tracker service UUIDs identify unnamed trackers`() {
        org.junit.Assert.assertEquals("Tracker", BleDeviceTypeIdentifier.identify(null, listOf("0000feed-0000-1000-8000-00805f9b34fb")).category)
        org.junit.Assert.assertEquals("Tracker", BleDeviceTypeIdentifier.identify(null, listOf("0000FD5A-0000-1000-8000-00805F9B34FB")).category)
        org.junit.Assert.assertEquals("Unknown", BleDeviceTypeIdentifier.identify(null, listOf("0000180f-0000-1000-8000-00805f9b34fb")).category)
    }
}

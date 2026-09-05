package com.abhishek.zerodroid.features.usb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbDeviceDatabaseTest {

    @Test
    fun `looks up by numeric vid and pid`() {
        val ducky = UsbDeviceDatabase.lookup(0x1FC9, 0x0083)
        assertNotNull(ducky)
        assertEquals("USB Rubber Ducky", ducky!!.productName)
        assertEquals(ThreatLevel.DANGER, ducky.threatLevel)
    }

    @Test
    fun `looks up by string case insensitively`() {
        assertEquals("Flipper Zero", UsbDeviceDatabase.lookup("0483:5740")!!.productName)
        assertEquals("Flipper Zero", UsbDeviceDatabase.lookup("0483:5740".lowercase())!!.productName)
    }

    @Test
    fun `unknown devices return null`() {
        assertNull(UsbDeviceDatabase.lookup(0x1234, 0x5678))
        assertNull(UsbDeviceDatabase.lookup("FFFF:FFFF"))
    }

    @Test
    fun `consumer devices are safe and attack tools are not`() {
        assertEquals(ThreatLevel.SAFE, UsbDeviceDatabase.lookup(0x18D1, 0x4EE7)!!.threatLevel)
        assertTrue(UsbDeviceDatabase.lookup(0x0CF3, 0x9271)!!.threatLevel == ThreatLevel.DANGER)
        assertEquals(ThreatLevel.NORMAL, UsbDeviceDatabase.lookup(0x0BDA, 0x2838)!!.threatLevel)
    }

    private fun device(interfaces: List<Int>, manufacturer: String? = "Acme", product: String? = "Widget") =
        UsbDeviceInfo(
            vendorId = 0x0ABC, productId = 0x0DEF, deviceName = "/dev/bus/usb/001/002",
            manufacturerName = manufacturer, productName = product, deviceClass = 0, deviceSubclass = 0,
            interfaceCount = interfaces.size,
            interfaces = interfaces.mapIndexed { i, cls -> UsbInterfaceInfo(i, cls, 0, 0, 0) }
        )

    @Test
    fun `device info formats ids and class names`() {
        val d = device(listOf(3, 8))
        assertEquals("0ABC:0DEF", d.vidPid)
        assertEquals("Per-Interface", d.deviceClassName)
        assertEquals(listOf("HID", "Mass Storage"), d.interfaces.map { it.className })
        assertEquals("Unknown (42)", device(emptyList()).copy(deviceClass = 42).deviceClassName)
    }

    @Test
    fun `BadUSB indicators fire on HID plus storage and anonymous HID`() {
        assertEquals(listOf(BadUsbIndicator.HID_PLUS_STORAGE), device(listOf(3, 8)).badUsbIndicators)
        assertEquals(listOf(BadUsbIndicator.HID_NO_IDENTITY), device(listOf(3), manufacturer = null).badUsbIndicators)
        assertEquals(
            listOf(BadUsbIndicator.HID_PLUS_STORAGE, BadUsbIndicator.HID_NO_IDENTITY),
            device(listOf(3, 8), product = null).badUsbIndicators
        )
        assertTrue(device(listOf(8)).badUsbIndicators.isEmpty())
        assertTrue(device(listOf(3)).badUsbIndicators.isEmpty())
    }
}

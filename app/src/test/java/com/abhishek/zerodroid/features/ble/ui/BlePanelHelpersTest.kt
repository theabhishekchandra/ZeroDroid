package com.abhishek.zerodroid.features.ble.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlePanelHelpersTest {

    @Test
    fun `hci offsets are relative to the first packet`() {
        assertEquals("00:00.000", hciOffset(5_000_000, 5_000_000))
        assertEquals("00:01.203", hciOffset(6_203_000, 5_000_000))
        assertEquals("02:05.010", hciOffset(130_010_000, 5_000_000))
        assertEquals("00:00.000", hciOffset(1, 5))
    }

    @Test
    fun `sizes read naturally`() {
        assertEquals("unknown size", formatBytes(-1))
        assertEquals("512 B", formatBytes(512))
        assertEquals("2 KB", formatBytes(2048))
        assertEquals("1.8 MB", formatBytes(1_887_437))
    }

    @Test
    fun `gatt property bits map to the right tags`() {
        assertEquals(listOf("R"), propertyTags(0x02))
        assertEquals(listOf("WNR"), propertyTags(0x04))
        assertEquals(listOf("W"), propertyTags(0x08))
        assertEquals(listOf("R", "W", "N", "I"), propertyTags(0x02 or 0x08 or 0x10 or 0x20))
    }

    @Test
    fun `printable only for mostly-text values`() {
        assertEquals("WH-1000XM5", printable("WH-1000XM5".toByteArray()))
        assertEquals("v1.2", printable("v1.2\u0000\u0000".toByteArray()))
        assertNull(printable(byteArrayOf(0x57, 0x01, 0x02, 0x03)))
        assertNull(printable(ByteArray(0)))
    }
}

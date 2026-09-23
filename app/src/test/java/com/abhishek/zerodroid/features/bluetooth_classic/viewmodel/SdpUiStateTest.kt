package com.abhishek.zerodroid.features.bluetooth_classic.viewmodel

import com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothUuidDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SdpUiStateTest {

    private fun svc(short: String) = BluetoothUuidDatabase.lookup("0000$short-0000-1000-8000-00805f9b34fb")

    @Test
    fun `serial port is detected from the SPP service`() {
        assertTrue(SdpUiState(services = listOf(svc("110b"), svc("1101"))).hasSerialPort)
        assertFalse(SdpUiState(services = listOf(svc("110b"))).hasSerialPort)
    }

    @Test
    fun `standard and vendor uuids resolve`() {
        assertEquals("SPP", svc("1101").profileName)
        assertEquals("0x1101", svc("1101").shortUuid)
        assertEquals("vendor", BluetoothUuidDatabase.lookup("8a0f1b2c-0000-4000-8000-123456789abc").shortUuid)
    }
}

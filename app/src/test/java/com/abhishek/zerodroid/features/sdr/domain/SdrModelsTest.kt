package com.abhishek.zerodroid.features.sdr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SdrModelsTest {

    @Test
    fun `vid pid is zero padded upper case hex`() {
        val info = SdrDeviceInfo(0x0BDA, 0x2838, "RTL-SDR", "RTL2838UHIDIR (RTL-SDR v3)", isRtlSdr = true)
        assertEquals("0BDA:2838", info.vidPid)
        assertEquals("1D50:604B", info.copy(vendorId = 0x1D50, productId = 0x604B).vidPid)
    }

    @Test
    fun `default state has no devices or connection`() {
        val state = SdrState()
        assertTrue(state.devices.isEmpty())
        assertNull(state.connectedVidPid)
        assertNull(state.connectionError)
    }

    @Test
    fun `detector without a usb manager reports nothing`() {
        assertTrue(SdrDetector(null).detect().isEmpty())
    }
}

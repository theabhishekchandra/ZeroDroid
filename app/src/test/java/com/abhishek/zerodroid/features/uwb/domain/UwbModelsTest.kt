package com.abhishek.zerodroid.features.uwb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class UwbModelsTest {

    @Test
    fun `initial state is idle with empty inputs`() {
        val state = UwbState()
        assertFalse(state.isHardwareAvailable)
        assertFalse(state.isRanging)
        assertEquals(UwbRole.NONE, state.role)
        assertNull(state.measurement)
        assertEquals("", state.peerAddressInput)
    }

    @Test
    fun `device info defaults to unknown chipset`() {
        assertEquals("Unknown", UwbDeviceInfo(isAvailable = false).chipset)
    }

    @Test
    fun `measurement fields are optional`() {
        val m = UwbRangingMeasurement(distanceMeters = 1.5f)
        assertEquals(1.5f, m.distanceMeters!!, 0f)
        assertNull(m.azimuthDegrees)
    }
}

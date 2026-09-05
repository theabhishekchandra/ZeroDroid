package com.abhishek.zerodroid.features.wifi_direct.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WifiDirectModelsTest {

    @Test
    fun `peer status codes map to labels`() {
        fun label(status: Int) = WifiDirectPeer("Phone", "AA:BB", status = status).statusLabel

        assertEquals("Connected", label(0))
        assertEquals("Invited", label(1))
        assertEquals("Failed", label(2))
        assertEquals("Available", label(3))
        assertEquals("Unavailable", label(4))
        assertEquals("Unknown", label(99))
    }

    @Test
    fun `group defaults to no clients`() {
        val group = WifiDirectGroup("DIRECT-xy", null, isGroupOwner = true, ownerAddress = "AA")
        assertEquals(0, group.clients.size)
    }
}

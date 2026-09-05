package com.abhishek.zerodroid.features.camera.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class QrGeneratorTest {

    @Test
    fun `formats wifi payload per the WIFI URI scheme`() {
        assertEquals(
            "WIFI:T:WPA;S:HomeNet;P:secret123;;",
            QrGenerator.formatWifiContent("HomeNet", "secret123", QrGenerator.WifiSecurity.WPA)
        )
        assertEquals(
            "WIFI:T:nopass;S:Cafe;P:;;",
            QrGenerator.formatWifiContent("Cafe", "", QrGenerator.WifiSecurity.NONE)
        )
        assertEquals("WEP", QrGenerator.WifiSecurity.WEP.code)
    }

    @Test
    fun `generated wifi payload round trips through the content parser`() {
        val payload = QrGenerator.formatWifiContent("HomeNet", "secret123", QrGenerator.WifiSecurity.WPA)
        assertEquals(QrContentType.WIFI, QrContentParser.parse(payload).first)
    }
}

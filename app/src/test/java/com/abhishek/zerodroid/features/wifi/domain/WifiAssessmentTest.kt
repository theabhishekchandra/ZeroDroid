package com.abhishek.zerodroid.features.wifi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiAssessmentTest {

    private fun ap(
        ssid: String,
        bssid: String = "A4:2B:B0:00:00:01",
        channel24: Int? = 6,
        caps: String = "[WPA2-PSK-CCMP][ESS]",
        rssi: Int = -50
    ): WifiAccessPoint {
        val freq = if (channel24 != null) 2407 + channel24 * 5 else 5180
        return WifiAccessPoint(ssid = ssid, bssid = bssid, rssi = rssi, frequency = freq, capabilities = caps)
    }

    @Test
    fun `weak security covers open, WEP and WPA only`() {
        assertTrue(WifiAssessment.isWeak(ap("a", caps = "[ESS]").security))
        assertTrue(WifiAssessment.isWeak(ap("a", caps = "[WEP][ESS]").security))
        assertFalse(WifiAssessment.isWeak(ap("a").security))
        assertFalse(WifiAssessment.isWeak(ap("a", caps = "[WPA3-SAE][ESS]").security))
    }

    @Test
    fun `grades drop with WPS and weak encryption`() {
        assertEquals("A", WifiAssessment.grade(ap("a", caps = "[WPA3-SAE][ESS]")))
        assertEquals("B", WifiAssessment.grade(ap("a", caps = "[WPA3-SAE][WPS][ESS]")))
        assertEquals("B", WifiAssessment.grade(ap("a")))
        assertEquals("F", WifiAssessment.grade(ap("a", caps = "[ESS]")))
    }

    @Test
    fun `random MACs are detected from the locally administered bit`() {
        assertTrue(WifiAssessment.isRandomMac("DA:A1:19:00:00:00"))
        assertFalse(WifiAssessment.isRandomMac("A4:2B:B0:00:00:00"))
        assertEquals("OUI A4:2B:B0", WifiAssessment.hardwareLabel("a4:2b:b0:12:34:56"))
        assertEquals("Random MAC", WifiAssessment.hardwareLabel("06:00:00:00:00:00"))
    }

    @Test
    fun `best 2_4 GHz channel avoids overlapping neighbours`() {
        val aps = listOf(ap("a", channel24 = 1), ap("b", channel24 = 3), ap("c", channel24 = 6), ap("d", channel24 = 6))
        assertEquals(11, WifiAssessment.best24Channel(aps))
        assertNull(WifiAssessment.best24Channel(listOf(ap("five", channel24 = null))))
    }

    @Test
    fun `load is zero filled and busiest needs more than one network`() {
        val aps = listOf(ap("a", channel24 = 6), ap("b", channel24 = 6), ap("c", channel24 = 1))
        val load = WifiAssessment.load(aps, WifiAssessment.CHANNELS_24)
        assertEquals(11, load.size)
        assertEquals(2, load.first { it.channel == 6 }.networks)
        assertEquals(0, load.first { it.channel == 11 }.networks)
        assertEquals(ChannelLoad(6, 2), WifiAssessment.busiestChannel(aps, com.abhishek.zerodroid.core.util.WifiBand.BAND_2_4GHZ))
        assertNull(WifiAssessment.busiestChannel(listOf(ap("a")), com.abhishek.zerodroid.core.util.WifiBand.BAND_2_4GHZ))
    }

    @Test
    fun `look-alike names are one edit away and skip hidden networks`() {
        val aps = listOf(ap("Home_5G"), ap("H0me_5G"), ap("Home_5GX"), ap("Office"), ap("<Hidden>"))
        assertEquals(listOf("H0me_5G", "Home_5GX"), WifiAssessment.lookAlikes("Home_5G", aps))
    }

    @Test
    fun `checks flag open networks, WPS and twins from other hardware`() {
        val home = ap("Home", bssid = "A4:2B:B0:00:00:01", caps = "[WPS][ESS]")
        val twin = ap("Home", bssid = "78:8A:20:00:00:02")
        val checks = WifiAssessment.checks(home, listOf(home, twin))

        assertEquals(CheckOutcome.FAIL, checks[0].outcome)
        assertEquals(CheckOutcome.WARN, checks[1].outcome)
        assertEquals("Same name, different hardware", checks[2].title)
    }

    @Test
    fun `hidden networks skip the look-alike check and display as hidden`() {
        val hidden = ap("<Hidden>")
        assertEquals(2, WifiAssessment.checks(hidden, listOf(hidden)).size)
        assertEquals("[hidden]", WifiAssessment.displayName(hidden))
    }
}

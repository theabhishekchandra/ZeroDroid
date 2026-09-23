package com.abhishek.zerodroid.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FrequencyUtilsTest {

    @Test
    fun `2point4GHz channel 1 maps correctly`() {
        assertEquals(1, FrequencyUtils.frequencyToChannel(2412))
    }

    @Test
    fun `2point4GHz channel 14 is a special case`() {
        assertEquals(14, FrequencyUtils.frequencyToChannel(2484))
    }

    @Test
    fun `5GHz channel maps correctly`() {
        assertEquals(36, FrequencyUtils.frequencyToChannel(5180))
    }

    @Test
    fun `6GHz channel maps correctly`() {
        assertEquals(1, FrequencyUtils.frequencyToChannel(5955))
    }

    @Test
    fun `frequency outside any known band returns -1`() {
        assertEquals(-1, FrequencyUtils.frequencyToChannel(1000))
    }

    @Test
    fun `band detection for 2point4GHz`() {
        assertEquals(WifiBand.BAND_2_4GHZ, FrequencyUtils.frequencyToBand(2437))
    }

    @Test
    fun `band detection for 5GHz`() {
        assertEquals(WifiBand.BAND_5GHZ, FrequencyUtils.frequencyToBand(5500))
    }

    @Test
    fun `band detection for 6GHz`() {
        assertEquals(WifiBand.BAND_6GHZ, FrequencyUtils.frequencyToBand(6000))
    }

    @Test
    fun `band detection for unknown frequency`() {
        assertEquals(WifiBand.UNKNOWN, FrequencyUtils.frequencyToBand(100))
    }

    @Test
    fun `signal strength at or above -50 dBm is 100 percent`() {
        assertEquals(100, FrequencyUtils.signalToPercent(-40))
        assertEquals(100, FrequencyUtils.signalToPercent(-50))
    }

    @Test
    fun `signal strength at or below -100 dBm is 0 percent`() {
        assertEquals(0, FrequencyUtils.signalToPercent(-100))
        assertEquals(0, FrequencyUtils.signalToPercent(-120))
    }

    @Test
    fun `signal strength interpolates linearly between -100 and -50 dBm`() {
        assertEquals(50, FrequencyUtils.signalToPercent(-75))
        assertEquals(20, FrequencyUtils.signalToPercent(-90))
    }

    @Test
    fun `security type detects WPA3`() {
        assertEquals(SecurityType.WPA3, SecurityType.fromCapabilities("[WPA3-SAE][ESS]"))
    }

    @Test
    fun `security type detects WPA2 via RSN token`() {
        assertEquals(SecurityType.WPA2, SecurityType.fromCapabilities("[RSN-PSK][ESS]"))
    }

    @Test
    fun `security type detects WEP`() {
        assertEquals(SecurityType.WEP, SecurityType.fromCapabilities("[WEP][ESS]"))
    }

    @Test
    fun `security type treats blank capabilities as open`() {
        assertEquals(SecurityType.OPEN, SecurityType.fromCapabilities(""))
        assertEquals(SecurityType.OPEN, SecurityType.fromCapabilities("[ESS]"))
    }

    @Test
    fun `security type falls back to unknown`() {
        assertEquals(SecurityType.UNKNOWN, SecurityType.fromCapabilities("[SOMETHING-WEIRD]"))
    }

    @Test
    fun `only non-security flags means open, even with WPS`() {
        assertEquals(SecurityType.OPEN, SecurityType.fromCapabilities("[WPS][ESS]"))
        assertEquals(SecurityType.WPA2, SecurityType.fromCapabilities("[WPA2-PSK-CCMP][WPS][ESS]"))
        assertEquals(SecurityType.UNKNOWN, SecurityType.fromCapabilities("[OWE][ESS]"))
    }
}

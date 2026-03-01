package com.abhishek.zerodroid.core.util

enum class SecurityType(val label: String) {
    OPEN("Open"),
    WEP("WEP"),
    WPA("WPA"),
    WPA2("WPA2"),
    WPA3("WPA3"),
    UNKNOWN("Unknown");

    companion object {
        fun fromCapabilities(capabilities: String): SecurityType = when {
            "WPA3" in capabilities -> WPA3
            "WPA2" in capabilities || "RSN" in capabilities -> WPA2
            "WPA" in capabilities -> WPA
            "WEP" in capabilities -> WEP
            capabilities.isBlank() || "[ESS]" == capabilities -> OPEN
            else -> UNKNOWN
        }
    }
}

enum class WifiBand(val label: String) {
    BAND_2_4GHZ("2.4 GHz"),
    BAND_5GHZ("5 GHz"),
    BAND_6GHZ("6 GHz"),
    UNKNOWN("Unknown")
}

object FrequencyUtils {

    fun frequencyToChannel(frequencyMhz: Int): Int = when (frequencyMhz) {
        in 2412..2484 -> when (frequencyMhz) {
            2484 -> 14
            else -> (frequencyMhz - 2412) / 5 + 1
        }
        in 5170..5825 -> (frequencyMhz - 5000) / 5
        in 5955..7115 -> (frequencyMhz - 5955) / 5 + 1
        else -> -1
    }

    fun frequencyToBand(frequencyMhz: Int): WifiBand = when (frequencyMhz) {
        in 2400..2500 -> WifiBand.BAND_2_4GHZ
        in 5000..5900 -> WifiBand.BAND_5GHZ
        in 5925..7125 -> WifiBand.BAND_6GHZ
        else -> WifiBand.UNKNOWN
    }

    fun signalToPercent(rssi: Int): Int {
        return when {
            rssi >= -50 -> 100
            rssi <= -100 -> 0
            else -> 2 * (rssi + 100)
        }
    }
}

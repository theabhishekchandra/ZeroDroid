package com.abhishek.zerodroid.features.wifi.domain

import com.abhishek.zerodroid.core.util.FrequencyUtils
import com.abhishek.zerodroid.core.util.SecurityType
import com.abhishek.zerodroid.core.util.WifiBand

data class WifiAccessPoint(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val capabilities: String,
    val channelWidth: Int = 0
) {
    val channel: Int get() = FrequencyUtils.frequencyToChannel(frequency)
    val band: WifiBand get() = FrequencyUtils.frequencyToBand(frequency)
    val security: SecurityType get() = SecurityType.fromCapabilities(capabilities)
    val signalPercent: Int get() = FrequencyUtils.signalToPercent(rssi)
}

data class ChannelScore(
    val channel: Int,
    val band: WifiBand,
    val apCount: Int,
    val avgRssi: Int
)

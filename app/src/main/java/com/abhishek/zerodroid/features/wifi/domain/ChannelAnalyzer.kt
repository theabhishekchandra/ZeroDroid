package com.abhishek.zerodroid.features.wifi.domain

import com.abhishek.zerodroid.core.util.WifiBand

object ChannelAnalyzer {

    fun analyze(accessPoints: List<WifiAccessPoint>): List<ChannelScore> {
        return accessPoints
            .groupBy { it.channel }
            .map { (channel, aps) ->
                ChannelScore(
                    channel = channel,
                    band = aps.first().band,
                    apCount = aps.size,
                    avgRssi = aps.map { it.rssi }.average().toInt()
                )
            }
            .sortedBy { it.channel }
    }

    fun bestChannel(scores: List<ChannelScore>, band: WifiBand): Int? {
        val bandScores = scores.filter { it.band == band }
        if (bandScores.isEmpty()) return null
        return bandScores.minByOrNull { it.apCount }?.channel
    }
}

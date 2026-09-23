package com.abhishek.zerodroid.features.wifi.domain

import com.abhishek.zerodroid.core.util.SecurityType
import com.abhishek.zerodroid.core.util.WifiBand

/** Outcome of one security check on a network. */
enum class CheckOutcome { PASS, WARN, FAIL }

data class WifiCheck(val title: String, val detail: String, val outcome: CheckOutcome)

/** How congested one channel is, for the channel bars. */
data class ChannelLoad(val channel: Int, val networks: Int)

/**
 * Plain-language analysis of a WiFi scan: security grades, per-network checks and channel
 * advice. Pure functions so they can be tested without a radio.
 */
object WifiAssessment {

    val CHANNELS_24 = (1..11).toList()
    val CHANNELS_5 = listOf(36, 40, 44, 48, 149, 153, 157, 161)

    /** Networks whose encryption lets nearby people read or join traffic. */
    fun isWeak(security: SecurityType): Boolean =
        security == SecurityType.OPEN || security == SecurityType.WEP || security == SecurityType.WPA

    /** Letter grade shown in the detail view. */
    fun grade(ap: WifiAccessPoint): String = when (ap.security) {
        SecurityType.WPA3 -> if (hasWps(ap)) "B" else "A"
        SecurityType.WPA2 -> if (hasWps(ap)) "C" else "B"
        SecurityType.WPA -> "D"
        SecurityType.WEP, SecurityType.OPEN -> "F"
        SecurityType.UNKNOWN -> "?"
    }

    fun hasWps(ap: WifiAccessPoint): Boolean = "WPS" in ap.capabilities

    /** The scanner reports hidden networks as [HIDDEN_SSID]. */
    const val HIDDEN_SSID = "<Hidden>"

    fun isHidden(ap: WifiAccessPoint): Boolean = ap.ssid.isBlank() || ap.ssid == HIDDEN_SSID

    /** Name for display; hidden networks read as "[hidden]". */
    fun displayName(ap: WifiAccessPoint): String = if (isHidden(ap)) "[hidden]" else ap.ssid

    /**
     * True when the BSSID is locally administered (the "random" bit is set), which is what
     * phones and some routers use instead of a factory address.
     */
    fun isRandomMac(bssid: String): Boolean {
        val firstOctet = bssid.take(2).toIntOrNull(16) ?: return false
        return firstOctet and 0x02 != 0
    }

    /** First three octets of the address: the manufacturer prefix (OUI). */
    fun ouiOf(bssid: String): String = bssid.take(8).uppercase()

    /** "OUI A4:2B:B0" or "Random MAC", for row subtitles. */
    fun hardwareLabel(bssid: String): String =
        if (isRandomMac(bssid)) "Random MAC" else "OUI ${ouiOf(bssid)}"

    /** Network count per channel in [channels], zero-filled. */
    fun load(accessPoints: List<WifiAccessPoint>, channels: List<Int>): List<ChannelLoad> {
        val counts = accessPoints.groupingBy { it.channel }.eachCount()
        return channels.map { ChannelLoad(it, counts[it] ?: 0) }
    }

    /**
     * Best 2.4 GHz channel among the non-overlapping 1, 6 and 11, counting networks on
     * overlapping neighbours (±4 channels) because they interfere too.
     */
    fun best24Channel(accessPoints: List<WifiAccessPoint>): Int? {
        val on24 = accessPoints.filter { it.band == WifiBand.BAND_2_4GHZ }
        if (on24.isEmpty()) return null
        return listOf(1, 6, 11).minByOrNull { candidate ->
            on24.count { kotlin.math.abs(it.channel - candidate) <= 4 }
        }
    }

    /** Most crowded channel in a band, if any channel has more than one network. */
    fun busiestChannel(accessPoints: List<WifiAccessPoint>, band: WifiBand): ChannelLoad? =
        accessPoints.filter { it.band == band }
            .groupingBy { it.channel }.eachCount()
            .maxByOrNull { it.value }
            ?.takeIf { it.value > 1 }
            ?.let { ChannelLoad(it.key, it.value) }

    /** Other networks on the same channel as [ap]. */
    fun coChannel(ap: WifiAccessPoint, all: List<WifiAccessPoint>): Int =
        all.count { it.bssid != ap.bssid && it.channel == ap.channel }

    /** Names one edit away from [ssid] (H0me_5G vs Home_5G), a spoofing tell. */
    fun lookAlikes(ssid: String, all: List<WifiAccessPoint>): List<String> =
        all.map { it.ssid }
            .filter { it.isNotBlank() && it != HIDDEN_SSID && it != ssid && editDistanceAtMostOne(it, ssid) }
            .distinct()

    /** The checks listed under "Security rating" in the network detail. */
    fun checks(ap: WifiAccessPoint, all: List<WifiAccessPoint>): List<WifiCheck> = buildList {
        add(
            when (ap.security) {
                SecurityType.WPA3 -> WifiCheck("WPA3 encryption", "Strongest consumer option", CheckOutcome.PASS)
                SecurityType.WPA2 -> WifiCheck("WPA2 encryption", "Good; WPA3 is stronger if your router supports it", CheckOutcome.PASS)
                SecurityType.WPA -> WifiCheck("WPA encryption", "Outdated; can be cracked", CheckOutcome.WARN)
                SecurityType.WEP -> WifiCheck("WEP encryption", "Broken; can be cracked in minutes", CheckOutcome.FAIL)
                SecurityType.OPEN -> WifiCheck("No encryption", "Anyone nearby can read the traffic", CheckOutcome.FAIL)
                SecurityType.UNKNOWN -> WifiCheck("Unknown encryption", "The router didn’t advertise a known type", CheckOutcome.WARN)
            }
        )
        add(
            if (hasWps(ap)) WifiCheck("WPS advertised", "The 8-digit PIN can be brute-forced; turn WPS off", CheckOutcome.WARN)
            else WifiCheck("No WPS advertised", "Not exposed to PIN brute force", CheckOutcome.PASS)
        )
        if (!isHidden(ap)) {
            val twins = all.filter { it.ssid == ap.ssid && it.bssid != ap.bssid }
            val differentHardware = twins.filter { ouiOf(it.bssid) != ouiOf(ap.bssid) && !isRandomMac(it.bssid) }
            val alikes = lookAlikes(ap.ssid, all)
            add(
                when {
                    differentHardware.isNotEmpty() -> WifiCheck("Same name, different hardware", "${differentHardware.size} other AP with this name from another maker; check Rogue AP", CheckOutcome.WARN)
                    alikes.isNotEmpty() -> WifiCheck("Look-alike name nearby", alikes.take(2).joinToString(", "), CheckOutcome.WARN)
                    else -> WifiCheck("No look-alike networks", "No twins or one-letter-off names in range", CheckOutcome.PASS)
                }
            )
        }
    }

    private fun editDistanceAtMostOne(a: String, b: String): Boolean {
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                i++; j++
                continue
            }
            if (++edits > 1) return false
            when {
                a.length > b.length -> i++
                a.length < b.length -> j++
                else -> { i++; j++ }
            }
        }
        edits += (a.length - i) + (b.length - j)
        return edits <= 1
    }
}

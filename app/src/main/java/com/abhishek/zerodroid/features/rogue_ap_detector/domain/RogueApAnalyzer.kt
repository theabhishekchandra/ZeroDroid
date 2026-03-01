package com.abhishek.zerodroid.features.rogue_ap_detector.domain

import com.abhishek.zerodroid.core.util.SecurityType
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import java.util.UUID
import kotlin.math.abs
import kotlin.math.min

enum class ApThreatType(val label: String) {
    EVIL_TWIN("Evil Twin"),
    OPEN_IMPERSONATOR("Open Impersonator"),
    SSID_SPOOF("SSID Spoof"),
    WEAK_SECURITY("Weak Security"),
    HIDDEN_SUSPICIOUS("Hidden AP"),
    KARMA_ATTACK("Karma Attack")
}

enum class RiskLevel(val label: String) {
    CRITICAL("CRITICAL"),
    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW"),
    SAFE("SAFE")
}

data class RogueApAlert(
    val id: String = UUID.randomUUID().toString(),
    val threatType: ApThreatType,
    val riskLevel: RiskLevel,
    val title: String,
    val description: String,
    val suspiciousAp: WifiAccessPoint,
    val legitimateAp: WifiAccessPoint? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class RogueApState(
    val isScanning: Boolean = false,
    val totalAps: Int = 0,
    val alerts: List<RogueApAlert> = emptyList(),
    val safeAps: Int = 0,
    val suspiciousAps: Int = 0,
    val knownSsids: Set<String> = emptySet(),
    val error: String? = null
)

class RogueApAnalyzer {

    companion object {
        val COMMON_PUBLIC_SSIDS = setOf(
            "Starbucks", "attwifi", "xfinitywifi", "Google Starbucks", "NETGEAR",
            "linksys", "default", "FREE", "FreeWiFi", "Free WiFi", "PUBLIC",
            "Airport", "Hotel", "Guest", "McDonald's Free WiFi", "Marriott_GUEST",
            "HiltonHonors", "T-Mobile", "CableWiFi", "HOME", "DIRECT-",
            "AndroidAP", "iPhone", "Samsung", "Hotspot"
        )

        private const val EVIL_TWIN_RSSI_THRESHOLD = 15
        private const val HIDDEN_SSID_STRONG_SIGNAL = -50
        private const val SSID_SIMILARITY_THRESHOLD = 2
    }

    /**
     * Run all detection algorithms against a list of scanned access points.
     * Returns deduplicated alerts sorted by risk level, then timestamp.
     */
    fun analyze(
        accessPoints: List<WifiAccessPoint>,
        knownSsids: Set<String> = emptySet()
    ): List<RogueApAlert> {
        val alerts = mutableListOf<RogueApAlert>()

        alerts.addAll(detectEvilTwins(accessPoints))
        alerts.addAll(detectOpenImpersonators(accessPoints))
        alerts.addAll(detectSsidSpoofing(accessPoints))
        alerts.addAll(detectWeakSecurity(accessPoints, knownSsids))
        alerts.addAll(detectHiddenSuspicious(accessPoints))
        alerts.addAll(detectKarmaAttack(accessPoints))

        // Deduplicate by BSSID — keep the highest risk alert per BSSID
        val deduped = alerts
            .groupBy { it.suspiciousAp.bssid }
            .values
            .map { group -> group.minByOrNull { it.riskLevel.ordinal }!! }

        return deduped.sortedWith(
            compareBy<RogueApAlert> { it.riskLevel.ordinal }
                .thenByDescending { it.timestamp }
        )
    }

    /**
     * Evil Twin Detection: Same SSID broadcast from different BSSIDs.
     * Flags when security type differs or RSSI difference is significant.
     */
    private fun detectEvilTwins(accessPoints: List<WifiAccessPoint>): List<RogueApAlert> {
        val alerts = mutableListOf<RogueApAlert>()
        val bySSID = accessPoints
            .filter { it.ssid != "<Hidden>" && it.ssid.isNotBlank() }
            .groupBy { it.ssid }

        for ((ssid, aps) in bySSID) {
            if (aps.size < 2) continue

            // Sort by signal strength — strongest is likely legitimate
            val sorted = aps.sortedByDescending { it.rssi }
            val legitimate = sorted.first()

            for (suspect in sorted.drop(1)) {
                if (suspect.bssid == legitimate.bssid) continue

                val securityDiffers = suspect.security != legitimate.security
                val rssiDiffSignificant = abs(suspect.rssi - legitimate.rssi) > EVIL_TWIN_RSSI_THRESHOLD
                val differentOui = !sharesOui(suspect.bssid, legitimate.bssid)

                if (securityDiffers || (rssiDiffSignificant && differentOui)) {
                    val riskLevel = when {
                        securityDiffers && suspect.security == SecurityType.OPEN -> RiskLevel.CRITICAL
                        securityDiffers -> RiskLevel.HIGH
                        differentOui -> RiskLevel.HIGH
                        else -> RiskLevel.MEDIUM
                    }

                    val reasons = mutableListOf<String>()
                    if (securityDiffers) {
                        reasons.add("security mismatch (${suspect.security.label} vs ${legitimate.security.label})")
                    }
                    if (differentOui) {
                        reasons.add("different manufacturer (OUI)")
                    }
                    if (rssiDiffSignificant) {
                        reasons.add("signal delta ${abs(suspect.rssi - legitimate.rssi)}dBm")
                    }

                    alerts.add(
                        RogueApAlert(
                            threatType = ApThreatType.EVIL_TWIN,
                            riskLevel = riskLevel,
                            title = "Evil Twin: \"$ssid\"",
                            description = "Duplicate SSID from different AP — ${reasons.joinToString(", ")}. " +
                                "An attacker may be impersonating this network to intercept traffic.",
                            suspiciousAp = suspect,
                            legitimateAp = legitimate
                        )
                    )
                }
            }
        }
        return alerts
    }

    /**
     * Open Impersonator: Open network whose SSID matches a well-known public network.
     */
    private fun detectOpenImpersonators(accessPoints: List<WifiAccessPoint>): List<RogueApAlert> {
        return accessPoints
            .filter { ap ->
                ap.security == SecurityType.OPEN &&
                    ap.ssid != "<Hidden>" &&
                    COMMON_PUBLIC_SSIDS.any { known ->
                        ap.ssid.equals(known, ignoreCase = true) ||
                            ap.ssid.contains(known, ignoreCase = true)
                    }
            }
            .map { ap ->
                RogueApAlert(
                    threatType = ApThreatType.OPEN_IMPERSONATOR,
                    riskLevel = RiskLevel.HIGH,
                    title = "Open Impersonator: \"${ap.ssid}\"",
                    description = "Open network using a common public SSID. " +
                        "This could be a rogue AP set up to harvest credentials from unsuspecting users.",
                    suspiciousAp = ap
                )
            }
    }

    /**
     * SSID Spoofing: Detects SSIDs that are visually similar to other scanned SSIDs
     * (homoglyphs, extra spaces, zero-width characters, case variations).
     */
    private fun detectSsidSpoofing(accessPoints: List<WifiAccessPoint>): List<RogueApAlert> {
        val alerts = mutableListOf<RogueApAlert>()
        val validAps = accessPoints.filter { it.ssid != "<Hidden>" && it.ssid.isNotBlank() }

        for (i in validAps.indices) {
            for (j in i + 1 until validAps.size) {
                val ap1 = validAps[i]
                val ap2 = validAps[j]

                // Skip if identical SSID (handled by evil twin detection)
                if (ap1.ssid == ap2.ssid) continue

                val distance = levenshteinDistance(
                    ap1.ssid.lowercase().trim(),
                    ap2.ssid.lowercase().trim()
                )

                if (distance in 1..SSID_SIMILARITY_THRESHOLD) {
                    val hasExtraSpaces = ap1.ssid.contains("  ") || ap2.ssid.contains("  ") ||
                        ap1.ssid != ap1.ssid.trim() || ap2.ssid != ap2.ssid.trim()

                    // The weaker-security AP is more suspicious
                    val (suspect, target) = if (ap1.security.ordinal > ap2.security.ordinal) {
                        ap1 to ap2
                    } else {
                        ap2 to ap1
                    }

                    alerts.add(
                        RogueApAlert(
                            threatType = ApThreatType.SSID_SPOOF,
                            riskLevel = if (hasExtraSpaces) RiskLevel.HIGH else RiskLevel.MEDIUM,
                            title = "SSID Spoof: \"${suspect.ssid}\"",
                            description = "Looks similar to \"${target.ssid}\" (edit distance: $distance). " +
                                "May use lookalike characters or extra spaces to trick users into connecting.",
                            suspiciousAp = suspect,
                            legitimateAp = target
                        )
                    )
                }
            }
        }
        return alerts
    }

    /**
     * Weak Security: Flags WEP or completely open networks.
     * Lower risk if SSID is in the known/trusted list.
     */
    private fun detectWeakSecurity(
        accessPoints: List<WifiAccessPoint>,
        knownSsids: Set<String>
    ): List<RogueApAlert> {
        return accessPoints
            .filter { ap ->
                (ap.security == SecurityType.OPEN || ap.security == SecurityType.WEP) &&
                    ap.ssid != "<Hidden>" &&
                    ap.ssid !in knownSsids
            }
            .map { ap ->
                val isWep = ap.security == SecurityType.WEP
                RogueApAlert(
                    threatType = ApThreatType.WEAK_SECURITY,
                    riskLevel = if (isWep) RiskLevel.MEDIUM else RiskLevel.LOW,
                    title = if (isWep) "WEP Network: \"${ap.ssid}\""
                    else "Open Network: \"${ap.ssid}\"",
                    description = if (isWep)
                        "WEP encryption is broken and can be cracked in minutes. " +
                            "Treat this network as if it were unencrypted."
                    else
                        "Completely open network with no encryption. " +
                            "All traffic is visible to anyone within range.",
                    suspiciousAp = ap
                )
            }
    }

    /**
     * Hidden AP with strong signal: A hidden SSID broadcasting at high power
     * could indicate a rogue device trying to stay under the radar.
     */
    private fun detectHiddenSuspicious(accessPoints: List<WifiAccessPoint>): List<RogueApAlert> {
        return accessPoints
            .filter { ap ->
                (ap.ssid == "<Hidden>" || ap.ssid.isBlank()) &&
                    ap.rssi >= HIDDEN_SSID_STRONG_SIGNAL
            }
            .map { ap ->
                RogueApAlert(
                    threatType = ApThreatType.HIDDEN_SUSPICIOUS,
                    riskLevel = RiskLevel.MEDIUM,
                    title = "Hidden AP (Strong Signal)",
                    description = "Hidden SSID broadcasting at ${ap.rssi}dBm (${ap.signalPercent}%). " +
                        "Strong hidden networks may be rogue APs avoiding casual detection. " +
                        "BSSID: ${ap.bssid}, Ch: ${ap.channel}, Security: ${ap.security.label}.",
                    suspiciousAp = ap
                )
            }
    }

    /**
     * Karma Attack Pattern: Multiple SSIDs broadcast from APs sharing the same
     * OUI prefix (first 3 octets of BSSID). A single device broadcasting many
     * network names is a strong indicator of a pineapple/karma attack.
     */
    private fun detectKarmaAttack(accessPoints: List<WifiAccessPoint>): List<RogueApAlert> {
        val alerts = mutableListOf<RogueApAlert>()
        val validAps = accessPoints.filter { it.ssid != "<Hidden>" && it.ssid.isNotBlank() }

        // Group by first 3 octets of BSSID (OUI prefix)
        val byOui = validAps.groupBy { it.bssid.take(8).uppercase() }

        for ((oui, aps) in byOui) {
            if (aps.size < 4) continue // Need at least 4 distinct SSIDs from same OUI

            val distinctSsids = aps.map { it.ssid }.distinct()
            if (distinctSsids.size < 4) continue

            // Flag all APs from this OUI
            for (ap in aps) {
                alerts.add(
                    RogueApAlert(
                        threatType = ApThreatType.KARMA_ATTACK,
                        riskLevel = RiskLevel.CRITICAL,
                        title = "Karma Attack: \"${ap.ssid}\"",
                        description = "${distinctSsids.size} different SSIDs from OUI $oui. " +
                            "A single device broadcasting multiple network names is a strong indicator " +
                            "of a WiFi Pineapple or karma attack tool. SSIDs: ${distinctSsids.take(5).joinToString(", ") { "\"$it\"" }}.",
                        suspiciousAp = ap
                    )
                )
            }
        }
        return alerts
    }

    /**
     * Check if two BSSIDs share the same OUI (first 3 octets / manufacturer).
     */
    private fun sharesOui(bssid1: String, bssid2: String): Boolean {
        return bssid1.take(8).equals(bssid2.take(8), ignoreCase = true)
    }

    /**
     * Compute the Levenshtein edit distance between two strings.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[len1][len2]
    }
}

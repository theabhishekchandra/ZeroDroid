package com.abhishek.zerodroid.features.deauth_detector.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import java.util.UUID
import kotlin.math.abs

// ── Data Models ──────────────────────────────────────────────────────────────

enum class AttackType(val label: String) {
    DEAUTH_FLOOD("Deauth Flood"),
    SIGNAL_JAMMING("Signal Jamming"),
    AP_DISAPPEARANCE("AP Disappearance"),
    RAPID_RECONNECT("Rapid Reconnect"),
    CHANNEL_HOPPING("Channel Hopping")
}

enum class AlertLevel(val label: String) {
    CRITICAL("CRITICAL"),
    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW")
}

data class DeauthEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: AttackType,
    val level: AlertLevel,
    val title: String,
    val detail: String,
    val affectedSsid: String?,
    val affectedBssid: String?,
    val timestamp: Long = System.currentTimeMillis()
)

data class ApSnapshot(
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val channel: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class DeauthState(
    val isMonitoring: Boolean = false,
    val events: List<DeauthEvent> = emptyList(),
    val monitoringDurationMs: Long = 0,
    val connectedSsid: String? = null,
    val connectedBssid: String? = null,
    val connectedRssi: Int = 0,
    val connectedChannel: Int = 0,
    val disconnectCount: Int = 0,
    val apHistory: Map<String, List<ApSnapshot>> = emptyMap(),
    val isUnderAttack: Boolean = false,
    val error: String? = null
)

// ── Analyzer ─────────────────────────────────────────────────────────────────

class DeauthAnalyzer(private val context: Context) {

    companion object {
        // Deauth flood thresholds
        private const val DEAUTH_FLOOD_COUNT = 3
        private const val DEAUTH_FLOOD_WINDOW_MS = 60_000L

        // Signal jamming thresholds
        private const val JAMMING_RSSI_DROP = 30

        // AP disappearance thresholds
        private const val AP_DISAPPEARANCE_CYCLES = 2

        // Rapid reconnect thresholds
        private const val RAPID_RECONNECT_COUNT = 5
        private const val RAPID_RECONNECT_WINDOW_MS = 120_000L

        // Rolling history window
        private const val HISTORY_WINDOW_MS = 60_000L
    }

    private val wifiManager: WifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Rolling state
    private val disconnectTimestamps = mutableListOf<Long>()
    private val reconnectTimestamps = mutableListOf<Long>() // connect->disconnect->connect cycles
    private val apSnapshots = mutableMapOf<String, MutableList<ApSnapshot>>() // bssid -> snapshots
    private var previousScanBssids = setOf<String>()
    private var connectedApMissingCycles = 0
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastConnectionState = true // assume connected at start

    // Track reconnect patterns per BSSID
    private val reconnectEvents = mutableMapOf<String, MutableList<Long>>()

    /**
     * Register a connectivity callback to track disconnect/reconnect events.
     * Call this once when monitoring starts.
     */
    fun startConnectivityTracking(onDisconnect: () -> Unit, onReconnect: () -> Unit) {
        stopConnectivityTracking()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!lastConnectionState) {
                    // Reconnected after a disconnect
                    lastConnectionState = true
                    onReconnect()
                }
            }

            override fun onLost(network: Network) {
                lastConnectionState = false
                val now = System.currentTimeMillis()
                disconnectTimestamps.add(now)
                onDisconnect()
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback!!)
        lastConnectionState = true
    }

    /**
     * Unregister the connectivity callback.
     */
    fun stopConnectivityTracking() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: IllegalArgumentException) {
                // Already unregistered
            }
        }
        networkCallback = null
    }

    /**
     * Record a reconnect event for the given BSSID.
     */
    fun recordReconnect(bssid: String) {
        val now = System.currentTimeMillis()
        reconnectEvents.getOrPut(bssid) { mutableListOf() }.add(now)
    }

    /**
     * Get the currently connected WiFi info.
     * Returns a triple of (ssid, bssid, rssi) or null if not connected.
     */
    @Suppress("DEPRECATION")
    fun getConnectedWifiInfo(): Triple<String?, String?, Int> {
        val info = wifiManager.connectionInfo ?: return Triple(null, null, 0)
        val ssid = info.ssid?.removePrefix("\"")?.removeSuffix("\"")
        val bssid = info.bssid
        val rssi = info.rssi
        return if (bssid != null && bssid != "02:00:00:00:00:00") {
            Triple(ssid, bssid, rssi)
        } else {
            Triple(null, null, 0)
        }
    }

    /**
     * Run all detection algorithms on the latest scan results.
     * Updates internal rolling history and returns any new events detected.
     */
    fun analyze(
        currentScan: List<WifiAccessPoint>,
        connectedSsid: String?,
        connectedBssid: String?
    ): List<DeauthEvent> {
        val now = System.currentTimeMillis()
        val events = mutableListOf<DeauthEvent>()

        // Update rolling AP snapshot history
        updateApHistory(currentScan, now)

        // Prune old data
        pruneOldData(now)

        // Run each detection algorithm
        detectDeauthFlood(now)?.let { events.add(it) }
        detectSignalJamming(connectedBssid, currentScan)?.let { events.add(it) }
        detectApDisappearance(connectedSsid, connectedBssid, currentScan)?.let { events.add(it) }
        detectRapidReconnect(connectedBssid)?.let { events.add(it) }
        detectChannelHopping(connectedBssid)?.let { events.add(it) }

        // Update previous scan state
        previousScanBssids = currentScan.map { it.bssid }.toSet()

        return events
    }

    /**
     * Get the current AP snapshot history (for UI display).
     */
    fun getApHistory(): Map<String, List<ApSnapshot>> = apSnapshots.toMap()

    /**
     * Get disconnect timestamps within the rolling window (for timeline UI).
     */
    fun getDisconnectTimestamps(): List<Long> = disconnectTimestamps.toList()

    /**
     * Get total disconnect count within the rolling window.
     */
    fun getDisconnectCount(): Int = disconnectTimestamps.size

    /**
     * Reset all internal state.
     */
    fun reset() {
        disconnectTimestamps.clear()
        reconnectTimestamps.clear()
        apSnapshots.clear()
        previousScanBssids = emptySet()
        connectedApMissingCycles = 0
        reconnectEvents.clear()
        lastConnectionState = true
    }

    // ── Detection Algorithms ─────────────────────────────────────────────────

    /**
     * 1. Deauth Flood Detection
     * If disconnected and reconnected >3 times in 60 seconds -> CRITICAL
     */
    private fun detectDeauthFlood(now: Long): DeauthEvent? {
        val recentDisconnects = disconnectTimestamps.count { ts ->
            now - ts < DEAUTH_FLOOD_WINDOW_MS
        }

        if (recentDisconnects > DEAUTH_FLOOD_COUNT) {
            val (ssid, bssid, _) = getConnectedWifiInfo()
            return DeauthEvent(
                type = AttackType.DEAUTH_FLOOD,
                level = AlertLevel.CRITICAL,
                title = "Deauth Flood Detected",
                detail = "$recentDisconnects disconnections in the last 60s. " +
                    "Your device is being forcibly disconnected from the network. " +
                    "This is a strong indicator of an active deauthentication attack.",
                affectedSsid = ssid,
                affectedBssid = bssid
            )
        }
        return null
    }

    /**
     * 2. Signal Jamming Detection
     * If connected AP's RSSI drops >30dBm suddenly while other APs remain stable -> HIGH
     */
    private fun detectSignalJamming(
        connectedBssid: String?,
        currentScan: List<WifiAccessPoint>
    ): DeauthEvent? {
        if (connectedBssid == null) return null

        val history = apSnapshots[connectedBssid] ?: return null
        if (history.size < 2) return null

        val currentAp = currentScan.find { it.bssid == connectedBssid } ?: return null
        val previousSnapshot = history[history.size - 2] // second-to-last entry
        val rssiDrop = previousSnapshot.rssi - currentAp.rssi

        if (rssiDrop >= JAMMING_RSSI_DROP) {
            // Check if other APs remain stable (not a general environment change)
            val otherApsStable = checkOtherApsStable(connectedBssid, currentScan)

            if (otherApsStable) {
                return DeauthEvent(
                    type = AttackType.SIGNAL_JAMMING,
                    level = AlertLevel.HIGH,
                    title = "Signal Jamming Suspected",
                    detail = "Signal dropped ${rssiDrop}dBm (from ${previousSnapshot.rssi}dBm to " +
                        "${currentAp.rssi}dBm) while nearby APs remain stable. " +
                        "Targeted RF jamming may be in progress against this network.",
                    affectedSsid = currentAp.ssid,
                    affectedBssid = connectedBssid
                )
            }
        }
        return null
    }

    /**
     * Check if other (non-connected) APs have relatively stable signal levels.
     */
    private fun checkOtherApsStable(
        excludeBssid: String,
        currentScan: List<WifiAccessPoint>
    ): Boolean {
        var stableCount = 0
        var checkedCount = 0

        for (ap in currentScan) {
            if (ap.bssid == excludeBssid) continue
            val history = apSnapshots[ap.bssid] ?: continue
            if (history.size < 2) continue

            checkedCount++
            val prev = history[history.size - 2].rssi
            val curr = ap.rssi
            if (abs(prev - curr) < 15) {
                stableCount++
            }
        }

        // Consider stable if at least half of checked APs are stable,
        // or if we don't have enough data to compare (fewer than 2 APs checked)
        return checkedCount < 2 || stableCount >= checkedCount / 2
    }

    /**
     * 3. AP Disappearance Detection
     * If the connected AP vanishes from scan results for 2+ cycles while other APs
     * are still visible -> HIGH
     */
    private fun detectApDisappearance(
        connectedSsid: String?,
        connectedBssid: String?,
        currentScan: List<WifiAccessPoint>
    ): DeauthEvent? {
        if (connectedBssid == null) return null

        val connectedVisible = currentScan.any { it.bssid == connectedBssid }

        if (!connectedVisible && currentScan.isNotEmpty()) {
            connectedApMissingCycles++

            if (connectedApMissingCycles >= AP_DISAPPEARANCE_CYCLES) {
                return DeauthEvent(
                    type = AttackType.AP_DISAPPEARANCE,
                    level = AlertLevel.HIGH,
                    title = "Connected AP Vanished",
                    detail = "\"${connectedSsid ?: "Unknown"}\" ($connectedBssid) has been absent from " +
                        "scan results for $connectedApMissingCycles consecutive cycles while " +
                        "${currentScan.size} other APs are still visible. " +
                        "The AP may have been taken down or is being jammed.",
                    affectedSsid = connectedSsid,
                    affectedBssid = connectedBssid
                )
            }
        } else {
            connectedApMissingCycles = 0
        }
        return null
    }

    /**
     * 4. Rapid Reconnect Pattern Detection
     * If same BSSID shows connect->disconnect->connect cycle >5 times in 2 minutes -> CRITICAL
     */
    private fun detectRapidReconnect(connectedBssid: String?): DeauthEvent? {
        if (connectedBssid == null) return null

        val now = System.currentTimeMillis()
        val events = reconnectEvents[connectedBssid] ?: return null

        val recentReconnects = events.count { ts ->
            now - ts < RAPID_RECONNECT_WINDOW_MS
        }

        if (recentReconnects >= RAPID_RECONNECT_COUNT) {
            val (ssid, _, _) = getConnectedWifiInfo()
            return DeauthEvent(
                type = AttackType.RAPID_RECONNECT,
                level = AlertLevel.CRITICAL,
                title = "Rapid Reconnect Pattern",
                detail = "$recentReconnects reconnect cycles in the last 2 minutes on " +
                    "\"${ssid ?: "Unknown"}\" ($connectedBssid). " +
                    "This connect-disconnect-connect loop is characteristic of " +
                    "an active deauthentication attack forcing your device to repeatedly rejoin.",
                affectedSsid = ssid,
                affectedBssid = connectedBssid
            )
        }
        return null
    }

    /**
     * 5. Channel Hopping Detection
     * If the connected AP changes channel unexpectedly (without the network going down) -> MEDIUM
     */
    private fun detectChannelHopping(connectedBssid: String?): DeauthEvent? {
        if (connectedBssid == null) return null

        val history = apSnapshots[connectedBssid] ?: return null
        if (history.size < 2) return null

        val current = history.last()
        val previous = history[history.size - 2]

        if (current.channel != previous.channel && current.channel != 0 && previous.channel != 0) {
            return DeauthEvent(
                type = AttackType.CHANNEL_HOPPING,
                level = AlertLevel.MEDIUM,
                title = "Unexpected Channel Change",
                detail = "\"${current.ssid}\" ($connectedBssid) switched from " +
                    "channel ${previous.channel} to channel ${current.channel}. " +
                    "Unexpected channel changes can indicate an attacker forcing the AP " +
                    "to switch channels, or a rogue AP attempting to lure clients.",
                affectedSsid = current.ssid,
                affectedBssid = connectedBssid
            )
        }
        return null
    }

    // ── Internal Helpers ─────────────────────────────────────────────────────

    /**
     * Update the rolling AP snapshot history with the latest scan results.
     */
    private fun updateApHistory(scan: List<WifiAccessPoint>, timestamp: Long) {
        for (ap in scan) {
            val snapshots = apSnapshots.getOrPut(ap.bssid) { mutableListOf() }
            snapshots.add(
                ApSnapshot(
                    bssid = ap.bssid,
                    ssid = ap.ssid,
                    rssi = ap.rssi,
                    channel = ap.channel,
                    timestamp = timestamp
                )
            )
        }
    }

    /**
     * Remove data older than the rolling window from all tracking structures.
     */
    private fun pruneOldData(now: Long) {
        // Prune disconnect timestamps
        disconnectTimestamps.removeAll { now - it > DEAUTH_FLOOD_WINDOW_MS }

        // Prune reconnect timestamps
        reconnectTimestamps.removeAll { now - it > RAPID_RECONNECT_WINDOW_MS }

        // Prune reconnect events per BSSID
        for ((_, events) in reconnectEvents) {
            events.removeAll { now - it > RAPID_RECONNECT_WINDOW_MS }
        }
        reconnectEvents.entries.removeAll { it.value.isEmpty() }

        // Prune AP snapshot history
        for ((bssid, snapshots) in apSnapshots) {
            snapshots.removeAll { now - it.timestamp > HISTORY_WINDOW_MS }
        }
        apSnapshots.entries.removeAll { it.value.isEmpty() }
    }
}

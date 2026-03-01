package com.abhishek.zerodroid.features.wifi.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WifiScanner(
    private val context: Context,
    private val wifiManager: WifiManager
) {

    @Suppress("DEPRECATION")
    fun scan(): Flow<List<WifiAccessPoint>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val results = wifiManager.scanResults.map { result ->
                    WifiAccessPoint(
                        ssid = result.SSID.ifBlank { "<Hidden>" },
                        bssid = result.BSSID,
                        rssi = result.level,
                        frequency = result.frequency,
                        capabilities = result.capabilities,
                        channelWidth = result.channelWidth
                    )
                }.sortedByDescending { it.rssi }
                trySend(results)
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )

        @Suppress("DEPRECATION")
        wifiManager.startScan()

        val current = wifiManager.scanResults.map { result ->
            WifiAccessPoint(
                ssid = result.SSID.ifBlank { "<Hidden>" },
                bssid = result.BSSID,
                rssi = result.level,
                frequency = result.frequency,
                capabilities = result.capabilities,
                channelWidth = result.channelWidth
            )
        }.sortedByDescending { it.rssi }
        trySend(current)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }
}

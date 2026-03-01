package com.abhishek.zerodroid.features.wardriving.domain

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.net.wifi.WifiManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WardrivingCollector(
    private val context: Context,
    private val wifiManager: WifiManager
) {
    private var locationClient: FusedLocationProviderClient? = null

    @SuppressLint("MissingPermission")
    fun collect(): Flow<List<WardrivingRecord>> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(context)
        locationClient = client

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).setMinUpdateIntervalMillis(1000).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val scanResults = wifiManager.scanResults
                val records = scanResults.map { ap ->
                    WardrivingRecord(
                        bssid = ap.BSSID,
                        ssid = ap.SSID.takeIf { it.isNotBlank() },
                        rssi = ap.level,
                        frequency = ap.frequency,
                        capabilities = ap.capabilities,
                        lat = location.latitude,
                        lng = location.longitude
                    )
                }
                trySend(records)
                wifiManager.startScan()
            }
        }

        client.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        wifiManager.startScan()

        awaitClose {
            client.removeLocationUpdates(callback)
            locationClient = null
        }
    }
}

package com.abhishek.zerodroid.features.gps.domain

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.LocationManager
import android.location.OnNmeaMessageListener
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

class GpsTracker(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun track(): Flow<GpsState> = callbackFlow {
        var currentState = GpsState(isTracking = true)
        val nmeaBuffer = mutableListOf<String>()

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        ).setMinUpdateIntervalMillis(500L).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                currentState = currentState.copy(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    speed = location.speed,
                    bearing = location.bearing,
                    accuracy = location.accuracy,
                    provider = location.provider ?: "fused",
                    lastUpdateTime = location.time
                )
                trySend(currentState)
            }
        }

        val gnssCallback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val satellites = (0 until status.satelliteCount).map { i ->
                    SatelliteInfo(
                        svid = status.getSvid(i),
                        constellationType = status.getConstellationType(i),
                        cn0DbHz = status.getCn0DbHz(i),
                        elevationDeg = status.getElevationDegrees(i),
                        azimuthDeg = status.getAzimuthDegrees(i),
                        usedInFix = status.usedInFix(i)
                    )
                }
                val usedCount = satellites.count { it.usedInFix }
                currentState = currentState.copy(
                    satellites = satellites,
                    satelliteCount = usedCount
                )
                trySend(currentState)
            }
        }

        val nmeaListener = OnNmeaMessageListener { message, _ ->
            val trimmed = message.trim()
            if (trimmed.isNotEmpty()) {
                nmeaBuffer.add(0, trimmed)
                if (nmeaBuffer.size > 50) nmeaBuffer.removeLast()
                currentState = currentState.copy(nmeaSentences = nmeaBuffer.toList())
            }
        }

        fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        locationManager.registerGnssStatusCallback(gnssCallback, android.os.Handler(Looper.getMainLooper()))
        locationManager.addNmeaListener(nmeaListener, android.os.Handler(Looper.getMainLooper()))

        awaitClose {
            fusedClient.removeLocationUpdates(locationCallback)
            locationManager.unregisterGnssStatusCallback(gnssCallback)
            locationManager.removeNmeaListener(nmeaListener)
        }
    }
}

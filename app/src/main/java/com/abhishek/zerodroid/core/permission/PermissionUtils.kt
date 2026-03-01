package com.abhishek.zerodroid.core.permission

import android.Manifest
import android.os.Build

object PermissionUtils {

    fun blePermissions(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun wifiPermissions(): List<String> = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun cameraPermissions(): List<String> = listOf(
        Manifest.permission.CAMERA
    )

    fun phonePermissions(): List<String> = listOf(
        Manifest.permission.READ_PHONE_STATE
    )

    fun audioPermissions(): List<String> = listOf(
        Manifest.permission.RECORD_AUDIO
    )

    fun cellTowerPermissions(): List<String> = buildList {
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun locationPermissions(): List<String> = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun gpsPermissions(): List<String> = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun wardrivingPermissions(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun wifiDirectPermissions(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }

    fun gpsSpoofPermissions(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.READ_PHONE_STATE)
    }

    fun hiddenCameraPermissions(): List<String> = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }
}

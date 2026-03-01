package com.abhishek.zerodroid.features.uwb.domain

import android.content.Context
import android.content.pm.PackageManager

class UwbService(private val context: Context) {

    val isAvailable: Boolean
        get() = context.packageManager.hasSystemFeature("android.hardware.uwb")

    fun getDeviceInfo(): UwbDeviceInfo {
        val available = isAvailable
        val capabilities = buildList {
            if (available) {
                add("FiRa compliant ranging")
                add("Distance measurement")
                add("Angle of Arrival (AoA)")
                add("Time of Flight (ToF)")

                if (context.packageManager.hasSystemFeature("android.hardware.uwb")) {
                    add("802.15.4z HRP UWB")
                }
            }
        }

        return UwbDeviceInfo(
            isAvailable = available,
            chipset = if (available) "UWB Chip (detected via PackageManager)" else "Not available",
            capabilities = capabilities
        )
    }
}

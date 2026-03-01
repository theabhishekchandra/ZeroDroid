package com.abhishek.zerodroid.features.uwb.domain

data class UwbDeviceInfo(
    val isAvailable: Boolean,
    val chipset: String = "Unknown",
    val capabilities: List<String> = emptyList()
)

data class UwbState(
    val isHardwareAvailable: Boolean = false,
    val deviceInfo: UwbDeviceInfo? = null
)

package com.abhishek.zerodroid.features.sdr.domain

data class SdrDeviceInfo(
    val vendorId: Int,
    val productId: Int,
    val deviceName: String,
    val chipset: String,
    val isRtlSdr: Boolean
) {
    val vidPid: String get() = "%04X:%04X".format(vendorId, productId)
}

data class SdrState(
    val devices: List<SdrDeviceInfo> = emptyList(),
    val hasUsbHost: Boolean = false
)

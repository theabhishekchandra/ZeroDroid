package com.abhishek.zerodroid.features.usbcamera.domain

data class UsbCameraInfo(
    val cameraId: String,
    val isExternal: Boolean,
    val deviceName: String,
    val vendorId: Int? = null,
    val productId: Int? = null,
    val resolutions: List<String> = emptyList()
) {
    val vidPid: String? get() = if (vendorId != null && productId != null) {
        "%04X:%04X".format(vendorId, productId)
    } else null
}

data class UsbCameraState(
    val hasUsbHost: Boolean = false,
    val usbVideoDevices: List<UsbVideoDevice> = emptyList(),
    val camera2ExternalCameras: List<UsbCameraInfo> = emptyList(),
    val isPreviewActive: Boolean = false,
    val error: String? = null
)

data class UsbVideoDevice(
    val vendorId: Int,
    val productId: Int,
    val deviceName: String,
    val manufacturerName: String?
) {
    val vidPid: String get() = "%04X:%04X".format(vendorId, productId)
}

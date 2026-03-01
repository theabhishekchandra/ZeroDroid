package com.abhishek.zerodroid.features.usb.domain

data class UsbDeviceInfo(
    val vendorId: Int,
    val productId: Int,
    val deviceName: String,
    val manufacturerName: String?,
    val productName: String?,
    val deviceClass: Int,
    val deviceSubclass: Int,
    val interfaceCount: Int,
    val interfaces: List<UsbInterfaceInfo> = emptyList()
) {
    val vidPid: String get() = "%04X:%04X".format(vendorId, productId)
    val deviceClassName: String get() = usbClassToName(deviceClass)
    val badUsbIndicators: List<BadUsbIndicator> get() = buildList {
        val classSet = interfaces.map { it.interfaceClass }.toSet()
        if (classSet.contains(USB_CLASS_HID) && classSet.contains(USB_CLASS_MASS_STORAGE)) {
            add(BadUsbIndicator.HID_PLUS_STORAGE)
        }
        if (classSet.contains(USB_CLASS_HID) && (productName == null || manufacturerName == null)) {
            add(BadUsbIndicator.HID_NO_IDENTITY)
        }
    }

    companion object {
        const val USB_CLASS_HID = 3
        const val USB_CLASS_MASS_STORAGE = 8
    }
}

data class UsbInterfaceInfo(
    val id: Int,
    val interfaceClass: Int,
    val interfaceSubclass: Int,
    val interfaceProtocol: Int,
    val endpointCount: Int,
    val endpoints: List<UsbEndpointInfo> = emptyList()
) {
    val className: String get() = usbClassToName(interfaceClass)
}

data class UsbEndpointInfo(
    val address: Int,
    val direction: String,
    val type: String,
    val maxPacketSize: Int
)

enum class BadUsbIndicator(val description: String) {
    HID_PLUS_STORAGE("Device combines HID (keyboard) and Mass Storage — potential BadUSB"),
    HID_NO_IDENTITY("HID device with missing manufacturer/product info")
}

private fun usbClassToName(cls: Int): String = when (cls) {
    0 -> "Per-Interface"
    1 -> "Audio"
    2 -> "CDC/Comm"
    3 -> "HID"
    5 -> "Physical"
    6 -> "Image"
    7 -> "Printer"
    8 -> "Mass Storage"
    9 -> "Hub"
    10 -> "CDC-Data"
    11 -> "Smart Card"
    13 -> "Content Security"
    14 -> "Video"
    15 -> "Personal Healthcare"
    16 -> "Audio/Video"
    220 -> "Diagnostic"
    224 -> "Wireless Controller"
    239 -> "Miscellaneous"
    254 -> "Application Specific"
    255 -> "Vendor Specific"
    else -> "Unknown ($cls)"
}

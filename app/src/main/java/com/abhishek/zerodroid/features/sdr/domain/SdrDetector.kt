package com.abhishek.zerodroid.features.sdr.domain

import android.hardware.usb.UsbManager

class SdrDetector(
    private val usbManager: UsbManager?
) {
    private val knownSdrDevices = mapOf(
        (0x0BDA to 0x2838) to "RTL2838UHIDIR (RTL-SDR v3)",
        (0x0BDA to 0x2832) to "RTL2832U",
        (0x0BDA to 0x2831) to "RTL2831U",
        (0x1D50 to 0x604B) to "HackRF One",
        (0x1D50 to 0x6089) to "HackRF Jawbreaker",
        (0x1FC9 to 0x000C) to "AirSpy Mini",
        (0x1FC9 to 0x60A1) to "AirSpy R2",
        (0x16D0 to 0x0F32) to "SDRplay RSP",
    )

    fun detect(): List<SdrDeviceInfo> {
        val devices = usbManager?.deviceList?.values ?: return emptyList()
        return devices.mapNotNull { device ->
            val key = device.vendorId to device.productId
            val chipset = knownSdrDevices[key]
            if (chipset != null) {
                SdrDeviceInfo(
                    vendorId = device.vendorId,
                    productId = device.productId,
                    deviceName = device.productName ?: device.deviceName,
                    chipset = chipset,
                    isRtlSdr = device.vendorId == 0x0BDA
                )
            } else null
        }
    }
}

package com.abhishek.zerodroid.features.usbcamera.domain

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager

class UsbCameraDetector(
    private val context: Context,
    private val usbManager: UsbManager?
) {
    companion object {
        const val USB_CLASS_VIDEO = 14
    }

    fun detectUsbVideoDevices(): List<UsbVideoDevice> {
        val devices = usbManager?.deviceList?.values ?: return emptyList()
        return devices.filter { device ->
            (0 until device.interfaceCount).any { i ->
                device.getInterface(i).interfaceClass == USB_CLASS_VIDEO
            }
        }.map { device ->
            UsbVideoDevice(
                vendorId = device.vendorId,
                productId = device.productId,
                deviceName = device.productName ?: device.deviceName,
                manufacturerName = device.manufacturerName
            )
        }
    }

    fun detectCamera2External(): List<UsbCameraInfo> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return emptyList()

        return try {
            cameraManager.cameraIdList.mapNotNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                    val configs = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val resolutions = configs?.getOutputSizes(android.graphics.ImageFormat.JPEG)
                        ?.map { "${it.width}x${it.height}" }
                        ?: emptyList()

                    UsbCameraInfo(
                        cameraId = id,
                        isExternal = true,
                        deviceName = "External Camera #$id",
                        resolutions = resolutions
                    )
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

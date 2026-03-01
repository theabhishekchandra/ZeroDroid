package com.abhishek.zerodroid.features.usb.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UsbDeviceManager(
    private val context: Context,
    private val usbManager: UsbManager?
) {

    fun observeDevices(): Flow<List<UsbDeviceInfo>> = callbackFlow {
        fun emitCurrentDevices() {
            val devices = usbManager?.deviceList?.values?.map { it.toInfo() } ?: emptyList()
            trySend(devices)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED,
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> emitCurrentDevices()
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        emitCurrentDevices()

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    fun getConnectedDevices(): List<UsbDeviceInfo> {
        return usbManager?.deviceList?.values?.map { it.toInfo() } ?: emptyList()
    }

    private fun UsbDevice.toInfo(): UsbDeviceInfo {
        val interfaces = (0 until interfaceCount).map { i ->
            val iface = getInterface(i)
            val endpoints = (0 until iface.endpointCount).map { e ->
                val ep = iface.getEndpoint(e)
                UsbEndpointInfo(
                    address = ep.address,
                    direction = if (ep.direction == android.hardware.usb.UsbConstants.USB_DIR_IN) "IN" else "OUT",
                    type = when (ep.type) {
                        android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "Control"
                        android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_ISOC -> "Isochronous"
                        android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK -> "Bulk"
                        android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_INT -> "Interrupt"
                        else -> "Unknown"
                    },
                    maxPacketSize = ep.maxPacketSize
                )
            }
            UsbInterfaceInfo(
                id = iface.id,
                interfaceClass = iface.interfaceClass,
                interfaceSubclass = iface.interfaceSubclass,
                interfaceProtocol = iface.interfaceProtocol,
                endpointCount = iface.endpointCount,
                endpoints = endpoints
            )
        }
        return UsbDeviceInfo(
            vendorId = vendorId,
            productId = productId,
            deviceName = deviceName,
            manufacturerName = manufacturerName,
            productName = productName,
            deviceClass = deviceClass,
            deviceSubclass = deviceSubclass,
            interfaceCount = interfaceCount,
            interfaces = interfaces
        )
    }
}

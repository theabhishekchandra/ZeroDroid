package com.abhishek.zerodroid.features.bluetooth_classic.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BluetoothClassicScanner(
    private val context: Context,
    private val bluetoothManager: BluetoothManager?
) {

    private val adapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    val isAvailable: Boolean
        get() = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<ClassicBluetoothDevice> {
        return adapter?.bondedDevices?.map { it.toClassicDevice(isPaired = true) } ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    fun discover(): Flow<List<ClassicBluetoothDevice>> = callbackFlow {
        val devices = mutableMapOf<String, ClassicBluetoothDevice>()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                        device?.let {
                            val classic = it.toClassicDevice(rssi = rssi)
                            devices[classic.address] = classic
                            trySend(devices.values.sortedByDescending { d -> d.rssi })
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        // Discovery finished naturally
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        adapter?.startDiscovery()

        awaitClose {
            adapter?.cancelDiscovery()
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun cancelDiscovery() {
        adapter?.cancelDiscovery()
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toClassicDevice(
        rssi: Int = 0,
        isPaired: Boolean = false
    ): ClassicBluetoothDevice {
        val majorClassName = when (bluetoothClass?.majorDeviceClass) {
            BluetoothClass.Device.Major.COMPUTER -> "Computer"
            BluetoothClass.Device.Major.PHONE -> "Phone"
            BluetoothClass.Device.Major.AUDIO_VIDEO -> "Audio/Video"
            BluetoothClass.Device.Major.NETWORKING -> "Networking"
            BluetoothClass.Device.Major.PERIPHERAL -> "Peripheral"
            BluetoothClass.Device.Major.IMAGING -> "Imaging"
            BluetoothClass.Device.Major.WEARABLE -> "Wearable"
            BluetoothClass.Device.Major.TOY -> "Toy"
            BluetoothClass.Device.Major.HEALTH -> "Health"
            BluetoothClass.Device.Major.MISC -> "Misc"
            BluetoothClass.Device.Major.UNCATEGORIZED -> "Uncategorized"
            else -> "Unknown"
        }

        return ClassicBluetoothDevice(
            name = name,
            address = address,
            rssi = rssi,
            bondState = bondState,
            majorClass = majorClassName,
            isPaired = isPaired || bondState == BluetoothDevice.BOND_BONDED
        )
    }
}

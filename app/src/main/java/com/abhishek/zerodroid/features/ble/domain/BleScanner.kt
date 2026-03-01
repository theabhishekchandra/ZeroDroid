package com.abhishek.zerodroid.features.ble.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class BleScanner(
    private val bluetoothManager: BluetoothManager?
) {
    companion object {
        private const val SCAN_TIMEOUT_MS = 30_000L
    }

    private val scanner: BluetoothLeScanner?
        get() = bluetoothManager?.adapter?.bluetoothLeScanner

    val isAvailable: Boolean
        get() = bluetoothManager?.adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun scan(): Flow<List<BleDevice>> = callbackFlow {
        val leScanner = scanner
        if (leScanner == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val devices = mutableMapOf<String, BleDevice>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = BleDevice(
                    name = result.device.name,
                    address = result.device.address,
                    rssi = result.rssi,
                    serviceUuids = result.scanRecord?.serviceUuids
                        ?.map { it.toString() } ?: emptyList(),
                    lastSeen = System.currentTimeMillis()
                )
                devices[device.address] = device
                trySend(devices.values.sortedByDescending { it.rssi })
            }

            override fun onScanFailed(errorCode: Int) {
                close(Exception("BLE scan failed with error code: $errorCode"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        leScanner.startScan(null, settings, callback)

        // Auto-stop after 30 seconds to save battery
        launch {
            delay(SCAN_TIMEOUT_MS)
            leScanner.stopScan(callback)
            close()
        }

        awaitClose {
            leScanner.stopScan(callback)
        }
    }
}

package com.abhishek.zerodroid.features.ble.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
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
    /**
     * Scans for [timeoutMs] then closes. [lowLatency] trades battery for a report on every
     * advertisement, which Locate needs to follow signal strength in real time.
     */
    fun scan(lowLatency: Boolean = false, timeoutMs: Long = SCAN_TIMEOUT_MS): Flow<List<BleDevice>> = callbackFlow {
        val leScanner = scanner
        if (leScanner == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val devices = mutableMapOf<String, BleDevice>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val resolvedName = result.scanRecord?.deviceName ?: result.device.name
                Log.d(
                    "BleScanner",
                    "addr=${result.device.address} rssi=${result.rssi} scanRecordName=${result.scanRecord?.deviceName} " +
                        "deviceName=${result.device.name} bytes=${result.scanRecord?.bytes?.size} raw=${result.scanRecord?.bytes?.joinToString("") { "%02x".format(it) }}"
                )
                val device = BleDevice(
                    name = resolvedName ?: devices[result.device.address]?.name,
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
            .setScanMode(if (lowLatency) ScanSettings.SCAN_MODE_LOW_LATENCY else ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        leScanner.startScan(null, settings, callback)

        // Auto-stop to save battery
        launch {
            delay(timeoutMs)
            leScanner.stopScan(callback)
            close()
        }

        awaitClose {
            leScanner.stopScan(callback)
        }
    }
}

package com.abhishek.zerodroid.features.bluetooth_classic.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException

class SppConnectionManager(
    private val bluetoothManager: BluetoothManager?
) {
    companion object {
        private const val TAG = "SppConnectionManager"
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null

    private val _sppState = MutableStateFlow(SppState())
    val sppState: StateFlow<SppState> = _sppState.asStateFlow()

    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String) {
        _sppState.value = SppState(isConnecting = true)
        withContext(Dispatchers.IO) {
            try {
                val device = bluetoothManager?.adapter?.getRemoteDevice(deviceAddress)
                    ?: throw IOException("Device not found")

                bluetoothManager.adapter?.cancelDiscovery()

                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()

                _sppState.value = SppState(isConnected = true)

                // Start reading
                val buffer = ByteArray(1024)
                val inputStream = socket?.inputStream
                while (socket?.isConnected == true) {
                    try {
                        val bytesRead = inputStream?.read(buffer) ?: break
                        if (bytesRead > 0) {
                            val text = String(buffer, 0, bytesRead, Charsets.UTF_8)
                            val line = TerminalLine(text = text, isOutgoing = false)
                            _sppState.value = _sppState.value.copy(
                                lines = (_sppState.value.lines + line).takeLast(200)
                            )
                        }
                    } catch (e: IOException) {
                        break
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _sppState.value = SppState(
                    isConnected = false,
                    error = "Connection failed: ${e.message}"
                )
            }
        }
    }

    suspend fun send(text: String) {
        withContext(Dispatchers.IO) {
            try {
                socket?.outputStream?.write(text.toByteArray(Charsets.UTF_8))
                socket?.outputStream?.flush()
                val line = TerminalLine(text = text, isOutgoing = true)
                _sppState.value = _sppState.value.copy(
                    lines = (_sppState.value.lines + line).takeLast(200)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _sppState.value = _sppState.value.copy(
                    error = "Send failed: ${e.message}"
                )
            }
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (e: Exception) {
            Log.d(TAG, "Failed to close SPP socket during disconnect", e)
        }
        socket = null
        _sppState.value = SppState()
    }
}

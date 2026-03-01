package com.abhishek.zerodroid.features.bluetooth_classic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothClassicScanner
import com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothClassicState
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SppConnectionManager
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SppState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class BluetoothClassicViewModel(
    private val scanner: BluetoothClassicScanner,
    private val sppManager: SppConnectionManager
) : ViewModel() {

    private val _state = MutableStateFlow(BluetoothClassicState())
    val state: StateFlow<BluetoothClassicState> = _state.asStateFlow()

    val sppState: StateFlow<SppState> = sppManager.sppState

    private var scanJob: Job? = null

    val isAvailable: Boolean get() = scanner.isAvailable

    init {
        loadPairedDevices()
    }

    private fun loadPairedDevices() {
        _state.value = _state.value.copy(pairedDevices = scanner.getPairedDevices())
    }

    fun toggleScan() {
        if (_state.value.isScanning) stopScan() else startScan()
    }

    private fun startScan() {
        scanJob?.cancel()
        _state.value = _state.value.copy(isScanning = true, discoveredDevices = emptyList())
        scanJob = viewModelScope.launch {
            scanner.discover()
                .catch { e ->
                    _state.value = _state.value.copy(isScanning = false, error = e.message)
                }
                .collect { devices ->
                    _state.value = _state.value.copy(discoveredDevices = devices)
                }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanner.cancelDiscovery()
        scanJob = null
        _state.value = _state.value.copy(isScanning = false)
    }

    fun connectSpp(deviceAddress: String) {
        stopScan()
        viewModelScope.launch {
            sppManager.connect(deviceAddress)
        }
    }

    fun sendSpp(text: String) {
        viewModelScope.launch {
            sppManager.send(text)
        }
    }

    fun disconnectSpp() {
        sppManager.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
        sppManager.disconnect()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return BluetoothClassicViewModel(
                    app.container.bluetoothClassicScanner,
                    app.container.sppConnectionManager
                ) as T
            }
        }
    }
}

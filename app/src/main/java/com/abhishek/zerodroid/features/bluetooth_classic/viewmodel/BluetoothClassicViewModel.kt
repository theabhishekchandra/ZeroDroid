package com.abhishek.zerodroid.features.bluetooth_classic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothClassicScanner
import com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothClassicState
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SppConnectionManager
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SppState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.debug.DemoData
import com.abhishek.zerodroid.core.debug.observeDemoRequests

@HiltViewModel
class BluetoothClassicViewModel @Inject constructor(
    private val scanner: BluetoothClassicScanner,
    private val sppManager: SppConnectionManager,
    private val demoBus: DemoDataBus
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

    fun startScan() {
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
        stopScan()
        sppManager.disconnect()
    }

    init {
        observeDemoRequests(demoBus, DemoData.Routes.BLUETOOTH_CLASSIC) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        stopScan()
        _state.value = _state.value.copy(
            discoveredDevices = DemoData.classicDevices,
            pairedDevices = DemoData.classicDevices.filter { it.isPaired },
            error = null
        )
    }
}

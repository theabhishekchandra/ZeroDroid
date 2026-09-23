package com.abhishek.zerodroid.features.bluetooth_classic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothClassicScanner
import com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothClassicState
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SppConnectionManager
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SppState
import com.abhishek.zerodroid.core.sessions.ItemKind
import com.abhishek.zerodroid.core.sessions.SessionItem
import com.abhishek.zerodroid.core.sessions.SessionRepository
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
    private val sessions: SessionRepository,
    private val demoBus: DemoDataBus
) : ViewModel() {

    /** When the current run started; null when no run is in progress. */
    private var runStartedAt: Long? = null


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
        runStartedAt = System.currentTimeMillis()
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
        runStartedAt?.let { started ->
            runStartedAt = null
            recordSession(started)
        }
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

    private fun recordSession(startedAt: Long) {
        val devices = _state.value.discoveredDevices
        sessions.recordInBackground(
            tool = "bluetooth_classic",
            title = "Bluetooth Classic discovery",
            startedAt = startedAt,
            items = devices.map { d ->
                SessionItem(key = d.address, label = d.displayName, kind = ItemKind.BT, rssi = d.rssi.takeIf { it != 0 }, detail = d.majorClass)
            },
            summary = "${devices.size} discoverable device${if (devices.size == 1) "" else "s"}"
        )
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

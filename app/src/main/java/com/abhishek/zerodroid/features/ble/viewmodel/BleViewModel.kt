package com.abhishek.zerodroid.features.ble.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.features.ble.data.BleRepository
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.ble.domain.BleScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
class BleViewModel @Inject constructor(
    private val repository: BleRepository,
    private val demoBus: DemoDataBus
) : ViewModel() {

    private val _scanState = MutableStateFlow(BleScanState(isBluetoothEnabled = repository.isAvailable))
    val scanState: StateFlow<BleScanState> = _scanState.asStateFlow()

    private var scanJob: Job? = null
    private var autoStopJob: Job? = null

    val isAvailable: Boolean get() = repository.isAvailable

    fun toggleScan() {
        if (_scanState.value.isScanning) {
            stopScan()
        } else {
            startScan()
        }
    }

    fun startScan() {
        if (!repository.isAvailable) {
            _scanState.value = _scanState.value.copy(isBluetoothEnabled = false)
            return
        }

        scanJob?.cancel()
        _scanState.value = BleScanState(isScanning = true, isBluetoothEnabled = true)
        scanJob = viewModelScope.launch {
            repository.scan()
                .catch { e ->
                    _scanState.value = _scanState.value.copy(
                        isScanning = false,
                        error = e.message
                    )
                }
                .collect { devices ->
                    _scanState.value = _scanState.value.copy(devices = devices)
                }
        }
        autoStopJob?.cancel()
        autoStopJob = viewModelScope.launch {
            delay(AUTO_STOP_TIMEOUT_MS)
            stopScan()
        }
    }

    fun stopScan() {
        autoStopJob?.cancel()
        autoStopJob = null
        scanJob?.cancel()
        scanJob = null
        _scanState.value = _scanState.value.copy(isScanning = false)
    }

    companion object {
        private const val AUTO_STOP_TIMEOUT_MS = 30_000L
    }

    fun toggleBookmark(device: BleDevice) {
        viewModelScope.launch {
            repository.toggleBookmark(device)
            // The scan flow only pushes updates while a scan is actively being collected, but
            // bookmarking must also work on a stopped/idle results list — flip it locally too.
            _scanState.value = _scanState.value.copy(
                devices = _scanState.value.devices.map {
                    if (it.address == device.address) it.copy(isBookmarked = !it.isBookmarked) else it
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }

    init {
        observeDemoRequests(demoBus, DemoData.Routes.BLE) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        stopScan()
        _scanState.value = _scanState.value.copy(devices = DemoData.bleDevices, isBluetoothEnabled = true, error = null)
    }
}

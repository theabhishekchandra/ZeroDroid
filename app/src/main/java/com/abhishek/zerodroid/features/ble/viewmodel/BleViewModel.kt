package com.abhishek.zerodroid.features.ble.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.features.ble.data.BleRepository
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.ble.domain.BleScanState
import com.abhishek.zerodroid.core.sessions.ItemKind
import com.abhishek.zerodroid.core.sessions.SessionItem
import com.abhishek.zerodroid.core.sessions.SessionRepository
import com.abhishek.zerodroid.features.ble.domain.BleDeviceTypeIdentifier
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
    private val sessions: SessionRepository,
    private val demoBus: DemoDataBus
) : ViewModel() {

    /** When the current run started; null when no run is in progress. */
    private var runStartedAt: Long? = null


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
        runStartedAt = System.currentTimeMillis()
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
        runStartedAt?.let { started ->
            runStartedAt = null
            recordSession(started)
        }
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

    private fun recordSession(startedAt: Long) {
        val devices = _scanState.value.devices
        val typed = devices.map { it to BleDeviceTypeIdentifier.identify(it.name, it.serviceUuids).category }
        val trackers = typed.count { it.second == "Tracker" }
        sessions.recordInBackground(
            tool = "ble",
            title = "BLE scan",
            startedAt = startedAt,
            items = typed.map { (d, category) ->
                SessionItem(
                    key = d.address,
                    label = d.name ?: "[no name]",
                    kind = if (category == "Tracker") ItemKind.TRACKER else ItemKind.BLE,
                    rssi = d.rssi,
                    detail = category,
                    flagged = category == "Tracker"
                )
            },
            summary = "${devices.size} devices" + if (trackers > 0) " · $trackers tracker${if (trackers > 1) "s" else ""}" else ""
        )
    }

    override fun onCleared() {
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

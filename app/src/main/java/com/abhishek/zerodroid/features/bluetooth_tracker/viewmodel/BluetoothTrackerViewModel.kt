package com.abhishek.zerodroid.features.bluetooth_tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.DetectedTracker
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerIdentifier
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerScanState
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerType
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackingRisk
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BluetoothTrackerViewModel(
    private val bleScanner: BleScanner
) : ViewModel() {

    private val _state = MutableStateFlow(TrackerScanState())
    val state: StateFlow<TrackerScanState> = _state.asStateFlow()

    private val identifier = TrackerIdentifier()
    private val trackerMap = mutableMapOf<String, DetectedTracker>()
    private var scanJob: Job? = null
    private var timerJob: Job? = null
    private var scanStartTime: Long = 0L
    private var totalDevicesScanned = 0

    fun startScan() {
        if (_state.value.isScanning) return

        scanJob?.cancel()
        timerJob?.cancel()
        trackerMap.clear()
        totalDevicesScanned = 0
        scanStartTime = System.currentTimeMillis()

        _state.value = TrackerScanState(isScanning = true)

        // Timer to update scan duration every second
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val elapsed = System.currentTimeMillis() - scanStartTime
                _state.value = _state.value.copy(scanDurationMs = elapsed)
            }
        }

        scanJob = viewModelScope.launch {
            bleScanner.scan()
                .catch { e ->
                    _state.value = _state.value.copy(
                        isScanning = false,
                        error = "Scan failed: ${e.message}"
                    )
                    timerJob?.cancel()
                }
                .collect { devices ->
                    totalDevicesScanned = devices.size
                    val now = System.currentTimeMillis()

                    for (device in devices) {
                        val type = identifier.identify(device)
                        if (type == TrackerType.UNKNOWN) continue

                        val existing = trackerMap[device.address]
                        if (existing != null) {
                            // Update existing tracker
                            val updated = existing.copy(
                                rssi = device.rssi,
                                lastSeen = now,
                                seenCount = existing.seenCount + 1,
                                name = device.name ?: existing.name
                            )
                            trackerMap[device.address] = updated.copy(
                                risk = identifier.assessRisk(updated)
                            )
                        } else {
                            // New tracker detected
                            val tracker = DetectedTracker(
                                address = device.address,
                                name = device.name,
                                type = type,
                                rssi = device.rssi,
                                firstSeen = now,
                                lastSeen = now,
                                seenCount = 1,
                                risk = TrackingRisk.LOW
                            )
                            trackerMap[device.address] = tracker
                        }
                    }

                    // Rebuild the state with sorted trackers
                    val sortedTrackers = trackerMap.values
                        .sortedWith(
                            compareBy<DetectedTracker> { it.risk.ordinal }
                                .thenByDescending { it.seenCount }
                        )
                        .toList()

                    val highRisk = sortedTrackers.count {
                        it.risk == TrackingRisk.HIGH
                    }

                    _state.value = _state.value.copy(
                        trackers = sortedTrackers,
                        totalDevicesScanned = totalDevicesScanned,
                        highRiskCount = highRisk,
                        scanDurationMs = System.currentTimeMillis() - scanStartTime
                    )
                }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        timerJob?.cancel()
        scanJob = null
        timerJob = null
        _state.value = _state.value.copy(isScanning = false)
    }

    fun clearTrackers() {
        stopScan()
        trackerMap.clear()
        totalDevicesScanned = 0
        scanStartTime = 0L
        _state.value = TrackerScanState()
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return BluetoothTrackerViewModel(app.container.bleScanner) as T
            }
        }
    }
}

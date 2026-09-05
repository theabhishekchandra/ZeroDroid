package com.abhishek.zerodroid.features.bluetooth_tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.alerts.AlertSource
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.DetectedTracker
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerIdentifier
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerScanState
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerType
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackingRisk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.debug.DemoData
import com.abhishek.zerodroid.core.debug.observeDemoRequests

@HiltViewModel
class BluetoothTrackerViewModel @Inject constructor(
    private val bleScanner: BleScanner,
    private val alertCenterRepository: AlertCenterRepository,
    private val demoBus: DemoDataBus
) : ViewModel() {

    private val _state = MutableStateFlow(TrackerScanState())
    val state: StateFlow<TrackerScanState> = _state.asStateFlow()

    private val identifier = TrackerIdentifier()
    private val trackerMap = mutableMapOf<String, DetectedTracker>()
    private var scanJob: Job? = null
    private var timerJob: Job? = null
    private var scanStartTime: Long = 0L
    private var totalDevicesScanned = 0

    // Highest risk tier already reported per address, so we only alert again
    // when a tracker's risk climbs to a new tier, not on every BLE advertisement.
    private val reportedRiskTiers = mutableMapOf<String, TrackingRisk>()

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

                    reportRiskEscalations(sortedTrackers)

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
        reportedRiskTiers.clear()
        _state.value = TrackerScanState()
    }

    private fun reportRiskEscalations(trackers: List<DetectedTracker>) {
        val escalated = trackers.filter { tracker ->
            val rank = tracker.risk.severityRank()
            if (rank < MIN_ALERT_RANK) return@filter false
            val previousRank = reportedRiskTiers[tracker.address]?.severityRank() ?: -1
            if (rank > previousRank) {
                reportedRiskTiers[tracker.address] = tracker.risk
                true
            } else {
                false
            }
        }
        if (escalated.isEmpty()) return

        viewModelScope.launch {
            escalated.forEach { tracker ->
                alertCenterRepository.record(
                    source = AlertSource.BLUETOOTH_TRACKER,
                    severity = if (tracker.risk == TrackingRisk.HIGH) AlertSeverity.HIGH else AlertSeverity.MEDIUM,
                    title = "Possible unwanted tracker: ${tracker.displayName}",
                    detail = "${tracker.type.label} seen ${tracker.seenCount}x, address ${tracker.address}, " +
                        "signal ${tracker.signalPercent}%",
                    timestamp = tracker.lastSeen
                )
            }
        }
    }

    private fun TrackingRisk.severityRank(): Int = when (this) {
        TrackingRisk.HIGH -> 3
        TrackingRisk.MEDIUM -> 2
        TrackingRisk.LOW -> 1
        TrackingRisk.NONE -> 0
    }

    companion object {
        private const val MIN_ALERT_RANK = 2 // MEDIUM or HIGH only
    }

    override fun onCleared() {
        stopScan()
    }

    init {
        observeDemoRequests(demoBus, DemoData.Routes.BLUETOOTH_TRACKER) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        stopScan()
        _state.value = _state.value.copy(
            trackers = DemoData.trackers,
            totalDevicesScanned = 42,
            scanDurationMs = 95_000L,
            highRiskCount = DemoData.trackers.count { it.risk == TrackingRisk.HIGH },
            error = null
        )
    }
}

package com.abhishek.zerodroid.features.rf_bug_sweeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.BugDetection
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.BugSweepState
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.RfBugDetector
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.SweepMode
import com.abhishek.zerodroid.features.sensors.domain.MetalDetector
import com.abhishek.zerodroid.features.sensors.domain.SensorDataCollector
import com.abhishek.zerodroid.features.ultrasonic.domain.UltrasonicAnalyzer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class RfBugSweeperViewModel(
    private val bleScanner: BleScanner,
    private val ultrasonicAnalyzer: UltrasonicAnalyzer,
    private val sensorDataCollector: SensorDataCollector
) : ViewModel() {

    private val _state = MutableStateFlow(BugSweepState())
    val state: StateFlow<BugSweepState> = _state.asStateFlow()

    private val rfBugDetector = RfBugDetector()
    private val metalDetector = MetalDetector()

    // Per-source detection maps for deduplication
    private val bleDetections = mutableMapOf<String, BugDetection>()     // keyed by BLE address
    private var ultrasonicDetection: BugDetection? = null
    private var magneticDetection: BugDetection? = null

    private var bleJob: Job? = null
    private var ultrasonicJob: Job? = null
    private var magneticJob: Job? = null
    private var timerJob: Job? = null
    private var sweepStartMs: Long = 0L

    // ── Public API ─────────────────────────────────────────────────────

    fun startSweep(modes: Set<SweepMode> = setOf(SweepMode.ALL)) {
        val resolvedModes = if (modes.contains(SweepMode.ALL)) {
            setOf(SweepMode.BLE, SweepMode.ULTRASONIC, SweepMode.MAGNETIC)
        } else {
            modes
        }

        sweepStartMs = System.currentTimeMillis()

        _state.value = _state.value.copy(
            isSweeping = true,
            activeModes = resolvedModes,
            error = null
        )

        if (SweepMode.BLE in resolvedModes) startBleSweep()
        if (SweepMode.ULTRASONIC in resolvedModes) startUltrasonicSweep()
        if (SweepMode.MAGNETIC in resolvedModes) startMagneticSweep()
        startTimer()
    }

    fun stopSweep() {
        bleJob?.cancel()
        ultrasonicJob?.cancel()
        magneticJob?.cancel()
        timerJob?.cancel()
        bleJob = null
        ultrasonicJob = null
        magneticJob = null
        timerJob = null
        sensorDataCollector.stop()
        metalDetector.reset()

        _state.value = _state.value.copy(
            isSweeping = false,
            activeModes = emptySet()
        )
    }

    fun clearDetections() {
        bleDetections.clear()
        ultrasonicDetection = null
        magneticDetection = null
        metalDetector.reset()
        _state.value = BugSweepState()
    }

    fun calibrateMagnetic() {
        metalDetector.reset()
        magneticDetection = null
        rebuildDetections()
    }

    // ── BLE sweep ──────────────────────────────────────────────────────

    private fun startBleSweep() {
        bleJob?.cancel()
        bleJob = viewModelScope.launch {
            bleScanner.scan()
                .catch { e ->
                    _state.value = _state.value.copy(
                        error = "BLE scan error: ${e.message}"
                    )
                }
                .collect { devices ->
                    // Update total BLE device count
                    _state.value = _state.value.copy(bleDeviceCount = devices.size)

                    // Run detection analysis — dedup by address
                    bleDetections.clear()
                    val suspicious = rfBugDetector.analyseBleDevices(devices)
                    for (det in suspicious) {
                        // Use the device address extracted from the detection id
                        val key = det.id
                        bleDetections[key] = det
                    }
                    rebuildDetections()
                }
        }
    }

    // ── Ultrasonic sweep ───────────────────────────────────────────────

    private fun startUltrasonicSweep() {
        ultrasonicJob?.cancel()
        ultrasonicJob = viewModelScope.launch {
            ultrasonicAnalyzer.analyze()
                .catch { e ->
                    _state.value = _state.value.copy(
                        error = "Ultrasonic error: ${e.message}"
                    )
                }
                .collect { uState ->
                    if (uState.error != null) {
                        _state.value = _state.value.copy(error = "Ultrasonic: ${uState.error}")
                        return@collect
                    }

                    val detection = rfBugDetector.analyseUltrasonic(
                        peakFrequencyHz = uState.peakFrequency,
                        peakMagnitude = uState.peakMagnitude,
                        beaconCount = uState.detectedBeacons.size
                    )

                    ultrasonicDetection = detection
                    _state.value = _state.value.copy(
                        ultrasonicDetected = detection != null
                    )
                    rebuildDetections()
                }
        }
    }

    // ── Magnetic sweep ─────────────────────────────────────────────────

    private fun startMagneticSweep() {
        magneticJob?.cancel()
        sensorDataCollector.start()
        magneticJob = viewModelScope.launch {
            sensorDataCollector.magnetometer.collect { reading ->
                if (!reading.isAvailable || reading.values.isEmpty()) return@collect

                val metalState = metalDetector.update(reading.values)

                _state.value = _state.value.copy(
                    magneticBaseline = metalState.baseline,
                    magneticCurrent = metalState.currentMagnitude,
                    magneticDeviation = metalState.deviation
                )

                val detection = rfBugDetector.analyseMagnetic(
                    baseline = metalState.baseline,
                    current = metalState.currentMagnitude,
                    deviation = metalState.deviation
                )

                magneticDetection = detection
                rebuildDetections()
            }
        }
    }

    // ── Timer ──────────────────────────────────────────────────────────

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.value = _state.value.copy(
                    sweepDurationMs = System.currentTimeMillis() - sweepStartMs
                )
            }
        }
    }

    // ── Rebuild combined detection list ────────────────────────────────

    private fun rebuildDetections() {
        val all = mutableListOf<BugDetection>()
        all.addAll(bleDetections.values)
        ultrasonicDetection?.let { all.add(it) }
        magneticDetection?.let { all.add(it) }

        // Sort: CRITICAL first, then by severity ordinal, then newest first
        all.sortWith(
            compareBy<BugDetection> { it.severity.ordinal }
                .thenByDescending { it.timestamp }
        )

        _state.value = _state.value.copy(detections = all)
    }

    // ── Lifecycle ──────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        stopSweep()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                val c = app.container
                return RfBugSweeperViewModel(
                    bleScanner = c.bleScanner,
                    ultrasonicAnalyzer = c.ultrasonicAnalyzer,
                    sensorDataCollector = c.sensorDataCollector
                ) as T
            }
        }
    }
}

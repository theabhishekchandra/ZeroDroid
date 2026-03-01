package com.abhishek.zerodroid.features.gps_spoof_detector.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.celltower.domain.CellTowerAnalyzer
import com.abhishek.zerodroid.features.celltower.domain.CellTowerState
import com.abhishek.zerodroid.features.gps.domain.GpsState
import com.abhishek.zerodroid.features.gps.domain.GpsTracker
import com.abhishek.zerodroid.features.gps_spoof_detector.domain.GpsSpoofDetector
import com.abhishek.zerodroid.features.gps_spoof_detector.domain.GpsSpoofState
import com.abhishek.zerodroid.features.gps_spoof_detector.domain.SpoofCheckResult
import com.abhishek.zerodroid.features.sensors.domain.SensorDataCollector
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class GpsSpoofViewModel(
    private val gpsTracker: GpsTracker,
    private val cellTowerAnalyzer: CellTowerAnalyzer,
    private val wifiScanner: WifiScanner,
    private val sensorDataCollector: SensorDataCollector,
    private val spoofDetector: GpsSpoofDetector
) : ViewModel() {

    private val _state = MutableStateFlow(GpsSpoofState())
    val state: StateFlow<GpsSpoofState> = _state.asStateFlow()

    // Latest data from each source
    private var latestGps = GpsState()
    private var latestCell = CellTowerState()
    private var latestWifiAps: List<WifiAccessPoint> = emptyList()

    // Collection jobs
    private var gpsJob: Job? = null
    private var cellJob: Job? = null
    private var wifiJob: Job? = null
    private var analysisJob: Job? = null

    // History of results (keep last 20)
    private val resultHistory = mutableListOf<SpoofCheckResult>()

    fun startMonitoring() {
        if (_state.value.isMonitoring) return

        spoofDetector.reset()
        resultHistory.clear()
        _state.value = GpsSpoofState(
            isMonitoring = true,
            gpsStatus = "Starting...",
            cellStatus = "Starting...",
            wifiStatus = "Starting...",
            sensorStatus = "Starting..."
        )

        // Start sensor collection
        sensorDataCollector.start()
        _state.value = _state.value.copy(sensorStatus = "Active")

        // Start GPS tracking
        gpsJob = viewModelScope.launch {
            gpsTracker.track()
                .catch { e ->
                    _state.value = _state.value.copy(gpsStatus = "Error: ${e.message}")
                }
                .collect { gps ->
                    latestGps = gps
                    _state.value = _state.value.copy(
                        gpsStatus = if (gps.latitude != 0.0) "Locked (${gps.satelliteCount} sats)" else "Acquiring..."
                    )
                }
        }

        // Start cell tower monitoring
        cellJob = viewModelScope.launch {
            cellTowerAnalyzer.monitor()
                .catch { e ->
                    _state.value = _state.value.copy(cellStatus = "Error: ${e.message}")
                }
                .collect { cell ->
                    latestCell = cell
                    _state.value = _state.value.copy(
                        cellStatus = if (cell.currentCell != null) {
                            "${cell.currentCell.type.displayName} (CID: ${cell.currentCell.cid})"
                        } else "No cell info"
                    )
                }
        }

        // Start WiFi scanning
        wifiJob = viewModelScope.launch {
            wifiScanner.scan()
                .catch { e ->
                    _state.value = _state.value.copy(wifiStatus = "Error: ${e.message}")
                }
                .collect { aps ->
                    latestWifiAps = aps
                    _state.value = _state.value.copy(
                        wifiStatus = "${aps.size} APs visible"
                    )
                }
        }

        // Run analysis every 5 seconds
        analysisJob = viewModelScope.launch {
            // Initial delay to let sources collect data
            delay(2000)
            while (true) {
                runAnalysis()
                delay(5000)
            }
        }
    }

    fun stopMonitoring() {
        gpsJob?.cancel()
        cellJob?.cancel()
        wifiJob?.cancel()
        analysisJob?.cancel()
        gpsJob = null
        cellJob = null
        wifiJob = null
        analysisJob = null

        sensorDataCollector.stop()

        _state.value = _state.value.copy(
            isMonitoring = false,
            gpsStatus = "Idle",
            cellStatus = "Idle",
            wifiStatus = "Idle",
            sensorStatus = "Idle"
        )
    }

    private fun runAnalysis() {
        try {
            val accelerometer = sensorDataCollector.accelerometer.value
            val barometer = sensorDataCollector.barometer.value

            val result = spoofDetector.analyze(
                gps = latestGps,
                cell = latestCell.currentCell,
                wifiAps = latestWifiAps,
                accelerometer = accelerometer,
                barometer = barometer
            )

            resultHistory.add(0, result)
            if (resultHistory.size > 20) resultHistory.removeLast()

            val spoofDetected = result.spoofConfidence > 0.3f

            _state.value = _state.value.copy(
                spoofDetected = spoofDetected,
                confidence = result.spoofConfidence,
                results = resultHistory.toList(),
                error = null
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                error = "Analysis error: ${e.message}"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                val container = app.container
                return GpsSpoofViewModel(
                    gpsTracker = container.gpsTracker,
                    cellTowerAnalyzer = container.cellTowerAnalyzer,
                    wifiScanner = container.wifiScanner,
                    sensorDataCollector = container.sensorDataCollector,
                    spoofDetector = GpsSpoofDetector(app.applicationContext)
                ) as T
            }
        }
    }
}

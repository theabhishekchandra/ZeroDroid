package com.abhishek.zerodroid.features.sensors.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.sensors.domain.FloorState
import com.abhishek.zerodroid.features.sensors.domain.FloorTracker
import com.abhishek.zerodroid.features.sensors.domain.MetalDetector
import com.abhishek.zerodroid.features.sensors.domain.MetalDetectorState
import com.abhishek.zerodroid.features.sensors.domain.SensorDataCollector
import com.abhishek.zerodroid.features.sensors.domain.SensorReading
import com.abhishek.zerodroid.features.sensors.domain.TiltState
import com.abhishek.zerodroid.features.sensors.domain.VibrationSeverity
import com.abhishek.zerodroid.features.sensors.domain.VibrationState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.sqrt

class SensorViewModel(
    private val sensorDataCollector: SensorDataCollector
) : ViewModel() {

    companion object {
        private const val AUTO_STOP_TIMEOUT_MS = 60_000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return SensorViewModel(app.container.sensorDataCollector) as T
            }
        }
    }

    private val metalDetector = MetalDetector()
    private val floorTracker = FloorTracker()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var autoStopJob: Job? = null

    val accelerometer: StateFlow<SensorReading> = sensorDataCollector.accelerometer
    val gyroscope: StateFlow<SensorReading> = sensorDataCollector.gyroscope
    val magnetometer: StateFlow<SensorReading> = sensorDataCollector.magnetometer
    val barometer: StateFlow<SensorReading> = sensorDataCollector.barometer
    val light: StateFlow<SensorReading> = sensorDataCollector.light
    val proximity: StateFlow<SensorReading> = sensorDataCollector.proximity

    val metalDetectorState: StateFlow<MetalDetectorState> =
        sensorDataCollector.magnetometer
            .map { reading ->
                if (reading.values.size >= 3) {
                    metalDetector.update(reading.values)
                } else {
                    MetalDetectorState()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetalDetectorState())

    val floorState: StateFlow<FloorState> =
        sensorDataCollector.barometer
            .map { reading ->
                if (reading.values.isNotEmpty()) {
                    floorTracker.update(reading.values[0])
                } else {
                    FloorState()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FloorState())

    val compassHeading: StateFlow<Float> =
        sensorDataCollector.magnetometer
            .map { reading ->
                if (reading.values.size >= 3) {
                    val mx = reading.values[0]
                    val my = reading.values[1]
                    var heading = Math.toDegrees(atan2(my.toDouble(), mx.toDouble())).toFloat()
                    if (heading < 0) heading += 360f
                    heading
                } else 0f
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val tiltState: StateFlow<TiltState> =
        sensorDataCollector.accelerometer
            .map { reading ->
                if (reading.values.size >= 3) {
                    val x = reading.values[0]
                    val y = reading.values[1]
                    val z = reading.values[2]
                    val pitch = Math.toDegrees(atan2(x.toDouble(), sqrt((y * y + z * z).toDouble()))).toFloat()
                    val roll = Math.toDegrees(atan2(y.toDouble(), sqrt((x * x + z * z).toDouble()))).toFloat()
                    TiltState(pitch = pitch, roll = roll, isLevel = kotlin.math.abs(pitch) < 2f && kotlin.math.abs(roll) < 2f)
                } else TiltState()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TiltState())

    private val vibrationHistory = mutableListOf<Float>()
    private var vibrationPeak = 0f

    val vibrationState: StateFlow<VibrationState> =
        sensorDataCollector.accelerometer
            .map { reading ->
                if (reading.values.size >= 3) {
                    val x = reading.values[0]
                    val y = reading.values[1]
                    val z = reading.values[2]
                    val magnitude = sqrt(x * x + y * y + z * z) - 9.81f
                    val absMag = kotlin.math.abs(magnitude)

                    synchronized(vibrationHistory) {
                        vibrationHistory.add(absMag)
                        if (vibrationHistory.size > 200) vibrationHistory.removeAt(0)
                        if (absMag > vibrationPeak) vibrationPeak = absMag
                    }

                    val severity = when {
                        absMag < 0.5f -> VibrationSeverity.NONE
                        absMag < 2f -> VibrationSeverity.LOW
                        absMag < 5f -> VibrationSeverity.MODERATE
                        absMag < 10f -> VibrationSeverity.HIGH
                        else -> VibrationSeverity.EXTREME
                    }

                    VibrationState(
                        currentMagnitude = absMag,
                        peakMagnitude = vibrationPeak,
                        severity = severity,
                        history = synchronized(vibrationHistory) { vibrationHistory.toList() }
                    )
                } else VibrationState()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VibrationState())

    fun resetVibrationPeak() {
        vibrationPeak = 0f
        synchronized(vibrationHistory) { vibrationHistory.clear() }
    }

    fun startSensors() {
        sensorDataCollector.start()
        _isMonitoring.value = true
        autoStopJob?.cancel()
        autoStopJob = viewModelScope.launch {
            delay(AUTO_STOP_TIMEOUT_MS)
            stopSensors()
        }
    }

    fun stopSensors() {
        autoStopJob?.cancel()
        autoStopJob = null
        sensorDataCollector.stop()
        _isMonitoring.value = false
    }

    fun toggleMonitoring() {
        if (_isMonitoring.value) stopSensors() else startSensors()
    }

    fun resetMetalDetector() {
        metalDetector.reset()
    }

    override fun onCleared() {
        super.onCleared()
        stopSensors()
    }
}

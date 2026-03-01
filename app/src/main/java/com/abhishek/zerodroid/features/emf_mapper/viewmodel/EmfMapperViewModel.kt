package com.abhishek.zerodroid.features.emf_mapper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.emf_mapper.domain.EmfDataProcessor
import com.abhishek.zerodroid.features.emf_mapper.domain.EmfMapperState
import com.abhishek.zerodroid.features.sensors.domain.MetalDetector
import com.abhishek.zerodroid.features.sensors.domain.SensorDataCollector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmfMapperViewModel(
    private val sensorDataCollector: SensorDataCollector
) : ViewModel() {

    private val metalDetector = MetalDetector()
    private val processor = EmfDataProcessor()

    private val _state = MutableStateFlow(EmfMapperState())
    val state: StateFlow<EmfMapperState> = _state.asStateFlow()

    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var recordingStartTime: Long = 0L

    init {
        // Check sensor availability on init
        viewModelScope.launch {
            sensorDataCollector.start()
            delay(200)
            val available = sensorDataCollector.magnetometer.value.isAvailable
            if (!available) {
                _state.value = _state.value.copy(
                    sensorAvailable = false,
                    error = "Magnetometer sensor not available on this device"
                )
            }
            sensorDataCollector.stop()
        }
    }

    fun startRecording() {
        if (_state.value.isRecording) return
        if (!_state.value.sensorAvailable) return

        sensorDataCollector.start()
        metalDetector.reset()
        recordingStartTime = System.currentTimeMillis()

        _state.value = _state.value.copy(
            isRecording = true,
            error = null
        )

        // Sensor collection at ~10Hz
        recordingJob = viewModelScope.launch {
            sensorDataCollector.magnetometer.collect { sensorReading ->
                if (!_state.value.isRecording) return@collect
                if (sensorReading.values.size < 3) return@collect

                val metalState = metalDetector.update(sensorReading.values)
                val emfReading = processor.processReading(sensorReading.values, metalState.baseline)

                val currentHistory = _state.value.history.toMutableList()
                currentHistory.add(emfReading)
                // Keep last 300 readings (30 seconds at 10Hz)
                if (currentHistory.size > 300) {
                    currentHistory.removeAt(0)
                }

                val isHotspot = processor.detectHotspot(emfReading)
                val hotspotCount = if (isHotspot) _state.value.hotspots + 1 else _state.value.hotspots

                val (min, max, avg) = processor.getStatistics(currentHistory)

                _state.value = _state.value.copy(
                    currentReading = emfReading,
                    baseline = metalState.baseline,
                    peakMagnitude = max,
                    minMagnitude = min,
                    avgMagnitude = avg,
                    history = currentHistory,
                    hotspots = hotspotCount
                )

                delay(100) // ~10Hz sampling
            }
        }

        // Duration timer
        timerJob = viewModelScope.launch {
            while (_state.value.isRecording) {
                _state.value = _state.value.copy(
                    recordingDurationMs = System.currentTimeMillis() - recordingStartTime
                )
                delay(1000)
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        timerJob?.cancel()
        timerJob = null
        sensorDataCollector.stop()

        _state.value = _state.value.copy(
            isRecording = false,
            recordingDurationMs = System.currentTimeMillis() - recordingStartTime
        )
    }

    fun resetBaseline() {
        metalDetector.reset()
        _state.value = _state.value.copy(baseline = 0f)
    }

    fun clearHistory() {
        recordingJob?.cancel()
        recordingJob = null
        timerJob?.cancel()
        timerJob = null
        sensorDataCollector.stop()
        metalDetector.reset()

        _state.value = EmfMapperState(sensorAvailable = _state.value.sensorAvailable)
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        timerJob?.cancel()
        sensorDataCollector.stop()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return EmfMapperViewModel(app.container.sensorDataCollector) as T
            }
        }
    }
}

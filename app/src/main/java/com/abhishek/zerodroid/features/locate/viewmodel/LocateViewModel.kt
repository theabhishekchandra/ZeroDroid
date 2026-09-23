package com.abhishek.zerodroid.features.locate.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.locate.domain.LocateReading
import com.abhishek.zerodroid.features.locate.domain.LocateTracker
import com.abhishek.zerodroid.features.locate.domain.LocateTrend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocateUiState(
    val address: String = "",
    val label: String = "",
    val running: Boolean = false,
    val reading: LocateReading? = null,
    /** Recent smoothed values, oldest first, for the trail. */
    val trail: List<Int> = emptyList(),
    val lost: Boolean = false,
    val beep: Boolean = true,
    val haptics: Boolean = true,
    val error: String? = null
)

/** Follows one BLE device's signal so the person can walk towards it. */
@HiltViewModel
class LocateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scanner: BleScanner
) : ViewModel() {

    private val tracker = LocateTracker()

    private val _state = MutableStateFlow(
        LocateUiState(
            address = savedStateHandle.get<String>("address").orEmpty(),
            label = savedStateHandle.get<String>("label").orEmpty().trim()
        )
    )
    val state: StateFlow<LocateUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        tracker.reset()
        _state.update { it.copy(running = true, reading = null, trail = emptyList(), lost = false, error = null) }
        job = viewModelScope.launch {
            launch {
                while (isActive) {
                    delay(1_000)
                    if (tracker.isLost(System.currentTimeMillis())) {
                        _state.update { it.copy(lost = true, reading = it.reading?.copy(trend = LocateTrend.LOST)) }
                    }
                }
            }
            // Each scan closes after its timeout, so keep restarting while locating.
            while (isActive) {
                scanner.scan(lowLatency = true, timeoutMs = SCAN_WINDOW_MS)
                    .catch { e -> _state.update { it.copy(error = e.message ?: "Bluetooth scan failed") } }
                    .collect { devices -> devices.firstOrNull { it.address.equals(_state.value.address, true) }?.let { onSample(it.rssi, it.lastSeen) } }
                if (_state.value.error != null) break
            }
            _state.update { it.copy(running = false) }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.update { it.copy(running = false) }
    }

    fun toggleBeep() = _state.update { it.copy(beep = !it.beep) }
    fun toggleHaptics() = _state.update { it.copy(haptics = !it.haptics) }

    private var lastSeen = 0L

    /** The scanner re-emits the whole device map; only a fresh advertisement is a new sample. */
    internal fun onSample(rssi: Int, seenAt: Long) {
        if (seenAt == lastSeen) return
        lastSeen = seenAt
        val reading = tracker.add(rssi, System.currentTimeMillis())
        _state.update { it.copy(reading = reading, lost = false, trail = (it.trail + reading.rssi).takeLast(TRAIL)) }
    }

    override fun onCleared() {
        job?.cancel()
    }

    companion object {
        const val SCAN_WINDOW_MS = 60_000L
        const val TRAIL = 60
    }
}

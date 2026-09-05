package com.abhishek.zerodroid.features.gps.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.features.gps.domain.GpsState
import com.abhishek.zerodroid.features.gps.domain.GpsTracker
import com.abhishek.zerodroid.features.gps.domain.SatelliteInfo
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
class GpsViewModel @Inject constructor(
    private val gpsTracker: GpsTracker,
    private val demoBus: DemoDataBus
) : ViewModel() {

    private val _state = MutableStateFlow(GpsState())
    val state: StateFlow<GpsState> = _state.asStateFlow()

    private var trackingJob: Job? = null

    fun toggleTracking() {
        if (_state.value.isTracking) stopTracking() else startTracking()
    }

    fun startTracking() {
        trackingJob?.cancel()
        _state.value = GpsState(isTracking = true)
        trackingJob = viewModelScope.launch {
            gpsTracker.track()
                .catch { e ->
                    _state.value = _state.value.copy(
                        isTracking = false,
                        error = e.message
                    )
                }
                .collect { state ->
                    _state.value = state.copy(satellites = state.satellites.sortedInFixOrder())
                }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _state.value = _state.value.copy(isTracking = false)
    }

    override fun onCleared() {
        stopTracking()
    }

    private fun List<SatelliteInfo>.sortedInFixOrder(): List<SatelliteInfo> =
        sortedWith(compareByDescending<SatelliteInfo> { it.usedInFix }.thenByDescending { it.cn0DbHz })

    init {
        observeDemoRequests(demoBus, DemoData.Routes.GPS) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        stopTracking()
        _state.value = DemoData.gpsState
    }
}

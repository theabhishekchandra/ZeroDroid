package com.abhishek.zerodroid.features.gps.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.gps.domain.GpsState
import com.abhishek.zerodroid.features.gps.domain.GpsTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class GpsViewModel(
    private val gpsTracker: GpsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(GpsState())
    val state: StateFlow<GpsState> = _state.asStateFlow()

    private var trackingJob: Job? = null

    fun toggleTracking() {
        if (_state.value.isTracking) stopTracking() else startTracking()
    }

    private fun startTracking() {
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
                    _state.value = state
                }
        }
    }

    private fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _state.value = _state.value.copy(isTracking = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return GpsViewModel(app.container.gpsTracker) as T
            }
        }
    }
}

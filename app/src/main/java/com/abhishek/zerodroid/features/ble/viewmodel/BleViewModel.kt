package com.abhishek.zerodroid.features.ble.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.ble.data.BleRepository
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.ble.domain.BleScanState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class BleViewModel(
    private val repository: BleRepository
) : ViewModel() {

    private val _scanState = MutableStateFlow(BleScanState())
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

    private fun startScan() {
        scanJob?.cancel()
        _scanState.value = BleScanState(isScanning = true)
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
        autoStopJob?.cancel()
        autoStopJob = null
        scanJob?.cancel()
        scanJob = null
        _scanState.value = _scanState.value.copy(isScanning = false)
    }

    companion object {
        private const val AUTO_STOP_TIMEOUT_MS = 30_000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return BleViewModel(app.container.bleRepository) as T
            }
        }
    }

    fun toggleBookmark(device: BleDevice) {
        viewModelScope.launch {
            repository.toggleBookmark(device)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}

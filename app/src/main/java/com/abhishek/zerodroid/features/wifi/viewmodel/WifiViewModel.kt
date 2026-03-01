package com.abhishek.zerodroid.features.wifi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.core.util.WifiBand
import com.abhishek.zerodroid.features.wifi.domain.ChannelAnalyzer
import com.abhishek.zerodroid.features.wifi.domain.ChannelScore
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WifiViewModel(
    private val wifiScanner: WifiScanner
) : ViewModel() {

    private val _accessPoints = MutableStateFlow<List<WifiAccessPoint>>(emptyList())
    val accessPoints: StateFlow<List<WifiAccessPoint>> = _accessPoints.asStateFlow()

    private val _channelScores = MutableStateFlow<List<ChannelScore>>(emptyList())
    val channelScores: StateFlow<List<ChannelScore>> = _channelScores.asStateFlow()

    private val _selectedBand = MutableStateFlow<WifiBand?>(null)
    val selectedBand: StateFlow<WifiBand?> = _selectedBand.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanJob: Job? = null
    private var autoStopJob: Job? = null

    fun startScan() {
        if (scanJob?.isActive == true) return
        _isScanning.value = true
        scanJob = viewModelScope.launch {
            wifiScanner.scan().collect { aps ->
                _accessPoints.value = aps
                _channelScores.value = ChannelAnalyzer.analyze(aps)
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
        _isScanning.value = false
    }

    companion object {
        private const val AUTO_STOP_TIMEOUT_MS = 30_000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return WifiViewModel(app.container.wifiScanner) as T
            }
        }
    }

    fun selectBand(band: WifiBand?) {
        _selectedBand.value = band
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}

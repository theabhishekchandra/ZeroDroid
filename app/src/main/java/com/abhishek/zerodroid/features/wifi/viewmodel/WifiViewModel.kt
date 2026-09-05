package com.abhishek.zerodroid.features.wifi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.util.WifiBand
import com.abhishek.zerodroid.features.wifi.domain.ChannelAnalyzer
import com.abhishek.zerodroid.features.wifi.domain.ChannelScore
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.catch

@HiltViewModel
class WifiViewModel @Inject constructor(
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var scanJob: Job? = null
    private var autoStopJob: Job? = null

    fun startScan() {
        if (scanJob?.isActive == true) return
        _isScanning.value = true
        _error.value = null
        scanJob = viewModelScope.launch {
            wifiScanner.scan()
                .catch { e ->
                    // A revoked location permission or a disabled adapter must not take the
                    // whole process down; report it like the other scanners do.
                    _isScanning.value = false
                    _error.value = "WiFi scan error: ${e.message}"
                }
                .collect { aps ->
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
    }

    fun selectBand(band: WifiBand?) {
        _selectedBand.value = band
    }

    override fun onCleared() {
        stopScan()
    }
}

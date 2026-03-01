package com.abhishek.zerodroid.features.rogue_ap_detector.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RogueApAnalyzer
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RogueApState
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class RogueApViewModel(
    private val wifiScanner: WifiScanner
) : ViewModel() {

    private val analyzer = RogueApAnalyzer()

    private val _state = MutableStateFlow(RogueApState())
    val state: StateFlow<RogueApState> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun startScan() {
        if (scanJob?.isActive == true) return
        _state.value = _state.value.copy(isScanning = true, error = null)

        scanJob = viewModelScope.launch {
            wifiScanner.scan()
                .catch { e ->
                    _state.value = _state.value.copy(
                        isScanning = false,
                        error = "WiFi scan error: ${e.message}"
                    )
                }
                .collect { accessPoints ->
                    val currentKnown = _state.value.knownSsids
                    val alerts = analyzer.analyze(accessPoints, currentKnown)

                    val suspiciousBssids = alerts.map { it.suspiciousAp.bssid }.toSet()
                    val safeCount = accessPoints.count { it.bssid !in suspiciousBssids }

                    _state.value = _state.value.copy(
                        isScanning = true,
                        totalAps = accessPoints.size,
                        alerts = alerts,
                        safeAps = safeCount,
                        suspiciousAps = alerts.size,
                        error = null
                    )
                }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _state.value = _state.value.copy(isScanning = false)
    }

    fun addKnownSsid(ssid: String) {
        val trimmed = ssid.trim()
        if (trimmed.isBlank()) return
        _state.value = _state.value.copy(
            knownSsids = _state.value.knownSsids + trimmed
        )
    }

    fun removeKnownSsid(ssid: String) {
        _state.value = _state.value.copy(
            knownSsids = _state.value.knownSsids - ssid
        )
    }

    fun clearAlerts() {
        _state.value = _state.value.copy(
            alerts = emptyList(),
            suspiciousAps = 0,
            safeAps = _state.value.totalAps
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return RogueApViewModel(app.container.wifiScanner) as T
            }
        }
    }
}

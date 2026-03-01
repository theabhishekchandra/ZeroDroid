package com.abhishek.zerodroid.features.deauth_detector.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.deauth_detector.domain.DeauthAnalyzer
import com.abhishek.zerodroid.features.deauth_detector.domain.DeauthState
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class DeauthDetectorViewModel(
    private val wifiScanner: WifiScanner,
    private val context: Context
) : ViewModel() {

    private val analyzer = DeauthAnalyzer(context)

    private val _state = MutableStateFlow(DeauthState())
    val state: StateFlow<DeauthState> = _state.asStateFlow()

    private var monitorJob: Job? = null
    private var timerJob: Job? = null
    private var monitoringStartTime = 0L

    fun startMonitoring() {
        if (monitorJob?.isActive == true) return

        analyzer.reset()
        monitoringStartTime = System.currentTimeMillis()

        _state.value = DeauthState(isMonitoring = true)

        // Register connectivity tracking for disconnect/reconnect detection
        analyzer.startConnectivityTracking(
            onDisconnect = {
                viewModelScope.launch {
                    val current = _state.value
                    _state.value = current.copy(
                        disconnectCount = analyzer.getDisconnectCount()
                    )
                }
            },
            onReconnect = {
                viewModelScope.launch {
                    val (_, bssid, _) = analyzer.getConnectedWifiInfo()
                    if (bssid != null) {
                        analyzer.recordReconnect(bssid)
                    }
                }
            }
        )

        // Start monitoring duration timer
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - monitoringStartTime
                _state.value = _state.value.copy(monitoringDurationMs = elapsed)
            }
        }

        // Start continuous WiFi scanning and analysis
        monitorJob = viewModelScope.launch {
            wifiScanner.scan()
                .catch { e ->
                    _state.value = _state.value.copy(
                        isMonitoring = false,
                        error = "WiFi scan error: ${e.message}"
                    )
                }
                .collect { accessPoints ->
                    // Get current connection info
                    val (ssid, bssid, rssi) = analyzer.getConnectedWifiInfo()
                    val connectedAp = accessPoints.find { it.bssid == bssid }
                    val connectedChannel = connectedAp?.channel ?: 0

                    // Run detection algorithms
                    val newEvents = analyzer.analyze(accessPoints, ssid, bssid)

                    // Merge new events with existing ones (newest first, dedup by id)
                    val current = _state.value
                    val existingIds = current.events.map { it.id }.toSet()
                    val uniqueNewEvents = newEvents.filter { it.id !in existingIds }
                    val allEvents = (uniqueNewEvents + current.events)

                    val isUnderAttack = allEvents.any {
                        val age = System.currentTimeMillis() - it.timestamp
                        age < 30_000 // Attack active if any event within last 30 seconds
                    }

                    _state.value = current.copy(
                        isMonitoring = true,
                        events = allEvents,
                        connectedSsid = ssid,
                        connectedBssid = bssid,
                        connectedRssi = rssi,
                        connectedChannel = connectedChannel,
                        disconnectCount = analyzer.getDisconnectCount(),
                        apHistory = analyzer.getApHistory(),
                        isUnderAttack = isUnderAttack,
                        error = null
                    )
                }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        timerJob?.cancel()
        timerJob = null
        analyzer.stopConnectivityTracking()
        _state.value = _state.value.copy(isMonitoring = false)
    }

    fun clearEvents() {
        analyzer.reset()
        _state.value = _state.value.copy(
            events = emptyList(),
            disconnectCount = 0,
            apHistory = emptyMap(),
            isUnderAttack = false
        )
    }

    /**
     * Get disconnect timestamps for the timeline visualization.
     */
    fun getDisconnectTimestamps(): List<Long> = analyzer.getDisconnectTimestamps()

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return DeauthDetectorViewModel(app.container.wifiScanner, app.applicationContext) as T
            }
        }
    }
}

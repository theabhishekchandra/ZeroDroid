package com.abhishek.zerodroid.features.rogue_ap_detector.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.alerts.AlertSource
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RiskLevel
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RogueApAlert
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RogueApAnalyzer
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RogueApState
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
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
class RogueApViewModel @Inject constructor(
    private val wifiScanner: WifiScanner,
    private val alertCenterRepository: AlertCenterRepository,
    private val demoBus: DemoDataBus
) : ViewModel() {

    private val analyzer = RogueApAnalyzer()

    private val _state = MutableStateFlow(RogueApState())
    val state: StateFlow<RogueApState> = _state.asStateFlow()

    private var scanJob: Job? = null

    // Keys of alerts already forwarded to the Alert Center this session, so a
    // persistent rogue AP doesn't get re-recorded on every WiFi scan cycle.
    private val reportedAlertKeys = mutableSetOf<String>()

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

                    reportNewAlerts(alerts)

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
        reportedAlertKeys.clear()
        _state.value = _state.value.copy(
            alerts = emptyList(),
            suspiciousAps = 0,
            safeAps = _state.value.totalAps
        )
    }

    private fun reportNewAlerts(alerts: List<RogueApAlert>) {
        val newAlerts = alerts.filter { reportedAlertKeys.add("${it.suspiciousAp.bssid}:${it.threatType}") }
        if (newAlerts.isEmpty()) return
        viewModelScope.launch {
            newAlerts.forEach { alert ->
                alertCenterRepository.record(
                    source = AlertSource.ROGUE_AP,
                    severity = alert.riskLevel.toAlertSeverity(),
                    title = alert.title,
                    detail = alert.description,
                    timestamp = alert.timestamp
                )
            }
        }
    }

    private fun RiskLevel.toAlertSeverity(): AlertSeverity = when (this) {
        RiskLevel.CRITICAL -> AlertSeverity.CRITICAL
        RiskLevel.HIGH -> AlertSeverity.HIGH
        RiskLevel.MEDIUM, RiskLevel.SAFE -> AlertSeverity.MEDIUM
        RiskLevel.LOW -> AlertSeverity.LOW
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }

    init {
        observeDemoRequests(demoBus, DemoData.Routes.ROGUE_AP) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        stopScan()
        _state.value = _state.value.copy(totalAps = 9, alerts = DemoData.rogueAlerts, safeAps = 6, suspiciousAps = 3, error = null)
    }
}

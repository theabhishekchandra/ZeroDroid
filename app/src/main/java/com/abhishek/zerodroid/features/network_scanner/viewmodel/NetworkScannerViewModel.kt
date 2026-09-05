package com.abhishek.zerodroid.features.network_scanner.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.features.network_scanner.domain.NetworkScanState
import com.abhishek.zerodroid.features.network_scanner.domain.NetworkVulnerabilityScanner
import com.abhishek.zerodroid.features.network_scanner.domain.VulnerabilityLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.debug.DemoData
import com.abhishek.zerodroid.core.debug.observeDemoRequests

@HiltViewModel
class NetworkScannerViewModel @Inject constructor(
    private val scanner: NetworkVulnerabilityScanner,
    @ApplicationContext private val appContext: Context,
    private val demoBus: DemoDataBus
) : ViewModel() {

    private val _state = MutableStateFlow(NetworkScanState())
    val state: StateFlow<NetworkScanState> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun startScan() {
        if (scanJob?.isActive == true) return

        val subnet = scanner.getSubnet(appContext)
        if (subnet == null) {
            _state.value = _state.value.copy(
                error = "Cannot determine local network. Ensure WiFi is connected."
            )
            return
        }

        scanJob = viewModelScope.launch {
            _state.value = NetworkScanState(
                isScanning = true,
                subnet = subnet,
                scanPhase = "Initializing"
            )

            try {
                val devices = scanner.scanNetwork(subnet) { progress, phase ->
                    _state.value = _state.value.copy(
                        progress = progress,
                        scanPhase = phase,
                        currentIp = if (progress < 1f) "$subnet.*" else null
                    )
                }

                val totalVulns = devices.sumOf { it.vulnerabilities.size }
                val criticalCount = devices.sumOf { device ->
                    device.vulnerabilities.count { it.level == VulnerabilityLevel.CRITICAL }
                }

                _state.value = _state.value.copy(
                    isScanning = false,
                    progress = 1f,
                    currentIp = null,
                    devices = devices,
                    totalVulnerabilities = totalVulns,
                    criticalCount = criticalCount,
                    scanPhase = "Complete"
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.value = _state.value.copy(
                    isScanning = false,
                    scanPhase = "Error",
                    error = "Scan failed: ${e.message}"
                )
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _state.value = _state.value.copy(
            isScanning = false,
            scanPhase = "Cancelled",
            currentIp = null
        )
    }

    fun rescanDevice(ip: String) {
        viewModelScope.launch {
            try {
                val updatedDevice = scanner.rescanSingleDevice(ip)
                val currentDevices = _state.value.devices.toMutableList()
                val index = currentDevices.indexOfFirst { it.ip == ip }

                if (updatedDevice != null) {
                    if (index >= 0) {
                        currentDevices[index] = updatedDevice
                    } else {
                        currentDevices.add(updatedDevice)
                    }
                } else if (index >= 0) {
                    // Device no longer responding, remove it
                    currentDevices.removeAt(index)
                }

                // Re-sort by vulnerability count
                val sorted = currentDevices.sortedByDescending { it.vulnerabilities.size }
                val totalVulns = sorted.sumOf { it.vulnerabilities.size }
                val criticalCount = sorted.sumOf { device ->
                    device.vulnerabilities.count { it.level == VulnerabilityLevel.CRITICAL }
                }

                _state.value = _state.value.copy(
                    devices = sorted,
                    totalVulnerabilities = totalVulns,
                    criticalCount = criticalCount
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.value = _state.value.copy(
                    error = "Rescan failed for $ip: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }

    init {
        observeDemoRequests(demoBus, DemoData.Routes.NETWORK_SCANNER) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        stopScan()
        val demo = DemoData.networkDevices
        _state.value = _state.value.copy(
            subnet = "192.168.1",
            progress = 1f,
            currentIp = null,
            devices = demo,
            totalVulnerabilities = demo.sumOf { it.vulnerabilities.size },
            criticalCount = demo.sumOf { d -> d.vulnerabilities.count { it.level == VulnerabilityLevel.CRITICAL } },
            scanPhase = "Complete",
            error = null
        )
    }
}

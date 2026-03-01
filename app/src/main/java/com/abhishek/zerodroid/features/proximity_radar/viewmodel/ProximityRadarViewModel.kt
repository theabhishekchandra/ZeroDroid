package com.abhishek.zerodroid.features.proximity_radar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.proximity_radar.domain.DeviceCategory
import com.abhishek.zerodroid.features.proximity_radar.domain.ProximityCalculator
import com.abhishek.zerodroid.features.proximity_radar.domain.RadarDevice
import com.abhishek.zerodroid.features.proximity_radar.domain.RadarState
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProximityRadarViewModel(
    private val bleScanner: BleScanner,
    private val wifiScanner: WifiScanner
) : ViewModel() {

    private val _state = MutableStateFlow(RadarState())
    val state: StateFlow<RadarState> = _state.asStateFlow()

    private var bleScanJob: Job? = null
    private var wifiScanJob: Job? = null

    private val bleDevices = mutableMapOf<String, RadarDevice>()
    private val wifiDevices = mutableMapOf<String, RadarDevice>()

    fun toggleScan() {
        if (_state.value.isScanning) {
            stopScan()
        } else {
            startScan()
        }
    }

    fun startScan() {
        bleScanJob?.cancel()
        wifiScanJob?.cancel()
        bleDevices.clear()
        wifiDevices.clear()

        _state.value = RadarState(isScanning = true)

        bleScanJob = viewModelScope.launch {
            bleScanner.scan()
                .catch { e ->
                    _state.value = _state.value.copy(
                        error = "BLE scan error: ${e.message}"
                    )
                }
                .collect { devices ->
                    devices.forEach { ble ->
                        val category = ProximityCalculator.classifyBleDevice(ble.serviceUuids)
                        val distance = ProximityCalculator.estimateDistance(ble.rssi)
                        bleDevices[ble.address] = RadarDevice(
                            id = ble.address,
                            name = ble.displayName,
                            category = category,
                            rssi = ble.rssi,
                            estimatedDistanceM = distance,
                            angle = ProximityCalculator.stableAngle(ble.address),
                            lastSeen = ble.lastSeen,
                            signalPercent = ble.signalPercent
                        )
                    }
                    mergeAndUpdateState()
                }
        }

        wifiScanJob = viewModelScope.launch {
            wifiScanner.scan()
                .catch { e ->
                    _state.value = _state.value.copy(
                        error = "WiFi scan error: ${e.message}"
                    )
                }
                .collect { accessPoints ->
                    accessPoints.forEach { ap ->
                        val distance = ProximityCalculator.estimateDistance(ap.rssi, txPower = -40)
                        wifiDevices[ap.bssid] = RadarDevice(
                            id = ap.bssid,
                            name = ap.ssid,
                            category = DeviceCategory.WIFI_AP,
                            rssi = ap.rssi,
                            estimatedDistanceM = distance,
                            angle = ProximityCalculator.stableAngle(ap.bssid),
                            lastSeen = System.currentTimeMillis(),
                            signalPercent = ap.signalPercent
                        )
                    }
                    mergeAndUpdateState()
                }
        }
    }

    fun stopScan() {
        bleScanJob?.cancel()
        wifiScanJob?.cancel()
        bleScanJob = null
        wifiScanJob = null
        _state.value = _state.value.copy(isScanning = false)
    }

    private fun mergeAndUpdateState() {
        val allDevices = (bleDevices.values + wifiDevices.values)
            .sortedBy { it.estimatedDistanceM }

        val nearest = allDevices.firstOrNull()
        val scanRadius = ProximityCalculator.autoScanRadius(allDevices)

        _state.value = _state.value.copy(
            devices = allDevices,
            wifiCount = wifiDevices.size,
            bleCount = bleDevices.size,
            nearestDevice = nearest,
            scanRadius = scanRadius
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
                return ProximityRadarViewModel(app.container.bleScanner, app.container.wifiScanner) as T
            }
        }
    }
}

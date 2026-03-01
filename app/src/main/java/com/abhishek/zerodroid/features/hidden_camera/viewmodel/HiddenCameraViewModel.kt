package com.abhishek.zerodroid.features.hidden_camera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.hidden_camera.domain.CameraDetection
import com.abhishek.zerodroid.features.hidden_camera.domain.DetectionSource
import com.abhishek.zerodroid.features.hidden_camera.domain.HiddenCameraDetector
import com.abhishek.zerodroid.features.hidden_camera.domain.HiddenCameraScanState
import com.abhishek.zerodroid.features.sensors.domain.MetalDetector
import com.abhishek.zerodroid.features.sensors.domain.SensorDataCollector
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.math.abs

class HiddenCameraViewModel(
    private val detector: HiddenCameraDetector,
    private val wifiScanner: WifiScanner,
    private val bleScanner: BleScanner,
    private val sensorDataCollector: SensorDataCollector
) : ViewModel() {

    private val _state = MutableStateFlow(HiddenCameraScanState())
    val state: StateFlow<HiddenCameraScanState> = _state.asStateFlow()

    private val metalDetector = MetalDetector()
    private var wifiJob: Job? = null
    private var bleJob: Job? = null
    private var magneticJob: Job? = null
    private var networkJob: Job? = null

    // Track detection IDs to avoid duplicates
    private val wifiDetections = mutableMapOf<String, CameraDetection>() // keyed by BSSID
    private val bleDetections = mutableMapOf<String, CameraDetection>()  // keyed by address
    private var magneticDetection: CameraDetection? = null
    private val networkDetections = mutableMapOf<String, CameraDetection>() // keyed by IP

    fun startScan() {
        startWifiScan()
        startBleScan()
        startMagneticScan()
        _state.value = _state.value.copy(isScanning = true)
    }

    fun stopScan() {
        wifiJob?.cancel()
        bleJob?.cancel()
        magneticJob?.cancel()
        networkJob?.cancel()
        wifiJob = null
        bleJob = null
        magneticJob = null
        networkJob = null
        sensorDataCollector.stop()
        metalDetector.reset()
        _state.value = _state.value.copy(
            isScanning = false,
            activeMode = null,
            networkScanProgress = null
        )
    }

    fun startIrMode() {
        _state.value = _state.value.copy(irActive = true)
    }

    fun stopIrMode() {
        _state.value = _state.value.copy(irActive = false)
    }

    fun addIrDetection(detection: CameraDetection) {
        _state.value = _state.value.copy(
            detections = _state.value.detections + detection
        )
    }

    fun startNetworkScan() {
        if (networkJob?.isActive == true) return
        val subnet = detector.getLocalSubnet()
        if (subnet == null) {
            _state.value = _state.value.copy(error = "Cannot determine local network subnet")
            return
        }

        networkDetections.clear()
        networkJob = viewModelScope.launch {
            _state.value = _state.value.copy(networkScanProgress = "Starting scan on $subnet.x...")
            for (i in 1..254) {
                val ip = "$subnet.$i"
                _state.value = _state.value.copy(
                    networkScanProgress = "Scanning $ip ($i/254)"
                )
                val detection = detector.scanHost(ip)
                if (detection != null) {
                    networkDetections[ip] = detection
                    rebuildDetections()
                }
            }
            _state.value = _state.value.copy(networkScanProgress = null)
        }
    }

    fun clearDetections() {
        wifiDetections.clear()
        bleDetections.clear()
        magneticDetection = null
        networkDetections.clear()
        metalDetector.reset()
        _state.value = HiddenCameraScanState()
    }

    private fun startWifiScan() {
        wifiJob?.cancel()
        wifiJob = viewModelScope.launch {
            _state.value = _state.value.copy(activeMode = DetectionSource.WIFI)
            wifiScanner.scan()
                .catch { e ->
                    _state.value = _state.value.copy(error = "WiFi scan error: ${e.message}")
                }
                .collect { accessPoints ->
                    wifiDetections.clear()
                    for (ap in accessPoints) {
                        // Check OUI first (higher priority)
                        val ouiMatch = detector.matchWifiOui(ap)
                        if (ouiMatch != null) {
                            wifiDetections[ap.bssid] = ouiMatch
                            continue
                        }
                        // Then check SSID pattern
                        val ssidMatch = detector.matchWifiSsid(ap)
                        if (ssidMatch != null) {
                            wifiDetections[ap.bssid] = ssidMatch
                        }
                    }
                    rebuildDetections()
                }
        }
    }

    private fun startBleScan() {
        bleJob?.cancel()
        bleJob = viewModelScope.launch {
            bleScanner.scan()
                .catch { e ->
                    _state.value = _state.value.copy(error = "BLE scan error: ${e.message}")
                }
                .collect { devices ->
                    bleDetections.clear()
                    for (device in devices) {
                        // Check OUI first
                        val ouiMatch = detector.matchBleOui(device)
                        if (ouiMatch != null) {
                            bleDetections[device.address] = ouiMatch
                            continue
                        }
                        // Then check name pattern
                        val nameMatch = detector.matchBleDevice(device)
                        if (nameMatch != null) {
                            bleDetections[device.address] = nameMatch
                        }
                    }
                    rebuildDetections()
                }
        }
    }

    private fun startMagneticScan() {
        magneticJob?.cancel()
        sensorDataCollector.start()
        magneticJob = viewModelScope.launch {
            sensorDataCollector.magnetometer.collect { reading ->
                if (!reading.isAvailable || reading.values.isEmpty()) return@collect
                val metalState = metalDetector.update(reading.values)
                val anomalyDetection = detector.checkMagneticAnomaly(metalState.deviation)
                magneticDetection = anomalyDetection
                rebuildDetections()
            }
        }
    }

    private fun rebuildDetections() {
        val allDetections = mutableListOf<CameraDetection>()
        allDetections.addAll(wifiDetections.values)
        allDetections.addAll(bleDetections.values)
        magneticDetection?.let { allDetections.add(it) }
        allDetections.addAll(networkDetections.values)

        // Sort by threat level (HIGH first), then timestamp (newest first)
        allDetections.sortWith(compareBy<CameraDetection> { it.threatLevel.ordinal }.thenByDescending { it.timestamp })

        _state.value = _state.value.copy(
            detections = allDetections,
            wifiSuspects = wifiDetections.size,
            bleSuspects = bleDetections.size,
            magneticAnomaly = magneticDetection != null,
            networkSuspects = networkDetections.size
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
                val container = app.container
                return HiddenCameraViewModel(
                    detector = container.hiddenCameraDetector,
                    wifiScanner = container.wifiScanner,
                    bleScanner = container.bleScanner,
                    sensorDataCollector = container.sensorDataCollector
                ) as T
            }
        }
    }
}

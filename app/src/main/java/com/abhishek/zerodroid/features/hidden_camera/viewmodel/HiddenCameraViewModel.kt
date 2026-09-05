package com.abhishek.zerodroid.features.hidden_camera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.alerts.AlertSource
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.hidden_camera.domain.CameraDetection
import com.abhishek.zerodroid.features.hidden_camera.domain.DetectionSource
import com.abhishek.zerodroid.features.hidden_camera.domain.HiddenCameraDetector
import com.abhishek.zerodroid.features.hidden_camera.domain.HiddenCameraScanState
import com.abhishek.zerodroid.features.hidden_camera.domain.ThreatLevel
import com.abhishek.zerodroid.features.sensors.domain.MetalDetector
import com.abhishek.zerodroid.features.sensors.domain.SensorDataCollector
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.math.abs
import javax.inject.Inject
import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.debug.DemoData
import com.abhishek.zerodroid.core.debug.observeDemoRequests

@HiltViewModel
class HiddenCameraViewModel @Inject constructor(
    private val detector: HiddenCameraDetector,
    private val wifiScanner: WifiScanner,
    private val bleScanner: BleScanner,
    private val sensorDataCollector: SensorDataCollector,
    private val alertCenterRepository: AlertCenterRepository,
    private val demoBus: DemoDataBus
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

    // Keys already forwarded to the Alert Center this session.
    private val reportedDetectionKeys = mutableSetOf<String>()

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
        reportedDetectionKeys.clear()
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

        reportNewDetections(allDetections)

        _state.value = _state.value.copy(
            detections = allDetections,
            wifiSuspects = wifiDetections.size,
            bleSuspects = bleDetections.size,
            magneticAnomaly = magneticDetection != null,
            networkSuspects = networkDetections.size
        )
    }

    private fun reportNewDetections(detections: List<CameraDetection>) {
        val newDetections = detections.filter { reportedDetectionKeys.add(it.dedupeKey()) }
        if (newDetections.isEmpty()) return
        viewModelScope.launch {
            newDetections.forEach { detection ->
                alertCenterRepository.record(
                    source = AlertSource.HIDDEN_CAMERA,
                    severity = detection.threatLevel.toAlertSeverity(),
                    title = detection.title,
                    detail = detection.detail,
                    timestamp = detection.timestamp
                )
            }
        }
    }

    private fun ThreatLevel.toAlertSeverity(): AlertSeverity = when (this) {
        ThreatLevel.HIGH -> AlertSeverity.HIGH
        ThreatLevel.MEDIUM -> AlertSeverity.MEDIUM
        ThreatLevel.LOW -> AlertSeverity.LOW
    }

    /**
     * Content-based identity for dedup. WIFI/BLE/NETWORK detail strings are
     * stable across rescans of the same device, but MAGNETIC's detail embeds a
     * continuously-changing sensor reading - key it by source alone so a single
     * ongoing anomaly doesn't get re-reported on every sensor sample.
     */
    private fun CameraDetection.dedupeKey(): String =
        if (source == DetectionSource.MAGNETIC) source.name else "$source:$title:$detail"

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }

    init {
        observeDemoRequests(demoBus, DemoData.Routes.HIDDEN_CAMERA) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        stopScan()
        val demo = DemoData.cameraDetections
        _state.value = _state.value.copy(
            detections = demo,
            wifiSuspects = demo.count { it.source == DetectionSource.WIFI },
            bleSuspects = demo.count { it.source == DetectionSource.BLE },
            magneticAnomaly = demo.any { it.source == DetectionSource.MAGNETIC },
            networkSuspects = demo.count { it.source == DetectionSource.NETWORK },
            error = null
        )
    }
}

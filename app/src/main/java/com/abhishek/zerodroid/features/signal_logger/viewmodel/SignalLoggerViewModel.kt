package com.abhishek.zerodroid.features.signal_logger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.signal_logger.domain.SignalLogEntry
import com.abhishek.zerodroid.features.signal_logger.domain.SignalLoggerState
import com.abhishek.zerodroid.features.signal_logger.domain.SignalStats
import com.abhishek.zerodroid.features.signal_logger.domain.SignalType
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SignalLoggerViewModel(
    private val wifiScanner: WifiScanner,
    private val bleScanner: BleScanner
) : ViewModel() {

    private val _state = MutableStateFlow(SignalLoggerState())
    val state: StateFlow<SignalLoggerState> = _state.asStateFlow()

    private var wifiJob: Job? = null
    private var bleJob: Job? = null
    private var timerJob: Job? = null

    private val stats = SignalStats()
    private val allEntries = mutableListOf<SignalLogEntry>()
    private var loggingStartTime = 0L

    fun startLogging() {
        if (_state.value.isLogging) return

        loggingStartTime = System.currentTimeMillis()
        _state.value = _state.value.copy(isLogging = true, error = null)

        startDurationTimer()
        startWifiCollection()
        startBleCollection()
    }

    fun stopLogging() {
        wifiJob?.cancel()
        bleJob?.cancel()
        timerJob?.cancel()
        wifiJob = null
        bleJob = null
        timerJob = null
        _state.value = _state.value.copy(isLogging = false)
    }

    fun clearLog() {
        stopLogging()
        allEntries.clear()
        stats.uniqueWifiAps.clear()
        stats.uniqueBleDevices.clear()
        stats.previousWifiAps.clear()
        stats.previousBleDevices.clear()
        stats.previousWifiRssi.clear()
        stats.previousBleRssi.clear()
        _state.value = SignalLoggerState()
    }

    fun exportLog(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val header = "Timestamp | Type | Source | Address | RSSI | Detail"
        val separator = "-".repeat(80)
        val lines = allEntries.joinToString("\n") { entry ->
            val ts = sdf.format(Date(entry.timestamp))
            val rssiStr = entry.rssi?.let { "${it}dBm" } ?: "N/A"
            "$ts | ${entry.type} | ${entry.source} | ${entry.address} | $rssiStr | ${entry.detail}"
        }
        return "$header\n$separator\n$lines"
    }

    private fun startDurationTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val elapsed = System.currentTimeMillis() - loggingStartTime
                val epm = if (elapsed > 0) {
                    allEntries.size.toFloat() / (elapsed / 60_000f)
                } else 0f
                _state.value = _state.value.copy(
                    loggingDurationMs = elapsed,
                    entriesPerMinute = epm
                )
            }
        }
    }

    private fun startWifiCollection() {
        wifiJob = viewModelScope.launch {
            wifiScanner.scan()
                .catch { e ->
                    _state.value = _state.value.copy(
                        error = "WiFi scan error: ${e.message}"
                    )
                }
                .collect { accessPoints ->
                    val currentBssids = accessPoints.map { it.bssid }.toSet()
                    val newEntries = mutableListOf<SignalLogEntry>()

                    // Detect new WiFi APs
                    val newAps = currentBssids - stats.previousWifiAps
                    val lostAps = stats.previousWifiAps - currentBssids

                    // Log all current APs
                    accessPoints.forEach { ap ->
                        val isNew = ap.bssid in newAps && stats.previousWifiAps.isNotEmpty()
                        val type = if (isNew) SignalType.WIFI_NEW else SignalType.WIFI_AP
                        val ssidLabel = ap.ssid.ifBlank { "<Hidden>" }

                        newEntries.add(
                            SignalLogEntry(
                                type = type,
                                source = ssidLabel,
                                address = ap.bssid,
                                rssi = ap.rssi,
                                detail = "Ch${ap.channel} ${ap.band} ${ap.security}",
                                isAnomaly = isNew && stats.previousWifiAps.isNotEmpty()
                            )
                        )

                        // Anomaly: Signal spike > 20dBm
                        stats.previousWifiRssi[ap.bssid]?.let { prevRssi ->
                            val delta = kotlin.math.abs(ap.rssi - prevRssi)
                            if (delta > 20) {
                                newEntries.add(
                                    SignalLogEntry(
                                        type = SignalType.ANOMALY,
                                        source = ssidLabel,
                                        address = ap.bssid,
                                        rssi = ap.rssi,
                                        detail = "Signal spike: ${prevRssi}dBm -> ${ap.rssi}dBm (delta: ${delta}dB)",
                                        isAnomaly = true
                                    )
                                )
                            }
                        }

                        // Anomaly: Hidden AP with strong signal
                        if (ap.ssid.isBlank() && ap.rssi > -50) {
                            newEntries.add(
                                SignalLogEntry(
                                    type = SignalType.ANOMALY,
                                    source = "<Hidden>",
                                    address = ap.bssid,
                                    rssi = ap.rssi,
                                    detail = "Hidden AP with strong signal (${ap.rssi}dBm)",
                                    isAnomaly = true
                                )
                            )
                        }

                        stats.previousWifiRssi[ap.bssid] = ap.rssi
                    }

                    // Log lost APs
                    if (stats.previousWifiAps.isNotEmpty()) {
                        lostAps.forEach { bssid ->
                            newEntries.add(
                                SignalLogEntry(
                                    type = SignalType.WIFI_LOST,
                                    source = "WiFi AP",
                                    address = bssid,
                                    detail = "AP disappeared from scan"
                                )
                            )
                        }
                    }

                    // Anomaly: Burst of new devices (>5 new in single cycle)
                    if (newAps.size > 5 && stats.previousWifiAps.isNotEmpty()) {
                        newEntries.add(
                            SignalLogEntry(
                                type = SignalType.ANOMALY,
                                source = "WiFi Burst",
                                address = "--",
                                detail = "${newAps.size} new APs in single scan (possible spoofing)",
                                isAnomaly = true
                            )
                        )
                    }

                    // Update tracking state
                    stats.uniqueWifiAps.addAll(currentBssids)
                    stats.previousWifiAps.clear()
                    stats.previousWifiAps.addAll(currentBssids)

                    addEntries(newEntries, wifiApCount = accessPoints.size, newWifi = newAps.size, lostWifi = lostAps.size)
                }
        }
    }

    private fun startBleCollection() {
        bleJob = viewModelScope.launch {
            bleScanner.scan()
                .catch { e ->
                    _state.value = _state.value.copy(
                        error = "BLE scan error: ${e.message}"
                    )
                }
                .collect { devices ->
                    val currentAddresses = devices.map { it.address }.toSet()
                    val newEntries = mutableListOf<SignalLogEntry>()

                    val newDevices = currentAddresses - stats.previousBleDevices
                    val lostDevices = stats.previousBleDevices - currentAddresses

                    // Log all current BLE devices
                    devices.forEach { device ->
                        val isNew = device.address in newDevices && stats.previousBleDevices.isNotEmpty()
                        val type = if (isNew) SignalType.BLE_NEW else SignalType.BLE_DEVICE

                        val uuidInfo = if (device.serviceUuids.isNotEmpty()) {
                            "UUIDs: ${device.serviceUuids.size}"
                        } else "No service UUIDs"

                        newEntries.add(
                            SignalLogEntry(
                                type = type,
                                source = device.displayName,
                                address = device.address,
                                rssi = device.rssi,
                                detail = uuidInfo,
                                isAnomaly = isNew && stats.previousBleDevices.isNotEmpty()
                            )
                        )

                        // Anomaly: Signal spike > 20dBm
                        stats.previousBleRssi[device.address]?.let { prevRssi ->
                            val delta = kotlin.math.abs(device.rssi - prevRssi)
                            if (delta > 20) {
                                newEntries.add(
                                    SignalLogEntry(
                                        type = SignalType.ANOMALY,
                                        source = device.displayName,
                                        address = device.address,
                                        rssi = device.rssi,
                                        detail = "Signal spike: ${prevRssi}dBm -> ${device.rssi}dBm (delta: ${delta}dB)",
                                        isAnomaly = true
                                    )
                                )
                            }
                        }

                        stats.previousBleRssi[device.address] = device.rssi
                    }

                    // Log lost BLE devices
                    if (stats.previousBleDevices.isNotEmpty()) {
                        lostDevices.forEach { address ->
                            newEntries.add(
                                SignalLogEntry(
                                    type = SignalType.BLE_LOST,
                                    source = "BLE Device",
                                    address = address,
                                    detail = "Device disappeared from scan"
                                )
                            )
                        }
                    }

                    // Anomaly: Burst of new BLE devices (>5 new in single cycle)
                    if (newDevices.size > 5 && stats.previousBleDevices.isNotEmpty()) {
                        newEntries.add(
                            SignalLogEntry(
                                type = SignalType.ANOMALY,
                                source = "BLE Burst",
                                address = "--",
                                detail = "${newDevices.size} new BLE devices in single scan (possible spoofing)",
                                isAnomaly = true
                            )
                        )
                    }

                    // Update tracking state
                    stats.uniqueBleDevices.addAll(currentAddresses)
                    stats.previousBleDevices.clear()
                    stats.previousBleDevices.addAll(currentAddresses)

                    addEntries(newEntries, bleDeviceCount = devices.size, newBle = newDevices.size, lostBle = lostDevices.size)
                }
        }
    }

    @Synchronized
    private fun addEntries(
        newEntries: List<SignalLogEntry>,
        wifiApCount: Int? = null,
        bleDeviceCount: Int? = null,
        newWifi: Int = 0,
        lostWifi: Int = 0,
        newBle: Int = 0,
        lostBle: Int = 0
    ) {
        allEntries.addAll(newEntries)

        // Keep rolling buffer of last 500
        if (allEntries.size > MAX_ENTRIES) {
            val excess = allEntries.size - MAX_ENTRIES
            allEntries.subList(0, excess).clear()
        }

        val anomalyDelta = newEntries.count { it.isAnomaly }
        val newDevicesDelta = newWifi + newBle
        val lostDevicesDelta = lostWifi + lostBle
        val elapsed = System.currentTimeMillis() - loggingStartTime
        val epm = if (elapsed > 0) allEntries.size.toFloat() / (elapsed / 60_000f) else 0f

        _state.value = _state.value.copy(
            entries = allEntries.toList(),
            totalEntries = allEntries.size,
            wifiApCount = wifiApCount ?: _state.value.wifiApCount,
            bleDeviceCount = bleDeviceCount ?: _state.value.bleDeviceCount,
            newDevicesCount = _state.value.newDevicesCount + newDevicesDelta,
            lostDevicesCount = _state.value.lostDevicesCount + lostDevicesDelta,
            anomalyCount = _state.value.anomalyCount + anomalyDelta,
            entriesPerMinute = epm
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopLogging()
    }

    companion object {
        private const val MAX_ENTRIES = 500

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                val c = app.container
                return SignalLoggerViewModel(c.wifiScanner, c.bleScanner) as T
            }
        }
    }
}

package com.abhishek.zerodroid.features.privacy_score.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.privacy_score.domain.CheckStatus
import com.abhishek.zerodroid.features.privacy_score.domain.PrivacyCheck
import com.abhishek.zerodroid.features.privacy_score.domain.PrivacyScoreCalculator
import com.abhishek.zerodroid.features.privacy_score.domain.PrivacyScoreState
import com.abhishek.zerodroid.features.sensors.domain.SensorDataCollector
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class PrivacyScoreViewModel(
    private val wifiScanner: WifiScanner,
    private val bleScanner: BleScanner,
    private val sensorDataCollector: SensorDataCollector,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(PrivacyScoreState())
    val state: StateFlow<PrivacyScoreState> = _state.asStateFlow()

    private var scanJob: Job? = null

    private val wifiManager: WifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun startScan() {
        if (scanJob?.isActive == true) return
        _state.value = PrivacyScoreState(isScanning = true)

        scanJob = viewModelScope.launch {
            try {
                val allChecks = mutableListOf<PrivacyCheck>()

                // ── Phase 1: WiFi Checks ────────────────────────────────
                val wifiAccessPoints = collectWifiAccessPoints()
                allChecks += PrivacyScoreCalculator.checkWifiEncryption(wifiManager, wifiAccessPoints)
                updateState(allChecks)

                allChecks += PrivacyScoreCalculator.checkEvilTwins(wifiAccessPoints)
                updateState(allChecks)

                allChecks += PrivacyScoreCalculator.checkOpenNetworks(wifiAccessPoints)
                updateState(allChecks)

                allChecks += PrivacyScoreCalculator.checkSsidPrivacy(wifiManager, wifiAccessPoints)
                updateState(allChecks)

                // ── Phase 2: BLE Checks ─────────────────────────────────
                val bleDevices = collectBleDevices()
                allChecks += PrivacyScoreCalculator.checkBleTrackers(bleDevices)
                updateState(allChecks)

                allChecks += PrivacyScoreCalculator.checkBluetoothDiscoverable(context)
                updateState(allChecks)

                allChecks += PrivacyScoreCalculator.checkBleDeviceCount(bleDevices)
                updateState(allChecks)

                // ── Phase 3: Device Checks ──────────────────────────────
                allChecks += PrivacyScoreCalculator.checkDeveloperOptions(context)
                updateState(allChecks)

                allChecks += PrivacyScoreCalculator.checkUsbDebugging(context)
                updateState(allChecks)

                allChecks += PrivacyScoreCalculator.checkMockLocations(context)
                updateState(allChecks)

                allChecks += PrivacyScoreCalculator.checkScreenLock(context)
                updateState(allChecks)

                allChecks += PrivacyScoreCalculator.checkDeviceEncryption(context)
                updateState(allChecks)

                allChecks += PrivacyScoreCalculator.checkSecurityPatch()
                updateState(allChecks)

                // ── Phase 4: Network Checks ─────────────────────────────
                allChecks += PrivacyScoreCalculator.checkPrivateDns(context)
                updateState(allChecks)

                allChecks += PrivacyScoreCalculator.checkVpnActive(context)
                updateState(allChecks)

                // ── Phase 5: Physical Checks ────────────────────────────
                sensorDataCollector.start()
                delay(500) // Let sensors warm up

                val magReading = sensorDataCollector.magnetometer.value
                allChecks += PrivacyScoreCalculator.checkMagneticAnomaly(magReading)
                updateState(allChecks)

                val hasAudioPerm = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                allChecks += PrivacyScoreCalculator.checkUltrasonicBeacons(hasAudioPerm)
                updateState(allChecks)

                sensorDataCollector.stop()

                // ── Finalize ────────────────────────────────────────────
                val score = PrivacyScoreCalculator.calculateScore(allChecks)
                val grade = PrivacyScoreCalculator.scoreToGrade(score)
                val categoryScores = PrivacyScoreCalculator.calculateCategoryScores(allChecks)

                _state.value = PrivacyScoreState(
                    isScanning = false,
                    score = score,
                    grade = grade,
                    checks = allChecks,
                    passCount = allChecks.count { it.status == CheckStatus.PASS },
                    warnCount = allChecks.count { it.status == CheckStatus.WARNING },
                    failCount = allChecks.count { it.status == CheckStatus.FAIL },
                    categoryScores = categoryScores,
                    lastScanTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isScanning = false,
                    error = e.message ?: "Scan failed unexpectedly"
                )
            }
        }
    }

    fun rescan() {
        scanJob?.cancel()
        scanJob = null
        _state.value = PrivacyScoreState()
        startScan()
    }

    private fun updateState(checks: List<PrivacyCheck>) {
        val score = PrivacyScoreCalculator.calculateScore(checks)
        _state.value = _state.value.copy(
            score = score,
            grade = PrivacyScoreCalculator.scoreToGrade(score),
            checks = checks.toList(),
            passCount = checks.count { it.status == CheckStatus.PASS },
            warnCount = checks.count { it.status == CheckStatus.WARNING },
            failCount = checks.count { it.status == CheckStatus.FAIL },
            categoryScores = PrivacyScoreCalculator.calculateCategoryScores(checks)
        )
    }

    private suspend fun collectWifiAccessPoints(): List<WifiAccessPoint> {
        return try {
            withTimeoutOrNull(5000L) {
                wifiScanner.scan()
                    .catch { emit(emptyList()) }
                    .first { it.isNotEmpty() }
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun collectBleDevices(): List<BleDevice> {
        if (!bleScanner.isAvailable) return emptyList()
        return try {
            val devices = mutableListOf<BleDevice>()
            withTimeoutOrNull(5000L) {
                bleScanner.scan()
                    .catch { emit(emptyList()) }
                    .collect { scanned ->
                        devices.clear()
                        devices.addAll(scanned)
                        if (devices.size > 5) return@collect  // Enough data to analyze
                    }
            }
            devices
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        sensorDataCollector.stop()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                val c = app.container
                return PrivacyScoreViewModel(
                    c.wifiScanner,
                    c.bleScanner,
                    c.sensorDataCollector,
                    app.applicationContext
                ) as T
            }
        }
    }
}

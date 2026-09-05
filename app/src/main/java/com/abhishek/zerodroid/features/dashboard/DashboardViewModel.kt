package com.abhishek.zerodroid.features.dashboard

import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import com.abhishek.zerodroid.core.alerts.UnifiedAlert
import com.abhishek.zerodroid.core.di.DashboardPrefs
import com.abhishek.zerodroid.core.hardware.HardwareChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DeviceInfo(
    val model: String,
    val androidVersion: String,
    val device: String,
    val board: String
) {
    companion object {
        /**
         * Reads the running device. Build fields are platform types that are null on the JVM,
         * so every read is null-safe; the ViewModel takes a [DeviceInfo] instead of calling
         * this directly so tests can supply a fixed one.
         */
        fun fromBuild(): DeviceInfo = DeviceInfo(
            model = "${Build.MANUFACTURER?.uppercase().orEmpty()} ${Build.MODEL.orEmpty()}".trim(),
            androidVersion = "${Build.VERSION.RELEASE.orEmpty()} (API ${Build.VERSION.SDK_INT})",
            device = Build.DEVICE.orEmpty(),
            board = Build.BOARD.orEmpty()
        )
    }
}

data class HardwareItem(
    val name: String,
    val isAvailable: Boolean
)

data class LastUsedFeature(
    val route: String,
    val title: String
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val hardwareChecker: HardwareChecker,
    @DashboardPrefs private val prefs: SharedPreferences,
    alertCenterRepository: AlertCenterRepository,
    val deviceInfo: DeviceInfo
) : ViewModel() {

    private val _hardwareItems = MutableStateFlow<List<HardwareItem>>(emptyList())
    val hardwareItems: StateFlow<List<HardwareItem>> = _hardwareItems.asStateFlow()

    private val _lastUsedFeature = MutableStateFlow<LastUsedFeature?>(null)
    val lastUsedFeature: StateFlow<LastUsedFeature?> = _lastUsedFeature.asStateFlow()

    val recentAlerts: StateFlow<List<UnifiedAlert>> = alertCenterRepository.alerts
        .map { it.take(RECENT_ALERTS_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalAlertCount: StateFlow<Int> = alertCenterRepository.alerts
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        _hardwareItems.value = listOf(
            HardwareItem("WiFi", hardwareChecker.hasWifi()),
            HardwareItem("Bluetooth", hardwareChecker.hasBluetooth()),
            HardwareItem("BLE", hardwareChecker.hasBluetoothLe()),
            HardwareItem("NFC", hardwareChecker.hasNfc()),
            HardwareItem("IR", hardwareChecker.hasIr()),
            HardwareItem("Camera", hardwareChecker.hasCamera()),
            HardwareItem("GPS", hardwareChecker.hasGps()),
            HardwareItem("USB Host", hardwareChecker.hasUsbHost()),
            HardwareItem("UWB", hardwareChecker.hasUwb()),
            HardwareItem("Wi-Fi Aware", hardwareChecker.hasWifiAware()),
            HardwareItem("Wi-Fi Direct", hardwareChecker.hasWifiDirect()),
            HardwareItem("Telephony", hardwareChecker.hasTelephony()),
            HardwareItem("Gyroscope", hardwareChecker.hasGyroscope()),
            HardwareItem("Barometer", hardwareChecker.hasBarometer())
        )

        val route = prefs.getString(KEY_LAST_ROUTE, null)
        val title = prefs.getString(KEY_LAST_TITLE, null)
        if (route != null && title != null) {
            _lastUsedFeature.value = LastUsedFeature(route, title)
        }
    }

    fun saveLastUsed(route: String, title: String) {
        prefs.edit {
            putString(KEY_LAST_ROUTE, route)
            putString(KEY_LAST_TITLE, title)
        }
        _lastUsedFeature.value = LastUsedFeature(route, title)
    }

    companion object {
        private const val KEY_LAST_ROUTE = "last_used_route"
        private const val KEY_LAST_TITLE = "last_used_title"
        private const val RECENT_ALERTS_LIMIT = 3
    }
}

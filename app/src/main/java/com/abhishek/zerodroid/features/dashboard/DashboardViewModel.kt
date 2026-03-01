package com.abhishek.zerodroid.features.dashboard

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.core.hardware.HardwareChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceInfo(
    val model: String = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
    val androidVersion: String = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val device: String = Build.DEVICE,
    val board: String = Build.BOARD
)

data class HardwareItem(
    val name: String,
    val isAvailable: Boolean
)

data class LastUsedFeature(
    val route: String,
    val title: String
)

class DashboardViewModel(
    private val hardwareChecker: HardwareChecker,
    private val prefs: SharedPreferences
) : ViewModel() {

    val deviceInfo = DeviceInfo()

    private val _hardwareItems = MutableStateFlow<List<HardwareItem>>(emptyList())
    val hardwareItems: StateFlow<List<HardwareItem>> = _hardwareItems.asStateFlow()

    private val _lastUsedFeature = MutableStateFlow<LastUsedFeature?>(null)
    val lastUsedFeature: StateFlow<LastUsedFeature?> = _lastUsedFeature.asStateFlow()

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
        prefs.edit()
            .putString(KEY_LAST_ROUTE, route)
            .putString(KEY_LAST_TITLE, title)
            .apply()
        _lastUsedFeature.value = LastUsedFeature(route, title)
    }

    companion object {
        private const val PREFS_NAME = "zerodroid_dashboard"
        private const val KEY_LAST_ROUTE = "last_used_route"
        private const val KEY_LAST_TITLE = "last_used_title"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                return DashboardViewModel(app.container.hardwareChecker, prefs) as T
            }
        }
    }
}

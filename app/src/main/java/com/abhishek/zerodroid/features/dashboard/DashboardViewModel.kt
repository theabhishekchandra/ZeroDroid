package com.abhishek.zerodroid.features.dashboard

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.alerts.UnifiedAlert
import com.abhishek.zerodroid.core.hardware.HardwareChecker
import com.abhishek.zerodroid.core.prefs.LastUsedFeature
import com.abhishek.zerodroid.core.prefs.ToolPreferences
import com.abhishek.zerodroid.navigation.ToolCatalog
import com.abhishek.zerodroid.navigation.ToolInfo
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

/** Counts behind the Home status card. */
data class AlertSummary(
    val total: Int = 0,
    val critical: Int = 0,
    val high: Int = 0,
    val medium: Int = 0,
    val low: Int = 0,
    val latestTimestamp: Long? = null
) {
    val worst: AlertSeverity?
        get() = when {
            critical > 0 -> AlertSeverity.CRITICAL
            high > 0 -> AlertSeverity.HIGH
            medium > 0 -> AlertSeverity.MEDIUM
            low > 0 -> AlertSeverity.LOW
            else -> null
        }

    /** "1 high · 2 medium", most severe first, skipping empty levels. */
    val breakdown: String
        get() = listOf(critical to "critical", high to "high", medium to "medium", low to "low")
            .filter { it.first > 0 }
            .joinToString(" · ") { "${it.first} ${it.second}" }

    companion object {
        fun from(alerts: List<UnifiedAlert>) = AlertSummary(
            total = alerts.size,
            critical = alerts.count { it.severity == AlertSeverity.CRITICAL },
            high = alerts.count { it.severity == AlertSeverity.HIGH },
            medium = alerts.count { it.severity == AlertSeverity.MEDIUM },
            low = alerts.count { it.severity == AlertSeverity.LOW },
            latestTimestamp = alerts.maxOfOrNull { it.timestamp }
        )
    }
}

/** How many catalog tools this phone can run, and what hardware it lacks. */
data class ToolSupport(
    val supported: Int,
    val total: Int,
    val missing: List<String>
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val hardwareChecker: HardwareChecker,
    private val toolPreferences: ToolPreferences,
    alertCenterRepository: AlertCenterRepository,
    val deviceInfo: DeviceInfo
) : ViewModel() {

    private val _hardwareItems = MutableStateFlow<List<HardwareItem>>(emptyList())
    val hardwareItems: StateFlow<List<HardwareItem>> = _hardwareItems.asStateFlow()

    val lastUsedFeature: StateFlow<LastUsedFeature?> = toolPreferences.lastUsed

    val pinnedTools: StateFlow<List<ToolInfo>> = toolPreferences.pinnedRoutes
        .map { routes -> routes.mapNotNull { ToolCatalog.forRoute(it) } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            toolPreferences.pinnedRoutes.value.mapNotNull { ToolCatalog.forRoute(it) }
        )

    val recentAlerts: StateFlow<List<UnifiedAlert>> = alertCenterRepository.alerts
        .map { it.take(RECENT_ALERTS_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalAlertCount: StateFlow<Int> = alertCenterRepository.alerts
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val alertSummary: StateFlow<AlertSummary> = alertCenterRepository.alerts
        .map { alerts -> AlertSummary.from(alerts.filter { it.isOpen }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertSummary())

    val toolSupport: ToolSupport

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

        val requirements = ToolCatalog.tools.map { it.requirement }
        val unsupported = requirements.filterNot { it.isAvailable(hardwareChecker) }
        val missing = unsupported.mapNotNull { it.missingLabel }.distinct() +
            if (hardwareChecker.hasBarometer()) emptyList() else listOf("no barometer")
        toolSupport = ToolSupport(
            supported = requirements.size - unsupported.size,
            total = requirements.size,
            missing = missing
        )
    }

    fun saveLastUsed(route: String, title: String) {
        toolPreferences.recordOpened(route, title)
    }

    fun togglePin(route: String): Boolean = toolPreferences.togglePin(route)

    companion object {
        private const val RECENT_ALERTS_LIMIT = 3
    }
}

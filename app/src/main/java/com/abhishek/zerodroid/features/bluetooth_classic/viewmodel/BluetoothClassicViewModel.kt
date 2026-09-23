package com.abhishek.zerodroid.features.bluetooth_classic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothClassicScanner
import com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothClassicState
import com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothUuidDatabase
import com.abhishek.zerodroid.features.bluetooth_classic.domain.ClassicBluetoothDevice
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SdpServiceDiscovery
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SdpServiceInfo
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SppConnectionManager
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SppState
import com.abhishek.zerodroid.core.sessions.ItemKind
import com.abhishek.zerodroid.core.sessions.SessionItem
import com.abhishek.zerodroid.core.sessions.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.debug.DemoData
import com.abhishek.zerodroid.core.debug.observeDemoRequests

/** The services (SDP records) of one device, as last read or queried. */
data class SdpUiState(
    val address: String? = null,
    val name: String? = null,
    val services: List<SdpServiceInfo> = emptyList(),
    val isQuerying: Boolean = false,
    /** True while showing what Android remembered from an earlier connection. */
    val isCached: Boolean = false,
    val error: String? = null
) {
    val hasSerialPort: Boolean get() = services.any { it.shortUuid == "0x1101" }
}

@HiltViewModel
class BluetoothClassicViewModel @Inject constructor(
    private val scanner: BluetoothClassicScanner,
    private val sppManager: SppConnectionManager,
    private val sdpDiscovery: SdpServiceDiscovery,
    private val sessions: SessionRepository,
    private val demoBus: DemoDataBus
) : ViewModel() {

    /** When the current run started; null when no run is in progress. */
    private var runStartedAt: Long? = null


    private val _state = MutableStateFlow(BluetoothClassicState())
    val state: StateFlow<BluetoothClassicState> = _state.asStateFlow()

    val sppState: StateFlow<SppState> = sppManager.sppState

    private var scanJob: Job? = null

    val isAvailable: Boolean get() = scanner.isAvailable

    init {
        loadPairedDevices()
    }

    private fun loadPairedDevices() {
        _state.value = _state.value.copy(pairedDevices = scanner.getPairedDevices())
    }

    fun toggleScan() {
        if (_state.value.isScanning) stopScan() else startScan()
    }

    fun startScan() {
        scanJob?.cancel()
        _state.value = _state.value.copy(isScanning = true, discoveredDevices = emptyList())
        runStartedAt = System.currentTimeMillis()
        scanJob = viewModelScope.launch {
            scanner.discover()
                .catch { e ->
                    _state.value = _state.value.copy(isScanning = false, error = e.message)
                }
                .collect { devices ->
                    _state.value = _state.value.copy(discoveredDevices = devices)
                }
        }
    }

    fun stopScan() {
        runStartedAt?.let { started ->
            runStartedAt = null
            recordSession(started)
        }
        scanJob?.cancel()
        scanner.cancelDiscovery()
        scanJob = null
        _state.value = _state.value.copy(isScanning = false)
    }

    private val _sdp = MutableStateFlow(SdpUiState())
    val sdp: StateFlow<SdpUiState> = _sdp.asStateFlow()
    private var sdpJob: Job? = null

    /** Shows the device's remembered services at once, and asks it directly if there are none. */
    fun openServices(device: ClassicBluetoothDevice) {
        stopScan()
        val demo = DemoData.classicServices[device.address]?.takeIf { demoShown }
            ?.map { BluetoothUuidDatabase.lookup("$it-0000-1000-8000-00805f9b34fb") }
        val cached = demo ?: runCatching { sdpDiscovery.getCachedServices(device.address) }.getOrDefault(emptyList())
        _sdp.value = SdpUiState(device.address, device.name, cached, isCached = cached.isNotEmpty())
        if (cached.isEmpty()) querySdp()
    }

    /** Live SDP query; classic devices must be in range and awake, so it gives up after a while. */
    fun querySdp() {
        val address = _sdp.value.address ?: return
        sdpJob?.cancel()
        _sdp.update { it.copy(isQuerying = true, error = null) }
        sdpJob = viewModelScope.launch {
            var failure: String? = null
            val result = withTimeoutOrNull(SDP_TIMEOUT_MS) {
                sdpDiscovery.discoverServices(address)
                    .catch { e -> failure = e.message ?: "Bluetooth refused the query" }
                    .firstOrNull()
            }
            _sdp.update {
                when {
                    failure != null -> it.copy(isQuerying = false, error = failure)
                    result == null -> it.copy(isQuerying = false, error = "No answer in ${SDP_TIMEOUT_MS / 1000} s. The device may be off, asleep or out of range.")
                    else -> it.copy(isQuerying = false, services = result, isCached = false)
                }
            }
        }
    }

    fun closeServices() {
        sdpJob?.cancel()
        _sdp.value = SdpUiState()
    }

    fun connectSpp(deviceAddress: String) {
        stopScan()
        viewModelScope.launch {
            sppManager.connect(deviceAddress)
        }
    }

    fun sendSpp(text: String) {
        viewModelScope.launch {
            sppManager.send(text)
        }
    }

    fun disconnectSpp() {
        sppManager.disconnect()
    }

    private fun recordSession(startedAt: Long) {
        val devices = _state.value.discoveredDevices
        sessions.recordInBackground(
            tool = "bluetooth_classic",
            title = "Bluetooth Classic discovery",
            startedAt = startedAt,
            items = devices.map { d ->
                SessionItem(key = d.address, label = d.displayName, kind = ItemKind.BT, rssi = d.rssi.takeIf { it != 0 }, detail = d.majorClass)
            },
            summary = "${devices.size} discoverable device${if (devices.size == 1) "" else "s"}"
        )
    }

    override fun onCleared() {
        sdpJob?.cancel()
        stopScan()
        sppManager.disconnect()
    }

    init {
        observeDemoRequests(demoBus, DemoData.Routes.BLUETOOTH_CLASSIC) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private var demoShown = false

    private fun loadDemoData() {
        stopScan()
        demoShown = true
        _state.value = _state.value.copy(
            discoveredDevices = DemoData.classicDevices,
            pairedDevices = DemoData.classicDevices.filter { it.isPaired },
            error = null
        )
    }

    companion object {
        const val SDP_TIMEOUT_MS = 15_000L
    }
}

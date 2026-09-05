package com.abhishek.zerodroid.features.uwb.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.features.uwb.domain.UwbRangingUpdate
import com.abhishek.zerodroid.features.uwb.domain.UwbRole
import com.abhishek.zerodroid.features.uwb.domain.UwbService
import com.abhishek.zerodroid.features.uwb.domain.UwbState
import dagger.hilt.android.lifecycle.HiltViewModel
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
class UwbViewModel @Inject constructor(
    private val uwbService: UwbService,
    private val demoBus: DemoDataBus
) : ViewModel() {

    private val _state = MutableStateFlow(
        UwbState(
            isHardwareAvailable = uwbService.isAvailable,
            deviceInfo = uwbService.getDeviceInfo()
        )
    )
    val state: StateFlow<UwbState> = _state.asStateFlow()

    private var rangingJob: Job? = null

    fun updatePeerAddressInput(value: String) {
        _state.value = _state.value.copy(peerAddressInput = value)
    }

    fun updateSessionIdInput(value: String) {
        _state.value = _state.value.copy(sessionIdInput = value)
    }

    fun updateSessionKeyInput(value: String) {
        _state.value = _state.value.copy(sessionKeyInput = value)
    }

    fun updateChannelInput(value: String) {
        _state.value = _state.value.copy(channelInput = value)
    }

    fun updatePreambleInput(value: String) {
        _state.value = _state.value.copy(preambleInput = value)
    }

    fun startAsController() {
        val peerAddress = normalizeHex(_state.value.peerAddressInput)
        if (peerAddress.isEmpty()) {
            _state.value = _state.value.copy(error = "Enter the peer device's UWB address first")
            return
        }
        stopRanging()
        _state.value = _state.value.copy(
            role = UwbRole.CONTROLLER,
            isRanging = true,
            measurement = null,
            error = null,
            statusMessage = "Opening controller session..."
        )
        rangingJob = viewModelScope.launch {
            uwbService.startControllerRanging(peerAddress).collect(::applyUpdate)
        }
    }

    fun startAsControlee() {
        val current = _state.value
        val peerAddress = normalizeHex(current.peerAddressInput)
        val sessionId = current.sessionIdInput.toIntOrNull()
        val sessionKey = normalizeHex(current.sessionKeyInput)
        val channel = current.channelInput.toIntOrNull()
        val preamble = current.preambleInput.toIntOrNull()

        if (peerAddress.isEmpty() || sessionId == null || sessionKey.length != 16 ||
            channel == null || preamble == null
        ) {
            _state.value = current.copy(
                error = "Fill in all controller-provided fields (address, session ID, " +
                    "16-hex-char session key, channel, preamble) before starting"
            )
            return
        }

        stopRanging()
        _state.value = _state.value.copy(
            role = UwbRole.CONTROLEE,
            isRanging = true,
            measurement = null,
            error = null,
            statusMessage = "Opening controlee session..."
        )
        rangingJob = viewModelScope.launch {
            uwbService.startControleeRanging(
                peerAddressHex = peerAddress,
                sessionId = sessionId,
                sessionKeyHex = sessionKey,
                channel = channel,
                preambleIndex = preamble
            ).collect(::applyUpdate)
        }
    }

    fun stopRanging() {
        rangingJob?.cancel()
        rangingJob = null
        _state.value = _state.value.copy(
            role = UwbRole.NONE,
            isRanging = false,
            statusMessage = null
        )
    }

    private fun applyUpdate(update: UwbRangingUpdate) {
        _state.value = when (update) {
            is UwbRangingUpdate.SessionReady -> _state.value.copy(
                localSession = update.config,
                statusMessage = "Session ready - waiting for peer..."
            )
            is UwbRangingUpdate.Initialized -> _state.value.copy(
                statusMessage = "Ranging initialized - waiting for first position..."
            )
            is UwbRangingUpdate.Position -> _state.value.copy(
                measurement = update.measurement,
                statusMessage = "Ranging",
                error = null
            )
            is UwbRangingUpdate.PeerDisconnected -> _state.value.copy(
                isRanging = false,
                statusMessage = "Peer disconnected (reason ${update.reason})"
            )
            is UwbRangingUpdate.Failure -> _state.value.copy(
                isRanging = false,
                error = "Ranging failed (reason ${update.reason})"
            )
            is UwbRangingUpdate.Error -> _state.value.copy(
                isRanging = false,
                error = update.message
            )
        }
    }

    private fun normalizeHex(input: String): String =
        input.trim().replace(":", "").replace(" ", "").uppercase()

    override fun onCleared() {
        super.onCleared()
        stopRanging()
    }

    init {
        observeDemoRequests(demoBus, DemoData.Routes.UWB) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        rangingJob?.cancel()
        rangingJob = null
        _state.value = _state.value.copy(
            isHardwareAvailable = true,
            deviceInfo = DemoData.uwbDeviceInfo,
            role = UwbRole.CONTROLLER,
            isRanging = true,
            localSession = DemoData.uwbSession,
            measurement = DemoData.uwbMeasurement,
            statusMessage = "Demo ranging session",
            error = null
        )
    }
}

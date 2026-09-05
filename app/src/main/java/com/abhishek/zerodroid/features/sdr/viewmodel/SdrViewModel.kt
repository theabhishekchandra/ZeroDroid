package com.abhishek.zerodroid.features.sdr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.hardware.HardwareChecker
import com.abhishek.zerodroid.core.usb.UsbConnectionManager
import com.abhishek.zerodroid.core.usb.UsbConnectionResult
import com.abhishek.zerodroid.core.usb.UsbOpenConnection
import com.abhishek.zerodroid.features.sdr.domain.SdrDetector
import com.abhishek.zerodroid.features.sdr.domain.SdrDeviceInfo
import com.abhishek.zerodroid.features.sdr.domain.SdrState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.debug.DemoData
import com.abhishek.zerodroid.core.debug.observeDemoRequests

@HiltViewModel
class SdrViewModel @Inject constructor(
    private val sdrDetector: SdrDetector,
    private val usbConnectionManager: UsbConnectionManager,
    hardwareChecker: HardwareChecker,
    private val demoBus: DemoDataBus
) : ViewModel() {

    private val _state = MutableStateFlow(SdrState(hasUsbHost = hardwareChecker.hasUsbHost()))
    val state: StateFlow<SdrState> = _state.asStateFlow()

    private var openConnection: UsbOpenConnection? = null

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(devices = sdrDetector.detect())
    }

    fun connect(device: SdrDeviceInfo) {
        viewModelScope.launch {
            _state.value = _state.value.copy(connectingVidPid = device.vidPid, connectionError = null)

            val usbDevice = usbConnectionManager.findDevice(device.vendorId, device.productId)
            if (usbDevice == null) {
                _state.value = _state.value.copy(
                    connectingVidPid = null,
                    connectionError = "${device.chipset} is no longer connected"
                )
                return@launch
            }

            when (val result = usbConnectionManager.requestPermissionAndOpen(usbDevice)) {
                is UsbConnectionResult.Success -> {
                    openConnection?.close()
                    openConnection = result.connection
                    _state.value = _state.value.copy(
                        connectingVidPid = null,
                        connectedVidPid = device.vidPid,
                        connectionError = null
                    )
                }
                is UsbConnectionResult.Failure -> {
                    _state.value = _state.value.copy(
                        connectingVidPid = null,
                        connectionError = result.reason
                    )
                }
            }
        }
    }

    fun disconnect() {
        openConnection?.close()
        openConnection = null
        _state.value = _state.value.copy(connectedVidPid = null)
    }

    override fun onCleared() {
        openConnection?.close()
        openConnection = null
    }

    init {
        observeDemoRequests(demoBus, DemoData.Routes.SDR) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        _state.value = _state.value.copy(hasUsbHost = true, devices = DemoData.sdrDevices, connectionError = null)
    }
}

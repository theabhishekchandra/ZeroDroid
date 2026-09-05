package com.abhishek.zerodroid.features.usbcamera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.hardware.HardwareChecker
import com.abhishek.zerodroid.core.usb.UsbConnectionManager
import com.abhishek.zerodroid.core.usb.UsbConnectionResult
import com.abhishek.zerodroid.core.usb.UsbOpenConnection
import com.abhishek.zerodroid.features.usbcamera.domain.UsbCameraDetector
import com.abhishek.zerodroid.features.usbcamera.domain.UsbCameraState
import com.abhishek.zerodroid.features.usbcamera.domain.UsbVideoDevice
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
class UsbCameraViewModel @Inject constructor(
    private val detector: UsbCameraDetector,
    private val usbConnectionManager: UsbConnectionManager,
    hardwareChecker: HardwareChecker,
    private val demoBus: DemoDataBus
) : ViewModel() {

    private val _state = MutableStateFlow(UsbCameraState(hasUsbHost = hardwareChecker.hasUsbHost()))
    val state: StateFlow<UsbCameraState> = _state.asStateFlow()

    private var openConnection: UsbOpenConnection? = null

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(
            usbVideoDevices = detector.detectUsbVideoDevices(),
            camera2ExternalCameras = detector.detectCamera2External()
        )
    }

    fun connect(device: UsbVideoDevice) {
        viewModelScope.launch {
            _state.value = _state.value.copy(connectingVidPid = device.vidPid, connectionError = null)

            val usbDevice = usbConnectionManager.findDevice(device.vendorId, device.productId)
            if (usbDevice == null) {
                _state.value = _state.value.copy(
                    connectingVidPid = null,
                    connectionError = "${device.deviceName} is no longer connected"
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
        observeDemoRequests(demoBus, DemoData.Routes.USB_CAMERA) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        _state.value = _state.value.copy(
            hasUsbHost = true,
            usbVideoDevices = DemoData.usbVideoDevices,
            camera2ExternalCameras = DemoData.usbCameraInfos,
            error = null
        )
    }
}

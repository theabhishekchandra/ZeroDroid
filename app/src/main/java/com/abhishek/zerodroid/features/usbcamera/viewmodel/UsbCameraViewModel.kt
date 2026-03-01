package com.abhishek.zerodroid.features.usbcamera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.usbcamera.domain.UsbCameraDetector
import com.abhishek.zerodroid.features.usbcamera.domain.UsbCameraState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UsbCameraViewModel(
    private val detector: UsbCameraDetector,
    hasUsbHost: Boolean
) : ViewModel() {

    private val _state = MutableStateFlow(UsbCameraState(hasUsbHost = hasUsbHost))
    val state: StateFlow<UsbCameraState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(
            usbVideoDevices = detector.detectUsbVideoDevices(),
            camera2ExternalCameras = detector.detectCamera2External()
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return UsbCameraViewModel(
                    app.container.usbCameraDetector,
                    app.container.hardwareChecker.hasUsbHost()
                ) as T
            }
        }
    }
}

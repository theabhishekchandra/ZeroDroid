package com.abhishek.zerodroid.features.usb.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.usb.domain.UsbDeviceInfo
import com.abhishek.zerodroid.features.usb.domain.UsbDeviceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UsbUiState(
    val devices: List<UsbDeviceInfo> = emptyList(),
    val selectedDevice: UsbDeviceInfo? = null
)

class UsbViewModel(
    private val usbDeviceManager: UsbDeviceManager
) : ViewModel() {

    private val _state = MutableStateFlow(UsbUiState())
    val state: StateFlow<UsbUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            usbDeviceManager.observeDevices().collect { devices ->
                _state.value = _state.value.copy(devices = devices)
            }
        }
    }

    fun selectDevice(device: UsbDeviceInfo?) {
        _state.value = _state.value.copy(selectedDevice = device)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return UsbViewModel(app.container.usbDeviceManager) as T
            }
        }
    }
}

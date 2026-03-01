package com.abhishek.zerodroid.features.uwb.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.uwb.domain.UwbService
import com.abhishek.zerodroid.features.uwb.domain.UwbState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UwbViewModel(
    uwbService: UwbService
) : ViewModel() {

    private val _state = MutableStateFlow(
        UwbState(
            isHardwareAvailable = uwbService.isAvailable,
            deviceInfo = uwbService.getDeviceInfo()
        )
    )
    val state: StateFlow<UwbState> = _state.asStateFlow()

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return UwbViewModel(app.container.uwbService) as T
            }
        }
    }
}

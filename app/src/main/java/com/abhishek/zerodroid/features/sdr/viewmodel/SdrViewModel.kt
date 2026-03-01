package com.abhishek.zerodroid.features.sdr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.sdr.domain.SdrDetector
import com.abhishek.zerodroid.features.sdr.domain.SdrState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SdrViewModel(
    private val sdrDetector: SdrDetector,
    hasUsbHost: Boolean
) : ViewModel() {

    private val _state = MutableStateFlow(SdrState(hasUsbHost = hasUsbHost))
    val state: StateFlow<SdrState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(devices = sdrDetector.detect())
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return SdrViewModel(
                    app.container.sdrDetector,
                    app.container.hardwareChecker.hasUsbHost()
                ) as T
            }
        }
    }
}

package com.abhishek.zerodroid.features.ir.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.ir.domain.FlipperIrParser
import com.abhishek.zerodroid.features.ir.domain.IrProtocol
import com.abhishek.zerodroid.features.ir.domain.IrRemoteButton
import com.abhishek.zerodroid.features.ir.domain.IrRemoteProfile
import com.abhishek.zerodroid.features.ir.domain.IrRemoteState
import com.abhishek.zerodroid.features.ir.domain.IrScreenTab
import com.abhishek.zerodroid.features.ir.domain.IrSignal
import com.abhishek.zerodroid.features.ir.domain.IrTransmitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IrViewModel(
    private val irTransmitter: IrTransmitter
) : ViewModel() {

    private val _state = MutableStateFlow(IrRemoteState(isIrAvailable = irTransmitter.isAvailable))
    val state: StateFlow<IrRemoteState> = _state.asStateFlow()

    fun setActiveTab(tab: IrScreenTab) {
        _state.value = _state.value.copy(activeTab = tab)
    }

    fun setProtocol(protocol: IrProtocol) {
        _state.value = _state.value.copy(selectedProtocol = protocol, frequency = protocol.defaultFrequency)
    }

    fun setFrequency(freq: Int) { _state.value = _state.value.copy(frequency = freq) }
    fun setCode(code: String) { _state.value = _state.value.copy(code = code) }

    fun transmit() {
        val s = _state.value
        val signal = IrSignal(protocol = s.selectedProtocol, frequency = s.frequency, code = s.code)
        val result = irTransmitter.transmit(signal)
        _state.value = _state.value.copy(lastTransmitResult = result)
    }

    fun transmitSignal(signal: IrSignal) {
        val result = irTransmitter.transmit(signal)
        _state.value = _state.value.copy(lastTransmitResult = result)
    }

    fun importFlipperFile(content: String) {
        val signals = FlipperIrParser.parse(content)
        _state.value = _state.value.copy(importedSignals = signals)
    }

    fun selectProfile(profile: IrRemoteProfile) {
        _state.value = _state.value.copy(selectedProfile = profile)
    }

    fun transmitRemoteButton(button: IrRemoteButton) {
        val signal = IrSignal(
            protocol = button.protocol, frequency = button.frequency,
            code = button.code.toString(16).uppercase(), name = button.label
        )
        val result = irTransmitter.transmit(signal)
        _state.value = _state.value.copy(lastTransmitResult = result)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return IrViewModel(app.container.irTransmitter) as T
            }
        }
    }
}

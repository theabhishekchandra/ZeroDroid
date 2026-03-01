package com.abhishek.zerodroid.features.ble.viewmodel

import android.bluetooth.BluetoothGattCharacteristic
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.ble.domain.CharacteristicDetailState
import com.abhishek.zerodroid.features.ble.domain.CharacteristicValue
import com.abhishek.zerodroid.features.ble.domain.GattCharacteristicInfo
import com.abhishek.zerodroid.features.ble.domain.GattConnectionState
import com.abhishek.zerodroid.features.ble.domain.GattExplorer
import com.abhishek.zerodroid.features.ble.domain.GattOperationResult
import com.abhishek.zerodroid.features.ble.domain.GattValueParsers
import com.abhishek.zerodroid.features.ble.domain.WriteMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GattViewModel(
    private val explorer: GattExplorer
) : ViewModel() {

    val connectionState: StateFlow<GattConnectionState> = explorer.connectionState

    private val _detailState = MutableStateFlow(CharacteristicDetailState())
    val detailState: StateFlow<CharacteristicDetailState> = _detailState.asStateFlow()

    init {
        // Collect notification values and update detail state
        viewModelScope.launch {
            explorer.notificationFlow.collect { (charUuid, value) ->
                val current = _detailState.value
                if (current.info?.uuid?.equals(charUuid, ignoreCase = true) == true) {
                    val parsed = GattValueParsers.parse(charUuid, value.rawBytes)
                    _detailState.value = current.copy(
                        lastReadValue = value,
                        notificationValues = current.notificationValues + value,
                        parsedDisplay = parsed ?: current.parsedDisplay
                    )
                }
            }
        }
    }

    fun connect(address: String) {
        explorer.connect(address)
    }

    fun disconnect() {
        explorer.disconnect()
    }

    fun selectCharacteristic(info: GattCharacteristicInfo) {
        _detailState.value = CharacteristicDetailState(
            info = info,
            isNotifying = explorer.isNotifying(info.uuid)
        )
    }

    fun clearDetailState() {
        _detailState.value = CharacteristicDetailState()
    }

    fun readCharacteristic() {
        val info = _detailState.value.info ?: return
        _detailState.value = _detailState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = explorer.readCharacteristic(info.serviceUuid, info.uuid)
            when (result) {
                is GattOperationResult.Success -> {
                    val parsed = GattValueParsers.parse(info.uuid, result.value.rawBytes)
                    _detailState.value = _detailState.value.copy(
                        lastReadValue = result.value,
                        parsedDisplay = parsed,
                        isLoading = false,
                        error = null
                    )
                }
                is GattOperationResult.Error -> {
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is GattOperationResult.WriteSuccess -> {
                    _detailState.value = _detailState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun writeCharacteristic() {
        val info = _detailState.value.info ?: return
        val input = _detailState.value.writeInput.trim()
        if (input.isEmpty()) return

        val bytes = when (_detailState.value.writeMode) {
            WriteMode.Hex -> parseHexInput(input) ?: run {
                _detailState.value = _detailState.value.copy(error = "Invalid hex input")
                return
            }
            WriteMode.Text -> input.toByteArray(Charsets.UTF_8)
        }

        val writeType = if (info.isWritableWithResponse) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }

        _detailState.value = _detailState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = explorer.writeCharacteristic(info.serviceUuid, info.uuid, bytes, writeType)
            when (result) {
                is GattOperationResult.WriteSuccess -> {
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        writeInput = "",
                        error = null
                    )
                }
                is GattOperationResult.Error -> {
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is GattOperationResult.Success -> {
                    _detailState.value = _detailState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun toggleNotification() {
        val info = _detailState.value.info ?: return
        val currentlyNotifying = _detailState.value.isNotifying
        _detailState.value = _detailState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = explorer.toggleNotification(info.serviceUuid, info.uuid, !currentlyNotifying)
            when (result) {
                is GattOperationResult.WriteSuccess -> {
                    _detailState.value = _detailState.value.copy(
                        isNotifying = !currentlyNotifying,
                        isLoading = false,
                        notificationValues = if (currentlyNotifying) _detailState.value.notificationValues else emptyList()
                    )
                }
                is GattOperationResult.Error -> {
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {
                    _detailState.value = _detailState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun readDescriptor(descUuid: String) {
        val info = _detailState.value.info ?: return
        viewModelScope.launch {
            val result = explorer.readDescriptor(info.serviceUuid, info.uuid, descUuid)
            when (result) {
                is GattOperationResult.Success -> {
                    _detailState.value = _detailState.value.copy(
                        descriptorValues = _detailState.value.descriptorValues + (descUuid to result.value)
                    )
                }
                is GattOperationResult.Error -> {
                    _detailState.value = _detailState.value.copy(error = result.message)
                }
                else -> {}
            }
        }
    }

    fun updateWriteInput(input: String) {
        _detailState.value = _detailState.value.copy(writeInput = input)
    }

    fun toggleWriteMode() {
        val newMode = when (_detailState.value.writeMode) {
            WriteMode.Hex -> WriteMode.Text
            WriteMode.Text -> WriteMode.Hex
        }
        _detailState.value = _detailState.value.copy(writeMode = newMode)
    }

    override fun onCleared() {
        super.onCleared()
        explorer.close()
    }

    private fun parseHexInput(input: String): ByteArray? {
        return try {
            val cleaned = input.replace(Regex("[^0-9A-Fa-f]"), "")
            if (cleaned.length % 2 != 0) return null
            cleaned.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                val explorer = GattExplorer(
                    context = app.applicationContext,
                    bluetoothManager = app.container.bluetoothManager
                )
                return GattViewModel(explorer) as T
            }
        }
    }
}

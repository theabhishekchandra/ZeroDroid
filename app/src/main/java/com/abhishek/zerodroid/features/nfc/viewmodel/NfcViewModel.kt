package com.abhishek.zerodroid.features.nfc.viewmodel

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.nfc.data.NfcRepository
import com.abhishek.zerodroid.features.nfc.domain.NfcState
import com.abhishek.zerodroid.features.nfc.domain.NfcTagManager
import com.abhishek.zerodroid.features.nfc.domain.WriteResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NfcViewModel(
    private val nfcTagManager: NfcTagManager,
    private val repository: NfcRepository,
    private val nfcTagFlow: SharedFlow<Tag>,
    isNfcAvailable: Boolean,
    isNfcEnabled: Boolean
) : ViewModel() {

    private val _state = MutableStateFlow(
        NfcState(isNfcAvailable = isNfcAvailable, isNfcEnabled = isNfcEnabled)
    )
    val state: StateFlow<NfcState> = _state.asStateFlow()

    private var lastTag: Tag? = null

    init {
        viewModelScope.launch {
            repository.getTagHistory().collect { history ->
                _state.value = _state.value.copy(tagHistory = history)
            }
        }
        viewModelScope.launch {
            nfcTagFlow.collect { tag ->
                lastTag = tag
                val tagInfo = nfcTagManager.parseTag(tag)
                repository.saveTag(tagInfo)

                if (_state.value.writeMode) {
                    // Don't overwrite lastTag display in write mode, handled by write functions
                } else {
                    _state.value = _state.value.copy(lastTag = tagInfo)
                }
            }
        }
    }

    fun setWriteMode(enabled: Boolean) {
        _state.value = _state.value.copy(writeMode = enabled, writeResult = null)
    }

    fun writeText(text: String) {
        val tag = lastTag
        if (tag == null) {
            _state.value = _state.value.copy(writeResult = WriteResult.Error("No tag present. Tap a tag first."))
            return
        }
        val result = nfcTagManager.writeNdefText(tag, text)
        _state.value = _state.value.copy(writeResult = result)
    }

    fun writeUri(uri: String) {
        val tag = lastTag
        if (tag == null) {
            _state.value = _state.value.copy(writeResult = WriteResult.Error("No tag present. Tap a tag first."))
            return
        }
        val result = nfcTagManager.writeNdefUri(tag, uri)
        _state.value = _state.value.copy(writeResult = result)
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                val c = app.container
                return NfcViewModel(
                    nfcTagManager = c.nfcTagManager,
                    repository = c.nfcRepository,
                    nfcTagFlow = c.nfcTagFlow,
                    isNfcAvailable = c.nfcAdapter != null,
                    isNfcEnabled = c.nfcAdapter?.isEnabled == true
                ) as T
            }
        }
    }
}

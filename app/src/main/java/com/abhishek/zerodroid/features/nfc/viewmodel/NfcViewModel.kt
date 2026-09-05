package com.abhishek.zerodroid.features.nfc.viewmodel

import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.di.NfcTagBus
import com.abhishek.zerodroid.features.nfc.data.NfcRepository
import com.abhishek.zerodroid.features.nfc.domain.NfcState
import com.abhishek.zerodroid.features.nfc.domain.NfcTagManager
import com.abhishek.zerodroid.features.nfc.domain.WriteResult
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
class NfcViewModel @Inject constructor(
    private val nfcTagManager: NfcTagManager,
    private val repository: NfcRepository,
    private val nfcTagBus: NfcTagBus,
    private val nfcAdapter: NfcAdapter?,
    private val demoBus: DemoDataBus
) : ViewModel() {

    private val _state = MutableStateFlow(
        NfcState(isNfcAvailable = nfcAdapter != null, isNfcEnabled = nfcAdapter?.isEnabled == true)
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
            nfcTagBus.tagFlow.collect { tag ->
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

    init {
        observeDemoRequests(demoBus, DemoData.Routes.NFC) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        _state.value = _state.value.copy(
            isNfcAvailable = true,
            isNfcEnabled = true,
            lastTag = DemoData.nfcTag,
            tagHistory = listOf(DemoData.nfcTag) + _state.value.tagHistory
        )
    }
}

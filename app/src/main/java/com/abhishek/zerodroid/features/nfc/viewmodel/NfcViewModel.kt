package com.abhishek.zerodroid.features.nfc.viewmodel

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.di.NfcTagBus
import com.abhishek.zerodroid.features.nfc.data.NfcRepository
import com.abhishek.zerodroid.features.nfc.domain.MifareClassicReader
import com.abhishek.zerodroid.features.nfc.domain.NfcState
import com.abhishek.zerodroid.features.nfc.domain.NfcTab
import com.abhishek.zerodroid.features.nfc.service.ZeroDroidHceService
import com.abhishek.zerodroid.features.nfc.domain.NfcTagManager
import com.abhishek.zerodroid.features.nfc.domain.WriteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val mifareReader = MifareClassicReader()

    companion object {
        /** The HCE capability container advertises a 255-byte NDEF file, minus its 2-byte length. */
        const val MAX_EMULATED_NDEF_BYTES = 253
    }

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

                when (_state.value.tab) {
                    // Write mode keeps the last read tag on screen; the write functions use [lastTag].
                    NfcTab.WRITE -> Unit
                    NfcTab.MIFARE -> {
                        _state.value = _state.value.copy(lastTag = tagInfo)
                        readMifare(tag)
                    }
                    else -> _state.value = _state.value.copy(lastTag = tagInfo)
                }
            }
        }
    }

    fun setTab(tab: NfcTab) {
        _state.value = _state.value.copy(tab = tab, writeResult = null)
    }

    /** Reads every sector of a MIFARE Classic tag with default plus custom keys. */
    private fun readMifare(tag: Tag) {
        if (MifareClassic.get(tag) == null) {
            _state.value = _state.value.copy(mifareMessage = "This tag isn’t MIFARE Classic, or this phone’s NFC chip can’t read it.")
            return
        }
        _state.value = _state.value.copy(isReadingMifare = true, mifareMessage = null)
        viewModelScope.launch {
            val sectors = runCatching {
                withContext(Dispatchers.IO) { mifareReader.readAllSectors(tag, _state.value.customKeys) }
            }.getOrElse { e ->
                _state.value = _state.value.copy(isReadingMifare = false, mifareMessage = "Read failed: ${e.message}")
                return@launch
            }
            val opened = sectors.count { it.isAuthenticated }
            _state.value = _state.value.copy(
                isReadingMifare = false,
                mifareSectors = sectors,
                mifareMessage = "$opened of ${sectors.size} sectors opened with known keys"
            )
        }
    }

    fun addCustomKey(key: ByteArray) {
        if (key.size != 6) return
        _state.value = _state.value.copy(customKeys = _state.value.customKeys + key)
    }

    fun removeCustomKey(index: Int) {
        _state.value = _state.value.copy(customKeys = _state.value.customKeys.filterIndexed { i, _ -> i != index })
    }

    fun mifareDump(): String = mifareReader.formatDump(_state.value.mifareSectors)

    /** Writes one 16-byte block using the key that opened its sector. */
    fun writeMifareBlock(blockIndex: Int, data: ByteArray) {
        val tag = lastTag ?: run {
            _state.value = _state.value.copy(mifareMessage = "Hold the tag to the phone again, then retry.")
            return
        }
        val sector = _state.value.mifareSectors.firstOrNull { s -> s.blocks.any { it.blockIndex == blockIndex } }
        val key = sector?.keyUsed?.let { hex -> hex.chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray() }
        if (sector == null || key == null || key.size != 6) {
            _state.value = _state.value.copy(mifareMessage = "No key is known for that sector.")
            return
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { mifareReader.writeBlock(tag, blockIndex, data, key, useKeyB = sector.keyType == "B") }.getOrDefault(false)
            }
            _state.value = _state.value.copy(mifareMessage = if (ok) "Block $blockIndex written" else "Write to block $blockIndex failed")
            if (ok) readMifare(tag)
        }
    }

    /**
     * Sets what this phone serves as an NFC Type 4 tag. Returns false if it's too long for the
     * 255-byte NDEF file the emulation service advertises.
     */
    fun setEmulatedPayload(payload: String, isUrl: Boolean): Boolean {
        val record = if (isUrl) NdefRecord.createUri(payload) else NdefRecord.createTextRecord("en", payload)
        val bytes = NdefMessage(record).toByteArray()
        if (bytes.size > MAX_EMULATED_NDEF_BYTES) return false
        ZeroDroidHceService.ndefData = bytes
        _state.value = _state.value.copy(emulatedPayload = payload, emulatedIsUrl = isUrl)
        return true
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

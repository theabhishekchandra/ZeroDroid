package com.abhishek.zerodroid.features.camera.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.camera.data.QrRepository
import com.abhishek.zerodroid.features.camera.domain.QrContentParser
import com.abhishek.zerodroid.features.camera.domain.QrGenerator
import com.abhishek.zerodroid.features.camera.domain.QrScanResult
import com.abhishek.zerodroid.features.camera.domain.QrScannerState
import com.abhishek.zerodroid.features.camera.domain.QrScreenTab
import com.abhishek.zerodroid.features.camera.domain.QrThreatAnalyzer
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class QrGeneratorInputType(val displayName: String) {
    TEXT("Text"), URL("URL"), WIFI("WiFi")
}

data class QrGeneratorState(
    val inputType: QrGeneratorInputType = QrGeneratorInputType.TEXT,
    val textInput: String = "",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: QrGenerator.WifiSecurity = QrGenerator.WifiSecurity.WPA,
    val generatedBitmap: Bitmap? = null,
    val encodedContent: String = "",
    val errorMessage: String? = null
) {
    val canGenerate: Boolean
        get() = when (inputType) {
            QrGeneratorInputType.TEXT -> textInput.isNotBlank()
            QrGeneratorInputType.URL -> textInput.isNotBlank()
            QrGeneratorInputType.WIFI -> wifiSsid.isNotBlank()
        }
}

class QrScannerViewModel(
    private val repository: QrRepository
) : ViewModel() {

    private val _state = MutableStateFlow(QrScannerState())
    val state: StateFlow<QrScannerState> = _state.asStateFlow()

    private val _generatorState = MutableStateFlow(QrGeneratorState())
    val generatorState: StateFlow<QrGeneratorState> = _generatorState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRecentScans().collect { history ->
                _state.value = _state.value.copy(scanHistory = history)
            }
        }
    }

    fun onBarcodeDetected(rawValue: String, format: Int) {
        val lastScan = _state.value.lastScan
        if (lastScan != null && lastScan.rawValue == rawValue &&
            System.currentTimeMillis() - lastScan.timestamp < 3000) return

        val (contentType, parsedContent) = QrContentParser.parse(rawValue)
        val (isThreat, threatReason) = QrThreatAnalyzer.analyze(rawValue, contentType)

        val formatName = when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_CODABAR -> "Codabar"
            else -> "Unknown"
        }

        val result = QrScanResult(rawValue = rawValue, format = formatName,
            contentType = contentType, parsedContent = parsedContent,
            isThreat = isThreat, threatReason = threatReason)
        _state.value = _state.value.copy(lastScan = result)
        viewModelScope.launch { repository.saveScan(result) }
    }

    fun toggleHistory() {
        _state.value = _state.value.copy(showHistory = !_state.value.showHistory)
    }

    fun setActiveTab(tab: QrScreenTab) {
        _state.value = _state.value.copy(activeTab = tab)
    }

    fun setGeneratorInputType(type: QrGeneratorInputType) {
        _generatorState.value = _generatorState.value.copy(inputType = type, generatedBitmap = null, errorMessage = null)
    }

    fun setGeneratorText(text: String) {
        _generatorState.value = _generatorState.value.copy(textInput = text, errorMessage = null)
    }

    fun setWifiSsid(ssid: String) {
        _generatorState.value = _generatorState.value.copy(wifiSsid = ssid, errorMessage = null)
    }

    fun setWifiPassword(password: String) {
        _generatorState.value = _generatorState.value.copy(wifiPassword = password, errorMessage = null)
    }

    fun setWifiSecurity(security: QrGenerator.WifiSecurity) {
        _generatorState.value = _generatorState.value.copy(wifiSecurity = security, errorMessage = null)
    }

    fun generateQrCode() {
        val genState = _generatorState.value
        val content = when (genState.inputType) {
            QrGeneratorInputType.TEXT -> genState.textInput
            QrGeneratorInputType.URL -> {
                val url = genState.textInput.trim()
                if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            }
            QrGeneratorInputType.WIFI -> QrGenerator.formatWifiContent(
                ssid = genState.wifiSsid, password = genState.wifiPassword, security = genState.wifiSecurity
            )
        }

        val bitmap = QrGenerator.generate(
            content = content, size = 512,
            foregroundColor = android.graphics.Color.parseColor("#00FF41"),
            backgroundColor = android.graphics.Color.parseColor("#1A1A1A")
        )

        _generatorState.value = if (bitmap != null) {
            _generatorState.value.copy(generatedBitmap = bitmap, encodedContent = content, errorMessage = null)
        } else {
            _generatorState.value.copy(generatedBitmap = null, errorMessage = "Failed to generate QR code.")
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return QrScannerViewModel(app.container.qrRepository) as T
            }
        }
    }
}

package com.abhishek.zerodroid.features.ble.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.ble.domain.HciPacketType
import com.abhishek.zerodroid.features.ble.domain.HciSnoopLog
import com.abhishek.zerodroid.features.ble.domain.HciSnoopParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

data class HciSnoopState(
    val isLoading: Boolean = false,
    val log: HciSnoopLog? = null,
    val error: String? = null,
    val filter: HciPacketType? = null,
    val loadedFromPath: String? = null
)

class HciSnoopViewModel(
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(HciSnoopState())
    val state: StateFlow<HciSnoopState> = _state.asStateFlow()

    private val parser = HciSnoopParser()

    companion object {
        private val KNOWN_LOG_PATHS = listOf(
            "/data/misc/bluetooth/logs/btsnoop_hci.log",
            "/sdcard/btsnoop_hci.log",
            "/data/log/bt/btsnoop_hci.log",
            "/storage/emulated/0/btsnoop_hci.log"
        )

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return HciSnoopViewModel(app) as T
            }
        }
    }

    fun loadLog() {
        if (_state.value.isLoading) return

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            var lastError: String? = null

            for (path in KNOWN_LOG_PATHS) {
                try {
                    val file = File(path)
                    if (!file.exists()) {
                        lastError = "File not found: $path"
                        continue
                    }
                    if (!file.canRead()) {
                        lastError = "Permission denied: $path (may require root)"
                        continue
                    }

                    val fileSize = file.length()
                    val inputStream = BufferedInputStream(FileInputStream(file))
                    val log = inputStream.use { parser.parse(it, fileSize) }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            log = log,
                            error = null,
                            loadedFromPath = path
                        )
                    }
                    return@launch
                } catch (e: Exception) {
                    lastError = "$path: ${e.message}"
                }
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    error = "Could not load HCI snoop log from known paths.\n$lastError\n\n" +
                            "Use \"Select File\" to manually pick a btsnoop_hci.log file, " +
                            "or ensure Bluetooth HCI snoop log is enabled in Developer Options."
                )
            }
        }
    }

    fun loadFromUri(uri: Uri) {
        if (_state.value.isLoading) return

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = application.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Could not open file from URI")

                // Get approximate file size
                val fileSize = contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    it.length
                } ?: -1L

                val bufferedStream = BufferedInputStream(inputStream)
                val log = bufferedStream.use { parser.parse(it, fileSize) }

                _state.update {
                    it.copy(
                        isLoading = false,
                        log = log,
                        error = null,
                        loadedFromPath = uri.lastPathSegment ?: uri.toString()
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to parse file: ${e.message}"
                    )
                }
            }
        }
    }

    fun setFilter(filter: HciPacketType?) {
        _state.update { it.copy(filter = filter) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

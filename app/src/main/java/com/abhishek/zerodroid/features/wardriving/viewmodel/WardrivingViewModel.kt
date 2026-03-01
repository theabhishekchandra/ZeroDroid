package com.abhishek.zerodroid.features.wardriving.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.wardriving.data.WardrivingRepository
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingRecord
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingSession
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingState
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingStats
import com.abhishek.zerodroid.features.wardriving.service.WardrivingScanService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

class WardrivingViewModel(
    private val repository: WardrivingRepository,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(WardrivingState())
    val state: StateFlow<WardrivingState> = _state.asStateFlow()

    private var collectJob: Job? = null

    fun startSession() {
        val sessionId = UUID.randomUUID().toString()
        val session = WardrivingSession(id = sessionId, isActive = true)
        _state.value = _state.value.copy(session = session, isScanning = true, records = emptyList(), error = null)

        // Start foreground service
        val intent = Intent(appContext, WardrivingScanService::class.java)
        appContext.startForegroundService(intent)

        collectJob = viewModelScope.launch {
            repository.collect()
                .catch { e -> _state.value = _state.value.copy(error = e.message, isScanning = false) }
                .collect { records ->
                    repository.saveRecords(sessionId, records)
                    val uniqueCount = repository.getUniqueBssidCount(sessionId)
                    val allRecords = (_state.value.records + records).takeLast(200)
                    _state.value = _state.value.copy(
                        records = allRecords,
                        session = _state.value.session?.copy(
                            recordCount = _state.value.session!!.recordCount + records.size,
                            uniqueBssids = uniqueCount
                        ),
                        stats = computeStats(allRecords, session)
                    )
                }
        }
    }

    fun stopSession() {
        collectJob?.cancel()
        collectJob = null
        val intent = Intent(appContext, WardrivingScanService::class.java)
        appContext.stopService(intent)
        _state.value = _state.value.copy(
            isScanning = false,
            session = _state.value.session?.copy(isActive = false)
        )
    }

    fun exportCsv(onExported: (String) -> Unit) {
        val sessionId = _state.value.session?.id ?: return
        viewModelScope.launch {
            try {
                val csv = repository.exportSession(sessionId)
                _state.value = _state.value.copy(exportStatus = "Exported ${csv.lines().size - 2} records")
                onExported(csv)
            } catch (e: Exception) {
                _state.value = _state.value.copy(exportStatus = "Export failed: ${e.message}")
            }
        }
    }

    private fun computeStats(records: List<WardrivingRecord>, session: WardrivingSession): WardrivingStats {
        val uniqueSsids = records.mapNotNull { it.ssid }.distinct().size
        val uniqueBssids = records.map { it.bssid }.distinct().size
        val openCount = records.count { cap ->
            val capabilities = cap.capabilities ?: ""
            capabilities.isEmpty() || capabilities == "[ESS]"
        }
        val securedCount = records.size - openCount
        val durationMs = if (records.isNotEmpty()) {
            System.currentTimeMillis() - session.startTime
        } else 0L

        return WardrivingStats(
            totalRecords = records.size,
            uniqueSsids = uniqueSsids,
            uniqueBssids = uniqueBssids,
            openCount = openCount,
            securedCount = securedCount,
            sessionDurationMs = durationMs
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return WardrivingViewModel(app.container.wardrivingRepository, app) as T
            }
        }
    }
}

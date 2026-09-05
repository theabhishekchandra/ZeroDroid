package com.abhishek.zerodroid.features.wardriving.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.features.wardriving.data.WardrivingRepository
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingRecord
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingSession
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingSessionState
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingState
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingStats
import com.abhishek.zerodroid.features.wardriving.service.WardrivingScanService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.debug.DemoData
import com.abhishek.zerodroid.core.debug.observeDemoRequests
import kotlinx.coroutines.CancellationException

@HiltViewModel
class WardrivingViewModel @Inject constructor(
    private val repository: WardrivingRepository,
    private val sessionState: WardrivingSessionState,
    @ApplicationContext private val appContext: Context,
    private val demoBus: DemoDataBus
) : ViewModel() {

    private val _state = MutableStateFlow(WardrivingState())
    val state: StateFlow<WardrivingState> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        // A session started before this ViewModel existed (e.g. the user navigated away and
        // back) is still running in the service — pick up observing it rather than losing it.
        sessionState.active.value?.let { active ->
            resumeObserving(active.sessionId, active.startTime)
        }
    }

    fun startSession() {
        val sessionId = UUID.randomUUID().toString()
        val startTime = sessionState.start(sessionId)
        _state.value = WardrivingState(
            session = WardrivingSession(id = sessionId, startTime = startTime, isActive = true),
            isScanning = true
        )

        val intent = Intent(appContext, WardrivingScanService::class.java).apply {
            putExtra(WardrivingScanService.EXTRA_SESSION_ID, sessionId)
        }
        appContext.startForegroundService(intent)

        observeRecords(sessionId, startTime)
    }

    private fun resumeObserving(sessionId: String, startTime: Long) {
        _state.value = _state.value.copy(
            session = WardrivingSession(id = sessionId, startTime = startTime, isActive = true),
            isScanning = true
        )
        observeRecords(sessionId, startTime)
    }

    private fun observeRecords(sessionId: String, startTime: Long) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.getSessionRecords(sessionId).collect { records ->
                val ordered = records.sortedBy { it.timestamp }
                _state.value = _state.value.copy(
                    records = ordered,
                    session = _state.value.session?.copy(
                        recordCount = ordered.size,
                        uniqueBssids = ordered.map { it.bssid }.distinct().size
                    ),
                    stats = computeStats(ordered, startTime)
                )
            }
        }
    }

    fun stopSession() {
        observeJob?.cancel()
        observeJob = null
        sessionState.stop()
        appContext.stopService(Intent(appContext, WardrivingScanService::class.java))
        _state.value = _state.value.copy(
            isScanning = false,
            session = _state.value.session?.copy(isActive = false)
        )
    }

    fun exportCsv(onExported: (Uri) -> Unit) {
        val sessionId = _state.value.session?.id ?: return
        viewModelScope.launch {
            try {
                val csv = repository.exportSession(sessionId)

                // Large sessions (thousands of records) blow Android's ~1MB Binder transaction
                // limit if passed as Intent.EXTRA_TEXT, failing with a generic "Failure from
                // system" — write to a file and share it via FileProvider instead, which the
                // receiving app reads directly rather than the OS marshalling it through Binder.
                val exportsDir = File(appContext.cacheDir, "exports").apply { mkdirs() }
                val file = File(exportsDir, "wardriving_${sessionId.take(8)}.csv")
                file.writeText(csv)
                val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)

                _state.value = _state.value.copy(exportStatus = "Exported ${csv.lines().size - 2} records")
                onExported(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(exportStatus = "Export failed: ${e.message}")
            }
        }
    }

    private fun computeStats(records: List<WardrivingRecord>, startTime: Long): WardrivingStats {
        val uniqueSsids = records.mapNotNull { it.ssid }.distinct().size
        val uniqueBssids = records.map { it.bssid }.distinct().size
        val openCount = records.count { cap ->
            val capabilities = cap.capabilities ?: ""
            capabilities.isEmpty() || capabilities == "[ESS]"
        }
        val securedCount = records.size - openCount
        val durationMs = if (records.isNotEmpty()) System.currentTimeMillis() - startTime else 0L

        return WardrivingStats(
            totalRecords = records.size,
            uniqueSsids = uniqueSsids,
            uniqueBssids = uniqueBssids,
            openCount = openCount,
            securedCount = securedCount,
            sessionDurationMs = durationMs
        )
    }

    init {
        observeDemoRequests(demoBus, DemoData.Routes.WARDRIVING) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        val demo = DemoData.wardrivingRecords
        _state.value = _state.value.copy(
            session = WardrivingSession(
                id = "demo",
                startTime = System.currentTimeMillis() - DemoData.wardrivingStats.sessionDurationMs,
                recordCount = demo.size,
                uniqueBssids = demo.map { it.bssid }.distinct().size,
                isActive = false
            ),
            records = demo,
            stats = DemoData.wardrivingStats,
            error = null
        )
    }
}

package com.abhishek.zerodroid.features.sessions.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.abhishek.zerodroid.core.prefs.AppSettings
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.debug.DemoData
import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.debug.observeDemoRequests
import com.abhishek.zerodroid.core.sessions.ExportFormat
import com.abhishek.zerodroid.core.sessions.Session
import com.abhishek.zerodroid.core.sessions.SessionCompare
import com.abhishek.zerodroid.core.sessions.SessionDiff
import com.abhishek.zerodroid.core.sessions.SessionExportService
import com.abhishek.zerodroid.core.sessions.SessionItem
import com.abhishek.zerodroid.core.sessions.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The Sessions tab: every saved run, with two-at-a-time selection for Compare and Export. */
@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val exporter: SessionExportService,
    private val settings: AppSettings,
    demoBus: DemoDataBus
) : ViewModel() {

    val redactDefault: Boolean get() = settings.redactExports.value
    val retentionDays: StateFlow<Int> = settings.retentionDays

    val sessions: StateFlow<List<Session>> = repository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selected = MutableStateFlow<List<String>>(emptyList())
    val selected: StateFlow<List<String>> = _selected.asStateFlow()

    init {
        viewModelScope.launch { repository.prune(settings.retentionMs) }
        observeDemoRequests(demoBus, DemoData.Routes.SESSIONS) { loadDemoData() }
    }

    /** Toggles [id]; at most two sessions can be selected, the oldest pick drops off. */
    fun toggleSelection(id: String) = _selected.update { current ->
        when {
            id in current -> current - id
            current.size >= 2 -> current.drop(1) + id
            else -> current + id
        }
    }

    fun clearSelection() {
        _selected.value = emptyList()
    }

    fun export(ids: List<String>, format: ExportFormat, redact: Boolean, onReady: (Uri) -> Unit) {
        viewModelScope.launch { onReady(exporter.export(ids, format, redact)) }
    }

    private fun loadDemoData() {
        viewModelScope.launch {
            DemoData.sessions.forEach { (s, items) ->
                repository.record(s.tool, s.title, s.startedAt, s.endedAt, items, s.summary, s.findingCount, s.place)
            }
        }
    }
}

/** One session in full: what was seen, where, and the actions on it. */
@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository,
    private val exporter: SessionExportService,
    private val settings: AppSettings
) : ViewModel() {

    val redactDefault: Boolean get() = settings.redactExports.value

    val id: String = checkNotNull(savedStateHandle["id"])

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val _items = MutableStateFlow<List<SessionItem>>(emptyList())
    val items: StateFlow<List<SessionItem>> = _items.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            _session.value = repository.get(id)
            _items.value = repository.items(id)
        }
    }

    fun rename(place: String) {
        viewModelScope.launch {
            repository.rename(id, place)
            reload()
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(id)
            _deleted.value = true
        }
    }

    fun export(format: ExportFormat, redact: Boolean, onReady: (Uri) -> Unit) {
        viewModelScope.launch { onReady(exporter.export(listOf(id), format, redact)) }
    }
}

data class CompareState(
    val before: Session? = null,
    val after: Session? = null,
    val diff: SessionDiff? = null
)

/** Two sessions side by side; the older one is always "before". */
@HiltViewModel
class CompareViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CompareState())
    val state: StateFlow<CompareState> = _state.asStateFlow()

    init {
        val a: String = checkNotNull(savedStateHandle["a"])
        val b: String = checkNotNull(savedStateHandle["b"])
        viewModelScope.launch {
            val sa = repository.get(a)
            val sb = repository.get(b)
            if (sa == null || sb == null) return@launch
            val (before, after) = if (sa.startedAt <= sb.startedAt) sa to sb else sb to sa
            _state.value = CompareState(
                before = before,
                after = after,
                diff = SessionCompare.diff(repository.items(before.id), repository.items(after.id))
            )
        }
    }
}

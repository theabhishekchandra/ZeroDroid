package com.abhishek.zerodroid.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.database.dao.SeenDeviceRow
import com.abhishek.zerodroid.core.sessions.SessionRepository
import com.abhishek.zerodroid.core.ui.HelpContent
import com.abhishek.zerodroid.features.sweep.domain.SweepPreset
import com.abhishek.zerodroid.navigation.ToolCatalog
import com.abhishek.zerodroid.navigation.ToolInfo
import com.abhishek.zerodroid.navigation.ZeroDroidScreen
import com.abhishek.zerodroid.navigation.sweepRunRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchResults(
    val query: String = "",
    val tools: List<ToolInfo> = emptyList(),
    val devices: List<SeenDeviceRow> = emptyList(),
    val actions: List<SearchAction> = emptyList(),
    val learn: List<LearnHit> = emptyList()
) {
    val isEmpty: Boolean get() = tools.isEmpty() && devices.isEmpty() && actions.isEmpty() && learn.isEmpty()
}

/** Search everything: tools, devices from saved sessions, quick actions and help text. */
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val sessions: SessionRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val devices = _query
        .debounce(150)
        .mapLatest { q -> runCatching { sessions.searchSeen(q) }.getOrDefault(emptyList()) }

    val results: StateFlow<SearchResults> = combine(_query, devices) { q, seen ->
        SearchResults(
            query = q,
            tools = SearchIndex.tools(ToolCatalog.tools, q),
            devices = if (q.isBlank()) emptyList() else seen,
            actions = SearchIndex.actions(ACTIONS, q),
            learn = SearchIndex.learn(ToolCatalog.tools, HelpContent.features, q)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())

    fun setQuery(q: String) {
        _query.value = q
    }

    companion object {
        val ACTIONS = listOf(
            SearchAction("Full room sweep", "Every check, about 2 minutes", sweepRunRoute(SweepPreset.FULL, ""), "hotel rental bug camera scan"),
            SearchAction("Tracker check", "30 s scan for AirTag, Tile, SmartTag", sweepRunRoute(SweepPreset.TRACKER_CHECK, ""), "airtag tile follow stalk"),
            SearchAction("Hidden camera sweep", "WiFi makers, BLE and magnetic", sweepRunRoute(SweepPreset.HIDDEN_CAMERA, ""), "spy cam"),
            SearchAction("Review alerts", "Open and resolved alerts", ZeroDroidScreen.AlertCenter.route, "threat triage"),
            SearchAction("Compare or export sessions", "Saved scans on this phone", ZeroDroidScreen.Sessions.route, "history pdf csv json report"),
            SearchAction("Watch rules", "Background alerts for trackers, rogue APs, 2G", "rules", "background notify follow automation"),
            SearchAction("Settings", "Privacy, retention, trusted networks", ZeroDroidScreen.Settings.route, "preferences delete data redact")
        )
    }
}

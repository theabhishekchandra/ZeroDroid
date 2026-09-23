package com.abhishek.zerodroid.features.alert_center.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import com.abhishek.zerodroid.core.alerts.AlertResolution
import com.abhishek.zerodroid.core.alerts.UnifiedAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.debug.DemoData
import com.abhishek.zerodroid.core.debug.observeDemoRequests

@HiltViewModel
class AlertCenterViewModel @Inject constructor(
    private val repository: AlertCenterRepository,
    private val demoBus: DemoDataBus
) : ViewModel() {

    val alerts: StateFlow<List<UnifiedAlert>> = repository.alerts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }

    fun resolve(alert: UnifiedAlert, resolution: AlertResolution) {
        viewModelScope.launch { repository.resolve(alert.id, resolution) }
    }

    fun reopen(alert: UnifiedAlert) {
        viewModelScope.launch { repository.reopen(alert.id) }
    }

    fun clearResolved() {
        viewModelScope.launch { repository.clearResolved() }
    }

    init {
        viewModelScope.launch { repository.pruneResolved() }
        observeDemoRequests(demoBus, DemoData.Routes.ALERT_CENTER) { loadDemoData() }
    }

    /** Debug-only: replaces live state with [DemoData] so the populated UI can be verified without hardware. */
    private fun loadDemoData() {
        viewModelScope.launch {
            DemoData.unifiedAlerts.forEach { repository.record(it.source, it.severity, it.title, it.detail, it.timestamp) }
        }
    }
}

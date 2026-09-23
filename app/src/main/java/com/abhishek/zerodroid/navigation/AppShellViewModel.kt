package com.abhishek.zerodroid.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** State the app shell needs across every tab: the Alerts badge count. */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    alertCenterRepository: AlertCenterRepository
) : ViewModel() {

    /** Only alerts still waiting for triage count towards the badge. */
    val alertCount: StateFlow<Int> = alertCenterRepository.openAlerts
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

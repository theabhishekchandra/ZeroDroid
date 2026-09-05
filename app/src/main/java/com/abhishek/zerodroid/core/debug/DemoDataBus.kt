package com.abhishek.zerodroid.core.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug-only channel that lets the top bar ask the ViewModel of the screen
 * currently on display to replace its live state with [DemoData].
 *
 * It exists so hardware-dependent screens (NFC, IR, UWB, SDR, USB, trackers,
 * threat lists, ...) can be visually verified on a phone that lacks the
 * hardware or has nothing suspicious nearby. The trigger is only rendered in
 * debug builds ([BuildConfig.DEBUG]) and [request] is a no-op in release, so
 * production behaviour is unchanged.
 */
@Singleton
class DemoDataBus @Inject constructor() {

    private val _requests = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Routes for which a demo load was requested. */
    val requests: SharedFlow<String> = _requests.asSharedFlow()

    fun request(route: String) {
        if (!BuildConfig.DEBUG) return
        _requests.tryEmit(route)
    }
}

/**
 * Subscribe a ViewModel to demo requests for its own [route]. Call from `init`.
 */
fun ViewModel.observeDemoRequests(bus: DemoDataBus, route: String, onRequest: () -> Unit) {
    if (!BuildConfig.DEBUG) return
    viewModelScope.launch {
        bus.requests.filter { it == route }.collect { onRequest() }
    }
}

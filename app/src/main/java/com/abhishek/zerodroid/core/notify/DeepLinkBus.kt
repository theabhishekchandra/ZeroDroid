package com.abhishek.zerodroid.core.notify

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes requested from outside the app (notifications, tiles, widgets). MainActivity puts them
 * here; the navigation host opens and consumes them.
 */
@Singleton
class DeepLinkBus @Inject constructor() {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun open(route: String) {
        _pending.value = route
    }

    fun consume() {
        _pending.value = null
    }

    companion object {
        const val EXTRA_ROUTE = "com.abhishek.zerodroid.ROUTE"
    }
}

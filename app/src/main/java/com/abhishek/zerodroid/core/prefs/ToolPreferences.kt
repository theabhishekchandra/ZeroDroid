package com.abhishek.zerodroid.core.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import com.abhishek.zerodroid.core.di.DashboardPrefs
import com.abhishek.zerodroid.navigation.ToolCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class LastUsedFeature(
    val route: String,
    val title: String
)

/**
 * Per-user tool choices shared by Home and Tools: which tools are pinned to Home and which
 * tool was opened last (shown as Resume).
 */
@Singleton
class ToolPreferences @Inject constructor(
    @DashboardPrefs private val prefs: SharedPreferences
) {
    private val _pinnedRoutes = MutableStateFlow(readPinned())
    val pinnedRoutes: StateFlow<List<String>> = _pinnedRoutes.asStateFlow()

    private val _lastUsed = MutableStateFlow(readLastUsed())
    val lastUsed: StateFlow<LastUsedFeature?> = _lastUsed.asStateFlow()

    fun isPinned(route: String): Boolean = route in _pinnedRoutes.value

    /** Pins or unpins a tool; returns true if it is pinned afterwards. */
    fun togglePin(route: String): Boolean {
        val current = _pinnedRoutes.value
        val next = if (route in current) current - route else current + route
        setPinned(next)
        return route in next
    }

    fun setPinned(routes: List<String>) {
        val cleaned = routes.distinct()
        prefs.edit { putString(KEY_PINNED, cleaned.joinToString(SEPARATOR)) }
        _pinnedRoutes.value = cleaned
    }

    fun recordOpened(route: String, title: String) {
        prefs.edit {
            putString(KEY_LAST_ROUTE, route)
            putString(KEY_LAST_TITLE, title)
        }
        _lastUsed.value = LastUsedFeature(route, title)
    }

    private fun readPinned(): List<String> {
        val stored = prefs.getString(KEY_PINNED, null) ?: return ToolCatalog.defaultPinnedRoutes
        return stored.split(SEPARATOR).filter { it.isNotBlank() }
    }

    private fun readLastUsed(): LastUsedFeature? {
        val route = prefs.getString(KEY_LAST_ROUTE, null)
        val title = prefs.getString(KEY_LAST_TITLE, null)
        return if (route != null && title != null) LastUsedFeature(route, title) else null
    }

    companion object {
        const val KEY_LAST_ROUTE = "last_used_route"
        const val KEY_LAST_TITLE = "last_used_title"
        const val KEY_PINNED = "pinned_routes"
        private const val SEPARATOR = ","
    }
}

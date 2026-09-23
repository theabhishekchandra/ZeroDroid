package com.abhishek.zerodroid.features.watch.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.abhishek.zerodroid.core.di.SettingsPrefs
import com.abhishek.zerodroid.features.watch.domain.RuleKind
import com.abhishek.zerodroid.features.watch.domain.WatchRule
import com.abhishek.zerodroid.features.watch.domain.WatchRuleCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Watch rules and the "pause all" switch, shared by the rules screen, the service and the tile. */
@Singleton
class WatchRuleStore @Inject constructor(
    @SettingsPrefs private val prefs: SharedPreferences
) {
    private val _rules = MutableStateFlow(WatchRuleCodec.decode(prefs.getString(KEY_RULES, null)))
    val rules: StateFlow<List<WatchRule>> = _rules.asStateFlow()

    private val _paused = MutableStateFlow(prefs.getBoolean(KEY_PAUSED, false))
    /** Set by "Pause all"; rules stay configured but nothing runs. */
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    /** Rules that should be running right now. */
    val active: List<WatchRule> get() = if (_paused.value) emptyList() else _rules.value.filter { it.enabled }

    fun setEnabled(id: String, enabled: Boolean) = save(_rules.value.map { if (it.id == id) it.copy(enabled = enabled) else it })

    fun addCustom(rssiThreshold: Int, minutes: Int): WatchRule {
        val rule = WatchRule("custom-${UUID.randomUUID()}", RuleKind.CUSTOM_BLE, enabled = true, rssiThreshold = rssiThreshold, minutes = minutes)
        save(_rules.value + rule)
        return rule
    }

    fun remove(id: String) = save(_rules.value.filterNot { it.id == id && !it.kind.builtIn })

    fun setPaused(paused: Boolean) {
        prefs.edit { putBoolean(KEY_PAUSED, paused) }
        _paused.value = paused
    }

    private fun save(rules: List<WatchRule>) {
        prefs.edit { putString(KEY_RULES, WatchRuleCodec.encode(rules)) }
        _rules.value = rules
    }

    companion object {
        const val KEY_RULES = "watch_rules"
        const val KEY_PAUSED = "watch_paused"
    }
}

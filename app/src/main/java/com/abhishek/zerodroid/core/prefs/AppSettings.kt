package com.abhishek.zerodroid.core.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import com.abhishek.zerodroid.core.di.SettingsPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** User settings that change behaviour across tools. Every option here is wired to something. */
@Singleton
class AppSettings @Inject constructor(
    @SettingsPrefs private val prefs: SharedPreferences
) {
    private val _redactExports = MutableStateFlow(prefs.getBoolean(KEY_REDACT, true))
    /** Default for the "Redact MAC addresses" switch on every export. */
    val redactExports: StateFlow<Boolean> = _redactExports.asStateFlow()

    private val _retentionDays = MutableStateFlow(prefs.getInt(KEY_RETENTION, 30))
    /** How long sessions are kept before they are pruned. */
    val retentionDays: StateFlow<Int> = _retentionDays.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEY_SCREEN_ON, true))
    /** Keeps the display awake while a room sweep runs. */
    val keepScreenOnDuringSweeps: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _trusted = MutableStateFlow(prefs.getStringSet(KEY_TRUSTED, emptySet()).orEmpty().toSet())
    /** Networks the user trusts; Rogue AP and sweeps flag their twins first. */
    val trustedNetworks: StateFlow<Set<String>> = _trusted.asStateFlow()

    private val _onboarded = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDED, false))
    /** False until the user has picked goals (or skipped) once. */
    val onboardingDone: StateFlow<Boolean> = _onboarded.asStateFlow()

    private val _hideOnLockScreen = MutableStateFlow(prefs.getBoolean(KEY_LOCK_PRIVATE, true))
    /** Lock-screen notifications say only "Possible tracker nearby" until unlocked. */
    val hideOnLockScreen: StateFlow<Boolean> = _hideOnLockScreen.asStateFlow()

    private val _myDevices = MutableStateFlow(prefs.getStringSet(KEY_MY_DEVICES, emptySet()).orEmpty().toSet())
    /** Bluetooth addresses marked "It's mine"; watch rules ignore them. */
    val myDevices: StateFlow<Set<String>> = _myDevices.asStateFlow()

    val retentionMs: Long get() = _retentionDays.value * 24L * 60 * 60 * 1000

    fun setRedactExports(on: Boolean) {
        prefs.edit { putBoolean(KEY_REDACT, on) }
        _redactExports.value = on
    }

    fun setRetentionDays(days: Int) {
        prefs.edit { putInt(KEY_RETENTION, days) }
        _retentionDays.value = days
    }

    fun setKeepScreenOn(on: Boolean) {
        prefs.edit { putBoolean(KEY_SCREEN_ON, on) }
        _keepScreenOn.value = on
    }

    fun setTrustedNetworks(ssids: Set<String>) {
        val clean = ssids.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        prefs.edit { putStringSet(KEY_TRUSTED, clean) }
        _trusted.value = clean
    }

    fun setOnboardingDone(done: Boolean) {
        prefs.edit { putBoolean(KEY_ONBOARDED, done) }
        _onboarded.value = done
    }

    fun setHideOnLockScreen(on: Boolean) {
        prefs.edit { putBoolean(KEY_LOCK_PRIVATE, on) }
        _hideOnLockScreen.value = on
    }

    fun markMine(address: String) {
        val next = _myDevices.value + address.uppercase()
        prefs.edit { putStringSet(KEY_MY_DEVICES, next) }
        _myDevices.value = next
    }

    fun forgetMine(address: String) {
        val next = _myDevices.value - address.uppercase()
        prefs.edit { putStringSet(KEY_MY_DEVICES, next) }
        _myDevices.value = next
    }

    companion object {
        const val KEY_REDACT = "redact_exports"
        const val KEY_RETENTION = "retention_days"
        const val KEY_SCREEN_ON = "keep_screen_on_sweeps"
        const val KEY_TRUSTED = "trusted_networks"
        const val KEY_ONBOARDED = "onboarding_done"
        const val KEY_LOCK_PRIVATE = "hide_on_lock_screen"
        const val KEY_MY_DEVICES = "my_devices"
        val RETENTION_CHOICES = listOf(7, 30, 90)
    }
}

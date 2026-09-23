package com.abhishek.zerodroid.features.sweep.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.notify.ZdNotifier
import com.abhishek.zerodroid.core.prefs.AppSettings
import com.abhishek.zerodroid.core.sessions.ItemKind
import com.abhishek.zerodroid.core.sessions.SessionItem
import com.abhishek.zerodroid.core.sessions.SessionRepository
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.hidden_camera.domain.HiddenCameraDetector
import com.abhishek.zerodroid.features.sensors.domain.SensorDataCollector
import com.abhishek.zerodroid.features.sweep.domain.CheckProgress
import com.abhishek.zerodroid.features.sweep.domain.CheckState
import com.abhishek.zerodroid.features.sweep.domain.FindingLevel
import com.abhishek.zerodroid.features.sweep.domain.SweepCheck
import com.abhishek.zerodroid.features.sweep.domain.SweepEvaluator
import com.abhishek.zerodroid.features.sweep.domain.SweepFinding
import com.abhishek.zerodroid.features.sweep.domain.SweepPreset
import com.abhishek.zerodroid.features.ultrasonic.domain.UltrasonicAnalyzer
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.features.wifi.domain.WifiAssessment
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.math.sqrt

enum class SweepPhase { READY, RUNNING, DONE }

data class SweepUiState(
    val preset: SweepPreset = SweepPreset.FULL,
    val place: String = "",
    val phase: SweepPhase = SweepPhase.READY,
    val progress: List<CheckProgress> = emptyList(),
    val findings: List<SweepFinding> = emptyList(),
    val elapsedMs: Long = 0,
    val estimatedMs: Long = 0,
    /** What the person should be doing right now. */
    val hint: String = "",
    val devicesHeard: Int = 0,
    val sessionId: String? = null,
    /** The last saved sweep of the same place, for "Compare with last". */
    val previousSessionId: String? = null,
    val startedAt: Long = 0
) {
    val doneCount: Int get() = progress.count { it.state == CheckState.DONE || it.state == CheckState.SKIPPED }
    val fraction: Float get() = if (progress.isEmpty()) 0f else doneCount.toFloat() / progress.size
}

/**
 * Runs a room sweep: one shared WiFi + BLE listening window for the radio checks, then the
 * microphone and magnetometer checks, then a report that is saved as a session.
 */
@HiltViewModel
class SweepViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wifiScanner: WifiScanner,
    private val bleScanner: BleScanner,
    private val ultrasonicAnalyzer: UltrasonicAnalyzer,
    private val sensors: SensorDataCollector,
    cameraDetector: HiddenCameraDetector,
    private val sessions: SessionRepository,
    private val settings: AppSettings,
    private val notifier: ZdNotifier
) : ViewModel() {

    private val evaluator = SweepEvaluator(cameraDetector)

    private val _state = MutableStateFlow(
        SweepUiState(
            preset = runCatching { SweepPreset.valueOf(savedStateHandle.get<String>("preset").orEmpty()) }.getOrDefault(SweepPreset.FULL),
            place = savedStateHandle.get<String>("place").orEmpty().trim()
        )
    )
    val state: StateFlow<SweepUiState> = _state.asStateFlow()

    val keepScreenOn: StateFlow<Boolean> = settings.keepScreenOnDuringSweeps

    private var runJob: Job? = null
    private var stepJob: Job? = null
    private var aps: List<WifiAccessPoint> = emptyList()
    private var devices: List<BleDevice> = emptyList()

    fun start() {
        if (runJob?.isActive == true) return
        val preset = _state.value.preset
        val startedAt = System.currentTimeMillis()
        _state.update {
            it.copy(
                phase = SweepPhase.RUNNING,
                progress = preset.checks.map { c -> CheckProgress(c) },
                findings = emptyList(),
                startedAt = startedAt,
                estimatedMs = (if (preset.needsRadios) RADIO_MS else 0L) +
                    (if (preset.needsMicrophone) ULTRASONIC_MS else 0L) +
                    (if (SweepCheck.MAGNETIC in preset.checks) MAGNETIC_MS else 0L)
            )
        }
        runJob = viewModelScope.launch {
            val ticker = launch {
                while (isActive) {
                    _state.update { it.copy(elapsedMs = System.currentTimeMillis() - startedAt) }
                    delay(1_000)
                }
            }
            if (preset.needsRadios) step { radios(preset) }
            if (preset.needsMicrophone) step { ultrasonic() }
            if (SweepCheck.MAGNETIC in preset.checks) step { magnetic() }
            ticker.cancel()
            finish()
        }
    }

    /** Runs one step as its own job so Skip can cancel just that step. */
    private suspend fun step(block: suspend () -> Unit) {
        coroutineScope {
            stepJob = launch { block() }
        }
    }

    /** Ends the current step early; its checks keep whatever they have so far. */
    fun skipCurrent() {
        stepJob?.cancel()
    }

    /** Stops the whole sweep and shows the report for what ran. */
    fun stop() {
        if (_state.value.phase != SweepPhase.RUNNING) return
        val job = runJob ?: return
        viewModelScope.launch {
            // Let each step's cleanup record what it heard before the report is built.
            job.cancelAndJoin()
            finish()
        }
    }

    private suspend fun radios(preset: SweepPreset) {
        val radioChecks = preset.checks.filter { it in SweepPreset.RADIO_CHECKS }
        radioChecks.forEach { setState(it, CheckState.RUNNING) }
        hint("Walk slowly around the room with the phone. Keep it about 30 cm from outlets, smoke detectors, vents and TVs.")
        try {
            withTimeoutOrNull(RADIO_MS) {
                coroutineScope {
                    if (preset.checks.any { it == SweepCheck.WIFI || it == SweepCheck.ROGUE_AP || it == SweepCheck.CAMERAS }) {
                        launch { wifiScanner.scan().catch { }.collect { aps = it } }
                    }
                    launch {
                        bleScanner.scan().catch { }.collect { list ->
                            devices = list
                            _state.update { it.copy(devicesHeard = aps.size + list.size) }
                            if (SweepCheck.TRACKERS in preset.checks) {
                                publish(SweepCheck.TRACKERS, evaluator.trackers(list).findings)
                            }
                        }
                    }
                }
            }
        } finally {
            radioChecks.forEach { check ->
                val result = when (check) {
                    SweepCheck.WIFI -> evaluator.wifi(aps)
                    SweepCheck.ROGUE_AP -> evaluator.rogueAp(aps, settings.trustedNetworks.value)
                    SweepCheck.TRACKERS -> evaluator.trackers(devices)
                    SweepCheck.CAMERAS -> evaluator.cameras(aps, devices)
                    else -> null
                } ?: return@forEach
                publish(check, result.findings)
                complete(check, result.summary, result.findings.size)
            }
        }
    }

    private suspend fun ultrasonic() {
        setState(SweepCheck.ULTRASONIC, CheckState.RUNNING)
        hint("Stand still in the middle of the room and stay quiet for 20 seconds.")
        var peakHz = 0f
        var peakMagnitude = 0f
        var beacons = 0
        var failed: String? = null
        try {
            withTimeoutOrNull(ULTRASONIC_MS) {
                ultrasonicAnalyzer.analyze().catch { failed = it.message }.collect { s ->
                    if (s.error != null) failed = s.error
                    if (s.peakMagnitude > peakMagnitude) {
                        peakMagnitude = s.peakMagnitude
                        peakHz = s.peakFrequency
                    }
                    beacons = maxOf(beacons, s.detectedBeacons.size)
                }
            }
        } finally {
            if (failed != null) {
                complete(SweepCheck.ULTRASONIC, "Couldn’t use the microphone", 0, skipped = true)
            } else {
                val result = evaluator.ultrasonic(peakHz, peakMagnitude, beacons)
                publish(SweepCheck.ULTRASONIC, result.findings)
                complete(SweepCheck.ULTRASONIC, result.summary, result.findings.size)
            }
        }
    }

    private suspend fun magnetic() {
        setState(SweepCheck.MAGNETIC, CheckState.RUNNING)
        hint("Hold still for a moment to set a baseline, then move the top of the phone slowly along walls and furniture.")
        val samples = mutableListOf<Float>()
        sensors.start()
        try {
            if (!sensors.magnetometer.first().isAvailable && withTimeoutOrNull(1_500) { sensors.magnetometer.first { it.isAvailable } } == null) {
                complete(SweepCheck.MAGNETIC, "No magnetometer on this phone", 0, skipped = true)
                return
            }
            withTimeoutOrNull(MAGNETIC_MS) {
                sensors.magnetometer.collect { r ->
                    if (r.isAvailable && r.values.size >= 3) {
                        samples += sqrt(r.values[0] * r.values[0] + r.values[1] * r.values[1] + r.values[2] * r.values[2])
                    }
                }
            }
        } finally {
            sensors.stop()
            if (samples.size >= 4 && _state.value.progress.firstOrNull { it.check == SweepCheck.MAGNETIC }?.state == CheckState.RUNNING) {
                val baseline = samples.take(minOf(10, samples.size)).average().toFloat()
                val result = evaluator.magnetic(baseline, samples.max())
                publish(SweepCheck.MAGNETIC, result.findings)
                complete(SweepCheck.MAGNETIC, result.summary, result.findings.size)
            } else if (_state.value.progress.firstOrNull { it.check == SweepCheck.MAGNETIC }?.state == CheckState.RUNNING) {
                complete(SweepCheck.MAGNETIC, "Stopped before a baseline was set", 0, skipped = true)
            }
        }
    }

    private suspend fun finish() {
        val now = System.currentTimeMillis()
        _state.update { s ->
            s.copy(
                phase = SweepPhase.DONE,
                hint = "",
                elapsedMs = now - s.startedAt,
                progress = s.progress.map { if (it.state == CheckState.QUEUED || it.state == CheckState.RUNNING) it.copy(state = CheckState.SKIPPED, result = it.result ?: "Not run") else it }
            )
        }
        val s = _state.value
        val place = s.place.ifBlank { null }
        val previous = place?.let { p ->
            sessions.sessions.first().firstOrNull { it.tool == SESSION_TOOL && it.place == p }?.id
        }
        val findingItems = s.findings.map {
            SessionItem(
                key = it.key ?: "${it.check}:${it.title}",
                label = it.title,
                kind = if (it.isBle) ItemKind.TRACKER else ItemKind.RF,
                rssi = it.rssi,
                detail = it.detail,
                flagged = it.level != FindingLevel.LOW
            )
        }
        val seen = aps.map { SessionItem(it.bssid, WifiAssessment.displayName(it), ItemKind.WIFI, it.rssi, "${it.security.label} · ch ${it.channel}") } +
            devices.map { SessionItem(it.address, it.name ?: "[no name]", ItemKind.BLE, it.rssi, "Bluetooth") }
        val flagged = s.findings.count { it.level != FindingLevel.LOW }
        val id = sessions.record(
            tool = SESSION_TOOL,
            title = s.preset.title,
            startedAt = s.startedAt,
            endedAt = now,
            items = (findingItems + seen).distinctBy { it.key },
            summary = "${s.progress.count { it.state == CheckState.DONE }} checks · " +
                if (flagged == 0) "nothing needs a look" else "$flagged thing${if (flagged > 1) "s" else ""} need a look",
            findingCount = flagged,
            place = place,
            keepEmpty = true
        )
        _state.update { it.copy(sessionId = id, previousSessionId = previous) }
        if (id != null) {
            notifier.postSweepFinished(
                sessionId = id,
                place = place,
                high = s.findings.count { it.level == FindingLevel.HIGH },
                medium = s.findings.count { it.level == FindingLevel.MEDIUM }
            )
        }
    }

    /** Replaces [check]'s findings; called live while radios listen and again at the end. */
    private fun publish(check: SweepCheck, findings: List<SweepFinding>) {
        _state.update { s ->
            val others = s.findings.filterNot { it.check == check }
            s.copy(findings = (others + findings).sortedBy { it.level.ordinal })
        }
    }

    private fun complete(check: SweepCheck, result: String, flags: Int, skipped: Boolean = false) {
        _state.update { s ->
            s.copy(progress = s.progress.map {
                if (it.check == check) it.copy(state = if (skipped) CheckState.SKIPPED else CheckState.DONE, result = result, flags = flags) else it
            })
        }
    }

    private fun setState(check: SweepCheck, state: CheckState) {
        _state.update { s -> s.copy(progress = s.progress.map { if (it.check == check) it.copy(state = state) else it }) }
    }

    private fun hint(text: String) = _state.update { it.copy(hint = text) }

    override fun onCleared() {
        runJob?.cancel()
        sensors.stop()
    }

    companion object {
        const val SESSION_TOOL = "sweep"
        const val RADIO_MS = 30_000L
        const val ULTRASONIC_MS = 20_000L
        const val MAGNETIC_MS = 20_000L
    }
}

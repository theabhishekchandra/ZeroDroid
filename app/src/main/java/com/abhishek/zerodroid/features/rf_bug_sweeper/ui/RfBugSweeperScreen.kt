package com.abhishek.zerodroid.features.rf_bug_sweeper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdCheckRow
import com.abhishek.zerodroid.core.ui.zd.ZdCheckStatus
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdScanControlBar
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdSwitchRow
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.formatElapsed
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.BugDetection
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.BugSweepState
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.BugType
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.SweepMode
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.ThreatSeverity
import com.abhishek.zerodroid.features.rf_bug_sweeper.viewmodel.RfBugSweeperViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.util.Locale

@Composable
fun RfBugSweeperScreen(
    viewModel: RfBugSweeperViewModel = hiltViewModel()
) {
    val requiredPermissions = remember {
        (PermissionUtils.blePermissions() + PermissionUtils.audioPermissions()).distinct()
    }

    PermissionGate(
        permissions = requiredPermissions,
        rationale = "The sweep listens for Bluetooth radio modules and ultrasonic tones, which need Bluetooth and microphone access."
    ) {
        RfBugSweeperContent(viewModel)
    }
}

private fun ThreatSeverity.zd(): ZdSeverity = when (this) {
    ThreatSeverity.CRITICAL -> ZdSeverity.CRITICAL
    ThreatSeverity.HIGH -> ZdSeverity.HIGH
    ThreatSeverity.MEDIUM -> ZdSeverity.MEDIUM
    ThreatSeverity.LOW -> ZdSeverity.LOW
}

private fun BugType.label(): String = when (this) {
    BugType.RF_TRANSMITTER -> "RADIO MODULE"
    BugType.ULTRASONIC_BEACON -> "ULTRASONIC"
    BugType.MAGNETIC_ANOMALY -> "MAGNETIC"
    BugType.SUSPICIOUS_BLE -> "BLE"
    BugType.UNKNOWN -> "UNKNOWN"
}

@Composable
private fun RfBugSweeperContent(viewModel: RfBugSweeperViewModel) {
    val state by viewModel.state.collectAsState()
    var selectedModes by remember {
        mutableStateOf(setOf(SweepMode.BLE, SweepMode.ULTRASONIC, SweepMode.MAGNETIC))
    }

    HardwareLifecycleEffect(
        isActive = state.isSweeping,
        onPause = viewModel::stopSweep,
        onResume = { viewModel.startSweep(selectedModes) }
    )

    Column(Modifier.fillMaxSize()) {
        ZdScanControlBar(
            running = state.isSweeping,
            onStart = { viewModel.startSweep(selectedModes) },
            onStop = viewModel::stopSweep,
            runningLabel = "Sweeping · ${formatElapsed(state.sweepDurationMs)}",
            runningDetail = "Walk slowly, 30 cm from surfaces",
            idleDetail = if (state.detections.isEmpty()) "Pick checks below, then start" else "Results kept",
            startLabel = "Sweep"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ZdSectionLabel("Checks") }
            item {
                ZdCard(verticalSpacing = 2.dp) {
                    listOf(
                        Triple(SweepMode.BLE, "Radio modules (BLE)", "HC-05, ESP32, nRF and other hobby radios"),
                        Triple(SweepMode.ULTRASONIC, "Ultrasonic beacons", "18–24 kHz tones, via the microphone"),
                        Triple(SweepMode.MAGNETIC, "Magnetic anomalies", "Electronics hidden behind surfaces")
                    ).forEach { (mode, label, detail) ->
                        ZdSwitchRow(
                            label = label,
                            description = detail,
                            checked = mode in selectedModes,
                            onCheckedChange = { on -> selectedModes = if (on) selectedModes + mode else selectedModes - mode }
                        )
                    }
                }
            }

            if (state.isSweeping || state.detections.isNotEmpty() || state.bleDeviceCount > 0) {
                item { LiveReadings(state, onCalibrate = viewModel::calibrateMagnetic) }
            }

            if (state.detections.isNotEmpty()) {
                item { ZdSectionLabel("Live findings", trailingText = "${state.detections.size}") }
                items(state.detections, key = { it.id }) { FindingCard(it) }
                item { ZdButton("Clear findings", onClick = viewModel::clearDetections, variant = ZdButtonVariant.Ghost, icon = ZdIcons.Trash) }
            } else if (!state.isSweeping) {
                item {
                    ZdCard {
                        Text("Before you start", style = ZdType.Label, color = ZdColors.Text)
                        Text(
                            "Stand in the middle of the room for the magnetic baseline, then walk slowly past outlets, smoke detectors, vents and TVs. Keep the phone about 30 cm from surfaces.",
                            style = ZdType.BodySmall,
                            color = ZdColors.Text2
                        )
                    }
                }
            }
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
            item { ZdFootnote("Smart plugs and DIY projects use the same radio modules as cheap bugs. A finding means “look closer”, not “found a bug”.") }
        }
    }
}

@Composable
private fun LiveReadings(state: BugSweepState, onCalibrate: () -> Unit) {
    ZdListCard(
        listOfNotNull(
            if (SweepMode.BLE in state.activeModes || state.bleDeviceCount > 0) "ble" else null,
            if (SweepMode.ULTRASONIC in state.activeModes) "ultra" else null,
            if (SweepMode.MAGNETIC in state.activeModes || state.magneticBaseline > 0f) "mag" else null
        )
    ) { key ->
        when (key) {
            "ble" -> ZdCheckRow(
                title = "Bluetooth",
                detail = "${state.bleDeviceCount} devices checked",
                status = if (state.detections.any { it.type == BugType.RF_TRANSMITTER || it.type == BugType.SUSPICIOUS_BLE }) ZdCheckStatus.WARN else ZdCheckStatus.PASS
            )
            "ultra" -> ZdCheckRow(
                title = "Ultrasonic 18–24 kHz",
                detail = if (state.ultrasonicDetected) "Tone detected" else "Quiet",
                status = if (state.ultrasonicDetected) ZdCheckStatus.WARN else ZdCheckStatus.PASS
            )
            else -> Column {
                ZdCheckRow(
                    title = "Magnetic field",
                    detail = String.format(Locale.US, "%.1f µT · baseline %.1f · %+.1f", state.magneticCurrent, state.magneticBaseline, state.magneticDeviation),
                    status = if (state.detections.any { it.type == BugType.MAGNETIC_ANOMALY }) ZdCheckStatus.WARN else ZdCheckStatus.PASS
                )
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End) {
                    ZdButton("Re-zero", onClick = onCalibrate, variant = ZdButtonVariant.Ghost, height = 36.dp)
                }
            }
        }
    }
}

@Composable
private fun FindingCard(detection: BugDetection) {
    val severity = detection.severity.zd()
    ZdCard(borderColor = severity.color.copy(alpha = 0.4f)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ZdSeverityBadge(severity)
            ZdTag(detection.type.label())
        }
        Text(detection.title, style = ZdType.Label, color = ZdColors.Text)
        Text(detection.detail, style = ZdType.BodySmall, color = ZdColors.Text2)
        Row {
            detection.rssi?.let { ZdStat("Signal", "$it dBm", Modifier.weight(1f)) }
            detection.frequency?.let { ZdStat("Frequency", String.format(Locale.US, "%.2f kHz", it / 1000f), Modifier.weight(1f)) }
            detection.fieldStrength?.let { ZdStat("Field", String.format(Locale.US, "%.1f µT", it), Modifier.weight(1f)) }
        }
    }
}

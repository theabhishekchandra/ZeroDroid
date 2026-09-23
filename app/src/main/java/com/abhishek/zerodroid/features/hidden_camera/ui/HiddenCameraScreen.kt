package com.abhishek.zerodroid.features.hidden_camera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.ZdToolScanBar
import com.abhishek.zerodroid.features.hidden_camera.domain.CameraDetection
import com.abhishek.zerodroid.features.hidden_camera.domain.DetectionSource
import com.abhishek.zerodroid.features.hidden_camera.domain.HiddenCameraScanState
import com.abhishek.zerodroid.features.hidden_camera.domain.ThreatLevel
import com.abhishek.zerodroid.features.hidden_camera.viewmodel.HiddenCameraViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

@Composable
fun HiddenCameraScreen(
    viewModel: HiddenCameraViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.hiddenCameraPermissions(),
        rationale = "Five detection methods use the camera, WiFi, Bluetooth and location, each of which Android gates behind a permission."
    ) {
        HiddenCameraContent(viewModel)
    }
}

private fun ThreatLevel.severity(): ZdSeverity = when (this) {
    ThreatLevel.HIGH -> ZdSeverity.HIGH
    ThreatLevel.MEDIUM -> ZdSeverity.MEDIUM
    ThreatLevel.LOW -> ZdSeverity.LOW
}

private fun DetectionSource.label(): String = when (this) {
    DetectionSource.IR -> "IR LENS"
    DetectionSource.WIFI -> "WIFI"
    DetectionSource.BLE -> "BLE"
    DetectionSource.MAGNETIC -> "MAGNETIC"
    DetectionSource.NETWORK -> "PORTS"
}

@Composable
private fun HiddenCameraContent(viewModel: HiddenCameraViewModel) {
    val state by viewModel.state.collectAsState()

    HardwareLifecycleEffect(
        isActive = state.isScanning,
        onPause = viewModel::stopScan,
        onResume = viewModel::startScan
    )
    // The IR camera preview is bound to the lifecycle by CameraX; only the flag needs resetting.
    DisposableEffect(Unit) {
        onDispose { viewModel.stopIrMode() }
    }

    Column(Modifier.fillMaxSize()) {
        ZdToolScanBar(
            running = state.isScanning,
            onStart = viewModel::startScan,
            onStop = viewModel::stopScan,
            runningNote = "WiFi + BLE + magnetic",
            idleNote = if (state.detections.isEmpty()) "Walk slowly while it runs" else "Results kept"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val likely = state.detections.count { it.threatLevel == ThreatLevel.HIGH }
            val possible = state.detections.count { it.threatLevel != ThreatLevel.HIGH }

            if (state.detections.isEmpty() && !state.isScanning && !state.irActive) {
                item {
                    ZdStatePanel(
                        kicker = if (state.error != null) "Couldn’t start" else "Ready",
                        kickerColor = if (state.error != null) ZdColors.Critical else ZdColors.Text3,
                        icon = ZdIcons.Camera,
                        iconTint = ZdColors.Accent,
                        iconBackground = ZdColors.AccentBg,
                        title = "Check a room for hidden cameras",
                        body = state.error ?: "Combines five weak signals, because no single one proves a camera. Connect to the room’s WiFi first so the port check can run.",
                        primaryAction = "Start scan" to viewModel::startScan,
                        primaryIcon = ZdIcons.Play,
                        fullScreen = false
                    )
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ZdSeverityBadge(if (likely > 0) ZdSeverity.HIGH else ZdSeverity.CLEAN, label = "$likely LIKELY")
                        ZdSeverityBadge(if (possible > 0) ZdSeverity.MEDIUM else ZdSeverity.CLEAN, label = "$possible POSSIBLE")
                        ZdTag("5 methods")
                    }
                }
            }

            item { ZdSectionLabel("Methods") }
            item { MethodsCard(state, onRunPorts = viewModel::startNetworkScan) }

            item { IrFinderCard(active = state.irActive, onToggle = { if (state.irActive) viewModel.stopIrMode() else viewModel.startIrMode() }, onDetect = viewModel::addIrDetection) }

            if (state.detections.isNotEmpty()) {
                item { ZdSectionLabel("Findings", trailingText = "${state.detections.size}") }
                items(state.detections, key = { it.id }) { FindingCard(it) }
                item { ZdButton("Clear findings", onClick = viewModel::clearDetections, variant = ZdButtonVariant.Ghost, icon = ZdIcons.Trash) }
            }
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
            item { ZdFootnote("A camera with no radio, recording to a memory card, can’t be seen by any app. Use the IR finder and a careful look as well.") }
        }
    }
}

@Composable
private fun MethodsCard(state: HiddenCameraScanState, onRunPorts: () -> Unit) {
    data class Method(val title: String, val detail: String, val hits: Int, val live: Boolean)
    val methods = listOf(
        Method("WiFi vendor and names", "Camera makers’ OUI prefixes and default camera SSIDs", state.wifiSuspects, false),
        Method("BLE camera signatures", "Bluetooth devices named or built like cameras", state.bleSuspects, false),
        Method("Magnetic anomaly", "Electronics behind surfaces bend the field", if (state.magneticAnomaly) 1 else 0, state.isScanning),
        Method("Streaming ports (RTSP/ONVIF)", state.networkScanProgress ?: "Scans your LAN for video ports like 554", state.networkSuspects, state.networkScanProgress != null)
    )
    ZdListCard(methods) { m ->
        ZdCheckRow(
            title = m.title,
            detail = m.detail,
            status = when {
                m.hits > 0 -> ZdCheckStatus.WARN
                !state.isScanning && m.title.startsWith("Streaming") && state.networkScanProgress == null && state.networkSuspects == 0 -> ZdCheckStatus.NA
                state.isScanning || state.detections.isNotEmpty() -> ZdCheckStatus.PASS
                else -> ZdCheckStatus.NA
            }
        )
    }
    ZdButton(
        if (state.networkScanProgress != null) "Scanning ports…" else "Scan this network’s ports",
        onClick = onRunPorts,
        enabled = state.networkScanProgress == null,
        variant = ZdButtonVariant.Secondary,
        icon = ZdIcons.Lan,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun IrFinderCard(active: Boolean, onToggle: () -> Unit, onDetect: (CameraDetection) -> Unit) {
    ZdCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("IR lens finder", style = ZdType.Label, color = ZdColors.Text)
                Text("Night-vision cameras glow as steady purple or white dots on the front camera.", style = ZdType.Caption, color = ZdColors.Text3)
            }
        }
        if (active) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                IrCameraView(onIrDetected = onDetect)
            }
            Text("Turn off the lights, then point at smoke detectors, clocks, chargers and vents.", style = ZdType.BodySmall, color = ZdColors.Text2)
        }
        ZdButton(
            if (active) "Close IR finder" else "Find with IR",
            onClick = onToggle,
            variant = if (active) ZdButtonVariant.Secondary else ZdButtonVariant.Primary,
            icon = ZdIcons.Camera,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FindingCard(detection: CameraDetection) {
    val severity = detection.threatLevel.severity()
    ZdCard(borderColor = severity.color.copy(alpha = 0.4f)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ZdSeverityBadge(severity)
            ZdTag(detection.source.label())
            detection.rssi?.let { Text("$it dBm", style = ZdType.Path, color = ZdColors.Text3) }
        }
        Text(detection.title, style = ZdType.Label, color = ZdColors.Text)
        Text(detection.detail, style = ZdType.BodySmall, color = ZdColors.Text2)
    }
}

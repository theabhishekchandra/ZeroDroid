package com.abhishek.zerodroid.features.deauth_detector.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import com.abhishek.zerodroid.core.ui.zd.ZdSignal
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdToolScanBar
import com.abhishek.zerodroid.features.deauth_detector.domain.AlertLevel
import com.abhishek.zerodroid.features.deauth_detector.domain.AttackType
import com.abhishek.zerodroid.features.deauth_detector.domain.DeauthEvent
import com.abhishek.zerodroid.features.deauth_detector.viewmodel.DeauthDetectorViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeauthDetectorScreen(
    viewModel: DeauthDetectorViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.wifiPermissions(),
        rationale = "Android needs location access before any app can watch WiFi connections."
    ) {
        DeauthDetectorContent(viewModel = viewModel)
    }
}

private fun AlertLevel.severity(): ZdSeverity = when (this) {
    AlertLevel.CRITICAL -> ZdSeverity.CRITICAL
    AlertLevel.HIGH -> ZdSeverity.HIGH
    AlertLevel.MEDIUM -> ZdSeverity.MEDIUM
    AlertLevel.LOW -> ZdSeverity.LOW
}

/** Each watched pattern with the rule that trips it, as shown under "Patterns watched". */
private val patterns = listOf(
    AttackType.DEAUTH_FLOOD to ("Deauth flood" to "More than 3 drops in 60 s"),
    AttackType.RAPID_RECONNECT to ("Rapid reconnect" to "Repeated reconnects in a short window"),
    AttackType.SIGNAL_JAMMING to ("Signal jamming" to "Sudden 30 dB signal drop"),
    AttackType.AP_DISAPPEARANCE to ("AP disappearance" to "Your network vanishes from scans"),
    AttackType.CHANNEL_HOPPING to ("Channel hopping" to "Your network changes channel")
)

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

@Composable
private fun DeauthDetectorContent(viewModel: DeauthDetectorViewModel) {
    val state by viewModel.state.collectAsState()

    HardwareLifecycleEffect(
        isActive = state.isMonitoring,
        onPause = viewModel::stopMonitoring,
        onResume = viewModel::startMonitoring
    )

    Column(Modifier.fillMaxSize()) {
        ZdToolScanBar(
            running = state.isMonitoring,
            onStart = viewModel::startMonitoring,
            onStop = viewModel::stopMonitoring,
            verb = "Monitoring",
            runningNote = "Connected network only",
            idleNote = if (state.events.isEmpty()) "Watches the WiFi you’re connected to" else "Results kept"
        )

        if (!state.isMonitoring && state.events.isEmpty()) {
            ZdStatePanel(
                kicker = if (state.error != null) "Couldn’t start" else "Ready",
                kickerColor = if (state.error != null) ZdColors.Critical else ZdColors.Text3,
                icon = ZdIcons.Warning,
                iconTint = ZdColors.Accent,
                iconBackground = ZdColors.AccentBg,
                title = "Is someone kicking you off WiFi?",
                body = state.error ?: "Connect to the network you want to watch, then start. Bad signal and a deauth attack look different: attacks drop you while the signal stays strong.",
                primaryAction = "Start monitoring" to viewModel::startMonitoring,
                primaryIcon = ZdIcons.Play
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.connectedSsid != null) {
                item {
                    ZdCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ZdSignal(state.connectedRssi)
                            Column(Modifier.weight(1f)) {
                                Text(state.connectedSsid.orEmpty(), style = ZdType.Heading, color = ZdColors.Text)
                                Text("Connected · ch ${state.connectedChannel} · ${state.connectedRssi} dBm", style = ZdType.Caption, color = ZdColors.Text3)
                            }
                            if (state.isUnderAttack) ZdSeverityBadge(ZdSeverity.CRITICAL, label = "ATTACK")
                        }
                    }
                }
            }

            item {
                ZdCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Disconnects, last 60 s", style = ZdType.Mono.copy(fontWeight = ZdType.Label.fontWeight), color = ZdColors.Text2, modifier = Modifier.weight(1f))
                        Text("${state.disconnectCount} total", style = ZdType.Label, color = if (state.disconnectCount > 0) ZdColors.Medium else ZdColors.Text3)
                    }
                    DisconnectTimeline(viewModel.getDisconnectTimestamps(), state.monitoringDurationMs)
                    Row {
                        Text("-60 s", style = ZdType.Path, color = ZdColors.Text3, modifier = Modifier.weight(1f))
                        Text("now", style = ZdType.Path, color = ZdColors.Text3)
                    }
                }
            }

            if (state.events.isNotEmpty()) {
                item { ZdSectionLabel("Events", trailingText = "${state.events.size}") }
                items(state.events, key = { it.id }) { EventCard(it) }
            }

            item { ZdSectionLabel("Patterns watched") }
            item {
                ZdListCard(patterns) { (type, text) ->
                    val hits = state.events.count { it.type == type }
                    val worst = state.events.filter { it.type == type }.minOfOrNull { it.level.ordinal }
                    ZdCheckRow(
                        title = text.first,
                        detail = text.second,
                        status = when {
                            hits == 0 -> ZdCheckStatus.PASS
                            worst != null && worst <= AlertLevel.HIGH.ordinal -> ZdCheckStatus.FAIL
                            else -> ZdCheckStatus.WARN
                        }
                    )
                }
            }

            if (state.events.isNotEmpty()) {
                item { ZdButton("Clear events", onClick = viewModel::clearEvents, variant = ZdButtonVariant.Ghost, icon = ZdIcons.Trash) }
            }
            item { ZdFootnote("Android doesn’t expose raw WiFi frames, so attacks are inferred from their effect on your connection. WPA3 networks resist forged deauths.") }
        }
    }
}

@Composable
private fun DisconnectTimeline(timestamps: List<Long>, monitoringDurationMs: Long) {
    val windowMs = 60_000L
    // Re-read the clock whenever the monitoring timer ticks so dots slide left.
    val now = remember(monitoringDurationMs) { System.currentTimeMillis() }
    val recent = timestamps.count { now - it in 0..windowMs }
    val axis = ZdColors.BorderStrong
    val dot = ZdColors.Critical
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .semantics { contentDescription = "$recent disconnects in the last 60 seconds" }
    ) {
        val y = size.height / 2
        drawLine(axis, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        for (i in 0..4) {
            val x = size.width * i / 4f
            drawLine(axis, Offset(x, y - 8f), Offset(x, y + 8f), strokeWidth = 2f)
        }
        timestamps.forEach { ts ->
            val age = now - ts
            if (age in 0..windowMs) {
                val x = size.width * (1f - age.toFloat() / windowMs)
                drawCircle(dot.copy(alpha = 0.3f), radius = 12f, center = Offset(x, y))
                drawCircle(dot, radius = 6f, center = Offset(x, y))
            }
        }
    }
}

@Composable
private fun EventCard(event: DeauthEvent) {
    val severity = event.level.severity()
    ZdCard(borderColor = severity.color.copy(alpha = 0.4f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ZdSeverityBadge(severity, label = event.type.label.uppercase())
            Text(timeFormat.format(Date(event.timestamp)), style = ZdType.Path, color = ZdColors.Text3, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Text(event.title, style = ZdType.Label, color = ZdColors.Text)
        Text(event.detail, style = ZdType.BodySmall, color = ZdColors.Text2)
        event.affectedSsid?.let { Text("Network: $it", style = ZdType.Caption, color = ZdColors.Text3) }
    }
}

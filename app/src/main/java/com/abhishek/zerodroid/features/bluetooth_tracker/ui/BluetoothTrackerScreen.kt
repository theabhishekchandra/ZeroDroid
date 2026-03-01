package com.abhishek.zerodroid.features.bluetooth_tracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ble.domain.BleDistanceEstimator
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.DetectedTracker
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerScanState
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerType
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackingRisk
import com.abhishek.zerodroid.features.bluetooth_tracker.viewmodel.BluetoothTrackerViewModel
import com.abhishek.zerodroid.ui.theme.SurfaceVariantDark
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalAmberGlow
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalCyanGlow
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalGreenGlow
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TerminalRedGlow
import com.abhishek.zerodroid.ui.theme.TextDim
import com.abhishek.zerodroid.ui.theme.TextPrimary
import com.abhishek.zerodroid.ui.theme.TextSecondary

@Composable
fun BluetoothTrackerScreen(
    viewModel: BluetoothTrackerViewModel = viewModel(factory = BluetoothTrackerViewModel.Factory)
) {
    PermissionGate(
        permissions = PermissionUtils.blePermissions(),
        rationale = "Bluetooth permission is needed to scan for nearby trackers that may be following you."
    ) {
        BluetoothTrackerContent(viewModel = viewModel)
    }
}

@Composable
private fun BluetoothTrackerContent(viewModel: BluetoothTrackerViewModel) {
    DisposableEffect(Unit) {
        onDispose { viewModel.stopScan() }
    }

    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Scan Control ─────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(4.dp))
            ScanControlRow(
                state = state,
                onStartScan = { viewModel.startScan() },
                onStopScan = { viewModel.stopScan() },
                onClear = { viewModel.clearTrackers() }
            )
        }

        // ── Error ────────────────────────────────────────────────────────
        state.error?.let { error ->
            item {
                Text(
                    text = "> ERROR: $error",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = TerminalRed
                )
            }
        }

        // ── High Risk Alert Banner ───────────────────────────────────────
        item {
            AnimatedVisibility(
                visible = state.highRiskCount > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                HighRiskAlertBanner(count = state.highRiskCount)
            }
        }

        // ── Summary Row ──────────────────────────────────────────────────
        if (state.totalDevicesScanned > 0 || state.trackers.isNotEmpty()) {
            item {
                SummaryRow(state = state)
            }
        }

        // ── Tracker List ─────────────────────────────────────────────────
        if (state.trackers.isEmpty() && !state.isScanning) {
            item {
                EmptyState(
                    icon = Icons.Default.Security,
                    title = "No trackers detected",
                    subtitle = "Tap Scan to search for AirTags, Tiles, SmartTags, and other Bluetooth trackers nearby"
                )
            }
        }

        items(state.trackers, key = { it.address }) { tracker ->
            TrackerItem(tracker = tracker)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ── Scan Control Row ─────────────────────────────────────────────────────────

@Composable
private fun ScanControlRow(
    state: TrackerScanState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isScanning) {
            Column {
                ScanningIndicator(
                    isScanning = true,
                    label = "Scanning for trackers..."
                )
                Text(
                    text = formatDuration(state.scanDurationMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextSecondary
                )
            }
        } else {
            Text(
                text = "> TRACKER SCANNER",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = TerminalGreen
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.trackers.isNotEmpty() && !state.isScanning) {
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear results",
                        tint = TextSecondary
                    )
                }
            }
            if (state.isScanning) {
                OutlinedButton(
                    onClick = onStopScan,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TerminalRed
                    )
                ) {
                    Text("Stop", fontFamily = FontFamily.Monospace)
                }
            } else {
                Button(
                    onClick = onStartScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalGreen
                    )
                ) {
                    Text(
                        text = "Scan",
                        fontFamily = FontFamily.Monospace,
                        color = SurfaceVariantDark
                    )
                }
            }
        }
    }
}

// ── High Risk Alert Banner ───────────────────────────────────────────────────

@Composable
private fun HighRiskAlertBanner(count: Int) {
    TerminalCard(
        glowColor = TerminalRed,
        glowAlpha = TerminalRedGlow,
        borderColor = TerminalRed,
        animated = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = TerminalRed,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "TRACKING ALERT",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TerminalRed
                )
                Text(
                    text = "$count high-risk tracker${if (count > 1) "s" else ""} detected following you",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextPrimary
                )
            }
        }
    }
}

// ── Summary Row ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryRow(state: TrackerScanState) {
    TerminalCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(label = "SCANNED", value = "${state.totalDevicesScanned}", color = TextPrimary)
            SummaryItem(label = "TRACKERS", value = "${state.trackers.size}", color = TerminalAmber)
            SummaryItem(label = "HIGH RISK", value = "${state.highRiskCount}", color = TerminalRed)
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = TextDim
        )
    }
}

// ── Tracker Item ─────────────────────────────────────────────────────────────

@Composable
private fun TrackerItem(tracker: DetectedTracker) {
    val riskColor = when (tracker.risk) {
        TrackingRisk.HIGH -> TerminalRed
        TrackingRisk.MEDIUM -> TerminalAmber
        TrackingRisk.LOW -> TerminalCyan
        TrackingRisk.NONE -> TerminalGreen
    }
    val riskGlow = when (tracker.risk) {
        TrackingRisk.HIGH -> TerminalRedGlow
        TrackingRisk.MEDIUM -> TerminalAmberGlow
        TrackingRisk.LOW -> TerminalCyanGlow
        TrackingRisk.NONE -> TerminalGreenGlow
    }
    val signalColor = when {
        tracker.signalPercent >= 60 -> TerminalGreen
        tracker.signalPercent >= 30 -> TerminalAmber
        else -> TerminalRed
    }

    val distance = BleDistanceEstimator.estimateDistance(tracker.rssi)
    val distanceLabel = BleDistanceEstimator.getDistanceLabel(distance)
    val minutesAgo = ((System.currentTimeMillis() - tracker.firstSeen) / 60000).toInt()

    TerminalCard(
        glowColor = riskColor,
        glowAlpha = riskGlow,
        borderColor = riskColor.copy(alpha = 0.6f),
        animated = tracker.risk == TrackingRisk.HIGH
    ) {
        // Top row: type badge + name + risk badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tracker type badge
            Text(
                text = trackerTypeLabel(tracker.type),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = riskColor,
                modifier = Modifier
                    .background(
                        color = riskColor.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Name + address
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tracker.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
                Text(
                    text = tracker.address,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextSecondary
                )
            }

            // Risk badge
            Text(
                text = "[${tracker.risk.label}]",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = riskColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Signal bar
        val bgColor = MaterialTheme.colorScheme.surface
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            drawRoundRect(
                color = bgColor,
                cornerRadius = CornerRadius(2f, 2f),
                size = size
            )
            drawRoundRect(
                color = signalColor,
                cornerRadius = CornerRadius(2f, 2f),
                size = Size(size.width * tracker.signalPercent / 100f, size.height)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom row: RSSI + distance + seen info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // RSSI + distance
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${tracker.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = signalColor
                )
                Text(
                    text = distanceLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextSecondary
                )
            }

            // Seen info
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (minutesAgo > 0) "First seen ${minutesAgo}m ago" else "Just detected",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextDim
                )
                Text(
                    text = "Seen ${tracker.seenCount}x",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextDim
                )
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun trackerTypeLabel(type: TrackerType): String = when (type) {
    TrackerType.AIRTAG -> "AIRTAG"
    TrackerType.SMARTTAG -> "SMARTTAG"
    TrackerType.TILE -> "TILE"
    TrackerType.CHIPOLO -> "CHIPOLO"
    TrackerType.PEBBLEBEE -> "PEBBLEBEE"
    TrackerType.GENERIC_TRACKER -> "TRACKER"
    TrackerType.UNKNOWN -> "UNKNOWN"
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d elapsed", minutes, seconds)
}

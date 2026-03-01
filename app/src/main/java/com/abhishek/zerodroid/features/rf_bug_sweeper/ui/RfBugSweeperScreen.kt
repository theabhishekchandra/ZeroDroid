package com.abhishek.zerodroid.features.rf_bug_sweeper.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.BugDetection
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.BugSweepState
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.BugType
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.SweepMode
import com.abhishek.zerodroid.features.rf_bug_sweeper.domain.ThreatSeverity
import com.abhishek.zerodroid.features.rf_bug_sweeper.viewmodel.RfBugSweeperViewModel
import com.abhishek.zerodroid.ui.theme.SurfaceVariantDark
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun RfBugSweeperScreen(
    viewModel: RfBugSweeperViewModel = viewModel(factory = RfBugSweeperViewModel.Factory)
) {
    val requiredPermissions = remember {
        (PermissionUtils.blePermissions() + PermissionUtils.audioPermissions()).distinct()
    }

    PermissionGate(
        permissions = requiredPermissions,
        rationale = "Bluetooth and microphone permissions are needed to scan for RF bugs, " +
                "ultrasonic beacons, and suspicious wireless devices."
    ) {
        RfBugSweeperContent(viewModel)
    }
}

@Composable
private fun RfBugSweeperContent(viewModel: RfBugSweeperViewModel) {
    DisposableEffect(Unit) {
        onDispose { viewModel.stopSweep() }
    }

    val state by viewModel.state.collectAsState()

    var selectedModes by remember {
        mutableStateOf(setOf(SweepMode.BLE, SweepMode.ULTRASONIC, SweepMode.MAGNETIC))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. Sweep Controls
        item {
            SweepControls(
                state = state,
                selectedModes = selectedModes,
                onToggleMode = { mode ->
                    selectedModes = if (mode in selectedModes) {
                        selectedModes - mode
                    } else {
                        selectedModes + mode
                    }
                },
                onSweep = {
                    if (state.isSweeping) viewModel.stopSweep()
                    else viewModel.startSweep(selectedModes)
                },
                onClear = { viewModel.clearDetections() },
                onCalibrate = { viewModel.calibrateMagnetic() }
            )
        }

        // 2. Threat Summary
        if (state.detections.isNotEmpty() || state.isSweeping) {
            item { ThreatSummary(state) }
        }

        // 3. Live Meters
        if (state.isSweeping || state.detections.isNotEmpty()) {
            item { LiveMetersRow(state) }
        }

        // 4. Detection List
        if (state.detections.isEmpty() && !state.isSweeping) {
            item {
                EmptyState(
                    icon = Icons.Default.Shield,
                    title = "No threats detected",
                    subtitle = "Tap SWEEP to scan for RF bugs, ultrasonic beacons, and magnetic anomalies"
                )
            }
        }

        items(state.detections, key = { it.id }) { detection ->
            DetectionCard(detection)
        }

        // 5. Error
        state.error?.let { error ->
            item {
                TerminalCard(glowColor = TerminalRed, borderColor = TerminalRed) {
                    Text(
                        text = "> ERROR: $error",
                        color = TerminalRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // 6. Instructions
        item { InstructionsCard() }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ── Sweep Controls ─────────────────────────────────────────────────────

@Composable
private fun SweepControls(
    state: BugSweepState,
    selectedModes: Set<SweepMode>,
    onToggleMode: (SweepMode) -> Unit,
    onSweep: () -> Unit,
    onClear: () -> Unit,
    onCalibrate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onSweep,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isSweeping) TerminalRed else TerminalGreen
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (state.isSweeping) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isSweeping) "STOP" else "SWEEP",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (state.detections.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear detections",
                        tint = TextSecondary
                    )
                }
            }

            if (SweepMode.MAGNETIC in state.activeModes) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onCalibrate) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recalibrate magnetic baseline",
                        tint = TextSecondary
                    )
                }
            }
        }

        // Mode toggle chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SweepModeChip(
                label = "BLE",
                icon = Icons.Default.Bluetooth,
                selected = SweepMode.BLE in selectedModes,
                enabled = !state.isSweeping,
                onClick = { onToggleMode(SweepMode.BLE) }
            )
            SweepModeChip(
                label = "Ultrasonic",
                icon = Icons.Default.GraphicEq,
                selected = SweepMode.ULTRASONIC in selectedModes,
                enabled = !state.isSweeping,
                onClick = { onToggleMode(SweepMode.ULTRASONIC) }
            )
            SweepModeChip(
                label = "Magnetic",
                icon = Icons.Default.Explore,
                selected = SweepMode.MAGNETIC in selectedModes,
                enabled = !state.isSweeping,
                onClick = { onToggleMode(SweepMode.MAGNETIC) }
            )
        }

        // Duration label
        if (state.isSweeping) {
            val seconds = state.sweepDurationMs / 1000
            val mins = seconds / 60
            val secs = seconds % 60
            Text(
                text = "> Sweep active  ${String.format("%02d:%02d", mins, secs)}",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SweepModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TerminalGreen.copy(alpha = 0.15f),
            selectedLabelColor = TerminalGreen,
            selectedLeadingIconColor = TerminalGreen
        )
    )
}

// ── Threat Summary ─────────────────────────────────────────────────────

@Composable
private fun ThreatSummary(state: BugSweepState) {
    val critCount = state.detections.count { it.severity == ThreatSeverity.CRITICAL }
    val highCount = state.detections.count { it.severity == ThreatSeverity.HIGH }
    val medCount = state.detections.count { it.severity == ThreatSeverity.MEDIUM }
    val lowCount = state.detections.count { it.severity == ThreatSeverity.LOW }
    val total = state.detections.size

    val summaryColor = when {
        critCount > 0 -> TerminalRed
        highCount > 0 -> TerminalRed
        medCount > 0 -> TerminalAmber
        total > 0 -> TerminalCyan
        else -> TerminalGreen
    }

    val animatedColor by animateColorAsState(targetValue = summaryColor, label = "summaryColor")

    TerminalCard(
        glowColor = animatedColor,
        borderColor = animatedColor,
        animated = critCount > 0
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (total == 0) "> Sweeping..." else "> $total Detection${if (total != 1) "s" else ""}",
                    color = animatedColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (state.isSweeping) {
                    ScanningIndicator(isScanning = true, label = "", color = animatedColor)
                }
            }

            if (total > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (critCount > 0) SeverityBadge("CRIT: $critCount", TerminalRed)
                    if (highCount > 0) SeverityBadge("HIGH: $highCount", TerminalRed)
                    if (medCount > 0) SeverityBadge("MED: $medCount", TerminalAmber)
                    if (lowCount > 0) SeverityBadge("LOW: $lowCount", TerminalCyan)
                }
            }
        }
    }
}

@Composable
private fun SeverityBadge(label: String, color: Color) {
    Text(
        text = "[$label]",
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

// ── Live Meters Row ────────────────────────────────────────────────────

@Composable
private fun LiveMetersRow(state: BugSweepState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Magnetic field meter
        if (SweepMode.MAGNETIC in state.activeModes || state.magneticBaseline > 0f) {
            MagneticMeter(
                baseline = state.magneticBaseline,
                current = state.magneticCurrent,
                deviation = state.magneticDeviation
            )
        }

        // Ultrasonic indicator
        if (SweepMode.ULTRASONIC in state.activeModes) {
            UltrasonicMeter(detected = state.ultrasonicDetected)
        }

        // BLE count
        if (SweepMode.BLE in state.activeModes || state.bleDeviceCount > 0) {
            BleMeter(
                totalDevices = state.bleDeviceCount,
                suspiciousCount = state.detections.count {
                    it.type == BugType.SUSPICIOUS_BLE || it.type == BugType.RF_TRANSMITTER
                }
            )
        }
    }
}

@Composable
private fun MagneticMeter(baseline: Float, current: Float, deviation: Float) {
    val absDeviation = abs(deviation)
    val meterColor = when {
        absDeviation > 60f -> TerminalRed
        absDeviation > 25f -> TerminalAmber
        else -> TerminalGreen
    }

    Column(
        modifier = Modifier
            .width(120.dp)
            .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
            .border(1.dp, meterColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MAG FIELD",
            color = meterColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${String.format("%.1f", current)} uT",
            color = meterColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))

        // Color bar
        val barFraction = (absDeviation / 100f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(meterColor.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .height(4.dp)
                    .background(meterColor, RoundedCornerShape(2.dp))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Base: ${String.format("%.1f", baseline)}",
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
        )
        Text(
            text = "Dev: ${String.format("%.1f", absDeviation)}",
            color = meterColor.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun UltrasonicMeter(detected: Boolean) {
    val color = if (detected) TerminalRed else TerminalGreen

    Column(
        modifier = Modifier
            .width(120.dp)
            .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ULTRASONIC",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (detected) "BEACON\nDETECTED" else "SILENT",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = if (detected) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun BleMeter(totalDevices: Int, suspiciousCount: Int) {
    val color = when {
        suspiciousCount > 0 -> TerminalAmber
        totalDevices > 0 -> TerminalGreen
        else -> TextSecondary
    }

    Column(
        modifier = Modifier
            .width(120.dp)
            .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BLE DEVICES",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$totalDevices",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "in range",
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
        )
        if (suspiciousCount > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$suspiciousCount suspicious",
                color = TerminalAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Detection Card ─────────────────────────────────────────────────────

@Composable
private fun DetectionCard(detection: BugDetection) {
    val color = when (detection.severity) {
        ThreatSeverity.CRITICAL -> TerminalRed
        ThreatSeverity.HIGH -> TerminalRed
        ThreatSeverity.MEDIUM -> TerminalAmber
        ThreatSeverity.LOW -> TerminalCyan
    }

    val typeBadge = when (detection.type) {
        BugType.RF_TRANSMITTER -> "RF"
        BugType.ULTRASONIC_BEACON -> "ULTRA"
        BugType.MAGNETIC_ANOMALY -> "MAG"
        BugType.SUSPICIOUS_BLE -> "BLE"
        BugType.UNKNOWN -> "???"
    }

    TerminalCard(
        glowColor = color,
        borderColor = color,
        animated = detection.severity == ThreatSeverity.CRITICAL
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type badge
                    Text(
                        text = "[$typeBadge]",
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    // Title
                    Text(
                        text = detection.title,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                // Severity
                Text(
                    text = detection.severity.name,
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Detail
            Text(
                text = detection.detail,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )

            // Readings + timestamp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    detection.rssi?.let { rssi ->
                        SignalBar(rssi = rssi, color = color)
                    }
                    detection.frequency?.let { freq ->
                        Text(
                            text = "${String.format("%.0f", freq)} Hz",
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                    detection.fieldStrength?.let { field ->
                        Text(
                            text = "${String.format("%.1f", field)} uT",
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
                Text(
                    text = formatTime(detection.timestamp),
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun SignalBar(rssi: Int, color: Color) {
    val bars = when {
        rssi >= -50 -> 4
        rssi >= -65 -> 3
        rssi >= -75 -> 2
        else -> 1
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "${rssi}dBm",
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((4 + i * 3).dp)
                    .background(
                        if (i <= bars) color else color.copy(alpha = 0.2f),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

// ── Instructions Card ──────────────────────────────────────────────────

@Composable
private fun InstructionsCard() {
    TerminalCard {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TerminalCyan,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = "> Sweep Tips",
                    color = TerminalCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Slowly move your phone along walls, furniture, outlets, and vents. " +
                            "Watch for magnetic spikes and ultrasonic signals. " +
                            "Suspicious BLE devices broadcasting from cheap modules may " +
                            "indicate hidden transmitters.",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

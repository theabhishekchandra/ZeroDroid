package com.abhishek.zerodroid.features.hidden_camera.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.abhishek.zerodroid.features.hidden_camera.domain.CameraDetection
import com.abhishek.zerodroid.features.hidden_camera.domain.DetectionSource
import com.abhishek.zerodroid.features.hidden_camera.domain.HiddenCameraScanState
import com.abhishek.zerodroid.features.hidden_camera.domain.ThreatLevel
import com.abhishek.zerodroid.features.hidden_camera.viewmodel.HiddenCameraViewModel
import com.abhishek.zerodroid.ui.theme.SeverityCritical
import com.abhishek.zerodroid.ui.theme.SeverityHigh
import com.abhishek.zerodroid.ui.theme.SeverityInfo
import com.abhishek.zerodroid.ui.theme.SeverityLow
import com.abhishek.zerodroid.ui.theme.SeverityMedium
import com.abhishek.zerodroid.ui.theme.SurfaceVariantDark
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HiddenCameraScreen(
    viewModel: HiddenCameraViewModel = viewModel(factory = HiddenCameraViewModel.Factory)
) {
    PermissionGate(
        permissions = PermissionUtils.hiddenCameraPermissions(),
        rationale = "Camera, WiFi, Bluetooth, and location permissions are needed to detect hidden cameras using multiple sensor methods."
    ) {
        HiddenCameraContent(viewModel)
    }
}

@Composable
private fun HiddenCameraContent(viewModel: HiddenCameraViewModel) {
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScan()
            viewModel.stopIrMode()
        }
    }

    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. Scan Controls
        item { ScanControls(state, viewModel) }

        // 2. IR Camera Preview (when active)
        if (state.irActive) {
            item { IrCameraSection(viewModel) }
        }

        // 3. Threat Summary
        if (state.detections.isNotEmpty() || state.isScanning) {
            item { ThreatSummary(state) }
        }

        // 4. Mode Status Cards
        item { ModeStatusRow(state) }

        // 5. Detection Results
        if (state.detections.isEmpty() && !state.isScanning) {
            item {
                EmptyState(
                    icon = Icons.Default.Shield,
                    title = "No threats detected",
                    subtitle = "Tap SCAN to sweep for hidden cameras using WiFi, BLE, and magnetic sensors"
                )
            }
        }

        items(state.detections, key = { it.id }) { detection ->
            DetectionCard(detection)
        }

        // Error display
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

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ScanControls(state: HiddenCameraScanState, viewModel: HiddenCameraViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main scan button
            Button(
                onClick = { if (state.isScanning) viewModel.stopScan() else viewModel.startScan() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isScanning) TerminalRed else TerminalGreen
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (state.isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isScanning) "STOP" else "SCAN",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (state.detections.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.clearDetections() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear detections",
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
            ModeChip(
                label = "IR",
                icon = Icons.Default.CameraAlt,
                active = state.irActive,
                count = null,
                onClick = { if (state.irActive) viewModel.stopIrMode() else viewModel.startIrMode() }
            )
            ModeChip(
                label = "WiFi",
                icon = Icons.Default.Wifi,
                active = state.isScanning,
                count = state.wifiSuspects.takeIf { it > 0 }
            )
            ModeChip(
                label = "BLE",
                icon = Icons.Default.Bluetooth,
                active = state.isScanning,
                count = state.bleSuspects.takeIf { it > 0 }
            )
            ModeChip(
                label = "Magnetic",
                icon = Icons.Default.Explore,
                active = state.isScanning,
                count = if (state.magneticAnomaly) 1 else null
            )
            ModeChip(
                label = "Network",
                icon = Icons.Default.Lan,
                active = state.networkScanProgress != null,
                count = state.networkSuspects.takeIf { it > 0 },
                onClick = { viewModel.startNetworkScan() }
            )
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    count: Int?,
    onClick: (() -> Unit)? = null
) {
    FilterChip(
        selected = active,
        onClick = { onClick?.invoke() },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (count != null) "$label ($count)" else label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
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

@Composable
private fun IrCameraSection(viewModel: HiddenCameraViewModel) {
    TerminalCard(glowColor = TerminalCyan, borderColor = TerminalCyan) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "> IR Camera Mode",
                color = TerminalCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                IrCameraView(
                    onIrDetected = { detection -> viewModel.addIrDetection(detection) }
                )
            }
        }
    }
}

@Composable
private fun ThreatSummary(state: HiddenCameraScanState) {
    val highCount = state.detections.count { it.threatLevel == ThreatLevel.HIGH }
    val mediumCount = state.detections.count { it.threatLevel == ThreatLevel.MEDIUM }
    val lowCount = state.detections.count { it.threatLevel == ThreatLevel.LOW }
    val total = state.detections.size

    val summaryColor = when {
        highCount > 0 -> TerminalRed
        mediumCount > 0 -> TerminalAmber
        total > 0 -> TerminalCyan
        else -> TerminalGreen
    }

    val animatedColor by animateColorAsState(targetValue = summaryColor, label = "summaryColor")

    TerminalCard(
        glowColor = animatedColor,
        borderColor = animatedColor,
        animated = highCount > 0
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (total == 0) "> Scanning..." else "> $total Threat${if (total != 1) "s" else ""} Found",
                    color = animatedColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (state.isScanning) {
                    ScanningIndicator(isScanning = true, label = "", color = animatedColor)
                }
            }

            if (total > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (highCount > 0) ThreatBadge("HIGH: $highCount", TerminalRed)
                    if (mediumCount > 0) ThreatBadge("MED: $mediumCount", TerminalAmber)
                    if (lowCount > 0) ThreatBadge("LOW: $lowCount", TerminalCyan)
                }
            }

            // Network scan progress
            state.networkScanProgress?.let { progress ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = progress,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ThreatBadge(label: String, color: Color) {
    Text(
        text = "[$label]",
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ModeStatusRow(state: HiddenCameraScanState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModeStatusCard(
            label = "IR",
            status = if (state.irActive) "Active" else "Ready",
            isActive = state.irActive,
            color = if (state.irActive) TerminalCyan else TextSecondary
        )
        ModeStatusCard(
            label = "WiFi",
            status = if (state.isScanning) "${state.wifiSuspects} suspects" else "Idle",
            isActive = state.isScanning,
            color = if (state.wifiSuspects > 0) TerminalAmber else if (state.isScanning) TerminalGreen else TextSecondary
        )
        ModeStatusCard(
            label = "BLE",
            status = if (state.isScanning) "${state.bleSuspects} suspects" else "Idle",
            isActive = state.isScanning,
            color = if (state.bleSuspects > 0) TerminalAmber else if (state.isScanning) TerminalGreen else TextSecondary
        )
        ModeStatusCard(
            label = "MAG",
            status = if (state.magneticAnomaly) "ANOMALY" else if (state.isScanning) "Normal" else "Idle",
            isActive = state.isScanning,
            color = if (state.magneticAnomaly) TerminalRed else if (state.isScanning) TerminalGreen else TextSecondary
        )
        ModeStatusCard(
            label = "NET",
            status = state.networkScanProgress?.substringAfterLast(" ")
                ?: if (state.networkSuspects > 0) "${state.networkSuspects} found" else "Idle",
            isActive = state.networkScanProgress != null,
            color = if (state.networkSuspects > 0) TerminalAmber
                else if (state.networkScanProgress != null) TerminalGreen
                else TextSecondary
        )
    }
}

@Composable
private fun ModeStatusCard(label: String, status: String, isActive: Boolean, color: Color) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (isActive) color else TextSecondary.copy(alpha = 0.5f),
                        CircleShape
                    )
            )
            Text(
                text = label,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = status,
            color = color.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun DetectionCard(detection: CameraDetection) {
    val color = when (detection.threatLevel) {
        ThreatLevel.HIGH -> TerminalRed
        ThreatLevel.MEDIUM -> TerminalAmber
        ThreatLevel.LOW -> TerminalCyan
    }

    val sourceBadge = when (detection.source) {
        DetectionSource.IR -> "IR"
        DetectionSource.WIFI -> "WiFi"
        DetectionSource.BLE -> "BLE"
        DetectionSource.MAGNETIC -> "MAG"
        DetectionSource.NETWORK -> "NET"
    }

    TerminalCard(glowColor = color, borderColor = color) {
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
                    // Source badge
                    Text(
                        text = "[$sourceBadge]",
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
                        fontSize = 13.sp
                    )
                }
                // Threat level
                Text(
                    text = detection.threatLevel.name,
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

            // Signal strength + timestamp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                detection.rssi?.let { rssi ->
                    SignalBar(rssi = rssi, color = color)
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

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

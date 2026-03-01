package com.abhishek.zerodroid.features.gps_spoof_detector.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.abhishek.zerodroid.features.gps_spoof_detector.domain.GpsSpoofState
import com.abhishek.zerodroid.features.gps_spoof_detector.domain.SpoofCheck
import com.abhishek.zerodroid.features.gps_spoof_detector.domain.SpoofCheckResult
import com.abhishek.zerodroid.features.gps_spoof_detector.viewmodel.GpsSpoofViewModel
import com.abhishek.zerodroid.ui.theme.SurfaceVariantDark
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TextDim
import com.abhishek.zerodroid.ui.theme.TextSecondary

// ── Screen entry point ─────────────────────────────────────────────────────────

@Composable
fun GpsSpoofScreen(
    viewModel: GpsSpoofViewModel = viewModel(factory = GpsSpoofViewModel.Factory)
) {
    PermissionGate(
        permissions = PermissionUtils.gpsSpoofPermissions(),
        rationale = "GPS, phone, and WiFi permissions are needed to cross-reference location data for spoof detection."
    ) {
        GpsSpoofContent(viewModel)
    }
}

@Composable
private fun GpsSpoofContent(viewModel: GpsSpoofViewModel) {
    DisposableEffect(Unit) {
        onDispose { viewModel.stopMonitoring() }
    }

    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. Start/Stop control
        item { MonitorControls(state, viewModel) }

        // 2. Overall verdict card
        if (state.results.isNotEmpty()) {
            item { VerdictCard(state) }
        }

        // 3. Source status row
        if (state.isMonitoring) {
            item { SourceStatusRow(state) }
        }

        // 4. Latest check results
        val latestResult = state.results.firstOrNull()
        if (latestResult != null) {
            item {
                Text(
                    text = "> Individual Checks",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(latestResult.checks, key = { it.name }) { check ->
                CheckResultCard(check)
            }

            // 5. GPS info card
            item { GpsInfoCard(latestResult, state) }

            // 6. Cell tower info card
            item { CellInfoCard(latestResult, state) }

            // 7. Distance comparison
            item { DistanceCard(latestResult) }
        }

        // Empty state
        if (state.results.isEmpty() && !state.isMonitoring) {
            item {
                EmptyState(
                    icon = Icons.Default.GpsFixed,
                    title = "GPS Spoof Detector",
                    subtitle = "Tap MONITOR to start cross-referencing GPS, cell tower, WiFi, and sensor data for spoofing indicators"
                )
            }
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

// ── Monitor Controls ───────────────────────────────────────────────────────────

@Composable
private fun MonitorControls(state: GpsSpoofState, viewModel: GpsSpoofViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "> GPS Spoof Detector",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace
        )

        Button(
            onClick = {
                if (state.isMonitoring) viewModel.stopMonitoring() else viewModel.startMonitoring()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isMonitoring) TerminalRed else TerminalGreen
            )
        ) {
            Icon(
                imageVector = if (state.isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (state.isMonitoring) "STOP" else "MONITOR",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ── Verdict Card ───────────────────────────────────────────────────────────────

@Composable
private fun VerdictCard(state: GpsSpoofState) {
    val verdictColor = when {
        state.confidence > 0.5f -> TerminalRed
        state.confidence > 0.2f -> TerminalAmber
        else -> TerminalGreen
    }
    val verdictLabel = when {
        state.confidence > 0.5f -> "SPOOFED"
        state.confidence > 0.2f -> "SUSPICIOUS"
        else -> "GENUINE"
    }
    val animatedColor by animateColorAsState(targetValue = verdictColor, label = "verdict")

    TerminalCard(
        glowColor = animatedColor,
        borderColor = animatedColor,
        animated = state.confidence > 0.5f
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = verdictLabel,
                        color = animatedColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        text = "Spoof Confidence",
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "%.0f%%".format(state.confidence * 100),
                    color = animatedColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Confidence bar
            LinearProgressIndicator(
                progress = { state.confidence },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = animatedColor,
                trackColor = animatedColor.copy(alpha = 0.15f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Failed / passed summary
            val latest = state.results.firstOrNull()
            if (latest != null) {
                val passed = latest.checks.count { it.passed }
                val failed = latest.checks.count { !it.passed }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "[PASS: $passed]",
                        color = TerminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "[FAIL: $failed]",
                        color = if (failed > 0) TerminalRed else TextDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.isMonitoring) {
                Spacer(modifier = Modifier.height(6.dp))
                ScanningIndicator(
                    isScanning = true,
                    label = "Monitoring (5s intervals)",
                    color = animatedColor
                )
            }
        }
    }
}

// ── Source Status Row ──────────────────────────────────────────────────────────

@Composable
private fun SourceStatusRow(state: GpsSpoofState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SourceStatusCard("GPS", state.gpsStatus, modifier = Modifier.weight(1f))
        SourceStatusCard("CELL", state.cellStatus, modifier = Modifier.weight(1f))
        SourceStatusCard("WIFI", state.wifiStatus, modifier = Modifier.weight(1f))
        SourceStatusCard("SENS", state.sensorStatus, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SourceStatusCard(label: String, status: String, modifier: Modifier = Modifier) {
    val isActive = status != "Idle" && !status.startsWith("Error")
    val color = when {
        status.startsWith("Error") -> TerminalRed
        isActive -> TerminalGreen
        else -> TextSecondary
    }

    Column(
        modifier = modifier
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
            fontSize = 8.sp,
            maxLines = 1
        )
    }
}

// ── Check Result Card ──────────────────────────────────────────────────────────

@Composable
private fun CheckResultCard(check: SpoofCheck) {
    val color = if (check.passed) TerminalGreen else TerminalRed
    val statusLabel = if (check.passed) "PASS" else "FAIL"

    TerminalCard(glowColor = color, borderColor = color) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> ${check.name}",
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "[$statusLabel]",
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = check.detail,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}

// ── GPS Info Card ──────────────────────────────────────────────────────────────

@Composable
private fun GpsInfoCard(result: SpoofCheckResult, state: GpsSpoofState) {
    val gps = result.gpsLocation

    TerminalCard(glowColor = TerminalCyan, borderColor = TerminalCyan) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> GPS Data",
                color = TerminalCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (gps != null) {
                InfoRow("Latitude", "%.6f".format(gps.first))
                InfoRow("Longitude", "%.6f".format(gps.second))

                // Extract altitude/speed/satellite info from check details
                val altCheck = result.checks.find { it.name == "Altitude Consistency" }
                altCheck?.let {
                    val altMatch = Regex("GPS: (\\S+)").find(it.detail)
                    altMatch?.let { m -> InfoRow("Altitude", m.groupValues[1]) }
                }

                val speedCheck = result.checks.find { it.name == "Accelerometer Correlation" }
                speedCheck?.let {
                    val speedMatch = Regex("GPS speed: (\\S+ \\S+)").find(it.detail)
                        ?: Regex("GPS (\\S+)").find(it.detail)
                    speedMatch?.let { m -> InfoRow("Speed", m.groupValues[1]) }
                }

                val satCheck = result.checks.find { it.name == "Satellite Count" }
                satCheck?.let {
                    val satMatch = Regex("(\\d+) satellites").find(it.detail)
                    satMatch?.let { m -> InfoRow("Satellites", m.groupValues[1]) }
                }
            } else {
                Text(
                    text = "Waiting for GPS fix...",
                    color = TextDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ── Cell Info Card ─────────────────────────────────────────────────────────────

@Composable
private fun CellInfoCard(result: SpoofCheckResult, state: GpsSpoofState) {
    TerminalCard(glowColor = TerminalAmber, borderColor = TerminalAmber) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> Cell Tower Data",
                color = TerminalAmber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            InfoRow("Status", state.cellStatus)

            result.cellLocation?.let { cell ->
                InfoRow("Network Lat", "%.6f".format(cell.first))
                InfoRow("Network Lon", "%.6f".format(cell.second))
            }

            result.gpsVsCellDistanceKm?.let { dist ->
                InfoRow("GPS-Cell Gap", "%.2f km".format(dist))
            }
        }
    }
}

// ── Distance Comparison Card ───────────────────────────────────────────────────

@Composable
private fun DistanceCard(result: SpoofCheckResult) {
    val hasCellDist = result.gpsVsCellDistanceKm != null
    val hasWifiDist = result.gpsVsWifiDistanceKm != null

    if (!hasCellDist && !hasWifiDist) return

    TerminalCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> Distance Comparison",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (hasCellDist) {
                val cellDist = result.gpsVsCellDistanceKm!!
                val cellColor = when {
                    cellDist > 50.0 -> TerminalRed
                    cellDist > 10.0 -> TerminalAmber
                    else -> TerminalGreen
                }
                DistanceBar(
                    label = "GPS vs Cell",
                    distanceKm = cellDist,
                    thresholdKm = 50.0,
                    color = cellColor
                )
            }

            if (hasCellDist && hasWifiDist) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (hasWifiDist) {
                val wifiDist = result.gpsVsWifiDistanceKm!!
                val wifiColor = when {
                    wifiDist > 5.0 -> TerminalRed
                    wifiDist > 1.0 -> TerminalAmber
                    else -> TerminalGreen
                }
                DistanceBar(
                    label = "GPS vs WiFi",
                    distanceKm = wifiDist,
                    thresholdKm = 5.0,
                    color = wifiColor
                )
            }
        }
    }
}

@Composable
private fun DistanceBar(label: String, distanceKm: Double, thresholdKm: Double, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Text(
                text = "%.2f km".format(distanceKm),
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (distanceKm / thresholdKm).coerceIn(0.0, 1.0).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
        Text(
            text = "Threshold: %.0f km".format(thresholdKm),
            color = TextDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────────

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

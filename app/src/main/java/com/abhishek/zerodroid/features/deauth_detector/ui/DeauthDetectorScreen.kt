package com.abhishek.zerodroid.features.deauth_detector.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import com.abhishek.zerodroid.features.deauth_detector.domain.AlertLevel
import com.abhishek.zerodroid.features.deauth_detector.domain.AttackType
import com.abhishek.zerodroid.features.deauth_detector.domain.DeauthEvent
import com.abhishek.zerodroid.features.deauth_detector.domain.DeauthState
import com.abhishek.zerodroid.features.deauth_detector.viewmodel.DeauthDetectorViewModel
import com.abhishek.zerodroid.ui.theme.SeverityCritical
import com.abhishek.zerodroid.ui.theme.SeverityHigh
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
fun DeauthDetectorScreen(
    viewModel: DeauthDetectorViewModel = viewModel(factory = DeauthDetectorViewModel.Factory)
) {
    PermissionGate(
        permissions = PermissionUtils.wifiPermissions(),
        rationale = "Location permission is required by Android to monitor WiFi networks and detect deauthentication attacks."
    ) {
        DeauthDetectorContent(viewModel)
    }
}

@Composable
private fun DeauthDetectorContent(viewModel: DeauthDetectorViewModel) {
    DisposableEffect(Unit) {
        onDispose { viewModel.stopMonitoring() }
    }

    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. Status Banner
        item { StatusBanner(state) }

        // 2. Connection Info
        if (state.isMonitoring && state.connectedBssid != null) {
            item { ConnectionInfoCard(state) }
        }

        // 3. Stats Row
        if (state.isMonitoring || state.events.isNotEmpty()) {
            item { StatsRow(state) }
        }

        // 4. Start/Stop + Clear Controls
        item { MonitorControls(state, viewModel) }

        // 5. Timeline Visualization
        if (state.isMonitoring && state.disconnectCount > 0) {
            item {
                DisconnectTimeline(
                    disconnectTimestamps = viewModel.getDisconnectTimestamps(),
                    monitoringDurationMs = state.monitoringDurationMs
                )
            }
        }

        // 6. Empty State
        if (state.events.isEmpty() && !state.isMonitoring) {
            item {
                EmptyState(
                    icon = Icons.Default.Shield,
                    title = "Deauth Attack Detector",
                    subtitle = "Monitors WiFi for deauthentication attacks, signal jamming, and connection anomalies. Tap START to begin."
                )
            }
        }

        // 7. Event Log (newest first)
        items(state.events, key = { it.id }) { event ->
            DeauthEventCard(event)
        }

        // 8. Error Display
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

// ── Status Banner ────────────────────────────────────────────────────────────

@Composable
private fun StatusBanner(state: DeauthState) {
    val statusColor by animateColorAsState(
        targetValue = when {
            state.isUnderAttack -> TerminalRed
            state.isMonitoring -> TerminalGreen
            else -> TextSecondary
        },
        label = "statusColor"
    )

    val statusText = when {
        state.isUnderAttack -> "ATTACK DETECTED"
        state.isMonitoring -> "MONITORING"
        else -> "IDLE"
    }

    val statusDetail = when {
        state.isUnderAttack -> "Anomalous WiFi activity detected. Review events below."
        state.isMonitoring -> "Continuously scanning for deauth attacks and anomalies."
        else -> "Tap START to begin monitoring your WiFi connection."
    }

    val isAnimated = state.isUnderAttack || state.isMonitoring

    TerminalCard(
        glowColor = statusColor,
        borderColor = statusColor,
        animated = isAnimated
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pulsing status dot
                    StatusDot(color = statusColor, animate = isAnimated)

                    Text(
                        text = "> $statusText",
                        color = statusColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                if (state.isMonitoring) {
                    ScanningIndicator(
                        isScanning = true,
                        label = "",
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusDetail,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatusDot(color: Color, animate: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusDot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = if (animate) 0.3f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .background(
                color = color.copy(alpha = if (animate) alpha else 1f),
                shape = CircleShape
            )
    )
}

// ── Connection Info ──────────────────────────────────────────────────────────

@Composable
private fun ConnectionInfoCard(state: DeauthState) {
    TerminalCard(borderColor = TerminalCyan.copy(alpha = 0.3f)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> Connection Info",
                color = TerminalCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            DetailRow(label = "SSID", value = state.connectedSsid ?: "Unknown", color = TerminalCyan)
            DetailRow(label = "BSSID", value = state.connectedBssid ?: "Unknown", color = TextSecondary)
            DetailRow(
                label = "Signal",
                value = "${state.connectedRssi}dBm",
                color = when {
                    state.connectedRssi >= -50 -> TerminalGreen
                    state.connectedRssi >= -70 -> TerminalAmber
                    else -> TerminalRed
                }
            )
            if (state.connectedChannel > 0) {
                DetailRow(label = "Channel", value = "${state.connectedChannel}", color = TextSecondary)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            color = TextSecondary.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.width(64.dp)
        )
        Text(
            text = value,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}

// ── Stats Row ────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(state: DeauthState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            label = "DURATION",
            value = formatDuration(state.monitoringDurationMs),
            color = TerminalGreen
        )
        StatItem(
            label = "DISCONNECTS",
            value = "${state.disconnectCount}",
            color = if (state.disconnectCount > 0) TerminalAmber else TerminalGreen
        )
        StatItem(
            label = "EVENTS",
            value = "${state.events.size}",
            color = when {
                state.events.any { it.level == AlertLevel.CRITICAL } -> SeverityCritical
                state.events.isNotEmpty() -> TerminalAmber
                else -> TerminalGreen
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = TextSecondary.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
        )
    }
}

// ── Monitor Controls ─────────────────────────────────────────────────────────

@Composable
private fun MonitorControls(state: DeauthState, viewModel: DeauthDetectorViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                if (state.isMonitoring) viewModel.stopMonitoring()
                else viewModel.startMonitoring()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isMonitoring) TerminalRed else TerminalGreen
            ),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (state.isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.isMonitoring) "STOP" else "START",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (state.events.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { viewModel.clearEvents() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear events",
                    tint = TextSecondary
                )
            }
        }
    }
}

// ── Disconnect Timeline ──────────────────────────────────────────────────────

@Composable
private fun DisconnectTimeline(
    disconnectTimestamps: List<Long>,
    monitoringDurationMs: Long
) {
    TerminalCard(borderColor = TerminalAmber.copy(alpha = 0.3f)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> Disconnect Timeline (last 60s)",
                color = TerminalAmber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val now = remember(monitoringDurationMs) { System.currentTimeMillis() }
            val windowMs = 60_000L

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                val width = size.width
                val centerY = size.height / 2

                // Draw the time axis line
                drawLine(
                    color = TextSecondary.copy(alpha = 0.3f),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                )

                // Draw tick marks at 15-second intervals
                for (i in 0..4) {
                    val x = width * (i / 4f)
                    drawLine(
                        color = TextSecondary.copy(alpha = 0.2f),
                        start = Offset(x, centerY - 6f),
                        end = Offset(x, centerY + 6f),
                        strokeWidth = 1f
                    )
                }

                // Draw disconnect event dots
                for (ts in disconnectTimestamps) {
                    val age = now - ts
                    if (age > windowMs || age < 0) continue

                    val x = width * (1f - (age.toFloat() / windowMs))
                    // Outer glow
                    drawCircle(
                        color = TerminalRed.copy(alpha = 0.3f),
                        radius = 8f,
                        center = Offset(x, centerY)
                    )
                    // Inner dot
                    drawCircle(
                        color = TerminalRed,
                        radius = 4f,
                        center = Offset(x, centerY)
                    )
                }
            }

            // Time axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "-60s",
                    color = TextSecondary.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = "-45s",
                    color = TextSecondary.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = "-30s",
                    color = TextSecondary.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = "-15s",
                    color = TextSecondary.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = "now",
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ── Event Card ───────────────────────────────────────────────────────────────

@Composable
private fun DeauthEventCard(event: DeauthEvent) {
    val color = when (event.level) {
        AlertLevel.CRITICAL -> SeverityCritical
        AlertLevel.HIGH -> SeverityHigh
        AlertLevel.MEDIUM -> SeverityMedium
        AlertLevel.LOW -> SeverityLow
    }

    val isCritical = event.level == AlertLevel.CRITICAL

    TerminalCard(
        glowColor = color,
        borderColor = color,
        animated = isCritical
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Attack type badge + Alert level + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attack type badge
                    Text(
                        text = "[${event.type.label}]",
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    // Alert level
                    Text(
                        text = event.level.label,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Timestamp
                Text(
                    text = formatTime(event.timestamp),
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = "> ${event.title}",
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Affected network info
            event.affectedSsid?.let { ssid ->
                DetailRow(label = "SSID", value = ssid, color = color)
            }
            event.affectedBssid?.let { bssid ->
                DetailRow(label = "BSSID", value = bssid, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Detail description
            Text(
                text = event.detail,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}

// ── Utilities ────────────────────────────────────────────────────────────────

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

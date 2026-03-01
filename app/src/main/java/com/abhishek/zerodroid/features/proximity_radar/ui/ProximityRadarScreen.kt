package com.abhishek.zerodroid.features.proximity_radar.ui

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.proximity_radar.domain.DeviceCategory
import com.abhishek.zerodroid.features.proximity_radar.domain.RadarDevice
import com.abhishek.zerodroid.features.proximity_radar.domain.RadarState
import com.abhishek.zerodroid.features.proximity_radar.viewmodel.ProximityRadarViewModel
import com.abhishek.zerodroid.ui.theme.BackgroundDark
import com.abhishek.zerodroid.ui.theme.SurfaceDark
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalGreenDim
import com.abhishek.zerodroid.ui.theme.TextDim
import com.abhishek.zerodroid.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

@Composable
fun ProximityRadarScreen(
    viewModel: ProximityRadarViewModel = viewModel(factory = ProximityRadarViewModel.Factory)
) {
    val combinedPermissions = (PermissionUtils.blePermissions() + PermissionUtils.wifiPermissions()).distinct()

    PermissionGate(
        permissions = combinedPermissions,
        rationale = "Bluetooth and WiFi permissions are needed to detect nearby wireless devices."
    ) {
        ProximityRadarContent(viewModel = viewModel)
    }
}

// ---------------------------------------------------------------------------
// Main content
// ---------------------------------------------------------------------------

@Composable
private fun ProximityRadarContent(viewModel: ProximityRadarViewModel) {
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
        // -- Top controls --------------------------------------------------------
        item {
            Spacer(modifier = Modifier.height(4.dp))
            ScanControlBar(
                state = state,
                onToggleScan = { viewModel.toggleScan() }
            )
        }

        // -- Error ---------------------------------------------------------------
        state.error?.let { error ->
            item {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // -- Radar ---------------------------------------------------------------
        item {
            RadarView(state = state)
        }

        // -- Device count summary ------------------------------------------------
        item {
            DeviceCountSummary(state = state)
        }

        // -- Nearest device highlight --------------------------------------------
        state.nearestDevice?.let { nearest ->
            item {
                NearestDeviceCard(device = nearest)
            }
        }

        // -- Empty state ---------------------------------------------------------
        if (state.devices.isEmpty() && !state.isScanning) {
            item {
                EmptyState(
                    icon = Icons.Default.Radar,
                    title = "No devices detected",
                    subtitle = "Tap Scan to search for nearby WiFi and Bluetooth devices"
                )
            }
        }

        // -- Device list ---------------------------------------------------------
        items(state.devices, key = { it.id }) { device ->
            DeviceListItem(
                device = device,
                isNearest = device.id == state.nearestDevice?.id
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ---------------------------------------------------------------------------
// Scan control bar
// ---------------------------------------------------------------------------

@Composable
private fun ScanControlBar(state: RadarState, onToggleScan: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isScanning) {
            ScanningIndicator(
                isScanning = true,
                label = "${state.devices.size} devices detected"
            )
        } else {
            Text(
                text = "> ${state.devices.size} devices detected",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (state.isScanning) {
            OutlinedButton(
                onClick = onToggleScan,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Stop", fontFamily = FontFamily.Monospace)
            }
        } else {
            Button(
                onClick = onToggleScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Scan", fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Radar Canvas view
// ---------------------------------------------------------------------------

@Composable
private fun RadarView(state: RadarState) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    // Pulsing scale for the nearest device dot
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val textMeasurer = rememberTextMeasurer()

    TerminalCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(BackgroundDark, shape = RoundedCornerShape(8.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val canvasCenter = center
                val radius = min(size.width, size.height) / 2f
                val scanRadius = state.scanRadius

                // -- Background circle -------------------------------------------
                drawCircle(
                    color = SurfaceDark,
                    radius = radius,
                    center = canvasCenter
                )

                // -- Concentric range rings --------------------------------------
                val ringFractions = listOf(0.33f, 0.66f, 1f)
                ringFractions.forEach { fraction ->
                    drawCircle(
                        color = TerminalGreen.copy(alpha = 0.15f),
                        radius = radius * fraction,
                        center = canvasCenter,
                        style = Stroke(width = 1f)
                    )
                }

                // -- Range ring distance labels ----------------------------------
                ringFractions.forEach { fraction ->
                    val distanceLabel = "${(scanRadius * fraction).toInt()}m"
                    val labelStyle = TextStyle(
                        color = TerminalGreenDim.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    val measured = textMeasurer.measure(distanceLabel, labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            canvasCenter.x + 4f,
                            canvasCenter.y - radius * fraction - measured.size.height
                        )
                    )
                }

                // -- Crosshair lines ---------------------------------------------
                val crosshairColor = TerminalGreen.copy(alpha = 0.1f)
                drawLine(
                    color = crosshairColor,
                    start = Offset(canvasCenter.x - radius, canvasCenter.y),
                    end = Offset(canvasCenter.x + radius, canvasCenter.y),
                    strokeWidth = 1f
                )
                drawLine(
                    color = crosshairColor,
                    start = Offset(canvasCenter.x, canvasCenter.y - radius),
                    end = Offset(canvasCenter.x, canvasCenter.y + radius),
                    strokeWidth = 1f
                )

                // -- Sweep line with fading trail --------------------------------
                if (state.isScanning) {
                    // Trail arc
                    rotate(degrees = sweepAngle - 30f, pivot = canvasCenter) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.7f to Color.Transparent,
                                    1f to TerminalGreen.copy(alpha = 0.3f)
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 30f,
                            useCenter = true,
                            topLeft = Offset(canvasCenter.x - radius, canvasCenter.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                    }

                    // Sweep line
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    drawLine(
                        color = TerminalGreen.copy(alpha = 0.8f),
                        start = canvasCenter,
                        end = Offset(
                            canvasCenter.x + radius * cos(sweepRad).toFloat(),
                            canvasCenter.y + radius * sin(sweepRad).toFloat()
                        ),
                        strokeWidth = 2f
                    )
                }

                // -- Device dots -------------------------------------------------
                state.devices.forEach { device ->
                    val distanceFraction = (device.estimatedDistanceM / scanRadius).coerceIn(0f, 1f)
                    val deviceRadius = radius * distanceFraction
                    val angleRad = Math.toRadians(device.angle.toDouble())
                    val dotX = canvasCenter.x + deviceRadius * cos(angleRad).toFloat()
                    val dotY = canvasCenter.y + deviceRadius * sin(angleRad).toFloat()
                    val isNearest = device.id == state.nearestDevice?.id

                    val dotColor = when {
                        device.estimatedDistanceM < 2f -> TerminalAmber
                        device.category == DeviceCategory.WIFI_AP -> TerminalCyan
                        else -> TerminalGreen
                    }

                    when (device.category) {
                        DeviceCategory.WIFI_AP -> {
                            // WiFi APs: small cyan squares
                            val halfSide = if (isNearest) 6f * pulseScale else 5f
                            drawRect(
                                color = dotColor,
                                topLeft = Offset(dotX - halfSide, dotY - halfSide),
                                size = Size(halfSide * 2, halfSide * 2)
                            )
                            if (isNearest) {
                                drawRect(
                                    color = dotColor.copy(alpha = 0.3f),
                                    topLeft = Offset(dotX - halfSide - 3f, dotY - halfSide - 3f),
                                    size = Size((halfSide + 3f) * 2, (halfSide + 3f) * 2),
                                    style = Stroke(width = 1.5f)
                                )
                            }
                        }
                        else -> {
                            // BLE devices / Beacons: small green circles
                            val dotRadius = if (isNearest) 6f * pulseScale else 4f
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(dotX, dotY)
                            )
                            if (isNearest) {
                                drawCircle(
                                    color = dotColor.copy(alpha = 0.3f),
                                    radius = dotRadius + 4f,
                                    center = Offset(dotX, dotY),
                                    style = Stroke(width = 1.5f)
                                )
                            }
                        }
                    }
                }

                // -- Center "YOU" label ------------------------------------------
                val youStyle = TextStyle(
                    color = TerminalGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                val youMeasured = textMeasurer.measure("YOU", youStyle)
                drawText(
                    textLayoutResult = youMeasured,
                    topLeft = Offset(
                        canvasCenter.x - youMeasured.size.width / 2f,
                        canvasCenter.y - youMeasured.size.height / 2f
                    )
                )

                // Center dot
                drawCircle(
                    color = TerminalGreen,
                    radius = 3f,
                    center = canvasCenter
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Device count summary
// ---------------------------------------------------------------------------

@Composable
private fun DeviceCountSummary(state: RadarState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CountBadge(label = "WiFi", count = state.wifiCount, color = TerminalCyan)
        CountBadge(label = "BLE", count = state.bleCount, color = TerminalGreen)
        CountBadge(label = "Total", count = state.devices.size, color = TerminalAmber)
    }
}

@Composable
private fun CountBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = TextSecondary
        )
    }
}

// ---------------------------------------------------------------------------
// Nearest device highlight card
// ---------------------------------------------------------------------------

@Composable
private fun NearestDeviceCard(device: RadarDevice) {
    val distColor = if (device.estimatedDistanceM < 2f) TerminalAmber else TerminalGreen

    TerminalCard(animated = true, glowColor = distColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "> NEAREST DEVICE",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = distColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "~${"%.1f".format(device.estimatedDistanceM)}m",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = distColor
                )
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextSecondary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Device list item
// ---------------------------------------------------------------------------

@Composable
private fun DeviceListItem(device: RadarDevice, isNearest: Boolean) {
    val categoryColor = when (device.category) {
        DeviceCategory.WIFI_AP -> TerminalCyan
        DeviceCategory.BLE_BEACON -> TerminalAmber
        else -> TerminalGreen
    }
    val distColor = if (device.estimatedDistanceM < 2f) TerminalAmber else categoryColor
    val categoryLabel = when (device.category) {
        DeviceCategory.WIFI_AP -> "WiFi"
        DeviceCategory.BLE_DEVICE -> "BLE"
        DeviceCategory.BLE_BEACON -> "Beacon"
        DeviceCategory.UNKNOWN -> "???"
    }

    TerminalCard(
        glowColor = if (isNearest) distColor else TerminalGreen,
        animated = isNearest
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category badge
            Box(
                modifier = Modifier
                    .background(categoryColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = categoryColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Name + ID
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = device.id,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextDim,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Distance + RSSI
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "~${"%.1f".format(device.estimatedDistanceM)}m",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = distColor
                )
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Signal strength bar
            SignalBar(percent = device.signalPercent, color = categoryColor)
        }
    }
}

// ---------------------------------------------------------------------------
// Signal strength bar
// ---------------------------------------------------------------------------

@Composable
private fun SignalBar(percent: Int, color: Color) {
    val barCount = 5
    val filledBars = (percent / 20).coerceIn(0, barCount)

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(barCount) { index ->
            val barHeight = (6 + index * 4).dp
            val isFilled = index < filledBars
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .background(
                        if (isFilled) color else color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

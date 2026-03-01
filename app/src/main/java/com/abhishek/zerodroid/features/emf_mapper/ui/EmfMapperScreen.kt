package com.abhishek.zerodroid.features.emf_mapper.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.emf_mapper.domain.EmfLevel
import com.abhishek.zerodroid.features.emf_mapper.domain.EmfMapperState
import com.abhishek.zerodroid.features.emf_mapper.domain.EmfReading
import com.abhishek.zerodroid.features.emf_mapper.viewmodel.EmfMapperViewModel
import com.abhishek.zerodroid.ui.theme.BackgroundDark
import com.abhishek.zerodroid.ui.theme.SurfaceVariantDark
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TextPrimary
import com.abhishek.zerodroid.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EmfMapperScreen(
    viewModel: EmfMapperViewModel = viewModel(factory = EmfMapperViewModel.Factory)
) {
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopRecording()
        }
    }

    val state by viewModel.state.collectAsState()

    if (!state.sensorAvailable) {
        SensorUnavailableContent()
        return
    }

    EmfMapperContent(state, viewModel)
}

@Composable
private fun SensorUnavailableContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyState(
            icon = Icons.Default.SensorsOff,
            title = "Magnetometer unavailable",
            subtitle = "This device does not have a magnetometer sensor required for EMF mapping"
        )
    }
}

@Composable
private fun EmfMapperContent(state: EmfMapperState, viewModel: EmfMapperViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. Live EMF Gauge
        item { EmfGauge(state.currentReading) }

        // 2. Recording timer
        if (state.isRecording) {
            item { RecordingTimer(state.recordingDurationMs) }
        }

        // 3. Record controls
        item { RecordControls(state, viewModel) }

        // 4. Statistics card
        if (state.currentReading != null) {
            item { StatisticsCard(state) }
        }

        // 5. 3-axis readout
        if (state.currentReading != null) {
            item { AxisReadout(state.currentReading!!) }
        }

        // 6. History graph
        if (state.history.isNotEmpty()) {
            item { HistoryGraph(state.history) }
        }

        // 7. Instructions
        if (!state.isRecording && state.history.isEmpty()) {
            item { InstructionsCard() }
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

// ---------------------------------------------------------------------------
// EMF Gauge (semicircular speedometer)
// ---------------------------------------------------------------------------

@Composable
private fun EmfGauge(reading: EmfReading?) {
    val magnitude = reading?.magnitude ?: 0f
    val level = reading?.level ?: EmfLevel.NORMAL
    val levelColor = levelColor(level)

    TerminalCard(
        glowColor = levelColor,
        borderColor = levelColor,
        animated = level == EmfLevel.HIGH || level == EmfLevel.EXTREME
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val textMeasurer = rememberTextMeasurer()

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .aspectRatio(2f)
            ) {
                val strokeWidth = 20.dp.toPx()
                val arcPadding = strokeWidth / 2 + 8.dp.toPx()
                val arcSize = Size(
                    width = size.width - arcPadding * 2,
                    height = (size.width - arcPadding * 2)
                )
                val arcTopLeft = Offset(arcPadding, size.height - arcSize.height / 2)

                // Background arc
                drawArc(
                    color = SurfaceVariantDark,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Green zone: 0-65 uT -> 0 to 58.5 degrees (65/200 * 180)
                drawArc(
                    color = TerminalGreen.copy(alpha = 0.3f),
                    startAngle = 180f,
                    sweepAngle = 58.5f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )

                // Amber zone: 65-105 uT -> 58.5 to 94.5 degrees (40/200 * 180 = 36)
                drawArc(
                    color = TerminalAmber.copy(alpha = 0.3f),
                    startAngle = 238.5f,
                    sweepAngle = 36f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )

                // Red zone: 105-200 uT -> 94.5 to 180 degrees (95/200 * 180 = 85.5)
                drawArc(
                    color = TerminalRed.copy(alpha = 0.3f),
                    startAngle = 274.5f,
                    sweepAngle = 85.5f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Needle
                val clampedMag = magnitude.coerceIn(0f, 200f)
                val needleAngleDeg = 180f + (clampedMag / 200f) * 180f
                val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
                val needleRadius = arcSize.width / 2 - 4.dp.toPx()
                val centerX = arcTopLeft.x + arcSize.width / 2
                val centerY = arcTopLeft.y + arcSize.height / 2
                val needleEnd = Offset(
                    x = centerX + (needleRadius * cos(needleAngleRad)).toFloat(),
                    y = centerY + (needleRadius * sin(needleAngleRad)).toFloat()
                )
                val needleColor = when {
                    clampedMag > 105f -> TerminalRed
                    clampedMag > 65f -> TerminalAmber
                    else -> TerminalGreen
                }

                // Needle line
                drawLine(
                    color = needleColor,
                    start = Offset(centerX, centerY),
                    end = needleEnd,
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )

                // Center dot
                drawCircle(
                    color = needleColor,
                    radius = 6.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = BackgroundDark,
                    radius = 3.dp.toPx(),
                    center = Offset(centerX, centerY)
                )

                // Scale labels: 0, 50, 100, 150, 200
                val labelRadius = arcSize.width / 2 + 14.dp.toPx()
                val scaleValues = listOf(0, 50, 100, 150, 200)
                for (value in scaleValues) {
                    val angleDeg = 180f + (value.toFloat() / 200f) * 180f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val labelX = centerX + (labelRadius * cos(angleRad)).toFloat()
                    val labelY = centerY + (labelRadius * sin(angleRad)).toFloat()

                    val textLayout = textMeasurer.measure(
                        text = value.toString(),
                        style = TextStyle(
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(
                            x = labelX - textLayout.size.width / 2,
                            y = labelY - textLayout.size.height / 2
                        )
                    )
                }

                // Tick marks
                for (i in 0..20) {
                    val tickValue = i * 10f
                    val tickAngleDeg = 180f + (tickValue / 200f) * 180f
                    val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())
                    val isMajor = i % 5 == 0
                    val tickInner = arcSize.width / 2 - strokeWidth / 2 - (if (isMajor) 10.dp.toPx() else 5.dp.toPx())
                    val tickOuter = arcSize.width / 2 - strokeWidth / 2
                    drawLine(
                        color = TextSecondary.copy(alpha = if (isMajor) 0.8f else 0.4f),
                        start = Offset(
                            x = centerX + (tickInner * cos(tickAngleRad)).toFloat(),
                            y = centerY + (tickInner * sin(tickAngleRad)).toFloat()
                        ),
                        end = Offset(
                            x = centerX + (tickOuter * cos(tickAngleRad)).toFloat(),
                            y = centerY + (tickOuter * sin(tickAngleRad)).toFloat()
                        ),
                        strokeWidth = if (isMajor) 2f else 1f
                    )
                }
            }

            // Digital readout
            Text(
                text = "%.1f".format(magnitude) + " \u00B5T",
                color = levelColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Level label
            Text(
                text = level.name,
                color = levelColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .background(levelColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Recording timer
// ---------------------------------------------------------------------------

@Composable
private fun RecordingTimer(durationMs: Long) {
    val totalSeconds = (durationMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeStr = "%02d:%02d".format(minutes, seconds)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScanningIndicator(isScanning = true, label = "", color = TerminalRed)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Recording: $timeStr",
            color = TerminalRed,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Record controls
// ---------------------------------------------------------------------------

@Composable
private fun RecordControls(state: EmfMapperState, viewModel: EmfMapperViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Start/Stop button
        Button(
            onClick = {
                if (state.isRecording) viewModel.stopRecording() else viewModel.startRecording()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isRecording) TerminalRed else TerminalGreen
            ),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (state.isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.isRecording) "STOP" else "RECORD",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Reset baseline
        OutlinedButton(
            onClick = { viewModel.resetBaseline() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalAmber)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset baseline",
                tint = TerminalAmber
            )
        }

        // Clear history
        if (state.history.isNotEmpty()) {
            IconButton(onClick = { viewModel.clearHistory() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear history",
                    tint = TextSecondary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Statistics card
// ---------------------------------------------------------------------------

@Composable
private fun StatisticsCard(state: EmfMapperState) {
    TerminalCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> Statistics",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: Baseline | Current | Peak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "BASELINE", value = "%.1f".format(state.baseline), unit = "\u00B5T")
                StatItem(
                    label = "CURRENT",
                    value = "%.1f".format(state.currentReading?.magnitude ?: 0f),
                    unit = "\u00B5T",
                    valueColor = levelColor(state.currentReading?.level ?: EmfLevel.NORMAL)
                )
                StatItem(label = "PEAK", value = "%.1f".format(state.peakMagnitude), unit = "\u00B5T", valueColor = TerminalRed)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Min | Avg | Hotspots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "MIN",
                    value = if (state.minMagnitude == Float.MAX_VALUE) "--" else "%.1f".format(state.minMagnitude),
                    unit = "\u00B5T"
                )
                StatItem(label = "AVG", value = "%.1f".format(state.avgMagnitude), unit = "\u00B5T")
                StatItem(
                    label = "HOTSPOTS",
                    value = state.hotspots.toString(),
                    unit = "",
                    valueColor = if (state.hotspots > 0) TerminalRed else TerminalGreen
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    unit: String,
    valueColor: Color = TextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = valueColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3-axis readout
// ---------------------------------------------------------------------------

@Composable
private fun AxisReadout(reading: EmfReading) {
    TerminalCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> 3-Axis Readout",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            AxisBar(label = "X", value = reading.x, maxValue = 200f, color = TerminalRed)
            Spacer(modifier = Modifier.height(6.dp))
            AxisBar(label = "Y", value = reading.y, maxValue = 200f, color = TerminalGreen)
            Spacer(modifier = Modifier.height(6.dp))
            AxisBar(label = "Z", value = reading.z, maxValue = 200f, color = TerminalAmber)
        }
    }
}

@Composable
private fun AxisBar(label: String, value: Float, maxValue: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.width(16.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .background(SurfaceVariantDark, RoundedCornerShape(2.dp))
        ) {
            val fraction = (kotlin.math.abs(value) / maxValue).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(12.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.5f), color)
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
        }

        Text(
            text = "%+.1f".format(value),
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.width(52.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// History graph (time series)
// ---------------------------------------------------------------------------

@Composable
private fun HistoryGraph(history: List<EmfReading>) {
    TerminalCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> History (last 30s)",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val maxMag = (history.maxOfOrNull { it.magnitude } ?: 100f).coerceAtLeast(80f)
                val w = size.width
                val h = size.height

                // Grid lines (horizontal)
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = h * i / gridLines
                    drawLine(
                        color = SurfaceVariantDark,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // ELEVATED threshold line (deviation >15 from typical ~45uT baseline)
                val elevatedThreshold = 60f // approximate elevated threshold
                if (elevatedThreshold < maxMag) {
                    val elevatedY = h - (elevatedThreshold / maxMag) * h
                    drawLine(
                        color = TerminalAmber.copy(alpha = 0.5f),
                        start = Offset(0f, elevatedY),
                        end = Offset(w, elevatedY),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                }

                // HIGH threshold line (deviation >40 from baseline ~45uT)
                val highThreshold = 85f // approximate high threshold
                if (highThreshold < maxMag) {
                    val highY = h - (highThreshold / maxMag) * h
                    drawLine(
                        color = TerminalRed.copy(alpha = 0.5f),
                        start = Offset(0f, highY),
                        end = Offset(w, highY),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                }

                if (history.size >= 2) {
                    // Build path
                    val linePath = Path()
                    val fillPath = Path()

                    history.forEachIndexed { i, reading ->
                        val x = (i.toFloat() / (history.size - 1)) * w
                        val y = h - (reading.magnitude / maxMag) * h

                        if (i == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }

                    // Close fill path
                    fillPath.lineTo(w, h)
                    fillPath.close()

                    // Area fill with gradient
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TerminalGreen.copy(alpha = 0.2f),
                                TerminalGreen.copy(alpha = 0.02f)
                            )
                        )
                    )

                    // Line
                    drawPath(
                        path = linePath,
                        color = TerminalGreen,
                        style = Stroke(width = 2f, cap = StrokeCap.Round)
                    )
                }
            }

            // X-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "-30s",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = "-20s",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = "-10s",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
                Text(
                    text = "now",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Instructions card
// ---------------------------------------------------------------------------

@Composable
private fun InstructionsCard() {
    TerminalCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> Instructions",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Slowly move your phone near walls, outlets, and objects. Spikes indicate electronic devices, wiring, or magnetic sources.",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LegendItem(color = TerminalGreen, label = "NORMAL", desc = "25-65 \u00B5T (Earth's field)")
                LegendItem(color = TerminalAmber, label = "ELEVATED", desc = ">15 \u00B5T deviation")
                LegendItem(color = TerminalRed, label = "HIGH", desc = ">40 \u00B5T deviation")
                LegendItem(color = TerminalRed, label = "EXTREME", desc = ">100 \u00B5T deviation")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, desc: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = "$label:",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Text(
            text = desc,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun levelColor(level: EmfLevel): Color {
    return when (level) {
        EmfLevel.NORMAL -> TerminalGreen
        EmfLevel.ELEVATED -> TerminalAmber
        EmfLevel.HIGH -> TerminalRed
        EmfLevel.EXTREME -> TerminalRed
    }
}

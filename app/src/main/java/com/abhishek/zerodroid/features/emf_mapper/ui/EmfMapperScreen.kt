package com.abhishek.zerodroid.features.emf_mapper.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
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
import androidx.compose.foundation.layout.PaddingValues
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdCheckRow
import com.abhishek.zerodroid.core.ui.zd.ZdCheckStatus
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdMetric
import com.abhishek.zerodroid.core.ui.zd.ZdScanControlBar
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.formatElapsed
import com.abhishek.zerodroid.ui.theme.JetBrainsMono
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.util.Locale

@Composable
fun EmfMapperScreen(
    viewModel: EmfMapperViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    HardwareLifecycleEffect(
        isActive = state.isRecording,
        onPause = viewModel::stopRecording,
        onResume = viewModel::startRecording
    )

    if (!state.sensorAvailable) {
        SensorUnavailableContent()
        return
    }

    EmfMapperContent(state, viewModel)
}

@Composable
private fun SensorUnavailableContent() {
    ZdStatePanel(
        kicker = "Not on this phone",
        icon = ZdIcons.Magnet,
        title = "Your phone has no magnetometer",
        body = "EMF mapping reads the magnetic field sensor, the same one the compass uses. This phone doesn’t report one."
    )
}

@Composable
private fun EmfMapperContent(state: EmfMapperState, viewModel: EmfMapperViewModel) {
    Column(Modifier.fillMaxSize()) {
        ZdScanControlBar(
            running = state.isRecording,
            onStart = viewModel::startRecording,
            onStop = viewModel::stopRecording,
            runningLabel = "Mapping · ${formatElapsed(state.recordingDurationMs)}",
            runningDetail = "${state.history.size} readings · ${state.hotspots} hotspots",
            idleDetail = if (state.history.isEmpty()) "Start away from metal to set a baseline" else "Readings kept",
            startLabel = "Record"
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { EmfGauge(state.currentReading) }
            if (state.currentReading != null) {
                item {
                    ZdCard {
                        Row {
                            ZdMetric(if (state.minMagnitude == Float.MAX_VALUE) "—" else "%.1f".format(Locale.US, state.minMagnitude), "Min µT", Modifier.weight(1f))
                            ZdMetric("%.1f".format(Locale.US, state.peakMagnitude), "Max µT", Modifier.weight(1f))
                            ZdMetric("%.1f".format(Locale.US, state.avgMagnitude), "Avg µT", Modifier.weight(1f))
                            ZdMetric("${state.hotspots}", "Hotspots", Modifier.weight(1f), valueColor = if (state.hotspots > 0) ZdColors.Medium else ZdColors.Text)
                        }
                        Text("Baseline %.1f µT".format(Locale.US, state.baseline), style = ZdType.Caption, color = ZdColors.Text3)
                    }
                }
                item { AxisReadout(state.currentReading) }
            }
            if (state.history.isNotEmpty()) {
                item { HistoryGraph(state.history) }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZdButton("Re-zero", onClick = viewModel::resetBaseline, variant = ZdButtonVariant.Secondary, icon = ZdIcons.Refresh, modifier = Modifier.weight(1f))
                    ZdButton("Clear", onClick = viewModel::clearHistory, variant = ZdButtonVariant.Ghost, icon = ZdIcons.Trash, enabled = state.history.isNotEmpty(), modifier = Modifier.weight(1f))
                }
            }
            item { ZdSectionLabel("Reading the field") }
            item {
                ZdListCard(
                    listOf(
                        Triple("Normal", "25–65 µT, Earth’s own field", ZdCheckStatus.PASS),
                        Triple("Elevated", "More than 15 µT above baseline: nearby electronics", ZdCheckStatus.WARN),
                        Triple("High", "More than 40 µT above: wiring, motors, speakers", ZdCheckStatus.WARN),
                        Triple("Extreme", "More than 100 µT above: a magnet very close", ZdCheckStatus.FAIL)
                    )
                ) { (title, detail, status) -> ZdCheckRow(title = title, detail = detail, status = status) }
            }
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
            item { ZdFootnote("Move the top of the phone slowly along walls and objects: the magnetometer sits near the top edge on most phones.") }
        }
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

    ZdCard(
        borderColor = if (level == EmfLevel.NORMAL) ZdColors.Border else levelColor.copy(alpha = 0.5f),
        verticalSpacing = 0.dp
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
                val isOverflow = magnitude > 200f
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

                // Overflow marker: the needle pins at the 200uT stop for anything above it, so
                // without this a 250uT reading and a 5000uT reading look identical on the dial --
                // draw an outward arrowhead past the red zone's end whenever that's happening.
                if (isOverflow) {
                    val tipRadius = arcSize.width / 2 + strokeWidth / 2 + 4.dp.toPx()
                    val tipX = centerX + tipRadius
                    val arrowSize = 8.dp.toPx()
                    val arrowPath = Path().apply {
                        moveTo(tipX, centerY)
                        lineTo(tipX + arrowSize, centerY - arrowSize / 2)
                        lineTo(tipX + arrowSize, centerY + arrowSize / 2)
                        close()
                    }
                    drawPath(path = arrowPath, color = TerminalRed)
                }

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
                            fontFamily = JetBrainsMono
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
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Level label
            Text(
                text = level.name,
                color = levelColor,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .background(levelColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            if (magnitude > 200f) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\u25B6 OFF SCALE \u2014 dial maxes at 200\u00B5T",
                    color = TerminalRed,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp
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
    ZdCard(verticalSpacing = 0.dp) {
        Column {
            Text("3-axis readout · µT", style = ZdType.Label, color = ZdColors.Text2)

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
            fontFamily = JetBrainsMono,
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
            fontFamily = JetBrainsMono,
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
    ZdCard(verticalSpacing = 0.dp) {
        Column {
            Text("Last 30 seconds", style = ZdType.Label, color = ZdColors.Text2)

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
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp
                )
                Text(
                    text = "-20s",
                    color = TextSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp
                )
                Text(
                    text = "-10s",
                    color = TextSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp
                )
                Text(
                    text = "now",
                    color = TextSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun levelColor(level: EmfLevel): Color {
    return when (level) {
        EmfLevel.NORMAL -> TerminalGreen
        EmfLevel.ELEVATED -> TerminalAmber
        EmfLevel.HIGH -> ZdColors.High
        EmfLevel.EXTREME -> TerminalRed
    }
}

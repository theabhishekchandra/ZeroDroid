package com.abhishek.zerodroid.features.sensors.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.sensors.domain.VibrationSeverity
import com.abhishek.zerodroid.features.sensors.domain.VibrationState
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import java.util.Locale

@Composable
fun VibrationDetectorView(
    state: VibrationState,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val severityColor = when (state.severity) {
        VibrationSeverity.NONE -> TerminalGreen
        VibrationSeverity.LOW -> TerminalGreen
        VibrationSeverity.MODERATE -> TerminalAmber
        VibrationSeverity.HIGH -> TerminalRed
        VibrationSeverity.EXTREME -> TerminalRed
    }

    TerminalCard(modifier = modifier, onClick = { expanded = !expanded }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> Vibration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${state.severity.label} \u00B7 ${String.format(Locale.US, "%.2f", state.currentMagnitude)} m/s\u00B2",
                style = MaterialTheme.typography.bodyMedium,
                color = severityColor
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(0.7f)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format(Locale.US, "%.2f m/s\u00B2", state.currentMagnitude), style = MaterialTheme.typography.bodyMedium, color = severityColor)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Peak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format(Locale.US, "%.2f m/s\u00B2", state.peakMagnitude), style = MaterialTheme.typography.bodyMedium, color = TerminalRed)
                            }
                        }
                    }
                    Button(onClick = onReset, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text("Reset", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Seismograph waveform
                val gridColor = TerminalGreen.copy(alpha = 0.2f)

                Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    val centerY = size.height / 2

                    // Grid
                    drawLine(color = gridColor, start = Offset(0f, centerY), end = Offset(size.width, centerY), strokeWidth = 0.5f)
                    drawLine(color = gridColor, start = Offset(0f, size.height * 0.25f), end = Offset(size.width, size.height * 0.25f), strokeWidth = 0.3f)
                    drawLine(color = gridColor, start = Offset(0f, size.height * 0.75f), end = Offset(size.width, size.height * 0.75f), strokeWidth = 0.3f)

                    if (state.history.isNotEmpty()) {
                        val path = Path()
                        val maxVal = (state.peakMagnitude.coerceAtLeast(2f))
                        val stepX = size.width / (state.history.size - 1).coerceAtLeast(1)

                        state.history.forEachIndexed { index, value ->
                            val x = index * stepX
                            val normalized = (value / maxVal).coerceIn(0f, 1f)
                            val y = centerY - normalized * size.height * 0.4f
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        // Glow
                        drawPath(path, severityColor.copy(alpha = 0.15f), style = Stroke(width = 6f))
                        // Line
                        drawPath(path, severityColor, style = Stroke(width = 2f))
                    }
                }
            }
        }
    }
}

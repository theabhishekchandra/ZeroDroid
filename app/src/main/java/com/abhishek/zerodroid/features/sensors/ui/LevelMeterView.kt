package com.abhishek.zerodroid.features.sensors.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.sensors.domain.TiltState
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import java.util.Locale

@Composable
fun LevelMeterView(tiltState: TiltState, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    val levelColor = when {
        tiltState.isLevel -> TerminalGreen
        kotlin.math.abs(tiltState.pitch) < 5f && kotlin.math.abs(tiltState.roll) < 5f -> TerminalAmber
        else -> TerminalRed
    }

    val statusText = if (tiltState.isLevel) "LEVEL" else "TILTED"

    TerminalCard(modifier = modifier, onClick = { expanded = !expanded }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> Level Meter",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${String.format(Locale.US, "%.1f", tiltState.pitch)}\u00B0 \u00B7 ${String.format(Locale.US, "%.1f", tiltState.roll)}\u00B0 \u00B7 $statusText",
                style = MaterialTheme.typography.bodyMedium,
                color = levelColor
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pitch", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.1f\u00B0", tiltState.pitch), style = MaterialTheme.typography.bodyLarge, color = levelColor)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(statusText, style = MaterialTheme.typography.bodyLarge, color = levelColor)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Roll", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.1f\u00B0", tiltState.roll), style = MaterialTheme.typography.bodyLarge, color = levelColor)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val gridColor = TerminalGreen.copy(alpha = 0.2f)

                Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1.5f)) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val maxRadius = size.minDimension / 2 * 0.9f

                    // Target rings
                    for (i in 1..4) {
                        val r = maxRadius * i / 4
                        drawCircle(color = gridColor, radius = r, center = Offset(cx, cy), style = Stroke(width = 1f))
                    }

                    // Crosshairs
                    drawLine(color = gridColor, start = Offset(cx - maxRadius, cy), end = Offset(cx + maxRadius, cy), strokeWidth = 0.5f)
                    drawLine(color = gridColor, start = Offset(cx, cy - maxRadius), end = Offset(cx, cy + maxRadius), strokeWidth = 0.5f)

                    // Bubble position (clamped)
                    val maxDeg = 45f
                    val clampedPitch = tiltState.pitch.coerceIn(-maxDeg, maxDeg)
                    val clampedRoll = tiltState.roll.coerceIn(-maxDeg, maxDeg)
                    val bubbleX = cx + (clampedRoll / maxDeg) * maxRadius
                    val bubbleY = cy + (clampedPitch / maxDeg) * maxRadius

                    // Bubble glow
                    drawCircle(color = levelColor.copy(alpha = 0.15f), radius = maxRadius * 0.15f, center = Offset(bubbleX, bubbleY))
                    // Bubble
                    drawCircle(color = levelColor, radius = maxRadius * 0.08f, center = Offset(bubbleX, bubbleY))
                    drawCircle(color = levelColor.copy(alpha = 0.5f), radius = maxRadius * 0.08f, center = Offset(bubbleX, bubbleY), style = Stroke(width = 2f))
                }
            }
        }
    }
}

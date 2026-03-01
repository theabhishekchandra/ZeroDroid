package com.abhishek.zerodroid.features.sensors.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassView(heading: Float, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    val direction = when {
        heading < 22.5f || heading >= 337.5f -> "N"
        heading < 67.5f -> "NE"
        heading < 112.5f -> "E"
        heading < 157.5f -> "SE"
        heading < 202.5f -> "S"
        heading < 247.5f -> "SW"
        heading < 292.5f -> "W"
        else -> "NW"
    }

    val ringColor = TerminalGreen
    val gridColor = TerminalGreen.copy(alpha = 0.3f)
    val northColor = TerminalRed

    TerminalCard(modifier = modifier, onClick = { expanded = !expanded }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> Compass",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${String.format(Locale.US, "%.0f", heading)}\u00B0 $direction",
                style = MaterialTheme.typography.bodyMedium,
                color = TerminalGreen
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.minDimension / 2 * 0.85f

                // Outer ring
                drawCircle(color = ringColor, radius = radius, center = Offset(centerX, centerY), style = Stroke(width = 2f))
                drawCircle(color = gridColor, radius = radius * 0.7f, center = Offset(centerX, centerY), style = Stroke(width = 1f))
                drawCircle(color = gridColor, radius = radius * 0.4f, center = Offset(centerX, centerY), style = Stroke(width = 1f))

                // Degree markings
                for (deg in 0 until 360 step 10) {
                    val angleRad = Math.toRadians(deg.toDouble() - 90)
                    val startR = if (deg % 30 == 0) radius * 0.88f else radius * 0.93f
                    val endR = radius
                    val x1 = centerX + (cos(angleRad) * startR).toFloat()
                    val y1 = centerY + (sin(angleRad) * startR).toFloat()
                    val x2 = centerX + (cos(angleRad) * endR).toFloat()
                    val y2 = centerY + (sin(angleRad) * endR).toFloat()
                    val tickColor = if (deg == 0) northColor else ringColor.copy(alpha = if (deg % 30 == 0) 0.8f else 0.4f)
                    drawLine(color = tickColor, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = if (deg % 30 == 0) 2f else 1f)
                }

                // Cardinal direction labels
                val labelRadius = radius * 0.75f
                val labels = listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270)
                labels.forEach { (label, deg) ->
                    val angleRad = Math.toRadians(deg.toDouble() - 90)
                    val x = centerX + (cos(angleRad) * labelRadius).toFloat()
                    val y = centerY + (sin(angleRad) * labelRadius).toFloat()
                    val paint = android.graphics.Paint().apply {
                        color = if (label == "N") android.graphics.Color.RED else android.graphics.Color.WHITE
                        textSize = radius * 0.12f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.MONOSPACE
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(label, x, y + paint.textSize / 3, paint)
                }

                // Rotating needle
                rotate(degrees = -heading, pivot = Offset(centerX, centerY)) {
                    // North pointer (red)
                    val needlePath = Path().apply {
                        moveTo(centerX, centerY - radius * 0.55f)
                        lineTo(centerX - radius * 0.06f, centerY)
                        lineTo(centerX + radius * 0.06f, centerY)
                        close()
                    }
                    drawPath(needlePath, northColor.copy(alpha = 0.8f))

                    // South pointer (green)
                    val southPath = Path().apply {
                        moveTo(centerX, centerY + radius * 0.55f)
                        lineTo(centerX - radius * 0.06f, centerY)
                        lineTo(centerX + radius * 0.06f, centerY)
                        close()
                    }
                    drawPath(southPath, ringColor.copy(alpha = 0.5f))

                    // Center dot
                    drawCircle(color = Color.White, radius = radius * 0.04f, center = Offset(centerX, centerY))
                }
            }
        }
    }
}

package com.abhishek.zerodroid.features.uwb.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.features.uwb.domain.UwbRangingMeasurement
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalGreenDark
import kotlin.math.cos
import kotlin.math.sin

private const val DISPLAY_MAX_DISTANCE_M = 10f

@Composable
fun UwbRangingView(
    modifier: Modifier = Modifier,
    measurement: UwbRangingMeasurement? = null
) {
    val ringColor = TerminalGreen
    val dimColor = TerminalGreenDark
    val peerColor = MaterialTheme.colorScheme.error

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = minOf(size.width, size.height) / 2 * 0.9f

        // Concentric rings
        for (i in 1..4) {
            val radius = maxRadius * i / 4
            drawCircle(
                color = dimColor,
                radius = radius,
                center = center,
                style = Stroke(width = 1f)
            )
        }

        // Cross-hair
        drawLine(dimColor, Offset(center.x - maxRadius, center.y), Offset(center.x + maxRadius, center.y), strokeWidth = 0.5f)
        drawLine(dimColor, Offset(center.x, center.y - maxRadius), Offset(center.x, center.y + maxRadius), strokeWidth = 0.5f)

        // Center dot (self)
        drawCircle(ringColor, radius = 6f, center = center)

        // Peer position, if a live distance reading is available. Azimuth of 0 points
        // "up" (away from self); missing azimuth falls back to straight ahead.
        val distance = measurement?.distanceMeters
        if (distance != null) {
            val clampedDistance = distance.coerceIn(0f, DISPLAY_MAX_DISTANCE_M)
            val radius = (clampedDistance / DISPLAY_MAX_DISTANCE_M) * maxRadius
            val azimuthRad = Math.toRadians((measurement.azimuthDegrees ?: 0f).toDouble())
            val peerOffset = Offset(
                x = center.x + (radius * sin(azimuthRad)).toFloat(),
                y = center.y - (radius * cos(azimuthRad)).toFloat()
            )
            drawCircle(peerColor, radius = 8f, center = peerOffset)
        }
    }
}

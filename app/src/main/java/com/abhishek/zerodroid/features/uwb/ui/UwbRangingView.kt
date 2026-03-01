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
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalGreenDark

@Composable
fun UwbRangingView(modifier: Modifier = Modifier) {
    val ringColor = TerminalGreen
    val dimColor = TerminalGreenDark

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
    }
}

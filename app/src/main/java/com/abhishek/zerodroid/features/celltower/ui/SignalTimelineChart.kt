package com.abhishek.zerodroid.features.celltower.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.ui.theme.TerminalGreen

@Composable
fun SignalTimelineChart(
    signalHistory: List<Int>,
    modifier: Modifier = Modifier
) {
    if (signalHistory.isEmpty()) return

    val lineColor = TerminalGreen
    val gridColor = TerminalGreen.copy(alpha = 0.2f)

    TerminalCard(modifier = modifier) {
        Text(text = "> Signal Timeline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            // Grid lines
            for (i in 0..4) {
                val y = size.height * i / 4
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
            }

            if (signalHistory.size < 2) return@Canvas

            val minSignal = -120f
            val maxSignal = -30f
            val range = maxSignal - minSignal

            val path = Path()
            val stepX = size.width / (signalHistory.size - 1).coerceAtLeast(1)

            signalHistory.forEachIndexed { index, signal ->
                val x = index * stepX
                val normalizedY = 1f - ((signal - minSignal) / range).coerceIn(0f, 1f)
                val y = normalizedY * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // Glow
            drawPath(path, lineColor.copy(alpha = 0.15f), style = Stroke(width = 6f))
            // Line
            drawPath(path, lineColor, style = Stroke(width = 2f))

            // Current value dot
            val lastX = (signalHistory.size - 1) * stepX
            val lastNorm = 1f - ((signalHistory.last() - minSignal) / range).coerceIn(0f, 1f)
            val lastY = lastNorm * size.height
            drawCircle(lineColor, radius = 4f, center = Offset(lastX, lastY))
        }

        // Labels
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Current: ${signalHistory.lastOrNull() ?: 0} dBm | Samples: ${signalHistory.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

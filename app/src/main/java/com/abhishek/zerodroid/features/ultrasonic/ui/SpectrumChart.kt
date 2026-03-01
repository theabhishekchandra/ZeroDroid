package com.abhishek.zerodroid.features.ultrasonic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.features.ultrasonic.domain.FrequencyBin
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalGreenDark

@Composable
fun SpectrumChart(
    bins: List<FrequencyBin>,
    modifier: Modifier = Modifier
) {
    val lineColor = TerminalGreen
    val gridColor = TerminalGreenDark
    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        if (bins.isEmpty()) return@Canvas

        val maxMag = bins.maxOf { it.magnitude }.coerceAtLeast(0.001f)
        val barWidth = size.width / bins.size

        // Grid lines
        for (i in 1..3) {
            val y = size.height * i / 4f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
        }

        // Spectrum path
        val path = Path().apply {
            moveTo(0f, size.height)
            bins.forEachIndexed { index, bin ->
                val x = index * barWidth + barWidth / 2
                val y = size.height - (bin.magnitude / maxMag * size.height * 0.9f)
                lineTo(x, y)
            }
            lineTo(size.width, size.height)
        }

        // Fill
        val fillPath = Path().apply {
            addPath(path)
            close()
        }
        drawPath(fillPath, TerminalGreenDark.copy(alpha = 0.3f))

        // Line
        val linePath = Path().apply {
            bins.forEachIndexed { index, bin ->
                val x = index * barWidth + barWidth / 2
                val y = size.height - (bin.magnitude / maxMag * size.height * 0.9f)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(linePath, lineColor, style = Stroke(width = 2f))
    }
}

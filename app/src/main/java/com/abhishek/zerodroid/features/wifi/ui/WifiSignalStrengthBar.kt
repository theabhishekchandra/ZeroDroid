package com.abhishek.zerodroid.features.wifi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun WifiSignalBars(signalPercent: Int, modifier: Modifier = Modifier) {
    val color = when {
        signalPercent >= 60 -> TerminalGreen
        signalPercent >= 30 -> TerminalAmber
        else -> TerminalRed
    }
    val dimColor = color.copy(alpha = 0.2f)
    val barCount = 5
    val activeBars = when {
        signalPercent >= 80 -> 5
        signalPercent >= 60 -> 4
        signalPercent >= 40 -> 3
        signalPercent >= 20 -> 2
        signalPercent > 0 -> 1
        else -> 0
    }

    Canvas(modifier = modifier.size(width = 24.dp, height = 20.dp)) {
        val barWidth = size.width / (barCount * 2 - 1)
        val gap = barWidth

        for (i in 0 until barCount) {
            val barHeight = size.height * (i + 1) / barCount
            val x = i * (barWidth + gap)
            val y = size.height - barHeight
            val barColor = if (i < activeBars) color else dimColor

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(1f, 1f)
            )
        }
    }
}

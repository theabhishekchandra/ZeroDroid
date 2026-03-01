package com.abhishek.zerodroid.features.wifi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.wifi.domain.ChannelScore
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TextSecondary

@Composable
fun WifiChannelChart(
    channelScores: List<ChannelScore>,
    modifier: Modifier = Modifier
) {
    if (channelScores.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val maxAps = channelScores.maxOf { it.apCount }.coerceAtLeast(1)
    val labelStyle = MaterialTheme.typography.labelSmall

    TerminalCard(modifier = modifier) {
        Text(
            text = "Channel Usage",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val barWidth = (size.width / channelScores.size.coerceAtLeast(1)) * 0.7f
            val gap = (size.width / channelScores.size.coerceAtLeast(1)) * 0.3f

            channelScores.forEachIndexed { index, score ->
                val barHeight = (score.apCount.toFloat() / maxAps) * (size.height - 20f)
                val x = index * (barWidth + gap)
                val y = size.height - barHeight - 16f

                drawRoundRect(
                    color = TerminalGreen.copy(alpha = 0.7f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Channel label
                val label = textMeasurer.measure(
                    text = "${score.channel}",
                    style = labelStyle.copy(color = TextSecondary)
                )
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(
                        x + (barWidth - label.size.width) / 2,
                        size.height - 14f
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${channelScores.size} active channels",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Max: ${maxAps} APs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

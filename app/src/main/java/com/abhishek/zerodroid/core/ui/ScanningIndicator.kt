package com.abhishek.zerodroid.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.ui.theme.TerminalGreen

@Composable
fun ScanningIndicator(
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    label: String = "Scanning...",
    color: Color = TerminalGreen
) {
    if (!isScanning) return

    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Scale"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Scale"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .drawBehind {
                    val center = this.center
                    val maxRadius = size.minDimension / 2
                    drawCircle(
                        color = color.copy(alpha = ring1Alpha),
                        radius = maxRadius * ring1Scale,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = color.copy(alpha = ring2Alpha * 0.6f),
                        radius = maxRadius * ring2Scale,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = color,
                        radius = 3f,
                        center = center
                    )
                }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

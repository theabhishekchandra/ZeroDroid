package com.abhishek.zerodroid.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.ui.theme.CardBorderGreen
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalGreenGlow

@Composable
fun TerminalCard(
    modifier: Modifier = Modifier,
    glowColor: Color = TerminalGreen,
    glowAlpha: Color = TerminalGreenGlow,
    borderColor: Color = CardBorderGreen,
    animated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cardGlow")
    val glowAlphaAnim by infiniteTransition.animateFloat(
        initialValue = if (animated) 0.15f else 0f,
        targetValue = if (animated) 0.4f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val cardModifier = modifier
        .fillMaxWidth()
        .drawBehind {
            // Outer glow
            if (animated) {
                drawRoundRect(
                    color = glowColor.copy(alpha = glowAlphaAnim * 0.3f),
                    topLeft = Offset(-4f, -4f),
                    size = Size(size.width + 8f, size.height + 8f),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = 6f)
                )
            }
            // Border
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor,
                        if (animated) glowColor.copy(alpha = 0.6f) else borderColor
                    )
                ),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 2f)
            )
        }
        .background(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        )
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )

    Box(modifier = cardModifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            content = content
        )
    }
}

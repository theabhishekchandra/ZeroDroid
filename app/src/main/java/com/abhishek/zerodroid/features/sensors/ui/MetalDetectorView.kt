package com.abhishek.zerodroid.features.sensors.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.sensors.domain.MetalDetectorState
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

@Composable
fun MetalDetectorView(
    state: MetalDetectorState,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val absDeviation = abs(state.deviation)
    val barColor = when {
        absDeviation > 50f -> TerminalRed
        absDeviation > 20f -> TerminalAmber
        else -> TerminalGreen
    }

    TerminalCard(modifier = modifier, onClick = { expanded = !expanded }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> Metal Detector",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = String.format(Locale.US, "%+.1f \u03BCT", state.deviation),
                style = MaterialTheme.typography.bodyMedium,
                color = barColor
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                // Deviation bar
                val bgColor = MaterialTheme.colorScheme.surface

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    drawRoundRect(
                        color = bgColor,
                        cornerRadius = CornerRadius(4f, 4f),
                        size = size
                    )
                    val fillWidth = min(absDeviation / 100f, 1f) * size.width
                    drawRoundRect(
                        color = barColor,
                        cornerRadius = CornerRadius(4f, 4f),
                        size = Size(fillWidth, size.height)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Baseline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            String.format(Locale.US, "%.1f \u03BCT", state.baseline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Deviation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            String.format(Locale.US, "%+.1f \u03BCT", state.deviation),
                            style = MaterialTheme.typography.bodyMedium,
                            color = barColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text("Recalibrate", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

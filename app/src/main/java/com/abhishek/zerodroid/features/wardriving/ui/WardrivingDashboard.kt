package com.abhishek.zerodroid.features.wardriving.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingStats
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun WardrivingDashboard(stats: WardrivingStats, modifier: Modifier = Modifier) {
    TerminalCard(modifier = modifier) {
        Text(text = "> Dashboard", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        // Stats row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBox("SSIDs", stats.uniqueSsids.toString(), TerminalCyan)
            StatBox("BSSIDs", stats.uniqueBssids.toString(), TerminalGreen)
            StatBox("Records", stats.totalRecords.toString(), TerminalAmber)
            StatBox("Duration", stats.formattedDuration, MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Security donut chart
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(80.dp)) {
                val total = (stats.openCount + stats.securedCount).coerceAtLeast(1).toFloat()
                val securedAngle = (stats.securedCount / total) * 360f
                val openAngle = 360f - securedAngle

                // Secured arc
                drawArc(
                    color = TerminalGreen,
                    startAngle = -90f,
                    sweepAngle = securedAngle,
                    useCenter = false,
                    style = Stroke(width = 12f),
                    topLeft = Offset(8f, 8f),
                    size = Size(size.width - 16f, size.height - 16f)
                )
                // Open arc
                drawArc(
                    color = TerminalRed,
                    startAngle = -90f + securedAngle,
                    sweepAngle = openAngle,
                    useCenter = false,
                    style = Stroke(width = 12f),
                    topLeft = Offset(8f, 8f),
                    size = Size(size.width - 16f, size.height - 16f)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(modifier = Modifier.size(8.dp)) { drawCircle(TerminalGreen) }
                    Text("Secured: ${stats.securedCount}", style = MaterialTheme.typography.bodySmall, color = TerminalGreen)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(modifier = Modifier.size(8.dp)) { drawCircle(TerminalRed) }
                    Text("Open: ${stats.openCount} (${String.format("%.0f", stats.openPercent)}%)", style = MaterialTheme.typography.bodySmall, color = TerminalRed)
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

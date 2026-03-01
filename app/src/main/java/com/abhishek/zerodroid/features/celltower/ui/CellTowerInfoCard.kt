package com.abhishek.zerodroid.features.celltower.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.celltower.domain.CellTowerInfo
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun CellTowerInfoCard(
    cell: CellTowerInfo,
    label: String = "",
    modifier: Modifier = Modifier
) {
    val signalColor = when {
        cell.signalPercent >= 60 -> TerminalGreen
        cell.signalPercent >= 30 -> TerminalAmber
        else -> TerminalRed
    }
    val bgColor = MaterialTheme.colorScheme.surface

    TerminalCard(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cell.type.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = buildString {
                        cell.mcc?.let { append("MCC:$it ") }
                        cell.mnc?.let { append("MNC:$it ") }
                        cell.lac?.let { append("LAC:$it ") }
                        cell.cid?.let { append("CID:$it") }
                    }.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${cell.rssi} dBm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = signalColor
                )
                cell.arfcn?.let {
                    Text(
                        text = "ARFCN: $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            drawRoundRect(color = bgColor, cornerRadius = CornerRadius(2f), size = size)
            drawRoundRect(
                color = signalColor,
                cornerRadius = CornerRadius(2f),
                size = Size(size.width * cell.signalPercent / 100f, size.height)
            )
        }
    }
}

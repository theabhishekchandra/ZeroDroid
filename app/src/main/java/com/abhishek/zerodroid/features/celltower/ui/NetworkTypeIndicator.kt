package com.abhishek.zerodroid.features.celltower.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun NetworkTypeIndicator(
    networkType: String?,
    signalStrength: Int,
    modifier: Modifier = Modifier
) {
    val type = networkType?.uppercase() ?: "UNKNOWN"
    val (label, color) = when {
        type.contains("NR") || type.contains("5G") -> "5G" to TerminalCyan
        type.contains("LTE") -> "LTE" to TerminalGreen
        type.contains("WCDMA") || type.contains("UMTS") || type.contains("HSPA") -> "3G" to TerminalAmber
        type.contains("GSM") || type.contains("EDGE") || type.contains("GPRS") -> "2G" to TerminalRed
        else -> type to MaterialTheme.colorScheme.onSurfaceVariant
    }

    TerminalCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "> Network Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = label, style = MaterialTheme.typography.displaySmall, color = color)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Signal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "$signalStrength dBm", style = MaterialTheme.typography.headlineSmall, color = color)
            }
        }
    }
}

package com.abhishek.zerodroid.features.celltower.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.features.celltower.domain.AlertSeverity
import com.abhishek.zerodroid.features.celltower.domain.ImsiCatcherAlert
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun ImsiCatcherAlertCard(
    alert: ImsiCatcherAlert,
    modifier: Modifier = Modifier
) {
    val borderColor = when (alert.severity) {
        AlertSeverity.HIGH -> TerminalRed
        AlertSeverity.MEDIUM -> TerminalAmber
        AlertSeverity.LOW -> MaterialTheme.colorScheme.outline
    }
    val labelColor = when (alert.severity) {
        AlertSeverity.HIGH -> TerminalRed
        AlertSeverity.MEDIUM -> TerminalAmber
        AlertSeverity.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "[${alert.severity}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
                Text(
                    text = " ${alert.type.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alert.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

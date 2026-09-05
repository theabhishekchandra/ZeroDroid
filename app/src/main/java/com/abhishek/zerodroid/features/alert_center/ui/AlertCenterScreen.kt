package com.abhishek.zerodroid.features.alert_center.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.alerts.UnifiedAlert
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.alert_center.viewmodel.AlertCenterViewModel
import com.abhishek.zerodroid.ui.theme.SeverityCritical
import com.abhishek.zerodroid.ui.theme.SeverityHigh
import com.abhishek.zerodroid.ui.theme.SeverityLow
import com.abhishek.zerodroid.ui.theme.SeverityMedium
import com.abhishek.zerodroid.ui.theme.TextDim
import java.text.DateFormat
import java.util.Date

@Composable
fun AlertCenterScreen(
    viewModel: AlertCenterViewModel = hiltViewModel()
) {
    val alerts by viewModel.alerts.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${alerts.size} alert(s) recorded",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (alerts.isNotEmpty()) {
                    OutlinedButton(onClick = viewModel::clearAll) {
                        Text("Clear All", maxLines = 1, softWrap = false)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Threats detected by Rogue AP, Deauth, GPS Spoof, Hidden Camera, " +
                    "and Tracker Scanner are collected here as they happen.",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }

        if (alerts.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.NotificationsActive,
                    title = "No alerts yet",
                    subtitle = "Run any security tool - alerts it raises will show up here automatically."
                )
            }
        }

        items(alerts, key = { it.id }) { alert -> AlertCard(alert) }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun AlertCard(alert: UnifiedAlert, modifier: Modifier = Modifier) {
    TerminalCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "[${alert.severity.name}]",
                style = MaterialTheme.typography.labelSmall,
                color = alert.severity.toColor()
            )
            Text(
                text = alert.source.label,
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "> ${alert.title}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = alert.detail,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(alert.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
    }
}

fun AlertSeverity.toColor() = when (this) {
    AlertSeverity.CRITICAL -> SeverityCritical
    AlertSeverity.HIGH -> SeverityHigh
    AlertSeverity.MEDIUM -> SeverityMedium
    AlertSeverity.LOW -> SeverityLow
}

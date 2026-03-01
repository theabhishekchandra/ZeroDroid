package com.abhishek.zerodroid.features.wifiaware.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.wifiaware.domain.WifiAwarePeer

@Composable
fun WifiAwarePeerItem(
    peer: WifiAwarePeer,
    modifier: Modifier = Modifier
) {
    TerminalCard(modifier = modifier) {
        Column {
            Text(
                text = "Service: ${peer.serviceName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "ID: ${peer.serviceId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            peer.matchFilter?.let {
                Text(
                    text = "Filter: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

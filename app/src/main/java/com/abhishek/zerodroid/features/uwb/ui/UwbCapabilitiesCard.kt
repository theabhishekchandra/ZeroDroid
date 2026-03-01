package com.abhishek.zerodroid.features.uwb.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.uwb.domain.UwbDeviceInfo

@Composable
fun UwbCapabilitiesCard(
    info: UwbDeviceInfo,
    modifier: Modifier = Modifier
) {
    TerminalCard(modifier = modifier) {
        Text(
            text = "> UWB Capabilities",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Chipset: ${info.chipset}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (info.capabilities.isNotEmpty()) {
            info.capabilities.forEach { cap ->
                Text(
                    text = "- $cap",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "No capabilities detected",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

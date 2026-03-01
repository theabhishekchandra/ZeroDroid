package com.abhishek.zerodroid.features.sdr.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.sdr.domain.SdrDeviceInfo
import com.abhishek.zerodroid.ui.theme.TerminalGreen

@Composable
fun SdrDeviceCard(
    device: SdrDeviceInfo,
    modifier: Modifier = Modifier
) {
    TerminalCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.chipset,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "VID:PID ${device.vidPid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = device.deviceName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (device.isRtlSdr) {
                Text(
                    text = "RTL-SDR",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalGreen
                )
            }
        }
    }
}

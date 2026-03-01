package com.abhishek.zerodroid.features.usb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.usb.domain.ThreatLevel
import com.abhishek.zerodroid.features.usb.domain.UsbDeviceDatabase
import com.abhishek.zerodroid.features.usb.domain.UsbDeviceInfo
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun UsbDeviceItem(
    device: UsbDeviceInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val knownDevice = UsbDeviceDatabase.lookup(device.vidPid)

    TerminalCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.productName ?: device.deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "VID:PID ${device.vidPid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    text = device.deviceClassName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${device.interfaceCount} interface(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (device.manufacturerName != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manufacturer: ${device.manufacturerName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        knownDevice?.let { known ->
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Category badge
                Text(
                    text = known.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = known.threatLevel.color,
                    modifier = Modifier
                        .background(known.threatLevel.color.copy(alpha = 0.1f), MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
                // Threat badge (only for non-safe)
                if (known.threatLevel != ThreatLevel.SAFE) {
                    Text(
                        text = known.threatLevel.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = known.threatLevel.color,
                        modifier = Modifier
                            .background(known.threatLevel.color.copy(alpha = 0.15f), MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            Text(text = known.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (device.badUsbIndicators.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            device.badUsbIndicators.forEach { indicator ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "WARNING:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalRed
                    )
                    Text(
                        text = indicator.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalAmber
                    )
                }
            }
        }
    }
}

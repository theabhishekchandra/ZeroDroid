package com.abhishek.zerodroid.features.ble.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.ble.domain.BleDeviceTypeIdentifier
import com.abhishek.zerodroid.features.ble.domain.BleDistanceEstimator
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun BleDeviceItem(
    device: BleDevice,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val signalColor = when {
        device.signalPercent >= 60 -> TerminalGreen
        device.signalPercent >= 30 -> TerminalAmber
        else -> TerminalRed
    }
    val bgColor = MaterialTheme.colorScheme.surface
    val deviceType = BleDeviceTypeIdentifier.identify(device.name, device.serviceUuids)
    val distance = BleDistanceEstimator.estimateDistance(device.rssi)
    val distanceLabel = BleDistanceEstimator.getDistanceLabel(distance)
    val proximitySymbol = BleDistanceEstimator.getProximityLabel(distance)

    TerminalCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = deviceType.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = deviceType.color,
                        modifier = Modifier
                            .background(
                                color = deviceType.color.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = signalColor
                )
                Text(
                    text = "$proximitySymbol $distanceLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = deviceType.color
                )
            }
            IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (device.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (device.isBookmarked) TerminalAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Signal bar
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            drawRoundRect(
                color = bgColor,
                cornerRadius = CornerRadius(2f, 2f),
                size = size
            )
            drawRoundRect(
                color = signalColor,
                cornerRadius = CornerRadius(2f, 2f),
                size = Size(size.width * device.signalPercent / 100f, size.height)
            )
        }

        // Service UUIDs
        if (device.serviceUuids.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Services: ${device.serviceUuids.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

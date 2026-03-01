package com.abhishek.zerodroid.features.wifi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun WifiAccessPointItem(
    ap: WifiAccessPoint,
    modifier: Modifier = Modifier
) {
    val signalColor = when {
        ap.signalPercent >= 60 -> TerminalGreen
        ap.signalPercent >= 30 -> TerminalAmber
        else -> TerminalRed
    }
    val bgColor = MaterialTheme.colorScheme.surface

    TerminalCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ap.ssid,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = ap.bssid,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${ap.rssi} dBm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = signalColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    WifiSignalBars(signalPercent = ap.signalPercent)
                }
                Text(
                    text = "Ch ${ap.channel} · ${ap.band.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Signal strength bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        ) {
            drawRoundRect(
                color = bgColor,
                cornerRadius = CornerRadius(3f, 3f),
                size = size
            )
            drawRoundRect(
                color = signalColor,
                cornerRadius = CornerRadius(3f, 3f),
                size = Size(size.width * ap.signalPercent / 100f, size.height)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WifiSecurityBadge(securityLabel = ap.security.label)
            Text(
                text = "${ap.signalPercent}%",
                style = MaterialTheme.typography.labelSmall,
                color = signalColor
            )
        }
    }
}

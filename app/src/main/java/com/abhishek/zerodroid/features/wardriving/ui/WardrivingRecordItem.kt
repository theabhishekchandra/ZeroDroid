package com.abhishek.zerodroid.features.wardriving.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingRecord
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun WardrivingRecordItem(
    record: WardrivingRecord,
    modifier: Modifier = Modifier
) {
    val signalColor = when {
        record.rssi >= -60 -> TerminalGreen
        record.rssi >= -80 -> TerminalAmber
        else -> TerminalRed
    }

    TerminalCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.ssid ?: "<hidden>",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = record.bssid,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${record.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = signalColor
                )
                Text(
                    text = "%.4f, %.4f".format(record.lat, record.lng),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

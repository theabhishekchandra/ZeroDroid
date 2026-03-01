package com.abhishek.zerodroid.features.ultrasonic.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
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
import com.abhishek.zerodroid.features.ultrasonic.domain.UltrasonicBeacon
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun BeaconAlertCard(
    beacon: UltrasonicBeacon,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, TerminalRed)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Ultrasonic Beacon Detected",
                style = MaterialTheme.typography.titleMedium,
                color = TerminalRed
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Center: %.1f Hz".format(beacon.centerFrequencyHz),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Bandwidth: %.1f Hz".format(beacon.bandwidth),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Magnitude: %.4f".format(beacon.magnitude),
                style = MaterialTheme.typography.labelSmall,
                color = TerminalAmber
            )
        }
    }
}

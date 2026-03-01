package com.abhishek.zerodroid.features.sdr.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard

@Composable
fun SdrInfoPanel(modifier: Modifier = Modifier) {
    TerminalCard(modifier = modifier) {
        Text(
            text = "> SDR Information",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ZeroDroid can detect RTL-SDR and other compatible SDR hardware connected via USB OTG.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Full signal processing requires a native librtlsdr implementation, which is outside the current scope. Use SDR Touch or RF Analyzer apps for full SDR functionality.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Supported: RTL2832U, RTL2838, HackRF, AirSpy",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

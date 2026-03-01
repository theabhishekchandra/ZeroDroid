package com.abhishek.zerodroid.features.camera.ui

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
import com.abhishek.zerodroid.features.camera.domain.QrScanResult
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun QrResultCard(
    result: QrScanResult,
    modifier: Modifier = Modifier
) {
    val borderColor = if (result.isThreat) TerminalRed else MaterialTheme.colorScheme.outline

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "[${result.format}] ${result.contentType.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.parsedContent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 5
            )

            if (result.isThreat) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "THREAT: ${result.threatReason ?: "Suspicious content"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalRed
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.rawValue,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

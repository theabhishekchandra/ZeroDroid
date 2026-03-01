package com.abhishek.zerodroid.features.nfc.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.nfc.domain.MifareSectorData
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun NfcMifarePanel(
    sectors: List<MifareSectorData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "> MIFARE Classic Dump (${sectors.size} sectors)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        val authenticated = sectors.count { it.isAuthenticated }
        val total = sectors.size
        TerminalCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sectors Read: $authenticated/$total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (authenticated == total) TerminalGreen else TerminalAmber
                )
                Text(
                    text = "${authenticated * 100 / total.coerceAtLeast(1)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TerminalGreen
                )
            }
        }

        sectors.forEach { sector ->
            MifareSectorCard(sector = sector)
        }
    }
}

@Composable
private fun MifareSectorCard(sector: MifareSectorData) {
    var expanded by remember { mutableStateOf(false) }

    TerminalCard(onClick = { expanded = !expanded }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sector ${sector.sectorIndex}",
                style = MaterialTheme.typography.titleSmall,
                color = if (sector.isAuthenticated) TerminalGreen else TerminalRed
            )
            if (sector.isAuthenticated) {
                Text(
                    text = "Key: ${sector.keyUsed}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text(
                    text = "LOCKED",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalRed
                )
            }
        }

        AnimatedVisibility(
            visible = expanded && sector.isAuthenticated,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                sector.blocks.forEach { block ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "B${String.format("%02d", block.blockIndex)}:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalAmber,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = " ${block.hexString}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

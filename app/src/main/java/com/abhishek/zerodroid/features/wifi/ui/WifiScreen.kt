package com.abhishek.zerodroid.features.wifi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.util.WifiBand
import com.abhishek.zerodroid.features.wifi.viewmodel.WifiViewModel
import com.abhishek.zerodroid.ui.theme.TerminalGreen

@Composable
fun WifiScreen(
    viewModel: WifiViewModel = viewModel(factory = WifiViewModel.Factory)
) {
    PermissionGate(
        permissions = PermissionUtils.wifiPermissions(),
        rationale = "Location permission is required by Android to scan WiFi networks."
    ) {
        WifiContent(viewModel = viewModel)
    }
}

@Composable
private fun WifiContent(viewModel: WifiViewModel) {
    val accessPoints by viewModel.accessPoints.collectAsState()
    val channelScores by viewModel.channelScores.collectAsState()
    val selectedBand by viewModel.selectedBand.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.stopScan() }
    }

    val filteredAps = if (selectedBand != null) {
        accessPoints.filter { it.band == selectedBand }
    } else {
        accessPoints
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Start/Stop control
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedBand == null,
                        onClick = { viewModel.selectBand(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    FilterChip(
                        selected = selectedBand == WifiBand.BAND_2_4GHZ,
                        onClick = { viewModel.selectBand(WifiBand.BAND_2_4GHZ) },
                        label = { Text("2.4 GHz") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    FilterChip(
                        selected = selectedBand == WifiBand.BAND_5GHZ,
                        onClick = { viewModel.selectBand(WifiBand.BAND_5GHZ) },
                        label = { Text("5 GHz") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                if (isScanning) {
                    OutlinedButton(onClick = { viewModel.stopScan() }) {
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = { viewModel.startScan() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalGreen.copy(alpha = 0.15f),
                            contentColor = TerminalGreen
                        )
                    ) {
                        Text("Scan")
                    }
                }
            }
        }

        if (!isScanning && accessPoints.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Wifi,
                    title = "WiFi Idle",
                    subtitle = "Tap Scan to discover nearby networks.\nAuto-stops after 30 seconds."
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isScanning) {
                        ScanningIndicator(
                            isScanning = true,
                            label = "${filteredAps.size} networks found"
                        )
                    } else {
                        Text(
                            text = "> ${filteredAps.size} networks found",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${accessPoints.count { it.band == WifiBand.BAND_2_4GHZ }} / ${accessPoints.count { it.band == WifiBand.BAND_5GHZ }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                val filteredScores = if (selectedBand != null) {
                    channelScores.filter { it.band == selectedBand }
                } else {
                    channelScores
                }
                WifiChannelChart(channelScores = filteredScores)
            }

            items(filteredAps, key = { it.bssid }) { ap ->
                WifiAccessPointItem(ap = ap)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

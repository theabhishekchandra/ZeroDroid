package com.abhishek.zerodroid.features.sdr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.ui.StatusIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.sdr.viewmodel.SdrViewModel

@Composable
fun SdrScreen(
    viewModel: SdrViewModel = viewModel(factory = SdrViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            StatusIndicator(isAvailable = state.hasUsbHost)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${state.devices.size} SDR device(s) detected",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { viewModel.refresh() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Rescan USB") }
        }

        if (state.devices.isEmpty()) {
            item {
                TerminalCard {
                    Text(
                        text = "> No SDR hardware detected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Connect an RTL-SDR dongle via OTG cable",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(state.devices) { device -> SdrDeviceCard(device = device) }

        item { SdrInfoPanel() }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

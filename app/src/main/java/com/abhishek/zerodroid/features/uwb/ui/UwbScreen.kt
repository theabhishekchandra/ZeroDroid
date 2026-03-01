package com.abhishek.zerodroid.features.uwb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.abhishek.zerodroid.features.uwb.viewmodel.UwbViewModel

@Composable
fun UwbScreen(
    viewModel: UwbViewModel = viewModel(factory = UwbViewModel.Factory)
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
            StatusIndicator(isAvailable = state.isHardwareAvailable)
        }

        if (!state.isHardwareAvailable) {
            item {
                TerminalCard {
                    Text(
                        text = "> UWB hardware not detected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ultra-Wideband requires specific hardware support (e.g., Google Pixel 6 Pro+, Samsung Galaxy S21+)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        state.deviceInfo?.let { info ->
            item { UwbCapabilitiesCard(info = info) }
        }

        item {
            TerminalCard {
                Text(
                    text = "> Ranging Radar",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                UwbRangingView()
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ranging requires a peer UWB device. Jetpack UWB alpha library skipped for AGP 9.0.1 compatibility.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

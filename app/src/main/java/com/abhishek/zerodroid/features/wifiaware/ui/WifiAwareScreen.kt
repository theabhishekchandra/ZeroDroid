package com.abhishek.zerodroid.features.wifiaware.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.ui.StatusIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.wifiaware.viewmodel.WifiAwareViewModel

@Composable
fun WifiAwareScreen(
    viewModel: WifiAwareViewModel = viewModel(factory = WifiAwareViewModel.Factory)
) {
    DisposableEffect(Unit) {
        onDispose { viewModel.detachSession() }
    }

    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            StatusIndicator(isAvailable = state.isAvailable)
        }

        if (!state.isAvailable) {
            item {
                TerminalCard {
                    Text(
                        text = "> Wi-Fi Aware (NAN) not available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "This device does not support Wi-Fi Aware / Neighbor Awareness Networking",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            WifiAwareControlPanel(
                state = state,
                onServiceNameChange = viewModel::setServiceName,
                onAttach = viewModel::attachSession,
                onDetach = viewModel::detachSession,
                onTogglePublish = viewModel::togglePublish,
                onToggleSubscribe = viewModel::toggleSubscribe
            )
        }

        state.error?.let { error ->
            item {
                Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        if (state.discoveredPeers.isNotEmpty()) {
            item {
                Text(
                    text = "> Discovered Peers (${state.discoveredPeers.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(state.discoveredPeers) { peer -> WifiAwarePeerItem(peer = peer) }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

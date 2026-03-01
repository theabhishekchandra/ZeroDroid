package com.abhishek.zerodroid.features.celltower.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.abhishek.zerodroid.features.celltower.viewmodel.CellTowerViewModel

@Composable
fun CellTowerScreen(
    viewModel: CellTowerViewModel = viewModel(factory = CellTowerViewModel.Factory)
) {
    PermissionGate(
        permissions = PermissionUtils.cellTowerPermissions(),
        rationale = "Phone and location permissions are needed to read cell tower information."
    ) {
        CellTowerContent(viewModel)
    }
}

@Composable
private fun CellTowerContent(viewModel: CellTowerViewModel) {
    DisposableEffect(Unit) {
        onDispose { viewModel.stopMonitoring() }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> Cell Tower Monitor",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (state.isMonitoring) {
                    OutlinedButton(
                        onClick = { viewModel.stopMonitoring() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Stop") }
                } else {
                    Button(
                        onClick = { viewModel.startMonitoring() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Monitor") }
                }
            }
        }

        state.error?.let { error ->
            item {
                Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        state.currentCell?.let { cell ->
            item {
                NetworkTypeIndicator(
                    networkType = cell.type.displayName,
                    signalStrength = cell.rssi
                )
            }
            item { CellTowerInfoCard(cell = cell, label = "> Registered Cell") }
            if (state.signalHistory.isNotEmpty()) {
                item { SignalTimelineChart(signalHistory = state.signalHistory) }
            }
        }

        if (state.neighbors.isNotEmpty()) {
            item {
                Text(
                    text = "> Neighbor Cells (${state.neighbors.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(state.neighbors) { cell -> CellTowerInfoCard(cell = cell) }
        }

        if (state.alerts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "> IMSI Catcher Alerts (${state.alerts.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(state.alerts) { alert -> ImsiCatcherAlertCard(alert = alert) }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

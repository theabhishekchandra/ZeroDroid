package com.abhishek.zerodroid.features.ble.ui

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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ble.viewmodel.BleViewModel

@Composable
fun BleScreen(
    viewModel: BleViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.blePermissions(),
        rationale = "Bluetooth permission is needed to scan for nearby BLE devices."
    ) {
        BleContent(viewModel = viewModel)
    }
}

@Composable
private fun BleContent(viewModel: BleViewModel) {
    val scanState by viewModel.scanState.collectAsState()

    HardwareLifecycleEffect(
        isActive = scanState.isScanning,
        onPause = viewModel::stopScan,
        onResume = viewModel::startScan
    )

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
                if (scanState.isScanning) {
                    ScanningIndicator(
                        isScanning = true,
                        label = "${scanState.devices.size} devices found"
                    )
                } else {
                    Text(
                        text = "> ${scanState.devices.size} devices found",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (scanState.isScanning) {
                    OutlinedButton(
                        onClick = { viewModel.toggleScan() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = { viewModel.toggleScan() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Scan")
                    }
                }
            }
        }

        scanState.error?.let { error ->
            item {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (!scanState.isBluetoothEnabled) {
            item {
                TerminalCard {
                    Text(
                        text = "> Bluetooth is off",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Turn on Bluetooth in system settings, then tap Scan again.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (scanState.devices.isEmpty() && !scanState.isScanning) {
            item {
                EmptyState(
                    icon = Icons.Default.Bluetooth,
                    title = "No BLE devices found",
                    subtitle = "Tap Scan to search for nearby Bluetooth Low Energy devices"
                )
            }
        }

        items(scanState.devices, key = { it.address }) { device ->
            BleDeviceItem(
                device = device,
                onBookmarkToggle = { viewModel.toggleBookmark(device) }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

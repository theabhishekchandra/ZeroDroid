package com.abhishek.zerodroid.features.bluetooth_classic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.bluetooth_classic.domain.ClassicBluetoothDevice
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SppState
import com.abhishek.zerodroid.features.bluetooth_classic.domain.TerminalLine
import com.abhishek.zerodroid.features.bluetooth_classic.viewmodel.BluetoothClassicViewModel
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun BluetoothClassicScreen(
    viewModel: BluetoothClassicViewModel = viewModel(factory = BluetoothClassicViewModel.Factory)
) {
    PermissionGate(
        permissions = PermissionUtils.blePermissions(),
        rationale = "Bluetooth permission is needed to scan for nearby Classic Bluetooth devices."
    ) {
        BluetoothClassicContent(viewModel = viewModel)
    }
}

@Composable
private fun BluetoothClassicContent(viewModel: BluetoothClassicViewModel) {
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScan()
            viewModel.disconnectSpp()
        }
    }

    val state by viewModel.state.collectAsState()
    val sppState by viewModel.sppState.collectAsState()

    var showTerminal by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<String?>(null) }

    // Show terminal when SPP connects, hide when disconnected
    LaunchedEffect(sppState.isConnected) {
        if (sppState.isConnected) {
            showTerminal = true
        }
    }

    if (showTerminal && selectedDevice != null) {
        SppTerminalPanel(
            sppState = sppState,
            deviceAddress = selectedDevice!!,
            onSend = { viewModel.sendSpp(it) },
            onDisconnect = {
                viewModel.disconnectSpp()
                showTerminal = false
                selectedDevice = null
            }
        )
    } else {
        DeviceScanContent(
            state = state,
            sppState = sppState,
            onToggleScan = { viewModel.toggleScan() },
            onConnectSpp = { address ->
                selectedDevice = address
                viewModel.connectSpp(address)
            }
        )
    }
}

@Composable
private fun DeviceScanContent(
    state: com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothClassicState,
    sppState: SppState,
    onToggleScan: () -> Unit,
    onConnectSpp: (String) -> Unit
) {
    val totalDevices = state.pairedDevices.size + state.discoveredDevices.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header row with count and scan button
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isScanning) {
                    ScanningIndicator(
                        isScanning = true,
                        label = "$totalDevices devices found"
                    )
                } else {
                    Text(
                        text = "> $totalDevices devices found",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (state.isScanning) {
                    OutlinedButton(
                        onClick = onToggleScan,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = onToggleScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Scan")
                    }
                }
            }
        }

        // Error message
        state.error?.let { error ->
            item {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // SPP connecting indicator
        if (sppState.isConnecting) {
            item {
                Text(
                    text = "> Connecting via SPP...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TerminalAmber,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // SPP error
        sppState.error?.let { error ->
            item {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = TerminalRed
                )
            }
        }

        // Paired devices section
        if (state.pairedDevices.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "> PAIRED DEVICES (${state.pairedDevices.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = TerminalAmber,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(state.pairedDevices, key = { "paired_${it.address}" }) { device ->
                ClassicDeviceItem(
                    device = device,
                    onConnectSpp = { onConnectSpp(device.address) }
                )
            }
        }

        // Discovered devices section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "> DISCOVERED DEVICES (${state.discoveredDevices.size})",
                style = MaterialTheme.typography.labelMedium,
                color = TerminalCyan,
                fontFamily = FontFamily.Monospace
            )
        }

        if (state.discoveredDevices.isEmpty() && !state.isScanning) {
            item {
                EmptyState(
                    icon = Icons.Default.Bluetooth,
                    title = "No devices discovered",
                    subtitle = "Tap Scan to search for nearby Classic Bluetooth devices"
                )
            }
        }

        items(state.discoveredDevices, key = { "disc_${it.address}" }) { device ->
            ClassicDeviceItem(
                device = device,
                onConnectSpp = { onConnectSpp(device.address) }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ClassicDeviceItem(
    device: ClassicBluetoothDevice,
    onConnectSpp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val signalColor = when {
        device.rssi >= -60 -> TerminalGreen
        device.rssi >= -80 -> TerminalAmber
        device.rssi == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> TerminalRed
    }

    TerminalCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = device.majorClass,
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalCyan,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }

                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (device.rssi != 0) {
                        Text(
                            text = "${device.rssi} dBm",
                            style = MaterialTheme.typography.labelSmall,
                            color = signalColor
                        )
                    }
                    Text(
                        text = device.bondStateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (device.isPaired) TerminalGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onConnectSpp,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TerminalCyan
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Connect SPP",
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("SPP", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SppTerminalPanel(
    sppState: SppState,
    deviceAddress: String,
    onSend: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new lines arrive
    LaunchedEffect(sppState.lines.size) {
        if (sppState.lines.isNotEmpty()) {
            listState.animateScrollToItem(sppState.lines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Terminal header
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "> SPP TERMINAL",
                    style = MaterialTheme.typography.labelLarge,
                    color = TerminalGreen,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = deviceAddress,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
            OutlinedButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TerminalRed
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Disconnect",
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Disconnect")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Connection status
        if (sppState.isConnecting) {
            Text(
                text = "> Connecting...",
                style = MaterialTheme.typography.bodySmall,
                color = TerminalAmber,
                fontFamily = FontFamily.Monospace
            )
        }

        sppState.error?.let { error ->
            Text(
                text = "> ERROR: $error",
                style = MaterialTheme.typography.bodySmall,
                color = TerminalRed,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Terminal output area
        TerminalCard(
            modifier = Modifier.weight(1f)
        ) {
            if (sppState.lines.isEmpty()) {
                Text(
                    text = if (sppState.isConnected) "> Connected. Waiting for data..." else "> No data yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(sppState.lines.size) { index ->
                        TerminalLineItem(line = sppState.lines[index])
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Type command...",
                        fontFamily = FontFamily.Monospace
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TerminalGreen
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TerminalGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = TerminalGreen
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onSend(inputText)
                            inputText = ""
                        }
                    }
                ),
                enabled = sppState.isConnected
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSend(inputText)
                        inputText = ""
                    }
                },
                enabled = sppState.isConnected && inputText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (sppState.isConnected && inputText.isNotBlank()) TerminalGreen
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun TerminalLineItem(line: TerminalLine) {
    val prefix = if (line.isOutgoing) "> " else "< "
    val color = if (line.isOutgoing) TerminalGreen else TerminalCyan

    Text(
        text = "$prefix${line.text}",
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontFamily = FontFamily.Monospace
    )
}

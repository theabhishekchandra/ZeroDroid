package com.abhishek.zerodroid.features.ble.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ble.domain.BleUuidDatabase
import com.abhishek.zerodroid.features.ble.domain.CharacteristicDetailState
import com.abhishek.zerodroid.features.ble.domain.CharacteristicValue
import com.abhishek.zerodroid.features.ble.domain.GattCharacteristicInfo
import com.abhishek.zerodroid.features.ble.domain.GattConnectionState
import com.abhishek.zerodroid.features.ble.domain.GattConnectionStatus
import com.abhishek.zerodroid.features.ble.domain.GattServiceInfo
import com.abhishek.zerodroid.features.ble.domain.WriteMode
import com.abhishek.zerodroid.features.ble.viewmodel.GattViewModel
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalAmberGlow
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalCyanGlow
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalGreenGlow
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TextDim
import com.abhishek.zerodroid.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GattExplorerScreen(
    deviceAddress: String,
    deviceName: String?,
    onBack: () -> Unit,
    viewModel: GattViewModel = viewModel(factory = GattViewModel.Factory)
) {
    val connState by viewModel.connectionState.collectAsState()
    val detailState by viewModel.detailState.collectAsState()

    DisposableEffect(deviceAddress) {
        viewModel.connect(deviceAddress)
        onDispose { viewModel.disconnect() }
    }

    if (detailState.info != null) {
        CharacteristicDetailPanel(
            state = detailState,
            onBack = { viewModel.clearDetailState() },
            onRead = { viewModel.readCharacteristic() },
            onWrite = { viewModel.writeCharacteristic() },
            onToggleNotify = { viewModel.toggleNotification() },
            onWriteInputChanged = { viewModel.updateWriteInput(it) },
            onToggleWriteMode = { viewModel.toggleWriteMode() },
            onReadDescriptor = { viewModel.readDescriptor(it) }
        )
    } else {
        ServiceListPanel(
            connectionState = connState,
            deviceName = deviceName,
            deviceAddress = deviceAddress,
            onBack = onBack,
            onCharacteristicSelected = { viewModel.selectCharacteristic(it) }
        )
    }
}

// ===== Panel 1: Service List =====

@Composable
private fun ServiceListPanel(
    connectionState: GattConnectionState,
    deviceName: String?,
    deviceAddress: String,
    onBack: () -> Unit,
    onCharacteristicSelected: (GattCharacteristicInfo) -> Unit
) {
    val expandedServices = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TerminalGreen
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deviceName ?: "Unknown Device",
                        style = MaterialTheme.typography.titleMedium,
                        color = TerminalGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = deviceAddress,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }
                ConnectionBadge(status = connectionState.connectionStatus)
            }
        }

        // Connection info card
        if (connectionState.isConnected) {
            item {
                TerminalCard {
                    Text(
                        text = "> Connection Info",
                        style = MaterialTheme.typography.titleSmall,
                        color = TerminalGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoColumn(label = "MTU", value = "${connectionState.mtu}")
                        InfoColumn(label = "Payload", value = "${connectionState.payloadSize} B")
                        InfoColumn(label = "Services", value = "${connectionState.services.size}")
                        InfoColumn(label = "Chars", value = "${connectionState.totalCharacteristics}")
                    }
                }
            }
        }

        // Loading / connecting state
        if (connectionState.connectionStatus == GattConnectionStatus.Connecting) {
            item {
                TerminalCard(animated = true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = TerminalGreen
                        )
                        Text(
                            text = "Connecting...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TerminalGreen
                        )
                    }
                }
            }
        }

        // Error
        connectionState.error?.let { error ->
            item {
                TerminalCard(glowColor = TerminalRed) {
                    Text(
                        text = "> Error: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = TerminalRed
                    )
                }
            }
        }

        // Empty state
        if (connectionState.services.isEmpty() && connectionState.connectionStatus == GattConnectionStatus.Disconnected) {
            item {
                EmptyState(
                    icon = Icons.Default.BluetoothDisabled,
                    title = "Not connected",
                    subtitle = "Connection to the GATT server was lost or could not be established"
                )
            }
        }

        // Service cards
        items(connectionState.services, key = { it.uuid }) { service ->
            val isExpanded = expandedServices[service.uuid] ?: false
            ServiceCard(
                service = service,
                isExpanded = isExpanded,
                onToggleExpand = { expandedServices[service.uuid] = !isExpanded },
                onCharacteristicSelected = onCharacteristicSelected
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ConnectionBadge(status: GattConnectionStatus) {
    val (color, label) = when (status) {
        GattConnectionStatus.Connected -> TerminalGreen to "Connected"
        GattConnectionStatus.Connecting -> TerminalAmber to "Connecting"
        GattConnectionStatus.Disconnecting -> TerminalAmber to "Disconnecting"
        GattConnectionStatus.Disconnected -> TerminalRed to "Disconnected"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            color = TerminalGreen
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceCard(
    service: GattServiceInfo,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCharacteristicSelected: (GattCharacteristicInfo) -> Unit
) {
    TerminalCard(onClick = onToggleExpand) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TerminalGreen
                )
                Text(
                    text = BleUuidDatabase.shortenUuid(service.uuid),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextDim
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${service.characteristics.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = TextDim.copy(alpha = 0.3f))
                service.characteristics.forEach { char ->
                    CharacteristicRow(
                        characteristic = char,
                        onClick = { onCharacteristicSelected(char) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacteristicRow(
    characteristic: GattCharacteristicInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = characteristic.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = BleUuidDatabase.shortenUuid(characteristic.uuid),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextDim,
                    fontSize = 10.sp
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    characteristic.propertiesList.forEach { prop ->
                        PropertyBadge(property = prop)
                    }
                }
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Details",
            tint = TextDim,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun PropertyBadge(property: String) {
    val color = when (property) {
        "Read" -> TerminalGreen
        "Write", "WriteNoResp", "SignedWrite" -> TerminalAmber
        "Notify", "Indicate" -> TerminalCyan
        else -> TextSecondary
    }
    Text(
        text = property,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontSize = 9.sp,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

// ===== Panel 2: Characteristic Detail =====

@Composable
private fun CharacteristicDetailPanel(
    state: CharacteristicDetailState,
    onBack: () -> Unit,
    onRead: () -> Unit,
    onWrite: () -> Unit,
    onToggleNotify: () -> Unit,
    onWriteInputChanged: (String) -> Unit,
    onToggleWriteMode: () -> Unit,
    onReadDescriptor: (String) -> Unit
) {
    val info = state.info ?: return
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TerminalGreen
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TerminalGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = info.uuid,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Action buttons
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (info.isReadable) {
                    Button(
                        onClick = onRead,
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Read", color = TerminalGreen)
                    }
                }
                if (info.isNotifiable || info.isIndicatable) {
                    Button(
                        onClick = onToggleNotify,
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isNotifying) TerminalCyan.copy(alpha = 0.3f)
                            else TerminalCyan.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (state.isNotifying) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = TerminalCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (state.isNotifying) "Stop" else if (info.isIndicatable) "Indicate" else "Notify",
                            color = TerminalCyan
                        )
                    }
                }
            }
        }

        // Loading indicator
        if (state.isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = TerminalGreen
                    )
                }
            }
        }

        // Error
        state.error?.let { error ->
            item {
                TerminalCard(glowColor = TerminalRed) {
                    Text(
                        text = "> $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = TerminalRed
                    )
                }
            }
        }

        // Write section
        if (info.isWritable) {
            item {
                TerminalCard(
                    glowColor = TerminalAmber,
                    glowAlpha = TerminalAmberGlow,
                    borderColor = TerminalAmber.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "> Write Value",
                        style = MaterialTheme.typography.titleSmall,
                        color = TerminalAmber
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.writeInput,
                            onValueChange = onWriteInputChanged,
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    if (state.writeMode == WriteMode.Hex) "FF 00 1A..." else "Hello...",
                                    color = TextDim
                                )
                            },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TerminalAmber
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TerminalAmber,
                                unfocusedBorderColor = TerminalAmber.copy(alpha = 0.3f),
                                cursorColor = TerminalAmber
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onToggleWriteMode,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalAmber)
                        ) {
                            Text(
                                if (state.writeMode == WriteMode.Hex) "HEX" else "TXT",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Button(
                            onClick = onWrite,
                            enabled = !state.isLoading && state.writeInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalAmber.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = TerminalAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send", color = TerminalAmber)
                        }
                    }
                }
            }
        }

        // Value display card
        state.lastReadValue?.let { value ->
            item {
                ValueDisplayCard(
                    value = value,
                    parsedDisplay = state.parsedDisplay,
                    timeFormatter = timeFormatter
                )
            }
        }

        // Notification log
        if (state.notificationValues.isNotEmpty()) {
            item {
                TerminalCard(
                    glowColor = TerminalCyan,
                    glowAlpha = TerminalCyanGlow,
                    borderColor = TerminalCyan.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "> Notification Log (${state.notificationValues.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = TerminalCyan
                    )
                }
            }
            items(
                state.notificationValues.reversed().take(50),
                key = { it.timestamp }
            ) { notifValue ->
                TerminalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = timeFormatter.format(Date(notifValue.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextDim,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${notifValue.byteCount} B",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDim,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = notifValue.hexString,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = TerminalCyan
                    )
                }
            }
        }

        // Descriptor list
        if (info.descriptors.isNotEmpty()) {
            item {
                TerminalCard {
                    Text(
                        text = "> Descriptors (${info.descriptors.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = TerminalGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    info.descriptors.forEach { desc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = desc.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BleUuidDatabase.shortenUuid(desc.uuid),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextDim,
                                    fontSize = 10.sp
                                )
                                // Show value if read
                                state.descriptorValues[desc.uuid]?.let { descValue ->
                                    Text(
                                        text = descValue.hexString,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = TerminalGreen,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { onReadDescriptor(desc.uuid) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TerminalGreen
                                )
                            ) {
                                Text("Read", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ValueDisplayCard(
    value: CharacteristicValue,
    parsedDisplay: String?,
    timeFormatter: SimpleDateFormat
) {
    TerminalCard(glowColor = TerminalGreen, glowAlpha = TerminalGreenGlow) {
        Text(
            text = "> Value",
            style = MaterialTheme.typography.titleSmall,
            color = TerminalGreen
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Parsed display
        parsedDisplay?.let { parsed ->
            Text(
                text = parsed,
                style = MaterialTheme.typography.bodyMedium,
                color = TerminalGreen
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Hex dump
        Text(
            text = "HEX:",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
        Text(
            text = value.hexString,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        // ASCII
        Text(
            text = "ASCII:",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
        Text(
            text = value.asciiString,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Metadata
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${value.byteCount} bytes",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
            Text(
                text = timeFormatter.format(Date(value.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = TextDim
            )
        }
    }
}

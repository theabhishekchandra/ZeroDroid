package com.abhishek.zerodroid.features.ble.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdCardShape
import com.abhishek.zerodroid.core.ui.zd.ZdDivider
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdScanControlBar
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.features.ble.domain.BleUuidDatabase
import com.abhishek.zerodroid.features.ble.domain.CharacteristicDetailState
import com.abhishek.zerodroid.features.ble.domain.CharacteristicValue
import com.abhishek.zerodroid.features.ble.domain.GattCharacteristicInfo
import com.abhishek.zerodroid.features.ble.domain.GattConnectionState
import com.abhishek.zerodroid.features.ble.domain.GattConnectionStatus
import com.abhishek.zerodroid.features.ble.domain.GattServiceInfo
import com.abhishek.zerodroid.features.ble.domain.WriteMode
import com.abhishek.zerodroid.features.ble.viewmodel.GattViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GATT Explorer: connect to one BLE device and browse the services and characteristics it
 * publishes, read and write values, subscribe to notifications, or dump everything.
 * System back leaves a characteristic before it leaves the device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GattExplorerScreen(
    deviceAddress: String,
    deviceName: String?,
    viewModel: GattViewModel = hiltViewModel()
) {
    val connState by viewModel.connectionState.collectAsState()
    val detailState by viewModel.detailState.collectAsState()
    var showDump by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(deviceAddress) {
        viewModel.connect(deviceAddress)
        onDispose { viewModel.disconnect() }
    }

    BackHandler(enabled = detailState.info != null) { viewModel.clearDetailState() }

    Column(Modifier.fillMaxSize()) {
        ZdScanControlBar(
            running = connState.isConnected || connState.connectionStatus == GattConnectionStatus.Connecting,
            onStart = { viewModel.connect(deviceAddress) },
            onStop = { viewModel.disconnect() },
            runningLabel = when (connState.connectionStatus) {
                GattConnectionStatus.Connecting -> "Connecting"
                GattConnectionStatus.Disconnecting -> "Disconnecting"
                else -> "Connected"
            },
            runningDetail = if (connState.isConnected) "MTU ${connState.mtu} · ${connState.services.size} services" else deviceAddress,
            idleLabel = "Disconnected",
            idleDetail = "${deviceName ?: deviceAddress} · tap to reconnect",
            startLabel = "Connect",
            stopLabel = "Disconnect"
        )

        val info = detailState.info
        if (info != null) {
            CharacteristicDetail(
                info = info,
                state = detailState,
                onRead = viewModel::readCharacteristic,
                onWrite = viewModel::writeCharacteristic,
                onToggleNotify = viewModel::toggleNotification,
                onWriteInputChanged = viewModel::updateWriteInput,
                onToggleWriteMode = viewModel::toggleWriteMode,
                onReadDescriptor = viewModel::readDescriptor
            )
        } else {
            ServiceList(
                state = connState,
                deviceName = deviceName,
                deviceAddress = deviceAddress,
                onDumpAll = { showDump = true },
                onCharacteristicSelected = viewModel::selectCharacteristic
            )
        }
    }

    if (showDump) {
        ModalBottomSheet(
            onDismissRequest = { showDump = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ZdColors.Surface,
            scrimColor = ZdColors.Scrim,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
        ) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Device dump", style = ZdType.Heading, color = ZdColors.Text)
                Text("Reads every readable characteristic into one JSON file you can copy, save or replay.", style = ZdType.BodySmall, color = ZdColors.Text2)
                BleDeviceDumpPanel(explorer = viewModel.explorer, connectionState = connState)
            }
        }
    }
}

// ── Service list ─────────────────────────────────────────────────────────────

@Composable
private fun ServiceList(
    state: GattConnectionState,
    deviceName: String?,
    deviceAddress: String,
    onDumpAll: () -> Unit,
    onCharacteristicSelected: (GattCharacteristicInfo) -> Unit
) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    if (state.services.isEmpty() && state.connectionStatus == GattConnectionStatus.Disconnected) {
        ZdStatePanel(
            kicker = if (state.error != null) "Connection failed" else "Not connected",
            kickerColor = if (state.error != null) ZdColors.Critical else ZdColors.Text3,
            icon = ZdIcons.Bluetooth,
            title = deviceName ?: deviceAddress,
            body = state.error ?: "The device may be out of range, asleep, or already connected to another phone. Tap Connect to try again.",
            note = "GATT is the table a BLE device publishes: services group related values, and each characteristic is one value you can read, write or subscribe to."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ZdCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(deviceName ?: "[no name]", style = ZdType.Heading, color = ZdColors.Text)
                        Text(
                            if (state.isConnected) "Connected · MTU ${state.mtu} · ${state.services.size} services"
                            else deviceAddress,
                            style = ZdType.Caption,
                            color = ZdColors.Text3
                        )
                    }
                    if (state.connectionStatus == GattConnectionStatus.Connecting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ZdColors.Accent)
                    } else {
                        ZdButton("Dump all", onClick = onDumpAll, variant = ZdButtonVariant.Secondary, icon = ZdIcons.Download, enabled = state.isConnected, height = 40.dp)
                    }
                }
                if (state.isConnected) {
                    Row {
                        ZdStat("MTU", "${state.mtu}", Modifier.weight(1f))
                        ZdStat("Payload", "${state.payloadSize} B", Modifier.weight(1f))
                        ZdStat("Services", "${state.services.size}", Modifier.weight(1f))
                        ZdStat("Chars", "${state.totalCharacteristics}", Modifier.weight(1f))
                    }
                }
            }
        }

        state.error?.let { error ->
            item { ZdFootnote(error, icon = ZdIcons.Warning) }
        }

        items(state.services, key = { it.uuid }) { service ->
            val isExpanded = expanded[service.uuid] ?: (state.services.size <= 3)
            ServiceCard(
                service = service,
                isExpanded = isExpanded,
                onToggle = { expanded[service.uuid] = !isExpanded },
                onCharacteristicSelected = onCharacteristicSelected
            )
        }

        if (state.services.any { s -> s.characteristics.any { it.isWritable } }) {
            item {
                ZdCard(background = ZdColors.MediumBg, borderColor = ZdColors.MediumBorder) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(ZdIcons.Warning, contentDescription = null, tint = ZdColors.Medium, modifier = Modifier.size(18.dp))
                        Text(
                            "Writing to unknown characteristics can change device settings or brick firmware. Only write to devices you own.",
                            style = ZdType.BodySmall,
                            color = ZdColors.Text
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(
    service: GattServiceInfo,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onCharacteristicSelected: (GattCharacteristicInfo) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(ZdCardShape)
            .background(ZdColors.Surface)
            .border(1.dp, ZdColors.Border, ZdCardShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clickable(onClick = onToggle, onClickLabel = if (isExpanded) "Collapse" else "Expand")
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(service.displayName, style = ZdType.Label, color = ZdColors.Text)
                Text(BleUuidDatabase.shortenUuid(service.uuid), style = ZdType.Path, color = ZdColors.Text3)
            }
            Text("${service.characteristics.size}", style = ZdType.Mono, color = ZdColors.Text3)
            Icon(
                if (isExpanded) ZdIcons.ChevronDown else ZdIcons.Chevron,
                contentDescription = null,
                tint = ZdColors.Text3,
                modifier = Modifier.size(16.dp)
            )
        }
        AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column {
                service.characteristics.forEach { char ->
                    ZdDivider()
                    CharacteristicRow(char, onClick = { onCharacteristicSelected(char) })
                }
            }
        }
    }
}

@Composable
private fun CharacteristicRow(characteristic: GattCharacteristicInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(start = 26.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(characteristic.displayName, style = ZdType.BodySmall, color = ZdColors.Text)
            Text(
                "${BleUuidDatabase.shortenUuid(characteristic.uuid)} · ${characteristic.propertiesList.joinToString(", ") { it.lowercase() }}",
                style = ZdType.Path,
                color = ZdColors.Text3
            )
        }
        Icon(ZdIcons.Chevron, contentDescription = null, tint = ZdColors.Text3, modifier = Modifier.size(16.dp))
    }
}

// ── Characteristic detail ────────────────────────────────────────────────────

@Composable
private fun CharacteristicDetail(
    info: GattCharacteristicInfo,
    state: CharacteristicDetailState,
    onRead: () -> Unit,
    onWrite: () -> Unit,
    onToggleNotify: () -> Unit,
    onWriteInputChanged: (String) -> Unit,
    onToggleWriteMode: () -> Unit,
    onReadDescriptor: (String) -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ZdCard {
                Text(info.displayName, style = ZdType.Heading, color = ZdColors.Text)
                Text(info.uuid, style = ZdType.Path, color = ZdColors.Text3)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    info.propertiesList.forEach { PropertyTag(it) }
                }
                if (info.isReadable || info.isNotifiable || info.isIndicatable) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                        if (info.isReadable) {
                            ZdButton("Read", onClick = onRead, enabled = !state.isLoading, modifier = Modifier.weight(1f))
                        }
                        if (info.isNotifiable || info.isIndicatable) {
                            ZdButton(
                                text = if (state.isNotifying) "Stop" else if (info.isIndicatable) "Indicate" else "Notify",
                                onClick = onToggleNotify,
                                enabled = !state.isLoading,
                                variant = if (state.isNotifying) ZdButtonVariant.Danger else ZdButtonVariant.Secondary,
                                icon = ZdIcons.Bell,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ZdColors.Accent)
                }
            }
        }

        state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }

        state.lastReadValue?.let { value ->
            item { ValueCard(value, state.parsedDisplay, timeFormatter) }
        }

        if (info.isWritable) {
            item {
                WriteCard(
                    input = state.writeInput,
                    mode = state.writeMode,
                    enabled = !state.isLoading,
                    onInput = onWriteInputChanged,
                    onToggleMode = onToggleWriteMode,
                    onWrite = onWrite
                )
            }
        }

        if (state.notificationValues.isNotEmpty()) {
            item { ZdSectionLabel("Notifications", trailingText = "${state.notificationValues.size}") }
            item {
                ZdListCard(state.notificationValues.reversed().take(50)) { v ->
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row {
                            Text(timeFormatter.format(Date(v.timestamp)), style = ZdType.Path, color = ZdColors.Text3, modifier = Modifier.weight(1f))
                            Text("${v.byteCount} B", style = ZdType.Path, color = ZdColors.Text3)
                        }
                        Text(v.hexString, style = ZdType.Mono, color = ZdColors.Info)
                    }
                }
            }
        }

        if (info.descriptors.isNotEmpty()) {
            item { ZdSectionLabel("Descriptors", trailingText = "${info.descriptors.size}") }
            item {
                ZdListCard(info.descriptors) { desc ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(desc.displayName, style = ZdType.BodySmall, color = ZdColors.Text)
                            Text(BleUuidDatabase.shortenUuid(desc.uuid), style = ZdType.Path, color = ZdColors.Text3)
                            state.descriptorValues[desc.uuid]?.let { Text(it.hexString, style = ZdType.Mono, color = ZdColors.Accent) }
                        }
                        ZdButton("Read", onClick = { onReadDescriptor(desc.uuid) }, variant = ZdButtonVariant.Secondary, height = 36.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyTag(property: String) {
    val (fg, bg) = when (property) {
        "Read" -> ZdColors.Accent to ZdColors.AccentBg
        "Write", "WriteNoResp", "SignedWrite" -> ZdColors.Medium to ZdColors.MediumBg
        "Notify", "Indicate" -> ZdColors.Info to ZdColors.InfoBg
        else -> ZdColors.Text2 to ZdColors.Surface2
    }
    ZdTag(property.uppercase(), color = fg, background = bg, border = bg)
}

@Composable
private fun ValueCard(value: CharacteristicValue, parsed: String?, timeFormatter: SimpleDateFormat) {
    ZdCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Value", style = ZdType.Label, color = ZdColors.Text2, modifier = Modifier.weight(1f))
            Text("${value.byteCount} B · ${timeFormatter.format(Date(value.timestamp))}", style = ZdType.Path, color = ZdColors.Text3)
        }
        if (parsed != null) {
            Text(parsed, style = ZdType.Heading.copy(fontSize = ZdType.Title.fontSize), color = ZdColors.Accent)
        }
        ZdStat("Hex", value.hexString.ifEmpty { "—" }, valueColor = ZdColors.Text)
        ZdStat("ASCII", value.asciiString.ifEmpty { "—" }, valueColor = ZdColors.Text2)
    }
}

@Composable
private fun WriteCard(
    input: String,
    mode: WriteMode,
    enabled: Boolean,
    onInput: (String) -> Unit,
    onToggleMode: () -> Unit,
    onWrite: () -> Unit
) {
    ZdCard(borderColor = ZdColors.MediumBorder) {
        Text("Write value", style = ZdType.Label, color = ZdColors.Medium)
        val shape = RoundedCornerShape(10.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(ZdColors.Bg)
                .border(1.dp, ZdColors.BorderStrong, shape)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (input.isEmpty()) {
                Text(if (mode == WriteMode.Hex) "FF 00 1A…" else "Hello…", style = ZdType.Mono.copy(fontSize = ZdType.Button.fontSize), color = ZdColors.Text3)
            }
            BasicTextField(
                value = input,
                onValueChange = onInput,
                singleLine = true,
                textStyle = ZdType.Mono.copy(fontSize = ZdType.Button.fontSize, color = ZdColors.Text),
                cursorBrush = SolidColor(ZdColors.Accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Value to write" }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZdButton(
                if (mode == WriteMode.Hex) "HEX" else "TEXT",
                onClick = onToggleMode,
                variant = ZdButtonVariant.Secondary,
                modifier = Modifier.weight(1f)
            )
            ZdButton(
                "Send",
                onClick = onWrite,
                enabled = enabled && input.isNotBlank(),
                icon = ZdIcons.Send,
                modifier = Modifier.weight(1f)
            )
        }
        Text("Tap HEX / TEXT to switch how your input is encoded.", style = ZdType.Caption, color = ZdColors.Text3)
    }
}

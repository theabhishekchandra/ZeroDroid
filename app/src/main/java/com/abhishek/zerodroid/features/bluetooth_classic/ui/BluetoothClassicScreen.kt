package com.abhishek.zerodroid.features.bluetooth_classic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCardShape
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconButton
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdScanControlBar
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.ZdTextField
import com.abhishek.zerodroid.core.ui.zd.ZdToolScanBar
import com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothClassicState
import com.abhishek.zerodroid.features.bluetooth_classic.domain.ClassicBluetoothDevice
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SppState
import com.abhishek.zerodroid.features.bluetooth_classic.domain.TerminalLine
import com.abhishek.zerodroid.features.bluetooth_classic.viewmodel.BluetoothClassicViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DISCOVERY_MS = 12_000L

/** OBD-II / ELM327 commands offered as one-tap chips in the serial terminal. */
private val quickCommands = listOf("ATZ", "ATI", "ATSP0", "0100", "010C", "010D")

@Composable
fun BluetoothClassicScreen(
    viewModel: BluetoothClassicViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.blePermissions(),
        rationale = "Android asks for these before any app can find or connect to Bluetooth devices."
    ) {
        BluetoothClassicContent(viewModel = viewModel)
    }
}

@Composable
private fun BluetoothClassicContent(viewModel: BluetoothClassicViewModel) {
    val state by viewModel.state.collectAsState()
    val sppState by viewModel.sppState.collectAsState()
    val sdp by viewModel.sdp.collectAsState()

    // Discovery is released in the background; the SPP link survives until the screen closes.
    HardwareLifecycleEffect(
        isActive = state.isScanning,
        onPause = viewModel::stopScan,
        onResume = viewModel::startScan
    )
    DisposableEffect(Unit) {
        onDispose { viewModel.disconnectSpp() }
    }

    var terminalFor by rememberSaveable { mutableStateOf<String?>(null) }

    val target = terminalFor
    val sdpAddress = sdp.address
    if (target == null && sdpAddress != null) {
        BackHandler(onBack = viewModel::closeServices)
        SdpServicePanel(
            state = sdp,
            onQuery = viewModel::querySdp,
            onOpenTerminal = {
                viewModel.closeServices()
                terminalFor = sdpAddress
                viewModel.connectSpp(sdpAddress)
            },
            onClose = viewModel::closeServices
        )
    } else if (target != null) {
        val device = (state.pairedDevices + state.discoveredDevices).firstOrNull { it.address == target }
        SppTerminal(
            state = sppState,
            title = device?.displayName ?: target,
            onSend = viewModel::sendSpp,
            onReconnect = { viewModel.connectSpp(target) },
            onClose = {
                viewModel.disconnectSpp()
                terminalFor = null
            }
        )
    } else {
        DeviceList(
            state = state,
            sppState = sppState,
            onStart = viewModel::startScan,
            onStop = viewModel::stopScan,
            onConnect = { address ->
                terminalFor = address
                viewModel.connectSpp(address)
            },
            onOpenServices = viewModel::openServices
        )
    }
}

@Composable
private fun DeviceList(
    state: BluetoothClassicState,
    sppState: SppState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onConnect: (String) -> Unit,
    onOpenServices: (ClassicBluetoothDevice) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ZdToolScanBar(
            running = state.isScanning,
            onStart = onStart,
            onStop = onStop,
            verb = "Discovering",
            runningNote = "Inquiry",
            autoStopMs = DISCOVERY_MS,
            idleNote = if (state.discoveredDevices.isEmpty()) "Finds devices in pairing mode" else "Results kept"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
            sppState.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }

            if (state.pairedDevices.isNotEmpty()) {
                item { ZdSectionLabel("Paired", trailingText = "${state.pairedDevices.size}") }
                item { ZdListCard(state.pairedDevices) { DeviceRow(it, onConnect, onOpenServices) } }
            }

            item { ZdSectionLabel("Discovered now", trailingText = "${state.discoveredDevices.size}") }
            if (state.discoveredDevices.isEmpty()) {
                item {
                    ZdStatePanel(
                        kicker = if (state.isScanning) "Discovering" else "Ready",
                        icon = ZdIcons.Bluetooth,
                        iconTint = ZdColors.Accent,
                        iconBackground = ZdColors.AccentBg,
                        title = "Speakers, cars and serial modules",
                        body = "Classic Bluetooth finds devices that are in pairing mode. Connect to one with a Serial Port (SPP) service to open a text terminal, e.g. an OBD-II car adapter or HC-05 module.",
                        primaryAction = if (state.isScanning) null else "Start discovery" to onStart,
                        primaryIcon = ZdIcons.Play,
                        fullScreen = false
                    )
                }
            } else {
                item { ZdListCard(state.discoveredDevices) { DeviceRow(it, onConnect, onOpenServices) } }
                item { ZdFootnote("Tap a device to see the services it offers.") }
            }
            item { ZdFootnote("Only connect to devices you own: serial commands can change a device’s settings.") }
        }
    }
}

@Composable
private fun DeviceRow(device: ClassicBluetoothDevice, onConnect: (String) -> Unit, onOpenServices: (ClassicBluetoothDevice) -> Unit) {
    ZdListRow(
        onClick = { onOpenServices(device) },
        title = device.displayName,
        subtitle = listOfNotNull(
            device.majorClass.takeIf { it.isNotBlank() },
            device.minorClass.takeIf { it.isNotBlank() },
            device.rssi.takeIf { it != 0 }?.let { "$it dBm" }
        ).joinToString(" · ").ifEmpty { device.address },
        leading = { ZdIconTile(ZdIcons.Bluetooth) },
        trailing = {
            if (device.isPaired) ZdTag("PAIRED", color = ZdColors.Accent, background = ZdColors.AccentBg, border = ZdColors.AccentBorder)
            else ZdTag("NEW")
            ZdButton("SPP", onClick = { onConnect(device.address) }, variant = ZdButtonVariant.Secondary, height = 36.dp)
        }
    )
}

private val lineTime = SimpleDateFormat("HH:mm:ss", Locale.US)

@Composable
private fun SppTerminal(
    state: SppState,
    title: String,
    onSend: (String) -> Unit,
    onReconnect: () -> Unit,
    onClose: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val send: (String) -> Unit = { text ->
        if (text.isNotBlank() && state.isConnected) {
            onSend(text)
            input = ""
        }
    }

    LaunchedEffect(state.lines.size) {
        if (state.lines.isNotEmpty()) listState.animateScrollToItem(state.lines.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        ZdScanControlBar(
            running = state.isConnected || state.isConnecting,
            onStart = onReconnect,
            onStop = onClose,
            runningLabel = if (state.isConnecting) "Connecting" else "Connected · SPP",
            runningDetail = title,
            idleLabel = "Disconnected",
            idleDetail = "Log kept · tap to reconnect",
            startLabel = "Connect",
            stopLabel = "Disconnect"
        )

        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = ZdType.Label, color = ZdColors.Text, modifier = Modifier.weight(1f))
            ZdIconButton(ZdIcons.Close, contentDescription = "Close terminal", onClick = onClose)
        }

        ZdChipRow {
            quickCommands.forEach { cmd -> ZdChip(cmd, selected = false, onClick = { send(cmd) }) }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(ZdCardShape)
                .background(ZdColors.Bg)
                .border(1.dp, ZdColors.Border, ZdCardShape),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (state.lines.isEmpty()) {
                item {
                    Text(
                        if (state.isConnected) "Connected. Send a command or wait for data." else "No data yet.",
                        style = ZdType.Mono,
                        color = ZdColors.Text3
                    )
                }
            }
            items(state.lines) { TerminalLineRow(it) }
            state.error?.let { item { Text(it, style = ZdType.Mono, color = ZdColors.Critical) } }
        }

        ZdTextField(
            value = input,
            onValueChange = { input = it },
            label = "COMMAND",
            placeholder = "Type a command…",
            enabled = state.isConnected,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send(input) }),
            trailing = {
                ZdIconButton(ZdIcons.Send, contentDescription = "Send", onClick = { send(input) }, tint = if (state.isConnected && input.isNotBlank()) ZdColors.Accent else ZdColors.Text3)
            },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun TerminalLineRow(line: TerminalLine) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(lineTime.format(Date(line.timestamp)), style = ZdType.Path, color = ZdColors.Text3, modifier = Modifier.width(60.dp))
        Text(if (line.isOutgoing) ">" else "<", style = ZdType.Mono, color = if (line.isOutgoing) ZdColors.Accent else ZdColors.Info)
        Text(line.text, style = ZdType.Mono, color = if (line.isOutgoing) ZdColors.Text else ZdColors.Info)
    }
}

package com.abhishek.zerodroid.features.ble.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconButton
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSignal
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdSwitchRow
import com.abhishek.zerodroid.core.ui.zd.ZdTabs
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.ZdToolScanBar
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.ble.domain.BleDeviceTypeIdentifier
import com.abhishek.zerodroid.features.ble.domain.BleDistanceEstimator
import com.abhishek.zerodroid.features.ble.viewmodel.BleViewModel
import com.abhishek.zerodroid.features.ble.viewmodel.HciSnoopViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

private const val AUTO_STOP_MS = 30_000L
private const val WEAK_RSSI = -85


@Composable
fun BleScreen(
    onOpenDevice: (address: String, name: String?, kind: String) -> Unit = { _, _, _ -> },
    viewModel: BleViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.blePermissions(),
        rationale = "Android asks for these before any app can see Bluetooth devices."
    ) {
        BleContent(viewModel = viewModel, onOpenDevice = onOpenDevice)
    }
}

@Composable
private fun BleContent(viewModel: BleViewModel, onOpenDevice: (String, String?, String) -> Unit) {
    val scanState by viewModel.scanState.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    HardwareLifecycleEffect(
        isActive = scanState.isScanning,
        onPause = viewModel::stopScan,
        onResume = viewModel::startScan
    )

    Column(Modifier.fillMaxSize()) {
        ZdToolScanBar(
            running = scanState.isScanning,
            onStart = viewModel::startScan,
            onStop = viewModel::stopScan,
            autoStopMs = AUTO_STOP_MS,
            idleNote = if (scanState.devices.isEmpty()) "Scans run for 30 s" else "Results kept"
        )
        ZdTabs(
            tabs = listOf("Devices", "HCI log"),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
        )
        when (tab) {
            0 -> DevicesTab(viewModel, onOpenDevice)
            else -> HciSnoopPanel(viewModel = hiltViewModel<HciSnoopViewModel>())
        }
    }
}

@Composable
private fun DevicesTab(viewModel: BleViewModel, onOpenDevice: (String, String?, String) -> Unit) {
    val scanState by viewModel.scanState.collectAsState()
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var hideWeak by rememberSaveable { mutableStateOf(false) }

    if (!scanState.isBluetoothEnabled) {
        ZdStatePanel(
            kicker = "Bluetooth is off",
            kickerColor = ZdColors.Medium,
            icon = ZdIcons.Bluetooth,
            title = "Turn on Bluetooth to scan",
            body = "Switch Bluetooth on from Quick Settings, then tap Start. Nothing else about your phone changes.",
            chipsLabel = "Still works without Bluetooth",
            chips = listOf("WiFi Analyzer", "NFC", "Sensors", "GPS")
        )
        return
    }

    if (scanState.devices.isEmpty()) {
        ZdStatePanel(
            kicker = if (scanState.isScanning) "Scanning" else scanState.error?.let { "Scan failed" } ?: "Ready",
            kickerColor = if (scanState.error != null) ZdColors.Critical else ZdColors.Text3,
            icon = ZdIcons.Bluetooth,
            iconTint = ZdColors.Accent,
            iconBackground = ZdColors.AccentBg,
            title = if (scanState.isScanning) "Looking for devices…" else "Find Bluetooth devices around you",
            body = scanState.error
                ?: if (scanState.isScanning) "Devices appear as they advertise. Most show up within 10 seconds."
                else "Headphones, watches, beacons, smart-home gear and trackers. You’ll see what each device is, about how far away it is, and anything that looks like a tracker.",
            primaryAction = if (scanState.isScanning) null else "Start scan" to viewModel::startScan,
            primaryIcon = ZdIcons.Play
        )
        return
    }

    val typed = scanState.devices.map { it to BleDeviceTypeIdentifier.identify(it.name, it.serviceUuids).category }
    val counts = typed.groupingBy { it.second }.eachCount()
    val shown = typed
        .filter { (d, c) -> (category == null || c == category) && (!hideWeak || d.rssi >= WEAK_RSSI) }
        .sortedByDescending { it.first.rssi }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ZdCard(verticalSpacing = 4.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${scanState.devices.size} devices nearby", style = ZdType.Heading, color = ZdColors.Text, modifier = Modifier.weight(1f))
                    val trackers = counts["Tracker"] ?: 0
                    if (trackers > 0) {
                        ZdTag("$trackers TRACKER${if (trackers > 1) "S" else ""}", color = ZdColors.Medium, background = ZdColors.MediumBg, border = ZdColors.MediumBorder)
                    }
                }
                Text(
                    counts.entries.sortedByDescending { it.value }.joinToString(" · ") { "${it.value} ${it.key.lowercase()}" },
                    style = ZdType.Caption,
                    color = ZdColors.Text3
                )
            }
        }
        item {
            ZdChipRow(contentPadding = 0.dp) {
                ZdChip("All ${scanState.devices.size}", selected = category == null, onClick = { category = null })
                counts.entries.sortedByDescending { it.value }.forEach { (c, n) ->
                    ZdChip("$c $n", selected = category == c, onClick = { category = c })
                }
            }
        }
        item {
            ZdSwitchRow(
                label = "Hide weak signals",
                checked = hideWeak,
                onCheckedChange = { hideWeak = it },
                trailingText = "< $WEAK_RSSI dBm"
            )
        }
        item {
            if (shown.isEmpty()) {
                ZdFootnote("No devices match these filters.")
            } else {
                ZdListCard(shown) { (device, cat) ->
                    DeviceRow(
                        device = device,
                        category = cat,
                        onOpen = { onOpenDevice(device.address, device.name, if (cat == "Tracker") "TRACKER" else "BLE") },
                        onBookmark = { viewModel.toggleBookmark(device) }
                    )
                }
            }
        }
        item {
            ZdFootnote("Distance is estimated from signal strength and can be off by 2× indoors. Tap a device for its history and GATT services.")
        }
    }
}

@Composable
private fun DeviceRow(device: BleDevice, category: String, onOpen: () -> Unit, onBookmark: () -> Unit) {
    val isTracker = category == "Tracker"
    ZdListRow(
        title = device.name ?: "[no name]",
        subtitle = "$category · ${BleDistanceEstimator.rangeLabel(device.rssi)}",
        leading = { ZdSignal(device.rssi) },
        onClick = onOpen,
        supporting = if (isTracker) {
            {
                ZdTag("TRACKER", color = ZdColors.Medium, background = ZdColors.MediumBg, border = ZdColors.MediumBorder)
            }
        } else null,
        trailing = {
            ZdIconButton(
                icon = ZdIcons.Pin,
                contentDescription = if (device.isBookmarked) "Remove bookmark" else "Bookmark",
                onClick = onBookmark,
                tint = if (device.isBookmarked) ZdColors.Accent else ZdColors.Text3
            )
        }
    )
}

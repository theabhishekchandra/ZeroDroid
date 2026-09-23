package com.abhishek.zerodroid.features.network_scanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdScanControlBar
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.features.network_scanner.domain.NetworkDevice
import com.abhishek.zerodroid.features.network_scanner.domain.VulnerabilityLevel
import com.abhishek.zerodroid.features.network_scanner.viewmodel.NetworkScannerViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

@Composable
fun NetworkScannerScreen(
    viewModel: NetworkScannerViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.wifiPermissions(),
        rationale = "Android needs location access before an app can read which WiFi network you’re on."
    ) {
        NetworkScannerContent(viewModel)
    }
}

private fun VulnerabilityLevel.zd(): ZdSeverity = when (this) {
    VulnerabilityLevel.CRITICAL -> ZdSeverity.CRITICAL
    VulnerabilityLevel.HIGH -> ZdSeverity.HIGH
    VulnerabilityLevel.MEDIUM -> ZdSeverity.MEDIUM
    VulnerabilityLevel.LOW, VulnerabilityLevel.INFO -> ZdSeverity.LOW
}

private fun NetworkDevice.worst(): VulnerabilityLevel? = vulnerabilities.minByOrNull { it.level.ordinal }?.level

@Composable
private fun NetworkScannerContent(viewModel: NetworkScannerViewModel) {
    val state by viewModel.state.collectAsState()
    var type by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedIp by rememberSaveable { mutableStateOf<String?>(null) }

    HardwareLifecycleEffect(
        isActive = state.isScanning,
        onPause = viewModel::stopScan,
        onResume = viewModel::startScan
    )

    Column(Modifier.fillMaxSize()) {
        ZdScanControlBar(
            running = state.isScanning,
            onStart = viewModel::startScan,
            onStop = viewModel::stopScan,
            runningLabel = "${state.scanPhase} · ${(state.progress * 100).toInt()}%",
            runningDetail = state.currentIp?.let { "Checking $it" } ?: "Your local network only",
            idleDetail = if (state.devices.isEmpty()) "Your local network only" else "Results kept"
        )
        if (state.isScanning) {
            LinearProgressIndicator(
                progress = { state.progress },
                color = ZdColors.Accent,
                trackColor = ZdColors.Surface3,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(3.dp)
            )
        }

        if (state.devices.isEmpty()) {
            ZdStatePanel(
                kicker = when {
                    state.error != null -> "Couldn’t scan"
                    state.isScanning -> "Scanning"
                    else -> "Ready"
                },
                kickerColor = if (state.error != null) ZdColors.Critical else ZdColors.Text3,
                icon = ZdIcons.Lan,
                iconTint = ZdColors.Accent,
                iconBackground = ZdColors.AccentBg,
                title = "What’s on your network?",
                body = state.error ?: "Finds every device on the WiFi you’re connected to, checks common ports, and explains risky services like Telnet or open camera streams.",
                note = "Only scan networks you own or have permission to test.",
                noteIcon = ZdIcons.Warning,
                primaryAction = if (state.isScanning) null else "Scan my network" to viewModel::startScan,
                primaryIcon = ZdIcons.Play
            )
            return@Column
        }

        val types = state.devices.groupingBy { it.deviceType }.eachCount()
        val shown = state.devices.filter { type == null || it.deviceType == type }
            .sortedWith(compareBy<NetworkDevice> { it.worst()?.ordinal ?: Int.MAX_VALUE }.thenBy { it.ip })

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ZdCard {
                    Text("${state.devices.size} devices on ${state.subnet ?: "your network"}", style = ZdType.Heading, color = ZdColors.Text)
                    Row {
                        ZdStat("Open ports", "${state.devices.sumOf { it.openPorts.size }}", Modifier.weight(1f))
                        ZdStat("Findings", "${state.totalVulnerabilities}", Modifier.weight(1f), valueColor = if (state.totalVulnerabilities > 0) ZdColors.Medium else ZdColors.Text)
                        ZdStat("Critical", "${state.criticalCount}", Modifier.weight(1f), valueColor = if (state.criticalCount > 0) ZdColors.Critical else ZdColors.Text)
                    }
                }
            }
            item {
                ZdChipRow(contentPadding = 0.dp) {
                    ZdChip("All ${state.devices.size}", selected = type == null, onClick = { type = null })
                    types.entries.sortedByDescending { it.value }.forEach { (t, n) ->
                        ZdChip("$t $n", selected = type == t, onClick = { type = t })
                    }
                }
            }
            item {
                ZdListCard(shown) { device ->
                    ZdListRow(
                        title = device.hostname ?: device.ip,
                        subtitle = listOfNotNull(
                            device.deviceType,
                            device.ip.takeIf { device.hostname != null },
                            "${device.openPorts.size} open port${if (device.openPorts.size == 1) "" else "s"}"
                        ).joinToString(" · "),
                        leading = { ZdIconTile(ZdIcons.Lan, size = 36.dp) },
                        showChevron = true,
                        onClick = { selectedIp = device.ip },
                        trailing = {
                            val worst = device.worst()
                            if (worst != null) ZdSeverityBadge(worst.zd()) else ZdTag("OK", color = ZdColors.Accent, background = ZdColors.AccentBg, border = ZdColors.AccentBorder)
                        }
                    )
                }
            }
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
            item { ZdFootnote("Only scan networks you own or have permission to check. Tap a device for its ports and what each finding means.") }
        }
    }

    state.devices.firstOrNull { it.ip == selectedIp }?.let { device ->
        DeviceSheet(device, onRescan = { viewModel.rescanDevice(device.ip) }, onDismiss = { selectedIp = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSheet(device: NetworkDevice, onRescan: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ZdColors.Surface,
        scrimColor = ZdColors.Scrim,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(device.hostname ?: device.ip, style = ZdType.Title, color = ZdColors.Text)
                    Text("${device.deviceType} · ${device.ip}", style = ZdType.Caption, color = ZdColors.Text3)
                }
                ZdButton("Rescan", onClick = onRescan, variant = ZdButtonVariant.Secondary, icon = ZdIcons.Refresh, height = 40.dp)
            }
            ZdSectionLabel("Open ports", trailingText = "${device.openPorts.size}")
            if (device.openPorts.isEmpty()) {
                ZdFootnote("No common ports answered.")
            } else {
                ZdListCard(device.openPorts) { port ->
                    ZdListRow(
                        title = "${port.port} · ${port.service.name}",
                        subtitle = port.banner?.take(80),
                        leading = null
                    )
                }
            }
            if (device.vulnerabilities.isNotEmpty()) {
                ZdSectionLabel("Findings", trailingText = "${device.vulnerabilities.size}")
                device.vulnerabilities.sortedBy { it.level.ordinal }.forEach { v ->
                    ZdCard(borderColor = v.level.zd().color.copy(alpha = 0.4f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            ZdSeverityBadge(v.level.zd())
                            ZdTag("PORT ${v.port}")
                        }
                        Text(v.title, style = ZdType.Label, color = ZdColors.Text)
                        Text(v.description, style = ZdType.BodySmall, color = ZdColors.Text2)
                        Text("Fix: ${v.recommendation}", style = ZdType.BodySmall, color = ZdColors.Accent)
                    }
                }
            }
        }
    }
}

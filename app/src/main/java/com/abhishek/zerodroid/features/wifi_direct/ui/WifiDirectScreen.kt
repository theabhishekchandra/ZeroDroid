package com.abhishek.zerodroid.features.wifi_direct.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.ZdToolScanBar
import com.abhishek.zerodroid.features.wifi_direct.domain.WifiDirectGroup
import com.abhishek.zerodroid.features.wifi_direct.domain.WifiDirectPeer
import com.abhishek.zerodroid.features.wifi_direct.viewmodel.WifiDirectViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors

@Composable
fun WifiDirectScreen(
    viewModel: WifiDirectViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.wifiDirectPermissions(),
        rationale = "Finding Wi-Fi Direct peers counts as discovering nearby devices, which Android gates."
    ) {
        WifiDirectContent(viewModel = viewModel)
    }
}

@Composable
private fun WifiDirectContent(viewModel: WifiDirectViewModel) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.initialize() }
    HardwareLifecycleEffect(
        isActive = state.isDiscovering,
        onPause = viewModel::stopDiscovery,
        onResume = viewModel::startDiscovery
    )

    Column(Modifier.fillMaxSize()) {
        ZdToolScanBar(
            running = state.isDiscovering,
            onStart = viewModel::startDiscovery,
            onStop = viewModel::stopDiscovery,
            verb = "Discovering",
            runningNote = "${state.peers.size} peers so far",
            idleNote = if (state.connectedGroup != null) "Group formed" else "Find phones, printers and TVs nearby"
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.isEnabled) item { ZdFootnote("Wi-Fi Direct is off. Turn WiFi on to discover peers.", icon = ZdIcons.Warning) }
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
            state.connectedGroup?.let { group ->
                item { GroupCard(group, onLeave = viewModel::disconnect) }
                item { ZdSectionLabel("Transfer") }
                item {
                    WifiDirectTransferPanel(
                        isGroupOwner = group.isGroupOwner,
                        groupOwnerAddress = group.ownerAddress,
                        transfer = viewModel.fileTransfer
                    )
                }
            }
            item { ZdSectionLabel("Nearby peers", trailingText = "${state.peers.size}") }
            item {
                if (state.peers.isEmpty()) {
                    ZdFootnote(if (state.isDiscovering) "Looking for peers… the other device must also be discovering." else "Tap Start to look for peers.")
                } else {
                    ZdListCard(state.peers) { PeerRow(it, onConnect = { viewModel.connect(it.deviceAddress) }) }
                }
            }
            item { ZdFootnote("One device becomes the group owner and acts like a small access point; the others join as clients.") }
        }
    }
}

@Composable
private fun GroupCard(group: WifiDirectGroup, onLeave: () -> Unit) {
    var showPass by rememberSaveable { mutableStateOf(false) }
    ZdCard(borderColor = ZdColors.AccentBorder) {
        ZdTag(
            if (group.isGroupOwner) "YOU ARE GROUP OWNER" else "JOINED AS CLIENT",
            color = ZdColors.Accent,
            background = ZdColors.AccentBg,
            border = ZdColors.AccentBorder
        )
        Row {
            ZdStat("Network", group.networkName, Modifier.weight(1f))
            ZdStat("Owner IP", group.ownerAddress ?: "—", Modifier.weight(1f))
        }
        Row {
            ZdStat("Passphrase", group.passphrase?.let { if (showPass) it else "••••••••" } ?: "—", Modifier.weight(1f))
            ZdStat("Clients", "${group.clients.size}", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (group.passphrase != null) {
                ZdButton(if (showPass) "Hide passphrase" else "Show passphrase", onClick = { showPass = !showPass }, variant = ZdButtonVariant.Secondary, modifier = Modifier.weight(1f), height = 40.dp)
            }
            ZdButton("Leave group", onClick = onLeave, variant = ZdButtonVariant.Danger, modifier = Modifier.weight(1f), height = 40.dp)
        }
    }
}

@Composable
private fun PeerRow(peer: WifiDirectPeer, onConnect: () -> Unit) {
    val connected = peer.status == 0
    ZdListRow(
        title = peer.deviceName.ifBlank { peer.deviceAddress },
        subtitle = "${peer.statusLabel} · ${peer.deviceAddress}",
        leading = { ZdIconTile(ZdIcons.Peers) },
        trailing = {
            if (connected) ZdTag("IN GROUP", color = ZdColors.Accent, background = ZdColors.AccentBg, border = ZdColors.AccentBorder)
            else ZdButton("Connect", onClick = onConnect, variant = ZdButtonVariant.Secondary, height = 36.dp)
        }
    )
}

package com.abhishek.zerodroid.features.wifiaware.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdScanControlBar
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTextField
import com.abhishek.zerodroid.core.util.formatAgo
import com.abhishek.zerodroid.features.wifiaware.viewmodel.WifiAwareViewModel

@Composable
fun WifiAwareScreen(
    viewModel: WifiAwareViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.wifiAwarePermissions(),
        rationale = "Finding Wi-Fi Aware peers counts as discovering nearby devices, which Android gates."
    ) {
        WifiAwareContent(viewModel = viewModel)
    }
}

@Composable
private fun WifiAwareContent(viewModel: WifiAwareViewModel) {
    val state by viewModel.state.collectAsState()

    HardwareLifecycleEffect(
        isActive = state.isSessionAttached,
        onPause = viewModel::pauseSession,
        onResume = viewModel::resumeSession
    )

    if (!state.isAvailable) {
        ZdStatePanel(
            kicker = "Not on this phone",
            icon = ZdIcons.Wifi,
            title = "Your phone doesn’t support Wi-Fi Aware",
            body = "Wi-Fi Aware (NAN) lets phones find each other with no router. It needs chipset support that many phones leave out.",
            note = "Wi-Fi Direct does a similar job and works on most phones."
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        ZdScanControlBar(
            running = state.isSessionAttached,
            onStart = viewModel::attachSession,
            onStop = viewModel::detachSession,
            runningLabel = when {
                state.isPublishing && state.isSubscribing -> "Publishing + subscribed"
                state.isPublishing -> "Publishing"
                state.isSubscribing -> "Subscribed"
                else -> "Cluster joined"
            },
            runningDetail = "Service “${state.serviceName}” · ${state.discoveredPeers.size} peers",
            idleLabel = "Not attached",
            idleDetail = "Join a NAN cluster to start",
            startLabel = "Attach"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ZdCard {
                    ZdTextField(
                        value = state.serviceName,
                        onValueChange = viewModel::setServiceName,
                        label = "SERVICE NAME",
                        placeholder = "zerodroid",
                        enabled = !state.isPublishing && !state.isSubscribing
                    )
                    ZdFootnote("Both phones must use the same name.", icon = null)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ZdChip("Publish", selected = state.isPublishing, onClick = viewModel::togglePublish)
                        ZdChip("Subscribe", selected = state.isSubscribing, onClick = viewModel::toggleSubscribe)
                    }
                }
            }
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
            item { ZdSectionLabel("Peers", trailingText = "${state.discoveredPeers.size}") }
            item {
                if (state.discoveredPeers.isEmpty()) {
                    ZdFootnote(if (state.isSubscribing) "Listening for peers publishing “${state.serviceName}”…" else "Subscribe to find peers, or Publish so others can find you.")
                } else {
                    ZdListCard(state.discoveredPeers) { peer ->
                        ZdListRow(
                            title = peer.serviceName,
                            subtitle = listOfNotNull("Peer ${peer.serviceId}", peer.matchFilter, formatAgo(peer.discoveredAt)).joinToString(" · "),
                            leading = { ZdIconTile(ZdIcons.Peers) }
                        )
                    }
                }
            }
            item { ZdFootnote("Works with no router or internet, typically within about 30 m. Messages are limited to 255 bytes.") }
        }
    }
}

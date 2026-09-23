package com.abhishek.zerodroid.features.bluetooth_tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
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
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdCheckRow
import com.abhishek.zerodroid.core.ui.zd.ZdCheckStatus
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdSignal
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdToolScanBar
import com.abhishek.zerodroid.core.util.formatSpan
import com.abhishek.zerodroid.features.ble.domain.BleDistanceEstimator
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.DetectedTracker
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackingRisk
import com.abhishek.zerodroid.features.bluetooth_tracker.viewmodel.BluetoothTrackerViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

@Composable
fun BluetoothTrackerScreen(
    onOpenDevice: (address: String, name: String) -> Unit = { _, _ -> },
    viewModel: BluetoothTrackerViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.blePermissions(),
        rationale = "Trackers are Bluetooth devices, so Android asks for these before any app can see them."
    ) {
        BluetoothTrackerContent(viewModel = viewModel, onOpenDevice = onOpenDevice)
    }
}

private fun TrackingRisk.severity(): ZdSeverity = when (this) {
    TrackingRisk.HIGH -> ZdSeverity.HIGH
    TrackingRisk.MEDIUM -> ZdSeverity.MEDIUM
    TrackingRisk.LOW -> ZdSeverity.LOW
    TrackingRisk.NONE -> ZdSeverity.CLEAN
}

@Composable
private fun BluetoothTrackerContent(viewModel: BluetoothTrackerViewModel, onOpenDevice: (String, String) -> Unit) {
    val state by viewModel.state.collectAsState()

    HardwareLifecycleEffect(
        isActive = state.isScanning,
        onPause = viewModel::stopScan,
        onResume = viewModel::startScan
    )

    Column(Modifier.fillMaxSize()) {
        ZdToolScanBar(
            running = state.isScanning,
            onStart = viewModel::startScan,
            onStop = viewModel::stopScan,
            runningNote = "BLE low-power · ${state.totalDevicesScanned} devices checked",
            idleNote = if (state.trackers.isEmpty()) "Keep it running while you move" else "Results kept"
        )

        if (state.trackers.isEmpty()) {
            ZdStatePanel(
                kicker = when {
                    state.error != null -> "Scan failed"
                    state.isScanning -> "Watching"
                    else -> "Ready"
                },
                kickerColor = if (state.error != null) ZdColors.Critical else ZdColors.Text3,
                icon = ZdIcons.Tracker,
                iconTint = ZdColors.Accent,
                iconBackground = ZdColors.AccentBg,
                title = if (state.isScanning) "No trackers so far" else "Is something following you?",
                body = state.error ?: if (state.isScanning) {
                    "Keep the phone with you. A tracker that reappears in several places over 10+ minutes is the pattern that matters."
                } else {
                    "Finds AirTags, Tiles, SmartTags, Chipolo and Pebblebee nearby, and flags the ones that keep showing up as you move."
                },
                primaryAction = if (state.isScanning) null else "Start scan" to viewModel::startScan,
                primaryIcon = ZdIcons.Play
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val following = state.trackers.filter { it.risk == TrackingRisk.HIGH }
            if (following.isNotEmpty()) {
                item { FollowingCard(following) }
            }
            item { ZdSectionLabel("Nearby trackers", trailingText = "${state.trackers.size}") }
            item {
                ZdListCard(state.trackers) { tracker -> TrackerRow(tracker, onClick = { onOpenDevice(tracker.address, tracker.displayName) }) }
            }
            item { ZdSectionLabel("How risk is decided") }
            item {
                ZdListCard(
                    listOf(
                        Triple("High", "Seen more than 5 times and for over 10 min", ZdCheckStatus.WARN),
                        Triple("Medium", "Seen more than 3 times", ZdCheckStatus.WARN),
                        Triple("Low", "Just appeared, not persistent yet", ZdCheckStatus.NA)
                    )
                ) { (title, detail, status) -> ZdCheckRow(title = title, detail = detail, status = status) }
            }
            item {
                ZdButton(
                    "Clear list",
                    onClick = viewModel::clearTrackers,
                    variant = ZdButtonVariant.Ghost,
                    icon = ZdIcons.Trash
                )
            }
            item {
                ZdFootnote("Trackers rotate their address every few minutes, so one tag can appear as a new entry. Your own tags show up too.")
            }
        }
    }
}

@Composable
private fun FollowingCard(following: List<DetectedTracker>) {
    val first = following.first()
    ZdCard(background = ZdColors.HighBg, borderColor = ZdColors.High.copy(alpha = 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(ZdIcons.Warning, contentDescription = null, tint = ZdColors.High, modifier = Modifier.size(20.dp))
            Text(
                if (following.size == 1) "1 tracker may be following you" else "${following.size} trackers may be following you",
                style = ZdType.Heading,
                color = ZdColors.Text,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            "${first.displayName} · seen ${first.seenCount}× over ${formatSpan(first.lastSeen - first.firstSeen)}, not paired with this phone.",
            style = ZdType.BodySmall,
            color = ZdColors.Text2
        )
        Text(
            "If it isn’t yours: check bags, pockets and your car. Most trackers can also make a sound from their owner’s app.",
            style = ZdType.Caption,
            color = ZdColors.Text3
        )
    }
}

@Composable
private fun TrackerRow(tracker: DetectedTracker, onClick: () -> Unit) {
    ZdListRow(
        onClick = onClick,
        title = tracker.displayName,
        subtitle = "${BleDistanceEstimator.rangeLabel(tracker.rssi)} · seen ${tracker.seenCount}× · ${formatSpan(tracker.lastSeen - tracker.firstSeen)}",
        leading = { ZdSignal(tracker.rssi) },
        trailing = { ZdSeverityBadge(tracker.risk.severity(), label = if (tracker.risk == TrackingRisk.NONE) "SEEN" else tracker.risk.label) }
    )
}

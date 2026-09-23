package com.abhishek.zerodroid.features.uwb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
import com.abhishek.zerodroid.core.ui.zd.ZdScanControlBar
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTextField
import com.abhishek.zerodroid.features.uwb.domain.UwbRole
import com.abhishek.zerodroid.features.uwb.domain.UwbState
import com.abhishek.zerodroid.features.uwb.viewmodel.UwbViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.util.Locale

@Composable
fun UwbScreen(
    viewModel: UwbViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.uwbPermissions(),
        rationale = "Ultra-wideband ranging is gated by Android’s nearby-devices permission."
    ) {
        UwbContent(viewModel = viewModel)
    }
}

@Composable
private fun UwbContent(viewModel: UwbViewModel) {
    val state by viewModel.state.collectAsState()

    // A ranging session needs fresh keys shared with the peer, so it is not auto-resumed.
    HardwareLifecycleEffect(
        isActive = state.isRanging,
        onPause = viewModel::stopRanging,
        resumeOnForeground = false
    )

    if (!state.isHardwareAvailable) {
        ZdStatePanel(
            kicker = "Not on this phone",
            icon = ZdIcons.Sweep,
            title = "Your phone has no UWB chip",
            body = "Ultra-wideband times radio pulses to within a nanosecond to measure distance to about 10 cm. Only some phones carry the chip, such as recent Pixel Pro and Galaxy Ultra models.",
            note = "AirTags use Apple’s own UWB protocol, which Android can’t range with even on UWB phones."
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        ZdScanControlBar(
            running = state.isRanging,
            onStart = viewModel::startAsController,
            onStop = viewModel::stopRanging,
            runningLabel = "Ranging · ${if (state.role == UwbRole.CONTROLLER) "controller" else "controlee"}",
            runningDetail = state.statusMessage ?: state.localSession?.let { "Session ${it.sessionId} · ch ${it.channel}" },
            idleLabel = "Not ranging",
            idleDetail = "Needs a second UWB phone running ZeroDroid",
            startLabel = "Controller"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ZdCard {
                    UwbRangingView(measurement = state.measurement)
                    val m = state.measurement
                    Row {
                        ZdStat("Distance", m?.distanceMeters?.let { "%.2f m".format(Locale.US, it) } ?: "—", Modifier.weight(1f), valueColor = ZdColors.Accent)
                        ZdStat("Azimuth", m?.azimuthDegrees?.let { "%.0f°".format(Locale.US, it) } ?: "—", Modifier.weight(1f))
                        ZdStat("Elevation", m?.elevationDegrees?.let { "%.0f°".format(Locale.US, it) } ?: "—", Modifier.weight(1f))
                    }
                }
            }
            if (state.isRanging) {
                item { SessionCard(state) }
            } else {
                item { SetupCard(state, viewModel) }
            }
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
            state.deviceInfo?.let { info ->
                item { ZdSectionLabel("This phone’s UWB") }
                item {
                    ZdListCard(listOf("UWB chip: ${info.chipset}") + info.capabilities) { cap ->
                        ZdCheckRow(title = cap, status = ZdCheckStatus.PASS)
                    }
                }
            }
            item { ZdFootnote("There’s no auto-discovery: one phone starts as controller and shows its session, the other types it in as controlee.") }
        }
    }
}

@Composable
private fun SessionCard(state: UwbState) {
    val session = state.localSession ?: return
    ZdCard {
        Text(
            if (state.role == UwbRole.CONTROLLER) "Share these with the other phone" else "Your address",
            style = ZdType.Label,
            color = ZdColors.Text
        )
        Row {
            ZdStat("Your address", session.localAddressHex, Modifier.weight(1f))
            if (state.role == UwbRole.CONTROLLER) ZdStat("Session ID", "${session.sessionId}", Modifier.weight(1f))
        }
        if (state.role == UwbRole.CONTROLLER) {
            ZdStat("Session key", session.sessionKeyHex)
            Row {
                ZdStat("Channel", "${session.channel}", Modifier.weight(1f))
                ZdStat("Preamble", "${session.preambleIndex}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SetupCard(state: UwbState, viewModel: UwbViewModel) {
    val number = KeyboardOptions(keyboardType = KeyboardType.Number)
    ZdCard(verticalSpacing = 12.dp) {
        Text("Start a session", style = ZdType.Label, color = ZdColors.Text)
        ZdTextField(state.peerAddressInput, viewModel::updatePeerAddressInput, label = "PEER UWB ADDRESS (HEX)", placeholder = "A1B2")
        ZdButton("Start as controller", onClick = viewModel::startAsController, modifier = Modifier.fillMaxWidth())
        Text("Or join the other phone’s session:", style = ZdType.BodySmall, color = ZdColors.Text2)
        ZdTextField(state.sessionIdInput, viewModel::updateSessionIdInput, label = "SESSION ID", keyboardOptions = number)
        ZdTextField(state.sessionKeyInput, viewModel::updateSessionKeyInput, label = "SESSION KEY (16 HEX)")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZdTextField(state.channelInput, viewModel::updateChannelInput, label = "CHANNEL", keyboardOptions = number, modifier = Modifier.weight(1f))
            ZdTextField(state.preambleInput, viewModel::updatePreambleInput, label = "PREAMBLE", keyboardOptions = number, modifier = Modifier.weight(1f))
        }
        ZdButton("Start as controlee", onClick = viewModel::startAsControlee, variant = ZdButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
    }
}

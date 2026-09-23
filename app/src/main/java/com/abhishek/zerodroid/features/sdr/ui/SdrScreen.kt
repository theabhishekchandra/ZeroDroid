package com.abhishek.zerodroid.features.sdr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdScanControlBar
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.ZdTagFlow
import com.abhishek.zerodroid.features.sdr.domain.SdrDetector
import com.abhishek.zerodroid.features.sdr.viewmodel.SdrViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

@Composable
fun SdrScreen(
    viewModel: SdrViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    if (!state.hasUsbHost) {
        ZdStatePanel(
            kicker = "Not on this phone",
            icon = ZdIcons.Usb,
            title = "Your phone can’t host USB devices",
            body = "SDR dongles plug in through USB-OTG, which needs USB host support. This phone doesn’t report it.",
            chipsLabel = "Recognised dongles",
            chips = SdrDetector.KNOWN_DEVICES.values.toList()
        )
        return
    }

    val connected = state.devices.firstOrNull { it.vidPid == state.connectedVidPid }
    Column(Modifier.fillMaxSize()) {
        ZdScanControlBar(
            running = connected != null,
            onStart = viewModel::refresh,
            onStop = viewModel::disconnect,
            runningLabel = "Device connected",
            runningDetail = connected?.let { "${it.chipset} · interface claimed" },
            idleLabel = if (state.devices.isEmpty()) "No SDR connected" else "${state.devices.size} dongle${if (state.devices.size > 1) "s" else ""} found",
            idleDetail = "Plug a dongle into USB-OTG",
            startLabel = "Rescan",
            stopLabel = "Eject"
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.devices.isEmpty()) {
                item {
                    ZdStatePanel(
                        kicker = "Waiting for hardware",
                        icon = ZdIcons.Radio,
                        iconTint = ZdColors.Accent,
                        iconBackground = ZdColors.AccentBg,
                        title = "Plug in an SDR dongle",
                        body = "A software-defined radio samples raw radio waves and lets software do the decoding. Connect one through a USB-OTG adapter, then tap Rescan.",
                        primaryAction = "Rescan USB" to viewModel::refresh,
                        primaryIcon = ZdIcons.Refresh,
                        fullScreen = false
                    )
                }
            } else {
                item { ZdSectionLabel("Detected", trailingText = "${state.devices.size}") }
                item {
                    ZdListCard(state.devices) { device ->
                        val isConnected = device.vidPid == state.connectedVidPid
                        val isConnecting = device.vidPid == state.connectingVidPid
                        ZdListRow(
                            title = device.deviceName,
                            subtitle = "${device.chipset} · ${device.vidPid}",
                            leading = { ZdIconTile(ZdIcons.Radio) },
                            trailing = {
                                when {
                                    isConnected -> ZdTag("READY", color = ZdColors.Accent, background = ZdColors.AccentBg, border = ZdColors.AccentBorder)
                                    else -> ZdButton(
                                        if (isConnecting) "Opening…" else "Connect",
                                        onClick = { viewModel.connect(device) },
                                        enabled = !isConnecting,
                                        variant = ZdButtonVariant.Secondary,
                                        height = 36.dp
                                    )
                                }
                            }
                        )
                    }
                }
            }
            state.connectionError?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
            item { ZdSectionLabel("Recognised dongles") }
            item {
                ZdCard {
                    ZdTagFlow(SdrDetector.KNOWN_DEVICES.values.toList())
                    Text(
                        "ZeroDroid detects and hands off SDR hardware; it doesn’t demodulate signals itself. Use an SDR app for listening.",
                        style = ZdType.BodySmall,
                        color = ZdColors.Text2
                    )
                }
            }
        }
    }
}

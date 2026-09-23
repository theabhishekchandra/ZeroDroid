package com.abhishek.zerodroid.features.bluetooth_classic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconButton
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SdpServiceInfo
import com.abhishek.zerodroid.features.bluetooth_classic.viewmodel.SdpUiState
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

/**
 * The services a Bluetooth Classic device advertises over SDP: audio, hands-free, serial port and
 * so on. Opens with what Android remembers, and can ask the device again.
 */
@Composable
fun SdpServicePanel(
    state: SdpUiState,
    onQuery: () -> Unit,
    onOpenTerminal: () -> Unit,
    onClose: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ZdIconTile(ZdIcons.Bluetooth)
            Column(Modifier.weight(1f)) {
                Text(
                    state.name ?: "Unknown device",
                    style = ZdType.Heading,
                    color = ZdColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(state.address.orEmpty(), style = ZdType.Mono, color = ZdColors.Text3)
            }
            ZdIconButton(ZdIcons.Close, contentDescription = "Close services", onClick = onClose)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ZdCard(verticalSpacing = 8.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.isQuerying) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = ZdColors.Info, strokeWidth = 2.dp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    state.isQuerying -> "Asking the device…"
                                    state.isCached -> "Remembered by Android"
                                    state.services.isNotEmpty() -> "Answered just now"
                                    else -> "No services yet"
                                },
                                style = ZdType.Label,
                                color = ZdColors.Text
                            )
                            Text(
                                if (state.isCached) "From an earlier connection; ask again for a fresh list."
                                else "SDP lists what a device offers before you connect.",
                                style = ZdType.Caption,
                                color = ZdColors.Text3
                            )
                        }
                        ZdButton(
                            if (state.services.isEmpty()) "Query" else "Ask again",
                            onClick = onQuery,
                            enabled = !state.isQuerying,
                            icon = ZdIcons.Refresh,
                            variant = ZdButtonVariant.Secondary,
                            height = 40.dp
                        )
                    }
                }
            }
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }

            if (state.hasSerialPort) {
                item {
                    ZdCard(background = ZdColors.AccentBg, borderColor = ZdColors.AccentBorder, verticalSpacing = 8.dp) {
                        Text("Has a serial port", style = ZdType.Label, color = ZdColors.Accent)
                        Text("You can open a text terminal to it, e.g. to talk to an OBD-II adapter or HC-05 module.", style = ZdType.BodySmall, color = ZdColors.Text2)
                        ZdButton("Open terminal", onClick = onOpenTerminal, icon = ZdIcons.Terminal, height = 40.dp)
                    }
                }
            }

            if (state.services.isNotEmpty()) {
                item { ZdSectionLabel("Services", trailingText = "${state.services.size}") }
                item { ZdListCard(state.services) { ServiceRow(it) } }
                if (state.services.any { it.shortUuid == "vendor" }) {
                    item { ZdFootnote("Vendor services use their own 128-bit IDs; only the maker knows what they do.") }
                }
            } else if (!state.isQuerying) {
                item {
                    ZdStatePanel(
                        kicker = "Nothing listed",
                        icon = ZdIcons.Bluetooth,
                        iconTint = ZdColors.Text3,
                        iconBackground = ZdColors.Surface2,
                        title = "Wake the device and ask again",
                        body = "Classic devices only answer while they’re on and in range; many only in pairing mode.",
                        fullScreen = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(service: SdpServiceInfo) {
    ZdListRow(
        title = service.profileName,
        titleMono = false,
        subtitle = service.description,
        trailing = {
            if (service.shortUuid == "vendor") ZdTag("VENDOR")
            else ZdTag(service.shortUuid, color = ZdColors.Info, background = ZdColors.InfoBg, border = ZdColors.InfoBorder)
        }
    )
}

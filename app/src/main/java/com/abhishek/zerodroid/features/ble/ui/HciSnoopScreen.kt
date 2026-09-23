package com.abhishek.zerodroid.features.ble.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdCardShape
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdMetric
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.features.ble.domain.HciPacket
import com.abhishek.zerodroid.features.ble.domain.HciPacketType
import com.abhishek.zerodroid.features.ble.domain.HciSnoopLog
import com.abhishek.zerodroid.features.ble.domain.toHexDump
import com.abhishek.zerodroid.features.ble.viewmodel.HciSnoopViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.util.Locale

/** Packets with an ATT error or failed status; what people usually look for first. */
internal fun HciPacket.isError(): Boolean = summary.contains("Error", ignoreCase = true)

/** Time since the first packet, as mm:ss.SSS. */
internal fun hciOffset(micros: Long, startMicros: Long): String {
    val d = (micros - startMicros).coerceAtLeast(0)
    val ms = d / 1_000
    return String.format(Locale.US, "%02d:%02d.%03d", ms / 60_000, (ms / 1_000) % 60, ms % 1_000)
}

internal fun formatBytes(size: Long): String = when {
    size < 0 -> "unknown size"
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> "${size / 1024} KB"
    else -> String.format(Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
}

private val HciPacketType.short: String
    get() = when (this) {
        HciPacketType.Command -> "CMD"
        HciPacketType.Event -> "EVT"
        HciPacketType.AclData -> "ACL"
        HciPacketType.Unknown -> "???"
    }

@Composable
fun HciSnoopPanel(viewModel: HciSnoopViewModel) {
    val state by viewModel.state.collectAsState()
    var errorsOnly by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.loadFromUri(it) }
    }
    val choose = { picker.launch(arrayOf("*/*")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val log = state.log
        if (log == null) {
            item {
                ZdStatePanel(
                    kicker = if (state.isLoading) "Reading" else "No log loaded",
                    icon = ZdIcons.Document,
                    iconTint = ZdColors.Info,
                    iconBackground = ZdColors.InfoBg,
                    title = "See what Bluetooth really sent",
                    body = "Android can record every Bluetooth packet. Turn on “Bluetooth HCI snoop log” in Developer options, toggle Bluetooth off and on, reproduce the problem, then load the log here.",
                    primaryAction = if (state.isLoading) null else "Load from this phone" to viewModel::loadLog,
                    primaryIcon = ZdIcons.Download,
                    fullScreen = false
                )
            }
            if (!state.isLoading) {
                item { ZdButton("Choose a log file", onClick = choose, variant = ZdButtonVariant.Secondary, icon = ZdIcons.Folder, modifier = Modifier.fillMaxWidth()) }
            }
        } else {
            item { FileCard(log, state.loadedFromPath, onReplace = choose) }
            item { Stats(log) }
            item {
                val errors = log.packets.count { it.isError() }
                ZdChipRow(contentPadding = 0.dp) {
                    ZdChip("All", selected = state.filter == null && !errorsOnly, onClick = { errorsOnly = false; viewModel.setFilter(null) })
                    listOf(HciPacketType.Command, HciPacketType.Event, HciPacketType.AclData).forEach { type ->
                        ZdChip(type.short, selected = state.filter == type && !errorsOnly, onClick = { errorsOnly = false; viewModel.setFilter(type) })
                    }
                    if (errors > 0) ZdChip("Errors $errors", selected = errorsOnly, onClick = { errorsOnly = true; viewModel.setFilter(null) })
                }
            }
            val start = log.packets.firstOrNull()?.timestampMicros ?: 0L
            val shown = log.packets.filter { (state.filter == null || it.packetType == state.filter) && (!errorsOnly || it.isError()) }
            if (shown.isEmpty()) {
                item { ZdFootnote("No packets match this filter.") }
            }
            items(shown, key = { it.index }) { PacketRow(it, start) }
        }

        if (state.isLoading) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = ZdColors.Accent, strokeWidth = 2.dp)
                    Text("  Parsing the log…", style = ZdType.Caption, color = ZdColors.Text3)
                }
            }
        }
        state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
        if (log != null) {
            item { ZdFootnote("Tap a packet to see its bytes. Logs can contain device addresses and data you exchanged; share them carefully.") }
        }
    }
}

@Composable
private fun FileCard(log: HciSnoopLog, source: String?, onReplace: () -> Unit) {
    ZdCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ZdIconTile(ZdIcons.Document, tint = ZdColors.Info, background = ZdColors.InfoBg)
            Column(Modifier.weight(1f)) {
                Text(
                    source?.substringAfterLast('/')?.ifBlank { null } ?: "btsnoop_hci.log",
                    style = ZdType.Label,
                    color = ZdColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("${formatBytes(log.fileSize)} · btsnoop v${log.version}", style = ZdType.Caption, color = ZdColors.Text3)
            }
            ZdButton("Replace", onClick = onReplace, variant = ZdButtonVariant.Ghost, height = 36.dp)
        }
    }
}

@Composable
private fun Stats(log: HciSnoopLog) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple("%,d".format(log.packetCount), "Packets", ZdColors.Text),
            Triple("${log.packets.count { it.packetType == HciPacketType.Command }}", "Commands", ZdColors.Text),
            Triple("${log.packets.count { it.packetType == HciPacketType.Event }}", "Events", ZdColors.Info),
            Triple("${log.packets.count { it.packetType == HciPacketType.AclData }}", "ACL", ZdColors.Accent)
        ).forEach { (value, label, color) ->
            ZdCard(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)) {
                ZdMetric(value, label, Modifier.fillMaxWidth(), valueColor = color)
            }
        }
    }
}

@Composable
private fun PacketRow(packet: HciPacket, startMicros: Long) {
    var expanded by remember { mutableStateOf(false) }
    val error = packet.isError()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(ZdCardShape)
            .background(ZdColors.Surface)
            .border(1.dp, if (error) ZdColors.Critical.copy(alpha = 0.4f) else ZdColors.Border, ZdCardShape)
            .clickable { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(hciOffset(packet.timestampMicros, startMicros), style = ZdType.Path, color = ZdColors.Text3)
            Text(if (packet.isSent) "→" else "←", style = ZdType.Label, color = if (packet.isSent) ZdColors.Accent else ZdColors.Info)
            Text(packet.packetType.short, style = ZdType.Label, color = if (error) ZdColors.Critical else ZdColors.Text2, modifier = Modifier.width(34.dp))
            Text(
                packet.summary,
                style = ZdType.Caption,
                color = ZdColors.Text2,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        if (expanded) {
            Text("#${packet.index} · ${packet.includedLength} of ${packet.originalLength} bytes", style = ZdType.Path, color = ZdColors.Text3)
            Text(
                packet.data.toHexDump(),
                style = ZdType.Mono,
                color = ZdColors.Text2,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ZdColors.Bg)
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
            )
        }
    }
}

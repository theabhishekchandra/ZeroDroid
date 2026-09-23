package com.abhishek.zerodroid.features.ble.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdDivider
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconButton
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.features.ble.domain.BleDeviceDump
import com.abhishek.zerodroid.features.ble.domain.BleDeviceDumper
import com.abhishek.zerodroid.features.ble.domain.DumpedCharacteristic
import com.abhishek.zerodroid.features.ble.domain.DumpedService
import com.abhishek.zerodroid.features.ble.domain.GattConnectionState
import com.abhishek.zerodroid.features.ble.domain.GattExplorer
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Storage helpers ─────────────────────────────────────────────────────────────

private const val DUMPS_DIR = "ble_dumps"

private fun getDumpsDir(context: Context): File {
    val dir = File(context.filesDir, DUMPS_DIR)
    if (!dir.exists()) dir.mkdirs()
    return dir
}

private fun saveDump(context: Context, dump: BleDeviceDump): File {
    val dir = getDumpsDir(context)
    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    val name = "${dump.deviceName ?: dump.deviceAddress}_${sdf.format(Date(dump.timestamp))}.json"
    val sanitized = name.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
    val file = File(dir, sanitized)
    file.writeText(dump.toJson().toString(2))
    return file
}

private fun loadSavedDumps(context: Context): List<Pair<String, BleDeviceDump>> {
    val dir = getDumpsDir(context)
    if (!dir.exists()) return emptyList()
    return dir.listFiles()
        ?.filter { it.extension == "json" }
        ?.sortedByDescending { it.lastModified() }
        ?.mapNotNull { file ->
            try {
                val json = JSONObject(file.readText())
                file.name to BleDeviceDump.fromJson(json)
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
}

private fun deleteDump(context: Context, fileName: String) {
    val file = File(getDumpsDir(context), fileName)
    if (file.exists()) file.delete()
}

// ── Main Panel ──────────────────────────────────────────────────────────────────

/** How many lines of JSON to preview before "Copy JSON" becomes the way to see the rest. */
private const val PREVIEW_LINES = 18

/** Reads every readable characteristic into one JSON file you can copy, keep or replay. */
@Composable
fun BleDeviceDumpPanel(
    explorer: GattExplorer,
    connectionState: GattConnectionState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dumper = remember(explorer) { BleDeviceDumper(explorer) }
    val isConnected = connectionState.isConnected

    var isDumping by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<BleDeviceDumper.DumpProgress?>(null) }
    var currentDump by remember { mutableStateOf<BleDeviceDump?>(null) }
    var isReplaying by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val savedDumps = remember { mutableStateListOf<Pair<String, BleDeviceDump>>() }

    suspend fun refreshSaved() {
        val dumps = withContext(Dispatchers.IO) { loadSavedDumps(context) }
        savedDumps.clear()
        savedDumps.addAll(dumps)
    }
    LaunchedEffect(Unit) { refreshSaved() }

    val replay: (BleDeviceDump) -> Unit = { dump ->
        scope.launch {
            isReplaying = true
            progress = null
            dumper.replayWrites(dump) { progress = it }
            isReplaying = false
            note = "Replayed ${dump.services.sumOf { s -> s.characteristics.count { it.isReplayable } }} writes"
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val dump = currentDump
        val p = progress
        if (isDumping || isReplaying) {
            Text(if (isDumping) "Reading every characteristic" else "Writing values back", style = ZdType.Heading, color = ZdColors.Text)
            LinearProgressIndicator(
                progress = { p?.fraction ?: 0f },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = if (isDumping) ZdColors.Accent else ZdColors.Info,
                trackColor = ZdColors.Surface3
            )
            p?.let {
                Text("${it.current} / ${it.total} · ${it.currentChar}", style = ZdType.Mono, color = ZdColors.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else if (dump == null) {
            ZdButton(
                "Dump every characteristic",
                onClick = {
                    scope.launch {
                        isDumping = true
                        progress = null
                        val result = dumper.dumpDevice { progress = it }
                        currentDump = result
                        isDumping = false
                        if (result != null) {
                            withContext(Dispatchers.IO) { saveDump(context, result) }
                            refreshSaved()
                            note = "Saved on this phone"
                        } else {
                            note = "Couldn’t read the device. Is it still connected?"
                        }
                    }
                },
                enabled = isConnected,
                icon = ZdIcons.Download,
                modifier = Modifier.fillMaxWidth()
            )
            if (!isConnected) ZdFootnote("Connect to the device first.", icon = ZdIcons.Info)
        }

        dump?.let { DumpResult(it, isConnected && !isReplaying && !isDumping, onReplay = { replay(it) }, onCopy = {
            copyJson(context, it)
            note = "JSON copied"
        }) }
        note?.let { Text(it, style = ZdType.Caption, color = ZdColors.Accent) }

        if (savedDumps.isNotEmpty()) {
            ZdSectionLabel("Saved dumps", trailingText = "${savedDumps.size}")
            ZdListCard(savedDumps.toList()) { (fileName, saved) ->
                ZdListRow(
                    title = saved.deviceName ?: saved.deviceAddress,
                    subtitle = "${saved.formattedTimestamp} · ${saved.totalCharacteristics} characteristics",
                    onClick = { currentDump = saved },
                    trailing = {
                        ZdIconButton(ZdIcons.Trash, contentDescription = "Delete dump", onClick = {
                            deleteDump(context, fileName)
                            savedDumps.removeAll { it.first == fileName }
                            if (currentDump == saved) currentDump = null
                        })
                    }
                )
            }
        }
    }
}

private fun copyJson(context: Context, dump: BleDeviceDump) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("BLE dump", dump.toJson().toString(2)))
}

@Composable
private fun DumpResult(dump: BleDeviceDump, canWrite: Boolean, onReplay: () -> Unit, onCopy: () -> Unit) {
    val replayable = dump.services.sumOf { s -> s.characteristics.count { it.isReplayable } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(dump.deviceName ?: dump.deviceAddress, style = ZdType.Heading, color = ZdColors.Text)
            Text(
                "${dump.services.size} services · ${dump.totalCharacteristics} characteristics · MTU ${dump.mtu}",
                style = ZdType.Caption,
                color = ZdColors.Text3
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${dump.successfulReads} read", style = ZdType.Label, color = ZdColors.Accent, modifier = Modifier.weight(1f))
            if (dump.failedReads > 0) Text("${dump.failedReads} need pairing or failed", style = ZdType.Label, color = ZdColors.Medium)
        }
        val lines = dump.toJson().toString(2).lines()
        Text(
            (lines.take(PREVIEW_LINES) + listOfNotNull(if (lines.size > PREVIEW_LINES) "  … ${lines.size - PREVIEW_LINES} more lines" else null)).joinToString("\n"),
            style = ZdType.Mono,
            color = ZdColors.Text2,
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ZdColors.Bg)
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZdButton("Copy JSON", onClick = onCopy, variant = ZdButtonVariant.Secondary, icon = ZdIcons.Copy, modifier = Modifier.weight(1f))
            ZdButton(
                if (replayable > 0) "Replay $replayable writes" else "Nothing to replay",
                onClick = onReplay,
                enabled = canWrite && replayable > 0,
                icon = ZdIcons.Refresh,
                modifier = Modifier.weight(1f)
            )
        }
        if (replayable > 0) {
            ZdFootnote("Replay writes the captured values back, which can change the device’s settings. Only use it on devices you own.", icon = ZdIcons.Warning)
        }
        ZdSectionLabel("Services", trailingText = "${dump.services.size}")
        ZdListCard(dump.services) { ServiceSection(it) }
    }
}

@Composable
private fun ServiceSection(service: DumpedService) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(ZdIcons.ChevronDown, contentDescription = null, tint = ZdColors.Text3, modifier = Modifier.size(16.dp).rotate(if (expanded) 180f else 0f))
            Text(service.displayName.ifBlank { service.uuid }, style = ZdType.Label, color = ZdColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("${service.characteristics.size}", style = ZdType.Path, color = ZdColors.Text3)
        }
        if (expanded) {
            service.characteristics.forEach { char ->
                ZdDivider()
                CharacteristicRow(char)
            }
        }
    }
}

@Composable
private fun CharacteristicRow(char: DumpedCharacteristic) {
    Column(Modifier.fillMaxWidth().padding(start = 40.dp, end = 14.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(char.displayName.ifBlank { char.uuid }, style = ZdType.BodySmall, color = ZdColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            propertyTags(char.properties).forEach { ZdTag(it) }
        }
        when {
            char.value != null -> {
                Text(char.hexString, style = ZdType.Mono, color = ZdColors.Text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                printable(char.value)?.let { Text("“$it”", style = ZdType.Caption, color = ZdColors.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            char.readError != null -> Text(char.readError, style = ZdType.Caption, color = ZdColors.Medium, maxLines = 2)
            else -> Text("Not readable", style = ZdType.Caption, color = ZdColors.Text3)
        }
    }
}

/** R, W, WNR, N, I from the GATT property bits. */
internal fun propertyTags(properties: Int): List<String> = buildList {
    if (properties and 0x02 != 0) add("R")
    if (properties and 0x04 != 0) add("WNR")
    if (properties and 0x08 != 0) add("W")
    if (properties and 0x10 != 0) add("N")
    if (properties and 0x20 != 0) add("I")
}

/** The value as text when it is mostly printable (a name, a version string), else null. */
internal fun printable(bytes: ByteArray): String? {
    if (bytes.isEmpty()) return null
    val text = bytes.toString(Charsets.UTF_8).trimEnd('\u0000')
    val ok = text.count { it.isLetterOrDigit() || it in " .,-_:/()+#" }
    return text.takeIf { it.isNotEmpty() && ok >= text.length * 0.8 }
}

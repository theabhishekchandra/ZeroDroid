package com.abhishek.zerodroid.features.ble.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ble.domain.BleDeviceDump
import com.abhishek.zerodroid.features.ble.domain.BleDeviceDumper
import com.abhishek.zerodroid.features.ble.domain.DumpedCharacteristic
import com.abhishek.zerodroid.features.ble.domain.DumpedService
import com.abhishek.zerodroid.features.ble.domain.GattConnectionState
import com.abhishek.zerodroid.features.ble.domain.GattExplorer
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    var dumpProgress by remember { mutableStateOf<BleDeviceDumper.DumpProgress?>(null) }
    var currentDump by remember { mutableStateOf<BleDeviceDump?>(null) }

    var isReplaying by remember { mutableStateOf(false) }
    var replayProgress by remember { mutableStateOf<BleDeviceDumper.DumpProgress?>(null) }

    val savedDumps = remember { mutableStateListOf<Pair<String, BleDeviceDump>>() }
    var showSaved by remember { mutableStateOf(false) }

    // Load saved dumps on first composition
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            loadSavedDumps(context)
        }.let { dumps ->
            savedDumps.clear()
            savedDumps.addAll(dumps)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Dump Controls ───────────────────────────────────────────────────
        TerminalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "> DEVICE DUMP",
                    color = TerminalCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isDumping = true
                                dumpProgress = null
                                val result = dumper.dumpDevice { progress ->
                                    dumpProgress = progress
                                }
                                currentDump = result
                                isDumping = false

                                // Auto-save
                                if (result != null) {
                                    withContext(Dispatchers.IO) {
                                        saveDump(context, result)
                                    }
                                    // Refresh saved list
                                    val refreshed = withContext(Dispatchers.IO) {
                                        loadSavedDumps(context)
                                    }
                                    savedDumps.clear()
                                    savedDumps.addAll(refreshed)
                                }
                            }
                        },
                        enabled = isConnected && !isDumping && !isReplaying,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalGreen.copy(alpha = 0.2f),
                            contentColor = TerminalGreen
                        )
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dump All", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showSaved = !showSaved },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TerminalAmber
                        )
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Saved (${savedDumps.size})",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }

                // Progress bar during dump
                if (isDumping && dumpProgress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val p = dumpProgress!!
                    Text(
                        text = "Reading ${p.current}/${p.total}: ${p.currentChar}",
                        color = TerminalAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { p.fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = TerminalGreen,
                        trackColor = TerminalGreen.copy(alpha = 0.1f)
                    )
                }

                // Progress bar during replay
                if (isReplaying && replayProgress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val p = replayProgress!!
                    Text(
                        text = "Writing ${p.current}/${p.total}: ${p.currentChar}",
                        color = TerminalAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { p.fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = TerminalCyan,
                        trackColor = TerminalCyan.copy(alpha = 0.1f)
                    )
                }
            }
        }

        // ── Dump Results ────────────────────────────────────────────────────
        currentDump?.let { dump ->
            DumpResultCard(
                dump = dump,
                isConnected = isConnected,
                isReplaying = isReplaying,
                onCopyJson = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("BLE Dump", dump.toJson().toString(2))
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Dump JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                onReplay = {
                    scope.launch {
                        isReplaying = true
                        replayProgress = null
                        dumper.replayWrites(dump) { progress ->
                            replayProgress = progress
                        }
                        isReplaying = false
                        Toast.makeText(context, "Replay complete", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // ── Saved Dumps ─────────────────────────────────────────────────────
        AnimatedVisibility(visible = showSaved) {
            TerminalCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "> SAVED DUMPS",
                        color = TerminalAmber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (savedDumps.isEmpty()) {
                        Text(
                            text = "No saved dumps found.",
                            color = TerminalAmber.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    } else {
                        savedDumps.forEach { (fileName, dump) ->
                            SavedDumpRow(
                                fileName = fileName,
                                dump = dump,
                                isConnected = isConnected,
                                isReplaying = isReplaying,
                                onLoad = { currentDump = dump },
                                onReplay = {
                                    scope.launch {
                                        isReplaying = true
                                        replayProgress = null
                                        dumper.replayWrites(dump) { progress ->
                                            replayProgress = progress
                                        }
                                        isReplaying = false
                                        Toast.makeText(context, "Replay complete", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDelete = {
                                    deleteDump(context, fileName)
                                    savedDumps.removeAll { it.first == fileName }
                                }
                            )
                            HorizontalDivider(
                                color = TerminalGreen.copy(alpha = 0.1f),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Dump Result Card ────────────────────────────────────────────────────────────

@Composable
private fun DumpResultCard(
    dump: BleDeviceDump,
    isConnected: Boolean,
    isReplaying: Boolean,
    onCopyJson: () -> Unit,
    onReplay: () -> Unit
) {
    TerminalCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .animateContentSize()
        ) {
            Text(
                text = "> DUMP RESULT",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Summary
            val writableCount = dump.services.sumOf { svc ->
                svc.characteristics.count { it.isReplayable }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SummaryLine("Device", dump.deviceName ?: "Unknown")
                SummaryLine("Address", dump.deviceAddress)
                SummaryLine("Time", dump.formattedTimestamp)
                SummaryLine("MTU", dump.mtu.toString())
                SummaryLine("Services", dump.services.size.toString())
                SummaryLine("Chars Read", "${dump.successfulReads} OK / ${dump.failedReads} failed")
                SummaryLine("Writable", "$writableCount replayable")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCopyJson,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalCyan)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy JSON", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }

                Button(
                    onClick = onReplay,
                    enabled = isConnected && !isReplaying && writableCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalCyan.copy(alpha = 0.2f),
                        contentColor = TerminalCyan
                    )
                ) {
                    Icon(
                        Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Replay Writes", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable service details
            dump.services.forEach { service ->
                ExpandableServiceSection(service = service)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ── Expandable Service Section ──────────────────────────────────────────────────

@Composable
private fun ExpandableServiceSection(service: DumpedService) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TerminalCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = service.displayName.ifBlank { service.uuid },
                color = TerminalCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${service.characteristics.size} chars",
                color = TerminalGreen.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                service.characteristics.forEach { char ->
                    CharacteristicDumpRow(char = char)
                }
            }
        }
    }
}

// ── Characteristic Row ──────────────────────────────────────────────────────────

@Composable
private fun CharacteristicDumpRow(char: DumpedCharacteristic) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = char.displayName.ifBlank { char.uuid },
            color = if (char.value != null) TerminalGreen else {
                if (char.readError != null) TerminalRed else TerminalAmber.copy(alpha = 0.5f)
            },
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (char.value != null) {
            Text(
                text = "HEX: ${char.hexString}",
                color = TerminalGreen.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            // Show ASCII if there are printable characters
            val ascii = char.value.map { b ->
                val c = b.toInt().toChar()
                if (c.isLetterOrDigit() || c.isWhitespace() || c in "!@#\$%^&*()-_=+[]{}|;:',.<>?/`~\"\\") c else '.'
            }.joinToString("")
            Text(
                text = "ASCII: $ascii",
                color = TerminalGreen.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else if (char.readError != null) {
            Text(
                text = "ERROR: ${char.readError}",
                color = TerminalRed.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = "not readable",
                color = TerminalAmber.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        // Property badges
        val props = buildList {
            if ((char.properties and 0x02) != 0) add("R")
            if ((char.properties and 0x04) != 0) add("W")
            if ((char.properties and 0x08) != 0) add("WNR")
            if ((char.properties and 0x10) != 0) add("N")
            if ((char.properties and 0x20) != 0) add("I")
        }
        if (props.isNotEmpty()) {
            Text(
                text = props.joinToString(" | "),
                color = TerminalCyan.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        }
    }
}

// ── Summary Line ────────────────────────────────────────────────────────────────

@Composable
private fun SummaryLine(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            color = TerminalGreen.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = TerminalGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Saved Dump Row ──────────────────────────────────────────────────────────────

@Composable
private fun SavedDumpRow(
    fileName: String,
    dump: BleDeviceDump,
    isConnected: Boolean,
    isReplaying: Boolean,
    onLoad: () -> Unit,
    onReplay: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dump.deviceName ?: dump.deviceAddress,
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${dump.formattedTimestamp} | ${dump.totalCharacteristics} chars",
                color = TerminalGreen.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        IconButton(onClick = onLoad, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Load dump",
                tint = TerminalCyan,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onReplay,
            enabled = isConnected && !isReplaying,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Replay,
                contentDescription = "Replay writes",
                tint = if (isConnected && !isReplaying) TerminalAmber else TerminalAmber.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete dump",
                tint = TerminalRed.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

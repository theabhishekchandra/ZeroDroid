package com.abhishek.zerodroid.features.signal_logger.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.signal_logger.domain.SignalLogEntry
import com.abhishek.zerodroid.features.signal_logger.domain.SignalLoggerState
import com.abhishek.zerodroid.features.signal_logger.domain.SignalType
import com.abhishek.zerodroid.features.signal_logger.viewmodel.SignalLoggerViewModel
import com.abhishek.zerodroid.ui.theme.SurfaceVariantDark
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalBlue
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TextDim
import com.abhishek.zerodroid.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// Filter enum
// ---------------------------------------------------------------------------

private enum class LogFilter(val label: String) {
    ALL("All"),
    WIFI("WiFi"),
    BLE("BLE"),
    ANOMALIES("Anomalies")
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

@Composable
fun SignalLoggerScreen(
    viewModel: SignalLoggerViewModel = viewModel(factory = SignalLoggerViewModel.Factory)
) {
    val combinedPermissions = (PermissionUtils.wifiPermissions() + PermissionUtils.blePermissions()).distinct()

    PermissionGate(
        permissions = combinedPermissions,
        rationale = "WiFi and Bluetooth permissions are needed to passively log wireless signal activity."
    ) {
        SignalLoggerContent(viewModel = viewModel)
    }
}

// ---------------------------------------------------------------------------
// Main content
// ---------------------------------------------------------------------------

@Composable
private fun SignalLoggerContent(viewModel: SignalLoggerViewModel) {
    DisposableEffect(Unit) {
        onDispose { viewModel.stopLogging() }
    }

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(LogFilter.ALL) }

    val filteredEntries = remember(state.entries, selectedFilter) {
        when (selectedFilter) {
            LogFilter.ALL -> state.entries
            LogFilter.WIFI -> state.entries.filter {
                it.type == SignalType.WIFI_AP || it.type == SignalType.WIFI_NEW || it.type == SignalType.WIFI_LOST
            }
            LogFilter.BLE -> state.entries.filter {
                it.type == SignalType.BLE_DEVICE || it.type == SignalType.BLE_NEW || it.type == SignalType.BLE_LOST
            }
            LogFilter.ANOMALIES -> state.entries.filter { it.isAnomaly }
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new entries arrive
    LaunchedEffect(filteredEntries.size) {
        if (filteredEntries.isNotEmpty() && state.isLogging) {
            listState.animateScrollToItem(filteredEntries.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // -- Controls ------------------------------------------------------------
        item {
            ControlBar(
                state = state,
                onToggle = {
                    if (state.isLogging) viewModel.stopLogging() else viewModel.startLogging()
                },
                onClear = { viewModel.clearLog() },
                onExport = {
                    val text = viewModel.exportLog()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Signal Log", text))
                    Toast.makeText(context, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // -- Live stats row ------------------------------------------------------
        item { LiveStatsRow(state = state) }

        // -- Activity rate -------------------------------------------------------
        item { ActivityRateBar(state = state) }

        // -- Filter chips --------------------------------------------------------
        item {
            FilterChipRow(
                selected = selectedFilter,
                onSelected = { selectedFilter = it },
                anomalyCount = state.anomalyCount
            )
        }

        // -- Error ---------------------------------------------------------------
        state.error?.let { error ->
            item {
                TerminalCard(glowColor = TerminalRed, borderColor = TerminalRed) {
                    Text(
                        text = "> ERROR: $error",
                        color = TerminalRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // -- Empty state ---------------------------------------------------------
        if (filteredEntries.isEmpty() && !state.isLogging) {
            item {
                EmptyState(
                    icon = Icons.Default.Timeline,
                    title = "No signal activity logged",
                    subtitle = "Tap START to begin passively recording WiFi and BLE signal activity"
                )
            }
        }

        // -- Log feed (main content) ---------------------------------------------
        items(filteredEntries, key = { it.id }) { entry ->
            LogEntryRow(entry = entry)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ---------------------------------------------------------------------------
// Control bar: Start/Stop + Clear + Export
// ---------------------------------------------------------------------------

@Composable
private fun ControlBar(
    state: SignalLoggerState,
    onToggle: () -> Unit,
    onClear: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isLogging) TerminalRed else TerminalGreen
            ),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (state.isLogging) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.isLogging) "STOP" else "START",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (state.isLogging) {
            ScanningIndicator(
                isScanning = true,
                label = "logging",
                color = TerminalGreen
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onExport,
            enabled = state.entries.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Export log",
                tint = if (state.entries.isNotEmpty()) TerminalCyan else TextDim
            )
        }

        IconButton(
            onClick = onClear,
            enabled = state.entries.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Clear log",
                tint = if (state.entries.isNotEmpty()) TextSecondary else TextDim
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Live stats row: Duration | WiFi | BLE | Anomalies
// ---------------------------------------------------------------------------

@Composable
private fun LiveStatsRow(state: SignalLoggerState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCard(
            label = "Duration",
            value = formatDuration(state.loggingDurationMs),
            color = TextSecondary
        )
        StatCard(
            label = "WiFi APs",
            value = "${state.wifiApCount}",
            color = TerminalCyan
        )
        StatCard(
            label = "BLE",
            value = "${state.bleDeviceCount}",
            color = TerminalGreen
        )
        StatCard(
            label = "Anomalies",
            value = "${state.anomalyCount}",
            color = if (state.anomalyCount > 0) TerminalRed else TextSecondary
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Activity rate bar
// ---------------------------------------------------------------------------

@Composable
private fun ActivityRateBar(state: SignalLoggerState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariantDark, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${"%.1f".format(state.entriesPerMinute)} events/min",
            color = TerminalGreen,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "+${state.newDevicesCount} new",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Text(
                text = "-${state.lostDevicesCount} lost",
                color = TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Text(
                text = "${state.totalEntries} total",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Filter chips
// ---------------------------------------------------------------------------

@Composable
private fun FilterChipRow(
    selected: LogFilter,
    onSelected: (LogFilter) -> Unit,
    anomalyCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LogFilter.entries.forEach { filter ->
            val label = if (filter == LogFilter.ANOMALIES && anomalyCount > 0) {
                "${filter.label} ($anomalyCount)"
            } else {
                filter.label
            }

            val chipColor = when (filter) {
                LogFilter.ALL -> TerminalGreen
                LogFilter.WIFI -> TerminalCyan
                LogFilter.BLE -> TerminalBlue
                LogFilter.ANOMALIES -> TerminalRed
            }

            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipColor.copy(alpha = 0.2f),
                    selectedLabelColor = chipColor,
                    labelColor = TextSecondary
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Log entry row (compact terminal-style)
// ---------------------------------------------------------------------------

@Composable
private fun LogEntryRow(entry: SignalLogEntry) {
    val typeColor = when (entry.type) {
        SignalType.WIFI_AP -> TerminalCyan
        SignalType.WIFI_NEW -> TerminalGreen
        SignalType.WIFI_LOST -> TextDim
        SignalType.BLE_DEVICE -> TerminalBlue
        SignalType.BLE_NEW -> TerminalGreen
        SignalType.BLE_LOST -> TextDim
        SignalType.ANOMALY -> TerminalRed
    }

    val bgColor = when {
        entry.type == SignalType.ANOMALY -> TerminalRed.copy(alpha = 0.08f)
        entry.isAnomaly -> TerminalAmber.copy(alpha = 0.06f)
        else -> Color.Transparent
    }

    val fontWeight = if (entry.type == SignalType.ANOMALY) FontWeight.Bold else FontWeight.Normal

    val timeStr = formatTimestamp(entry.timestamp)
    val typeTag = "[${entry.type}]"
    val rssiStr = entry.rssi?.let { "${it}dBm" } ?: ""
    val sourceStr = if (entry.source.length > 20) entry.source.take(20) + ".." else entry.source
    val addressShort = if (entry.address.length > 11) entry.address.takeLast(11) else entry.address

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timestamp
        Text(
            text = timeStr,
            color = TextDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = fontWeight
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Type tag
        Text(
            text = typeTag,
            color = typeColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Source name
        Text(
            text = "\"$sourceStr\"",
            color = typeColor.copy(alpha = 0.85f),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = fontWeight,
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Address (truncated)
        Text(
            text = "($addressShort)",
            color = TextDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            maxLines = 1
        )

        if (rssiStr.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = rssiStr,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = fontWeight
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Formatters
// ---------------------------------------------------------------------------

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

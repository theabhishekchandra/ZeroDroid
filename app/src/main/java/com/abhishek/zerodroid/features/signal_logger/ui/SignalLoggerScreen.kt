package com.abhishek.zerodroid.features.signal_logger.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdMetric
import com.abhishek.zerodroid.core.ui.zd.ZdScanControlBar
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.formatElapsed
import com.abhishek.zerodroid.features.signal_logger.domain.SignalLogEntry
import com.abhishek.zerodroid.features.signal_logger.domain.SignalType
import com.abhishek.zerodroid.features.signal_logger.viewmodel.SignalLoggerViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LogFilter(val label: String) {
    ALL("All"),
    WIFI("WiFi"),
    BLE("BLE"),
    ANOMALIES("Anomalies")
}

private const val SHOWN_EVENTS = 200

@Composable
fun SignalLoggerScreen(
    viewModel: SignalLoggerViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = (PermissionUtils.wifiPermissions() + PermissionUtils.blePermissions()).distinct(),
        rationale = "The logger listens to WiFi and Bluetooth at the same time, and Android gates both."
    ) {
        SignalLoggerContent(viewModel = viewModel)
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

@Composable
private fun SignalLoggerContent(viewModel: SignalLoggerViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(LogFilter.ALL) }

    HardwareLifecycleEffect(
        isActive = state.isLogging,
        onPause = viewModel::stopLogging,
        onResume = viewModel::startLogging
    )

    val shown = remember(state.entries, filter) {
        state.entries.filter {
            when (filter) {
                LogFilter.ALL -> true
                LogFilter.WIFI -> it.type in setOf(SignalType.WIFI_AP, SignalType.WIFI_NEW, SignalType.WIFI_LOST)
                LogFilter.BLE -> it.type in setOf(SignalType.BLE_DEVICE, SignalType.BLE_NEW, SignalType.BLE_LOST)
                LogFilter.ANOMALIES -> it.isAnomaly
            }
        }.asReversed().take(SHOWN_EVENTS)
    }

    Column(Modifier.fillMaxSize()) {
        ZdScanControlBar(
            running = state.isLogging,
            onStart = viewModel::startLogging,
            onStop = viewModel::stopLogging,
            runningLabel = "Logging · ${formatElapsed(state.loggingDurationMs)}",
            runningDetail = "WiFi + BLE in parallel · last 500 events",
            idleDetail = if (state.entries.isEmpty()) "Put the phone down and let it run" else "Log kept"
        )

        if (state.entries.isEmpty()) {
            ZdStatePanel(
                kicker = if (state.isLogging) "Listening" else "No sessions yet",
                icon = ZdIcons.Timeline,
                iconTint = ZdColors.Accent,
                iconBackground = ZdColors.AccentBg,
                title = "Record who comes and goes",
                body = state.error ?: "Keeps a timeline of WiFi and Bluetooth devices arriving and leaving. Start it, put the phone down, and check back.",
                primaryAction = if (state.isLogging) null else "Start logging" to viewModel::startLogging,
                primaryIcon = ZdIcons.Play
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ZdCard {
                    Row {
                        ZdMetric(formatElapsed(state.loggingDurationMs), "Duration", Modifier.weight(1f))
                        ZdMetric("${state.wifiApCount}", "WiFi", Modifier.weight(1f))
                        ZdMetric("${state.bleDeviceCount}", "BLE", Modifier.weight(1f))
                        ZdMetric("${state.anomalyCount}", "Anomaly", Modifier.weight(1f), valueColor = if (state.anomalyCount > 0) ZdColors.Medium else ZdColors.Text)
                    }
                    Text(
                        "+${state.newDevicesCount} new · −${state.lostDevicesCount} lost · ${"%.1f".format(Locale.US, state.entriesPerMinute)} events/min",
                        style = ZdType.Caption,
                        color = ZdColors.Text3
                    )
                }
            }
            item {
                ZdChipRow(contentPadding = 0.dp) {
                    LogFilter.entries.forEach { f ->
                        val label = if (f == LogFilter.ANOMALIES) "${f.label} ${state.anomalyCount}" else f.label
                        ZdChip(label, selected = filter == f, onClick = { filter = f })
                    }
                }
            }
            item {
                if (shown.isEmpty()) ZdFootnote("No events match this filter yet.")
                else ZdListCard(shown) { EventRow(it) }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZdButton(
                        "Copy log",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Signal Log", viewModel.exportLog()))
                        },
                        variant = ZdButtonVariant.Secondary,
                        icon = ZdIcons.Copy,
                        modifier = Modifier.weight(1f)
                    )
                    ZdButton("Clear", onClick = viewModel::clearLog, variant = ZdButtonVariant.Ghost, icon = ZdIcons.Trash, modifier = Modifier.weight(1f))
                }
            }
            if (state.entries.size > SHOWN_EVENTS) {
                item { ZdFootnote("Showing the newest $SHOWN_EVENTS events. Copy the log to see all ${state.entries.size}.") }
            }
        }
    }
}

@Composable
private fun EventRow(entry: SignalLogEntry) {
    val (tag, fg, bg) = when {
        entry.isAnomaly -> Triple("ANOMALY", ZdColors.Medium, ZdColors.MediumBg)
        entry.type == SignalType.WIFI_NEW || entry.type == SignalType.BLE_NEW -> Triple("NEW", ZdColors.Info, ZdColors.InfoBg)
        entry.type == SignalType.WIFI_LOST || entry.type == SignalType.BLE_LOST -> Triple("LOST", ZdColors.Text2, ZdColors.Surface2)
        else -> Triple("SEEN", ZdColors.Text3, ZdColors.Surface2)
    }
    val radio = if (entry.type.name.startsWith("WIFI")) "WiFi" else if (entry.type.name.startsWith("BLE")) "BLE" else "—"
    ZdListRow(
        title = entry.source,
        subtitle = "$radio · ${entry.detail}",
        leading = {
            Column(Modifier.width(64.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(timeFormat.format(Date(entry.timestamp)), style = ZdType.Path, color = ZdColors.Text3)
                ZdTag(tag, color = fg, background = bg, border = bg)
            }
        },
        trailing = { Text(entry.rssi?.toString() ?: "—", style = ZdType.Label, color = ZdColors.Text2) }
    )
}

package com.abhishek.zerodroid.features.wardriving.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdMetric
import com.abhishek.zerodroid.core.ui.zd.ZdProportionRow
import com.abhishek.zerodroid.core.ui.zd.ZdScanControlBar
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSignal
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.util.FrequencyUtils
import com.abhishek.zerodroid.core.util.SecurityType
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingRecord
import com.abhishek.zerodroid.features.wardriving.viewmodel.WardrivingViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WardrivingScreen(
    viewModel: WardrivingViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.wardrivingPermissions(),
        rationale = "Pairing networks with GPS positions needs location, and the background logger needs a notification."
    ) {
        WardrivingContent(viewModel)
    }
}

private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

@Composable
private fun WardrivingContent(viewModel: WardrivingViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val stats = state.stats

    Column(Modifier.fillMaxSize()) {
        ZdScanControlBar(
            running = state.isScanning,
            onStart = viewModel::startSession,
            onStop = viewModel::stopSession,
            runningLabel = "Logging · ${stats?.formattedDuration ?: "00:00"}",
            runningDetail = "GPS + WiFi · keeps running with the screen off",
            idleDetail = if (state.records.isEmpty()) "Walk, cycle or drive a route" else "Session kept · ready to export"
        )

        if (state.records.isEmpty() && !state.isScanning) {
            ZdStatePanel(
                kicker = if (state.error != null) "Couldn’t start" else "Ready",
                kickerColor = if (state.error != null) ZdColors.Critical else ZdColors.Text3,
                icon = ZdIcons.Map,
                iconTint = ZdColors.Accent,
                iconBackground = ZdColors.AccentBg,
                title = "Map the WiFi along a route",
                body = state.error ?: "Pairs every network your phone hears with the GPS position where it was strongest, then exports a WiGLE CSV. It only listens; it never connects.",
                primaryAction = "Start logging" to viewModel::startSession,
                primaryIcon = ZdIcons.Play
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (stats != null) {
                item {
                    ZdCard {
                        Row {
                            ZdMetric("${stats.uniqueBssids}", "Networks", Modifier.weight(1f))
                            ZdMetric("${stats.openCount}", "Open", Modifier.weight(1f), valueColor = if (stats.openCount > 0) ZdColors.Critical else ZdColors.Text)
                            ZdMetric("${stats.totalRecords}", "Records", Modifier.weight(1f))
                            ZdMetric(stats.formattedDuration, "Time", Modifier.weight(1f))
                        }
                        val total = stats.openCount + stats.securedCount
                        ZdProportionRow("SECURED", stats.securedCount, total, ZdColors.Accent)
                        ZdProportionRow("OPEN", stats.openCount, total, ZdColors.Critical)
                    }
                }
            }
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
            state.exportStatus?.let { item { ZdFootnote(it, icon = ZdIcons.Check) } }
            if (!state.isScanning && state.session != null) {
                item {
                    ZdButton(
                        "Export WiGLE CSV",
                        onClick = {
                            viewModel.exportCsv { uri ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "wardriving_export.csv")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export CSV"))
                            }
                        },
                        icon = ZdIcons.Share,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (state.records.isNotEmpty()) {
                item { ZdSectionLabel("Latest", trailingText = "${state.records.size}") }
                item { ZdListCard(state.records.takeLast(50).reversed()) { RecordRow(it) } }
            }
            item { ZdFootnote("Runs as a foreground service with a notification, so it keeps logging with the screen off.") }
        }
    }
}

@Composable
private fun RecordRow(record: WardrivingRecord) {
    val security = SecurityType.fromCapabilities(record.capabilities.orEmpty())
    val weak = security == SecurityType.OPEN || security == SecurityType.WEP
    ZdListRow(
        title = record.ssid?.takeIf { it.isNotBlank() } ?: "[hidden]",
        subtitle = listOfNotNull(
            record.frequency.takeIf { it > 0 }?.let { "ch ${FrequencyUtils.frequencyToChannel(it)}" },
            "%.4f, %.4f".format(Locale.US, record.lat, record.lng),
            timeFormat.format(Date(record.timestamp))
        ).joinToString(" · "),
        leading = { ZdSignal(record.rssi) },
        trailing = {
            ZdTag(
                security.label.uppercase(),
                color = if (weak) ZdColors.Critical else ZdColors.Accent,
                background = if (weak) ZdColors.CriticalBg else ZdColors.AccentBg,
                border = if (weak) ZdColors.CriticalBg else ZdColors.AccentBg
            )
        }
    )
}

package com.abhishek.zerodroid.features.celltower.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
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
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.ZdToolScanBar
import com.abhishek.zerodroid.features.celltower.domain.AlertSeverity
import com.abhishek.zerodroid.features.celltower.domain.AlertType
import com.abhishek.zerodroid.features.celltower.domain.CellTowerInfo
import com.abhishek.zerodroid.features.celltower.domain.CellType
import com.abhishek.zerodroid.features.celltower.domain.ImsiCatcherAlert
import com.abhishek.zerodroid.features.celltower.viewmodel.CellTowerViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

@Composable
fun CellTowerScreen(
    viewModel: CellTowerViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.cellTowerPermissions(),
        rationale = "Android needs these to share cell tower details with any app."
    ) {
        CellTowerContent(viewModel)
    }
}

/** Short generation label: LTE, 5G, 3G, 2G. */
private fun CellType.short(): String = when (this) {
    CellType.LTE -> "LTE"
    CellType.NR -> "5G"
    CellType.WCDMA, CellType.TDSCDMA -> "3G"
    CellType.GSM -> "2G"
    CellType.CDMA -> "CDMA"
    CellType.UNKNOWN -> "?"
}

private fun AlertSeverity.zd(): ZdSeverity = when (this) {
    AlertSeverity.HIGH -> ZdSeverity.HIGH
    AlertSeverity.MEDIUM -> ZdSeverity.MEDIUM
    AlertSeverity.LOW -> ZdSeverity.LOW
}

/** The IMSI-catcher indicators explained on screen, keyed to the analyzer's alert types. */
private val indicators = listOf(
    AlertType.LAC_CHANGE to "Area code change without moving",
    AlertType.SIGNAL_SPIKE to "Sudden signal jump (>20 dB)",
    AlertType.FORCED_2G_DOWNGRADE to "Forced 2G downgrade",
    AlertType.UNKNOWN_CELL to "Unknown cell identity"
)

@Composable
private fun CellTowerContent(viewModel: CellTowerViewModel) {
    val state by viewModel.state.collectAsState()

    HardwareLifecycleEffect(
        isActive = state.isMonitoring,
        onPause = viewModel::stopMonitoring,
        onResume = viewModel::startMonitoring
    )

    Column(Modifier.fillMaxSize()) {
        ZdToolScanBar(
            running = state.isMonitoring,
            onStart = viewModel::startMonitoring,
            onStop = viewModel::stopMonitoring,
            verb = "Monitoring",
            runningNote = "Records a signal timeline",
            idleNote = if (state.currentCell == null) "Best run for 5+ minutes while still" else "Results kept"
        )

        if (!state.isMonitoring && state.currentCell == null && state.neighbors.isEmpty()) {
            ZdStatePanel(
                kicker = if (state.error != null) "Couldn’t read cells" else "Ready",
                kickerColor = if (state.error != null) ZdColors.Critical else ZdColors.Text3,
                icon = ZdIcons.CellTower,
                iconTint = ZdColors.Accent,
                iconBackground = ZdColors.AccentBg,
                title = "Which tower is your phone talking to?",
                body = state.error ?: "Shows the cell you’re registered on, its neighbours, and signs of an IMSI catcher: fake towers that pull phones down to 2G.",
                primaryAction = "Start monitoring" to viewModel::startMonitoring,
                primaryIcon = ZdIcons.Play
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }

            val cell = state.currentCell
            if (cell == null) {
                item {
                    ZdCard {
                        Text(if (state.simAbsent) "No SIM card detected" else "No registered cell yet", style = ZdType.Label, color = ZdColors.Text)
                        Text(
                            if (state.simAbsent) "Without a SIM the phone only listens to nearby towers, so only neighbour cells can be shown."
                            else "Waiting for the modem to register on a network. Neighbour cells appear below as they are heard.",
                            style = ZdType.BodySmall,
                            color = ZdColors.Text2
                        )
                    }
                }
            } else {
                item { ServingCellCard(cell) }
                if (state.signalHistory.size > 1) {
                    item { SignalChart(state.signalHistory) }
                }
            }

            if (state.alerts.isNotEmpty()) {
                item { ZdSectionLabel("Events", trailingText = "${state.alerts.size}") }
                items(state.alerts) { AlertCard(it) }
            }

            item { ZdSectionLabel("Indicators") }
            item {
                ZdListCard(indicators) { (type, label) ->
                    val hits = state.alerts.count { it.type == type }
                    ZdCheckRow(
                        title = label,
                        detail = if (hits == 0) "None seen" else "$hits event${if (hits > 1) "s" else ""}",
                        status = if (hits == 0) ZdCheckStatus.PASS else ZdCheckStatus.WARN
                    )
                }
            }

            if (state.neighbors.isNotEmpty()) {
                item { ZdSectionLabel("Neighbours", trailingText = "${state.neighbors.size}") }
                item {
                    ZdListCard(state.neighbors) { n ->
                        ZdListRow(
                            title = listOfNotNull(n.pci?.let { "PCI $it" }, n.cid?.let { "CID $it" }).joinToString(" · ").ifEmpty { n.type.displayName },
                            subtitle = listOfNotNull(n.arfcn?.let { "ARFCN $it" }, n.carrierName).joinToString(" · ").ifEmpty { null },
                            leading = { GenerationTag(n.type) },
                            trailing = { Text("${n.rssi}", style = ZdType.Label, color = ZdColors.Text2) }
                        )
                    }
                }
            }
            item {
                ZdFootnote("These are indicators, not proof: moving between coverage areas can look similar. Repeated events while standing still matter most.")
            }
        }
    }
}

@Composable
private fun GenerationTag(type: CellType) {
    val bad = type == CellType.GSM
    ZdTag(
        type.short(),
        color = if (bad) ZdColors.Critical else ZdColors.Info,
        background = if (bad) ZdColors.CriticalBg else ZdColors.InfoBg,
        border = if (bad) ZdColors.CriticalBorder else ZdColors.InfoBorder
    )
}

@Composable
private fun ServingCellCard(cell: CellTowerInfo) {
    ZdCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GenerationTag(cell.type)
            Column(Modifier.weight(1f)) {
                Text(
                    listOfNotNull(cell.carrierName, if (cell.mcc != null && cell.mnc != null) "${cell.mcc}/${cell.mnc}" else null).joinToString(" · ").ifEmpty { cell.type.displayName },
                    style = ZdType.Label,
                    color = ZdColors.Text,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text("${cell.rssi} dBm", style = ZdType.Heading, color = ZdColors.Text)
        }
        Row {
            ZdStat(if (cell.type == CellType.LTE || cell.type == CellType.NR) "TAC" else "LAC", cell.lac?.toString() ?: "—", Modifier.weight(1f))
            ZdStat("Cell ID", cell.cid?.toString() ?: "—", Modifier.weight(1f))
        }
        Row {
            ZdStat("ARFCN", cell.arfcn?.toString() ?: "—", Modifier.weight(1f))
            ZdStat("PCI", cell.pci?.toString() ?: "—", Modifier.weight(1f))
        }
        if (cell.rsrq != null || cell.timingAdvance != null) {
            Row {
                ZdStat("RSRQ", cell.rsrq?.let { "$it dB" } ?: "—", Modifier.weight(1f))
                ZdStat("Distance (TA)", cell.distanceMeters?.let { "~$it m" } ?: "—", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SignalChart(history: List<Int>) {
    ZdCard {
        Row {
            Text("Signal, this session", style = ZdType.Mono.copy(fontWeight = ZdType.Label.fontWeight), color = ZdColors.Text2, modifier = Modifier.weight(1f))
            Text("${history.last()} dBm", style = ZdType.Label, color = ZdColors.Accent)
        }
        val line = ZdColors.Accent
        val grid = ZdColors.Border
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .semantics { contentDescription = "Signal from ${history.first()} to ${history.last()} dBm" }
        ) {
            val min = (history.min() - 5).toFloat()
            val max = (history.max() + 5).toFloat()
            val span = (max - min).coerceAtLeast(1f)
            for (i in 0..2) {
                val y = size.height * i / 2f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            val path = Path()
            history.forEachIndexed { i, v ->
                val x = size.width * i / (history.size - 1).coerceAtLeast(1)
                val y = size.height * (1f - (v - min) / span)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, line, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun AlertCard(alert: ImsiCatcherAlert) {
    val severity = alert.severity.zd()
    ZdCard(borderColor = severity.color.copy(alpha = 0.4f)) {
        ZdSeverityBadge(severity, label = alert.type.name.replace('_', ' '))
        Text(alert.description, style = ZdType.BodySmall, color = ZdColors.Text2)
    }
}

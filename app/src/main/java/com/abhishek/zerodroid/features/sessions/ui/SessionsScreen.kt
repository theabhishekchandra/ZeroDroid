package com.abhishek.zerodroid.features.sessions.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.sessions.ExportFormat
import com.abhishek.zerodroid.core.sessions.Session
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCardShape
import com.abhishek.zerodroid.core.ui.zd.ZdDivider
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdHeader
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdSwitchRow
import com.abhishek.zerodroid.core.debug.DemoDataAction
import com.abhishek.zerodroid.features.alert_center.domain.AlertTriage
import com.abhishek.zerodroid.features.alert_center.domain.DayBucket
import com.abhishek.zerodroid.features.sessions.viewmodel.SessionsViewModel
import com.abhishek.zerodroid.features.sweep.viewmodel.SweepViewModel
import com.abhishek.zerodroid.navigation.ToolCatalog
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
private val dayTimeFormat = SimpleDateFormat("EEE HH:mm", Locale.US)

/** Opens the system share sheet for an exported file. */
internal fun shareExport(context: Context, uri: Uri, format: ExportFormat) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = format.mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share export").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun SessionsScreen(
    onOpenSession: (String) -> Unit,
    onCompare: (String, String) -> Unit,
    viewModel: SessionsViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val retentionDays by viewModel.retentionDays.collectAsState()
    val context = LocalContext.current
    var showExport by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ZdHeader(path = "/sessions", title = if (selected.isEmpty()) "Sessions" else "${selected.size} selected") {
            DemoDataAction(route = "sessions")
        }
        if (selected.isNotEmpty()) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZdButton(
                    "Compare",
                    onClick = { onCompare(selected[0], selected[1]); viewModel.clearSelection() },
                    enabled = selected.size == 2,
                    icon = ZdIcons.Layers,
                    height = 40.dp,
                    modifier = Modifier.weight(1f)
                )
                ZdButton("Export", onClick = { showExport = true }, variant = ZdButtonVariant.Secondary, icon = ZdIcons.Share, height = 40.dp, modifier = Modifier.weight(1f))
                ZdButton("Cancel", onClick = viewModel::clearSelection, variant = ZdButtonVariant.Ghost, height = 40.dp)
            }
        }

        if (sessions.isEmpty()) {
            ZdStatePanel(
                kicker = "No sessions yet",
                icon = ZdIcons.Clock,
                iconTint = ZdColors.Accent,
                iconBackground = ZdColors.AccentBg,
                title = "Every scan is saved here",
                body = "When a scan finds something, it’s kept as a session on this phone: open it later, compare two runs to see what changed, or export a report."
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val now = System.currentTimeMillis()
            sessions.groupBy { AlertTriage.bucket(it.startedAt, now) }.toSortedMap().forEach { (bucket, group) ->
                item {
                    ZdSectionLabel(
                        when (bucket) {
                            DayBucket.TODAY -> "Today"
                            DayBucket.YESTERDAY -> "Yesterday"
                            DayBucket.EARLIER -> "Earlier"
                        }
                    )
                }
                items(group, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        showDay = bucket == DayBucket.EARLIER,
                        selected = session.id in selected,
                        selecting = selected.isNotEmpty(),
                        onOpen = { onOpenSession(session.id) },
                        onToggle = { viewModel.toggleSelection(session.id) }
                    )
                }
            }
            item {
                ZdFootnote("Stored only on this phone and kept $retentionDays days. Long-press two sessions to compare them.")
            }
        }
    }

    if (showExport) {
        ExportSheet(
            count = selected.size,
            redactDefault = viewModel.redactDefault,
            onDismiss = { showExport = false },
            onExport = { format, redact ->
                viewModel.export(selected, format, redact) { uri -> shareExport(context, uri, format) }
                showExport = false
                viewModel.clearSelection()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionRow(
    session: Session,
    showDay: Boolean,
    selected: Boolean,
    selecting: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit
) {
    val tool = ToolCatalog.forRoute(session.tool)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(ZdCardShape)
            .background(if (selected) ZdColors.AccentBg else ZdColors.Surface)
            .border(1.dp, if (selected) ZdColors.Accent else ZdColors.Border, ZdCardShape)
            .combinedClickable(onClick = { if (selecting) onToggle() else onOpen() }, onLongClick = onToggle)
            .semantics { onLongClick(label = if (selected) "Deselect" else "Select to compare") { onToggle(); true } }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ZdIconTile(tool?.icon ?: if (session.tool == SweepViewModel.SESSION_TOOL) ZdIcons.Sweep else ZdIcons.Clock)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(session.title, style = ZdType.Label, color = ZdColors.Text)
            Text(
                listOfNotNull(session.place, (if (showDay) dayTimeFormat else timeFormat).format(Date(session.startedAt))).joinToString(" · "),
                style = ZdType.Caption,
                color = ZdColors.Text3
            )
            Text(session.summary, style = ZdType.Caption, color = ZdColors.Text2)
        }
        if (session.isClean) ZdSeverityBadge(ZdSeverity.CLEAN)
        else ZdSeverityBadge(ZdSeverity.MEDIUM, label = "${session.findingCount}")
    }
}

/** Format picker for one or more sessions, with privacy on by default. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    count: Int,
    redactDefault: Boolean,
    onDismiss: () -> Unit,
    onExport: (ExportFormat, Boolean) -> Unit
) {
    var format by rememberSaveable { mutableStateOf(ExportFormat.PDF) }
    var redact by rememberSaveable { mutableStateOf(redactDefault) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ZdColors.Surface,
        scrimColor = ZdColors.Scrim,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (count == 1) "Export session" else "Export $count sessions", style = ZdType.Title, color = ZdColors.Text)
            Column(
                Modifier
                    .clip(ZdCardShape)
                    .background(ZdColors.Bg)
                    .border(1.dp, ZdColors.Border, ZdCardShape)
            ) {
                ExportFormat.entries.forEachIndexed { i, f ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .selectable(selected = format == f, role = Role.RadioButton, onClick = { format = f })
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ZdIconTile(
                            if (f == ExportFormat.PDF) ZdIcons.Document else ZdIcons.Download,
                            tint = if (format == f) ZdColors.Accent else ZdColors.Text3,
                            background = if (format == f) ZdColors.AccentBg else ZdColors.Surface2
                        )
                        Column(Modifier.weight(1f)) {
                            Text(f.label, style = ZdType.Label, color = ZdColors.Text)
                            Text(f.description, style = ZdType.Caption, color = ZdColors.Text3)
                        }
                    }
                    if (i < ExportFormat.entries.lastIndex) ZdDivider()
                }
            }
            ZdSwitchRow(
                label = "Redact MAC addresses",
                description = "Keeps the vendor prefix, hides the rest",
                checked = redact,
                onCheckedChange = { redact = it }
            )
            ZdButton("Share", onClick = { onExport(format, redact) }, icon = ZdIcons.Share, modifier = Modifier.fillMaxWidth())
            ZdFootnote("Use Share → Files to save a copy to Downloads.")
        }
    }
}

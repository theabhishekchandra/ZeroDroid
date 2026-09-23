package com.abhishek.zerodroid.features.sessions.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.sessions.ItemKind
import com.abhishek.zerodroid.core.sessions.SessionItem
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdDialog
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdSignal
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.ZdTextField
import com.abhishek.zerodroid.core.util.formatSpan
import com.abhishek.zerodroid.features.sessions.viewmodel.SessionDetailViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val whenFormat = SimpleDateFormat("EEE d MMM · HH:mm", Locale.US)

/** Device kinds that have their own detail page (history across sessions). */
internal val deviceKinds = setOf(ItemKind.BLE, ItemKind.TRACKER, ItemKind.WIFI, ItemKind.BT)

@Composable
fun SessionDetailScreen(
    onDeleted: () -> Unit,
    onOpenDevice: (SessionItem) -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsState()
    val items by viewModel.items.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    val context = LocalContext.current
    var showExport by rememberSaveable { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onDeleted() }
    val s = session ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ZdCard {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(s.title, style = ZdType.Heading, color = ZdColors.Text)
                        Text(listOfNotNull(s.place, whenFormat.format(Date(s.startedAt))).joinToString(" · "), style = ZdType.Caption, color = ZdColors.Text3)
                    }
                    if (s.isClean) ZdSeverityBadge(ZdSeverity.CLEAN) else ZdSeverityBadge(ZdSeverity.MEDIUM, label = "${s.findingCount} FOUND")
                }
                Text(s.summary, style = ZdType.BodySmall, color = ZdColors.Text2)
                Row {
                    ZdStat("Items", "${s.itemCount}", Modifier.weight(1f))
                    ZdStat("Findings", "${s.findingCount}", Modifier.weight(1f))
                    ZdStat("Duration", formatSpan(s.durationMs), Modifier.weight(1f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZdButton("Export", onClick = { showExport = true }, icon = ZdIcons.Share, modifier = Modifier.weight(1f), height = 44.dp)
                ZdButton(if (s.place == null) "Name place" else "Rename", onClick = { showRename = true }, variant = ZdButtonVariant.Secondary, modifier = Modifier.weight(1f), height = 44.dp)
            }
        }
        val flagged = items.filter { it.flagged }
        if (flagged.isNotEmpty()) {
            item { ZdSectionLabel("Findings", trailingText = "${flagged.size}") }
            item { ZdListCard(flagged) { ItemRow(it, onOpenDevice) } }
        }
        val rest = items.filterNot { it.flagged }
        if (rest.isNotEmpty()) {
            item { ZdSectionLabel("Seen", trailingText = "${rest.size}") }
            item { ZdListCard(rest) { ItemRow(it, onOpenDevice) } }
        }
        item { ZdButton("Delete session", onClick = viewModel::delete, variant = ZdButtonVariant.Ghost, icon = ZdIcons.Trash) }
        item { ZdFootnote("Tap a device to see every session it appeared in.") }
    }

    if (showExport) {
        ExportSheet(count = 1, redactDefault = viewModel.redactDefault, onDismiss = { showExport = false }, onExport = { format, redact ->
            viewModel.export(format, redact) { uri -> shareExport(context, uri, format) }
            showExport = false
        })
    }
    if (showRename) {
        var place by rememberSaveable { mutableStateOf(s.place.orEmpty()) }
        ZdDialog(
            title = "Where was this?",
            onDismiss = { showRename = false },
            confirmLabel = "Save",
            onConfirm = { viewModel.rename(place); showRename = false }
        ) {
            ZdTextField(value = place, onValueChange = { place = it }, label = "PLACE", placeholder = "Living room")
        }
    }
}

@Composable
internal fun ItemRow(item: SessionItem, onOpenDevice: ((SessionItem) -> Unit)?, prefix: String = "") {
    ZdListRow(
        title = prefix + item.label,
        subtitle = listOf(item.detail, item.key).filter { it.isNotBlank() }.joinToString(" · "),
        leading = item.rssi?.let { r -> { ZdSignal(r) } },
        showChevron = onOpenDevice != null && item.kind in deviceKinds,
        onClick = if (onOpenDevice != null && item.kind in deviceKinds) ({ onOpenDevice(item) }) else null,
        trailing = { ZdTag(item.kind.name) }
    )
}

/** What changed between two runs, newest additions first. */
@Composable
fun CompareScreen(
    onOpenDevice: (SessionItem) -> Unit,
    viewModel: com.abhishek.zerodroid.features.sessions.viewmodel.CompareViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val before = state.before
    val after = state.after
    val diff = state.diff
    if (before == null || after == null || diff == null) return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Before" to before, "After" to after).forEach { (label, s) ->
                    ZdCard(Modifier.weight(1f), verticalSpacing = 2.dp) {
                        Text(label.uppercase(), style = ZdType.Path, color = ZdColors.Text3)
                        Text(whenFormat.format(Date(s.startedAt)), style = ZdType.Label, color = ZdColors.Text)
                        Text(listOfNotNull(s.place, s.title).joinToString(" · "), style = ZdType.Caption, color = ZdColors.Text3)
                    }
                }
            }
        }
        item {
            ZdFootnote(
                "${formatSpan(after.startedAt - before.startedAt)} apart. Only differences are shown; ${diff.unchangedCount} item${if (diff.unchangedCount == 1) " was" else "s were"} the same in both.",
                icon = ZdIcons.Layers
            )
        }
        if (diff.isEmpty) {
            item {
                ZdCard(background = ZdColors.AccentBg, borderColor = ZdColors.AccentBorder) {
                    Text("Nothing changed", style = ZdType.Label, color = ZdColors.Accent)
                    Text("Every device and network in the newer run matched the older one.", style = ZdType.BodySmall, color = ZdColors.Text2)
                }
            }
        }
        if (diff.added.isNotEmpty()) {
            item { ZdSectionLabel("New", trailingText = "${diff.added.size}") }
            item { ZdListCard(diff.added) { ItemRow(it, onOpenDevice, prefix = "+ ") } }
        }
        if (diff.removed.isNotEmpty()) {
            item { ZdSectionLabel("Gone", trailingText = "${diff.removed.size}") }
            item { ZdListCard(diff.removed) { ItemRow(it, null, prefix = "− ") } }
        }
        if (diff.changed.isNotEmpty()) {
            item { ZdSectionLabel("Changed", trailingText = "${diff.changed.size}") }
            item {
                ZdListCard(diff.changed) { c ->
                    ZdListRow(title = "~ ${c.after.label}", subtitle = c.what, trailing = { ZdTag(c.after.kind.name) })
                }
            }
        }
    }
}

package com.abhishek.zerodroid.features.sweep.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdCardShape
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdHeader
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.ZdTextField
import com.abhishek.zerodroid.core.ui.zd.formatElapsed
import com.abhishek.zerodroid.features.sweep.domain.CheckProgress
import com.abhishek.zerodroid.features.sweep.domain.CheckState
import com.abhishek.zerodroid.features.sweep.domain.FindingLevel
import com.abhishek.zerodroid.features.sweep.domain.SweepFinding
import com.abhishek.zerodroid.features.sweep.domain.SweepPreset
import com.abhishek.zerodroid.features.sweep.viewmodel.SweepPhase
import com.abhishek.zerodroid.features.sweep.viewmodel.SweepUiState
import com.abhishek.zerodroid.features.sweep.viewmodel.SweepViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

internal fun FindingLevel.zd(): ZdSeverity = when (this) {
    FindingLevel.HIGH -> ZdSeverity.HIGH
    FindingLevel.MEDIUM -> ZdSeverity.MEDIUM
    FindingLevel.LOW -> ZdSeverity.LOW
}

// ── Presets (the Sweep tab) ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SweepPresetsScreen(onStart: (SweepPreset, String) -> Unit) {
    var preset by rememberSaveable { mutableStateOf(SweepPreset.FULL) }
    var place by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        ZdHeader(path = "/sweep/new", title = "Start a sweep")
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val focus = LocalFocusManager.current
                ZdTextField(
                    value = place,
                    onValueChange = { place = it },
                    label = "WHERE ARE YOU?",
                    placeholder = "Hotel room, living room…",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })
                )
            }
            item { ZdFootnote("Naming the place lets you compare this sweep with the next one there.", icon = null) }
            items(SweepPreset.entries) { p ->
                val selected = p == preset
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(ZdCardShape)
                        .background(if (selected) ZdColors.AccentBg else ZdColors.Surface)
                        .border(1.dp, if (selected) ZdColors.Accent else ZdColors.Border, ZdCardShape)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { preset = p })
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.title, style = ZdType.Label, color = ZdColors.Text, modifier = Modifier.weight(1f))
                        Text(p.duration, style = ZdType.Path, color = ZdColors.Text3)
                    }
                    Text(p.description, style = ZdType.Caption, color = ZdColors.Text2)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        p.checks.forEach { ZdTag(it.label) }
                    }
                }
            }
            item {
                ZdFootnote("A sweep finds signals, not proof: cameras with no radio, or recording to a memory card, can’t be detected by any app.")
            }
        }
        ZdButton(
            "Start ${preset.title.lowercase()}",
            onClick = { onStart(preset, place) },
            icon = ZdIcons.Play,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

// ── Running and report ───────────────────────────────────────────────────────

@Composable
fun SweepRunScreen(
    onLocate: (SweepFinding) -> Unit,
    onCompare: (String, String) -> Unit,
    onOpenSession: (String) -> Unit,
    onDone: () -> Unit,
    viewModel: SweepViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val permissions = buildList {
        if (state.preset.needsRadios) {
            addAll(PermissionUtils.wifiPermissions())
            addAll(PermissionUtils.blePermissions())
        }
        if (state.preset.needsMicrophone) addAll(PermissionUtils.audioPermissions())
    }.distinct()

    val body: @Composable () -> Unit = {
        LaunchedEffect(Unit) { if (state.phase == SweepPhase.READY) viewModel.start() }
        when (state.phase) {
            SweepPhase.DONE -> SweepReport(state, onLocate, onCompare, onOpenSession, onDone)
            else -> SweepRunning(state, viewModel)
        }
    }
    if (permissions.isEmpty()) body() else PermissionGate(
        permissions = permissions,
        rationale = "A sweep listens to WiFi and Bluetooth" + if (state.preset.needsMicrophone) " and the microphone." else "."
    ) { body() }
}

@Composable
private fun SweepRunning(state: SweepUiState, viewModel: SweepViewModel) {
    val keepOn by viewModel.keepScreenOn.collectAsState()
    val view = LocalView.current
    DisposableEffect(keepOn, state.phase) {
        view.keepScreenOn = keepOn && state.phase == SweepPhase.RUNNING
        onDispose { view.keepScreenOn = false }
    }
    // Back during a sweep stops it and shows the report instead of losing the run.
    BackHandler(enabled = state.phase == SweepPhase.RUNNING) { viewModel.stop() }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ZdCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { state.fraction },
                                modifier = Modifier.size(64.dp),
                                color = ZdColors.Accent,
                                trackColor = ZdColors.Surface3,
                                strokeWidth = 5.dp
                            )
                            Text("${(state.fraction * 100).toInt()}%", style = ZdType.Label, color = ZdColors.Text)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(state.place.ifBlank { state.preset.title }, style = ZdType.Heading, color = ZdColors.Text)
                            Text(
                                "${state.doneCount} of ${state.progress.size} checks done · ${formatElapsed(state.elapsedMs)}",
                                style = ZdType.Caption,
                                color = ZdColors.Text3
                            )
                            Text("${state.devicesHeard} radios heard so far", style = ZdType.Caption, color = ZdColors.Text3)
                        }
                    }
                }
            }
            if (state.hint.isNotBlank()) {
                item {
                    ZdCard(background = ZdColors.InfoBg, borderColor = ZdColors.InfoBorder) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(ZdIcons.Info, contentDescription = null, tint = ZdColors.Info, modifier = Modifier.size(18.dp))
                            Text(state.hint, style = ZdType.BodySmall, color = ZdColors.Text)
                        }
                    }
                }
            }
            item { ZdListCard(state.progress) { CheckRow(it) } }
            if (state.findings.isNotEmpty()) {
                item { ZdSectionLabel("Live findings", trailingText = "${state.findings.size}") }
                items(state.findings.take(5)) { f ->
                    ZdCard(borderColor = f.level.zd().color.copy(alpha = 0.4f), verticalSpacing = 4.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ZdSeverityBadge(f.level.zd())
                            Text(f.rssi?.let { "  $it dBm" } ?: "", style = ZdType.Path, color = ZdColors.Text3)
                        }
                        Text(f.title, style = ZdType.Label, color = ZdColors.Text)
                    }
                }
            }
        }
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ZdButton("Skip check", onClick = viewModel::skipCurrent, variant = ZdButtonVariant.Secondary, modifier = Modifier.weight(1f))
            ZdButton("Stop", onClick = viewModel::stop, variant = ZdButtonVariant.Danger, icon = ZdIcons.Stop, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CheckRow(p: CheckProgress) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(p.check.label, style = ZdType.BodySmall.copy(fontSize = ZdType.Body.fontSize.times(0.94f)), color = ZdColors.Text)
            Text(p.result ?: p.check.method, style = ZdType.Caption, color = ZdColors.Text3)
        }
        when (p.state) {
            CheckState.QUEUED -> ZdTag("QUEUED")
            CheckState.RUNNING -> ZdTag("SCANNING", color = ZdColors.Info, background = ZdColors.InfoBg, border = ZdColors.InfoBorder)
            CheckState.SKIPPED -> ZdTag("SKIPPED")
            CheckState.DONE -> if (p.flags > 0) ZdSeverityBadge(ZdSeverity.MEDIUM, label = "${p.flags} FLAG${if (p.flags > 1) "S" else ""}")
            else ZdSeverityBadge(ZdSeverity.CLEAN, label = "DONE")
        }
    }
}

@Composable
private fun SweepReport(
    state: SweepUiState,
    onLocate: (SweepFinding) -> Unit,
    onCompare: (String, String) -> Unit,
    onOpenSession: (String) -> Unit,
    onDone: () -> Unit
) {
    val needsLook = state.findings.filter { it.level != FindingLevel.LOW }
    val low = state.findings.filter { it.level == FindingLevel.LOW }
    val clean = state.progress.filter { it.state == CheckState.DONE && it.flags == 0 }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ZdCard {
                Text(
                    when (needsLook.size) {
                        0 -> "Nothing needs a look"
                        1 -> "1 thing needs a look"
                        else -> "${needsLook.size} things need a look"
                    },
                    style = ZdType.Title,
                    color = if (needsLook.isEmpty()) ZdColors.Accent else ZdColors.Text
                )
                Text(
                    listOfNotNull(state.place.ifBlank { null }, state.preset.title, formatElapsed(state.elapsedMs), "${state.progress.size} checks").joinToString(" · "),
                    style = ZdType.Caption,
                    color = ZdColors.Text3
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val high = needsLook.count { it.level == FindingLevel.HIGH }
                    val medium = needsLook.count { it.level == FindingLevel.MEDIUM }
                    if (high > 0) ZdSeverityBadge(ZdSeverity.HIGH, label = "$high HIGH")
                    if (medium > 0) ZdSeverityBadge(ZdSeverity.MEDIUM, label = "$medium MEDIUM")
                    ZdSeverityBadge(ZdSeverity.CLEAN, label = "${clean.size} CLEAN")
                }
            }
        }
        if (needsLook.isNotEmpty()) {
            items(needsLook) { f -> FindingCard(f, onLocate) }
        }
        if (low.isNotEmpty()) {
            item { ZdSectionLabel("Worth knowing", trailingText = "${low.size}") }
            items(low) { f -> FindingCard(f, onLocate) }
        }
        if (clean.isNotEmpty()) {
            item { ZdSectionLabel("Clean checks") }
            item { ZdListCard(clean) { CheckRow(it) } }
        }
        val skipped = state.progress.filter { it.state == CheckState.SKIPPED }
        if (skipped.isNotEmpty()) {
            item { ZdSectionLabel("Not completed") }
            item { ZdListCard(skipped) { CheckRow(it) } }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val previous = state.previousSessionId
                val current = state.sessionId
                if (previous != null && current != null) {
                    ZdButton("Compare with last sweep here", onClick = { onCompare(previous, current) }, icon = ZdIcons.Layers, modifier = Modifier.fillMaxWidth())
                }
                if (current != null) {
                    ZdButton("Open saved session", onClick = { onOpenSession(current) }, variant = ZdButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
                }
                ZdButton("Done", onClick = onDone, variant = ZdButtonVariant.Ghost, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            ZdFootnote(
                (if (state.sessionId != null) "Saved to Sessions. " else "") +
                    "Findings are indicators, not proof; innocent devices often share these signals."
            )
        }
    }
}

@Composable
private fun FindingCard(f: SweepFinding, onLocate: (SweepFinding) -> Unit) {
    ZdCard(borderColor = f.level.zd().color.copy(alpha = 0.4f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZdSeverityBadge(f.level.zd())
            Text(f.check.label, style = ZdType.Mono, color = ZdColors.Text3, modifier = Modifier.weight(1f))
            f.rssi?.let { Text("$it dBm", style = ZdType.Path, color = ZdColors.Text3) }
        }
        Text(f.title, style = ZdType.Label, color = ZdColors.Text)
        Text(f.detail, style = ZdType.BodySmall, color = ZdColors.Text2)
        if (f.isBle && f.key != null) {
            ZdButton("Locate", onClick = { onLocate(f) }, icon = ZdIcons.Crosshair, height = 40.dp)
        }
    }
}

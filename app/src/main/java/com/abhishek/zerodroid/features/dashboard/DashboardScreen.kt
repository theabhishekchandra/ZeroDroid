package com.abhishek.zerodroid.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.prefs.LastUsedFeature
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdHeader
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdScreenColumn
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.ZdTagFlow
import com.abhishek.zerodroid.core.util.formatAgo
import com.abhishek.zerodroid.navigation.ToolCatalog
import com.abhishek.zerodroid.navigation.ToolInfo
import com.abhishek.zerodroid.navigation.ZeroDroidScreen
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

/**
 * Home: threat status first, then pinned tools, where you left off, and what this phone can do.
 */
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val summary by viewModel.alertSummary.collectAsState()
    val pinned by viewModel.pinnedTools.collectAsState()
    val lastUsed by viewModel.lastUsedFeature.collectAsState()
    val hardwareItems by viewModel.hardwareItems.collectAsState()
    val support = viewModel.toolSupport
    var showHardware by rememberSaveable { mutableStateOf(false) }

    val openTool: (ToolInfo) -> Unit = { tool ->
        viewModel.saveLastUsed(tool.route, tool.name)
        onNavigate(tool.route)
    }

    Column(Modifier.fillMaxSize()) {
        ZdHeader(path = "/zerodroid · ${viewModel.deviceInfo.model}", title = "Home")

        ZdScreenColumn(spacing = 20.dp) {
            ThreatStatusCard(
                summary = summary,
                onRunSweep = { onNavigate(ZeroDroidScreen.RfBugSweeper.route) },
                onReview = { onNavigate(ZeroDroidScreen.AlertCenter.route) }
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ZdSectionLabel(
                    "Pinned",
                    actionLabel = "Edit",
                    onAction = { onNavigate(ZeroDroidScreen.Tools.route) }
                )
                if (pinned.isEmpty()) {
                    ZdCard {
                        Text(
                            "Nothing pinned yet. Long-press a tool in Tools to pin it here.",
                            style = ZdType.BodySmall,
                            color = ZdColors.Text2
                        )
                    }
                } else {
                    pinned.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { tool ->
                                PinnedTile(tool, Modifier.weight(1f)) { openTool(tool) }
                            }
                            if (row.size == 1) Column(Modifier.weight(1f)) {}
                        }
                    }
                }
            }

            ResumeSection(lastUsed) { last ->
                val tool = ToolCatalog.forRoute(last.route)
                if (tool != null) openTool(tool) else onNavigate(last.route)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ZdSectionLabel(
                    "This phone",
                    actionLabel = if (showHardware) "Hide" else "Details",
                    onAction = { showHardware = !showHardware }
                )
                ZdCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(support.supported.toString(), style = ZdType.Heading.copy(fontSize = ZdType.Title.fontSize.times(1.1f)), color = ZdColors.Accent)
                        Text("of ${support.total} tools work here", style = ZdType.BodySmall, color = ZdColors.Text2)
                    }
                    if (support.missing.isNotEmpty()) ZdTagFlow(support.missing)
                    if (showHardware) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Text(viewModel.deviceInfo.model, style = ZdType.Label, color = ZdColors.Text)
                            Text("Android ${viewModel.deviceInfo.androidVersion}", style = ZdType.Caption, color = ZdColors.Text3)
                            hardwareItems.forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.name, style = ZdType.BodySmall, color = ZdColors.Text2, modifier = Modifier.weight(1f))
                                    if (item.isAvailable) {
                                        ZdTag("YES", color = ZdColors.Accent, background = ZdColors.AccentBg, border = ZdColors.AccentBorder)
                                    } else {
                                        ZdTag("NO", color = ZdColors.Text3)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreatStatusCard(
    summary: AlertSummary,
    onRunSweep: () -> Unit,
    onReview: () -> Unit
) {
    val worst = summary.worst
    val (tint, tileBg) = when (worst) {
        AlertSeverity.CRITICAL -> ZdColors.Critical to ZdColors.CriticalBg
        AlertSeverity.HIGH -> ZdColors.High to ZdColors.HighBg
        AlertSeverity.MEDIUM -> ZdColors.Medium to ZdColors.MediumBg
        AlertSeverity.LOW -> ZdColors.Info to ZdColors.InfoBg
        null -> ZdColors.Accent to ZdColors.AccentBg
    }
    val title = when (summary.total) {
        0 -> "All clear"
        1 -> "1 open alert"
        else -> "${summary.total} open alerts"
    }
    val detail = if (summary.total == 0) {
        "Nothing flagged yet. Run a sweep to check the room you’re in."
    } else {
        listOfNotNull(summary.breakdown, summary.latestTimestamp?.let { "latest ${formatAgo(it)}" }).joinToString(" · ")
    }

    ZdCard(verticalSpacing = 14.dp, contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ZdIconTile(
                icon = if (worst == null) ZdIcons.ShieldCheck else ZdIcons.Warning,
                tint = tint,
                background = tileBg,
                size = 44.dp,
                iconSize = 22.dp
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = ZdType.Heading.copy(fontSize = ZdType.Heading.fontSize.times(1.06f)), color = ZdColors.Text)
                Text(detail, style = ZdType.BodySmall, color = ZdColors.Text2)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZdButton("Run sweep", onClick = onRunSweep, icon = ZdIcons.Sweep, modifier = Modifier.weight(1f))
            ZdButton("Review", onClick = onReview, variant = ZdButtonVariant.Secondary, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PinnedTile(tool: ToolInfo, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ZdCard(modifier = modifier.heightIn(min = 96.dp), onClick = onClick) {
        ZdIconTile(tool.icon)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tool.name, style = ZdType.Label, color = ZdColors.Text, maxLines = 1)
            Text(tool.job, style = ZdType.Caption, color = ZdColors.Text3, maxLines = 2)
        }
    }
}

@Composable
private fun ResumeSection(lastUsed: LastUsedFeature?, onResume: (LastUsedFeature) -> Unit) {
    val last = lastUsed ?: return
    val tool = remember(last.route) { ToolCatalog.forRoute(last.route) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ZdSectionLabel("Resume")
        ZdCard(contentPadding = PaddingValues(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ZdIconTile(tool?.icon ?: ZdIcons.Clock, tint = ZdColors.Text2, background = ZdColors.Surface2)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(tool?.name ?: last.title, style = ZdType.Label, color = ZdColors.Text)
                    Text("Last tool you opened", style = ZdType.Caption, color = ZdColors.Text3)
                }
                ZdButton("Open", onClick = { onResume(last) }, variant = ZdButtonVariant.Ghost, height = 40.dp)
            }
        }
    }
}

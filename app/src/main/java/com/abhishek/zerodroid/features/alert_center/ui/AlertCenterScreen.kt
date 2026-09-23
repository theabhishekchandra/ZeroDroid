package com.abhishek.zerodroid.features.alert_center.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.alerts.AlertSource
import com.abhishek.zerodroid.core.alerts.UnifiedAlert
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTabs
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.features.alert_center.domain.AlertFilter
import com.abhishek.zerodroid.features.alert_center.domain.AlertTriage
import com.abhishek.zerodroid.features.alert_center.domain.DayBucket
import com.abhishek.zerodroid.features.alert_center.viewmodel.AlertCenterViewModel
import com.abhishek.zerodroid.navigation.ZeroDroidScreen
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun AlertSeverity.zd(): ZdSeverity = when (this) {
    AlertSeverity.CRITICAL -> ZdSeverity.CRITICAL
    AlertSeverity.HIGH -> ZdSeverity.HIGH
    AlertSeverity.MEDIUM -> ZdSeverity.MEDIUM
    AlertSeverity.LOW -> ZdSeverity.LOW
}

/** The tool that raised each kind of alert, for the "Details" action. */
internal fun AlertSource.route(): String = when (this) {
    AlertSource.ROGUE_AP -> ZeroDroidScreen.RogueAp.route
    AlertSource.DEAUTH -> ZeroDroidScreen.DeauthDetector.route
    AlertSource.GPS_SPOOF -> ZeroDroidScreen.GpsSpoofDetector.route
    AlertSource.HIDDEN_CAMERA -> ZeroDroidScreen.HiddenCamera.route
    AlertSource.BLUETOOTH_TRACKER -> ZeroDroidScreen.BluetoothTracker.route
}

private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
private val dayFormat = SimpleDateFormat("EEE", Locale.US)

@Composable
fun AlertCenterScreen(
    onOpenTool: (String) -> Unit = {},
    viewModel: AlertCenterViewModel = hiltViewModel()
) {
    val alerts by viewModel.alerts.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf(AlertFilter.ALL) }

    val open = alerts.filter { it.isOpen }
    val resolved = alerts.filterNot { it.isOpen }

    Column(Modifier.fillMaxSize()) {
        ZdTabs(
            tabs = listOf("Open · ${open.size}", "Resolved · ${resolved.size}"),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (alerts.isEmpty()) {
            ZdStatePanel(
                kicker = "All clear",
                kickerColor = ZdColors.Accent,
                icon = ZdIcons.ShieldCheck,
                iconTint = ZdColors.Accent,
                iconBackground = ZdColors.AccentBg,
                title = "No alerts yet",
                body = "Tracker Scanner, Rogue AP, Deauth, GPS Spoof and Hidden Camera report anything suspicious here, in one place, so you can decide what it is."
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ZdChipRow(contentPadding = 0.dp) {
                    AlertFilter.entries.forEach { f -> ZdChip(f.label, selected = filter == f, onClick = { filter = f }) }
                }
            }
            if (tab == 0) {
                val shown = AlertTriage.filter(open, filter)
                item { WeekStrip(alerts) }
                if (shown.isEmpty()) {
                    item { ZdFootnote("Nothing open here. Resolved alerts are on the other tab.", icon = ZdIcons.Check) }
                }
                val now = System.currentTimeMillis()
                shown.groupBy { AlertTriage.bucket(it.timestamp, now) }.toSortedMap().forEach { (bucket, group) ->
                    item { ZdSectionLabel(bucket.label) }
                    items(group, key = { it.id }) { alert ->
                        OpenAlertCard(
                            alert = alert,
                            showDay = bucket == DayBucket.EARLIER,
                            onDetails = { onOpenTool(alert.source.route()) },
                            onResolve = { viewModel.resolve(alert, AlertTriage.resolveAction(alert.source).second) }
                        )
                    }
                }
            } else {
                val shown = AlertTriage.filter(resolved, filter)
                item { ZdFootnote("Resolved alerts are kept 30 days so you can spot patterns. Tap one to reopen it.") }
                if (shown.isNotEmpty()) {
                    item {
                        ZdListCard(shown) { alert ->
                            ZdListRow(
                                title = alert.title,
                                titleMono = false,
                                subtitle = "${alert.source.label} · ${dayFormat.format(Date(alert.timestamp))}",
                                onClick = { viewModel.reopen(alert) },
                                trailing = { ZdTag(alert.resolution?.label ?: "RESOLVED") }
                            )
                        }
                    }
                    item { ZdButton("Clear resolved", onClick = viewModel::clearResolved, variant = ZdButtonVariant.Ghost, icon = ZdIcons.Trash) }
                }
            }
        }
    }
}

@Composable
private fun OpenAlertCard(alert: UnifiedAlert, showDay: Boolean, onDetails: () -> Unit, onResolve: () -> Unit) {
    val severity = alert.severity.zd()
    ZdCard(borderColor = severity.color.copy(alpha = 0.4f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZdSeverityBadge(severity)
            Text(alert.source.label, style = ZdType.Mono, color = ZdColors.Text2, modifier = Modifier.weight(1f))
            Text(
                (if (showDay) dayFormat.format(Date(alert.timestamp)) + " " else "") + timeFormat.format(Date(alert.timestamp)),
                style = ZdType.Path,
                color = ZdColors.Text3
            )
        }
        Text(alert.title, style = ZdType.Label, color = ZdColors.Text)
        Text(alert.detail, style = ZdType.BodySmall, color = ZdColors.Text2)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZdButton(
                if (alert.source == AlertSource.BLUETOOTH_TRACKER) "Investigate" else "Details",
                onClick = onDetails,
                modifier = Modifier.weight(1f),
                height = 40.dp
            )
            ZdButton(
                AlertTriage.resolveAction(alert.source).first,
                onClick = onResolve,
                variant = ZdButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
                height = 40.dp
            )
        }
    }
}

/** Seven small stacked bars: high on top of medium, per day. */
@Composable
private fun WeekStrip(alerts: List<UnifiedAlert>) {
    val week = AlertTriage.week(alerts, System.currentTimeMillis())
    val max = week.maxOf { it.high + it.medium }.coerceAtLeast(1)
    val high = week.sumOf { it.high }
    val medium = week.sumOf { it.medium }
    ZdCard(verticalSpacing = 8.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("This week", style = ZdType.Label, color = ZdColors.Text2, modifier = Modifier.weight(1f))
            ZdTag("high $high", color = ZdColors.High, background = ZdColors.HighBg, border = ZdColors.HighBg)
            ZdTag("medium $medium", color = ZdColors.Medium, background = ZdColors.MediumBg, border = ZdColors.MediumBg)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { contentDescription = "$high high and $medium medium alerts in the last 7 days" },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            week.forEach { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val unit = 36f / max
                    if (day.high + day.medium == 0) {
                        Box(Modifier.width(14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(ZdColors.Surface3))
                    }
                    if (day.high > 0) Box(Modifier.width(14.dp).height((day.high * unit).dp).clip(RoundedCornerShape(2.dp)).background(ZdColors.High))
                    if (day.medium > 0) Box(Modifier.width(14.dp).height((day.medium * unit).dp).clip(RoundedCornerShape(2.dp)).background(ZdColors.Medium))
                    Text(day.dayLabel, style = ZdType.Path, color = ZdColors.Text3)
                }
            }
        }
    }
}

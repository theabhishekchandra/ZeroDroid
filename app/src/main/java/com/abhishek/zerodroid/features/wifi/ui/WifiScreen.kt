package com.abhishek.zerodroid.features.wifi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdBar
import com.abhishek.zerodroid.core.ui.zd.ZdBarChart
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdCheckRow
import com.abhishek.zerodroid.core.ui.zd.ZdCheckStatus
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdDivider
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdProportionRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSignal
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTabs
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.ZdToolScanBar
import com.abhishek.zerodroid.core.ui.zd.congestionColor
import com.abhishek.zerodroid.core.util.SecurityType
import com.abhishek.zerodroid.core.util.WifiBand
import com.abhishek.zerodroid.features.wifi.domain.ChannelLoad
import com.abhishek.zerodroid.features.wifi.domain.CheckOutcome
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.features.wifi.domain.WifiAssessment
import com.abhishek.zerodroid.features.wifi.viewmodel.WifiViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

private const val AUTO_STOP_MS = 30_000L

private enum class NetworkFilter(val label: String) { ALL("All"), BAND_24("2.4 GHz"), BAND_5("5 GHz"), WEAK("Weak security") }

@Composable
fun WifiScreen(
    viewModel: WifiViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.wifiPermissions(),
        rationale = "Android needs location access before any app can list WiFi networks."
    ) {
        WifiContent(viewModel = viewModel)
    }
}

@Composable
private fun WifiContent(viewModel: WifiViewModel) {
    val accessPoints by viewModel.accessPoints.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanError by viewModel.error.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf(NetworkFilter.ALL) }
    var selectedBssid by rememberSaveable { mutableStateOf<String?>(null) }

    HardwareLifecycleEffect(
        isActive = isScanning,
        onPause = viewModel::stopScan,
        onResume = viewModel::startScan
    )

    val sorted = accessPoints.sortedByDescending { it.rssi }

    Column(Modifier.fillMaxSize()) {
        ZdToolScanBar(
            running = isScanning,
            onStart = viewModel::startScan,
            onStop = viewModel::stopScan,
            autoStopMs = AUTO_STOP_MS,
            idleNote = if (accessPoints.isEmpty()) "Scans run for 30 s" else "Results kept"
        )

        if (accessPoints.isEmpty() && !isScanning) {
            ZdStatePanel(
                kicker = if (scanError != null) "Scan failed" else "Ready",
                kickerColor = if (scanError != null) ZdColors.Critical else ZdColors.Text3,
                icon = ZdIcons.Wifi,
                iconTint = ZdColors.Accent,
                iconBackground = ZdColors.AccentBg,
                title = "See the WiFi around you",
                body = scanError ?: "Find crowded channels, weak security and networks you don’t recognise. Tap Start to scan.",
                primaryAction = "Start scan" to viewModel::startScan,
                primaryIcon = ZdIcons.Play
            )
            return@Column
        }

        val weak = accessPoints.count { WifiAssessment.isWeak(it.security) }
        ZdTabs(
            tabs = listOf("Networks ${accessPoints.size}", "Channels", "Security"),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            scanError?.let { message ->
                item { ZdFootnote(message, icon = ZdIcons.Warning) }
            }
            when (tab) {
                0 -> networksTab(sorted, filter, weak, onFilter = { filter = it }, onOpen = { selectedBssid = it.bssid })
                1 -> channelsTab(accessPoints)
                else -> securityTab(sorted, weak, onOpen = { selectedBssid = it.bssid })
            }
        }
    }

    val selected = accessPoints.firstOrNull { it.bssid == selectedBssid }
    if (selected != null) {
        NetworkDetailSheet(ap = selected, all = accessPoints, onDismiss = { selectedBssid = null })
    }
}

private fun LazyListScope.networksTab(
    sorted: List<WifiAccessPoint>,
    filter: NetworkFilter,
    weak: Int,
    onFilter: (NetworkFilter) -> Unit,
    onOpen: (WifiAccessPoint) -> Unit
) {
    item {
        val load = WifiAssessment.load(sorted, WifiAssessment.CHANNELS_24)
        ZdCard(verticalSpacing = 8.dp, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("2.4 GHz congestion", style = ZdType.Mono.copy(fontWeight = ZdType.Label.fontWeight), color = ZdColors.Text2, modifier = Modifier.weight(1f))
                WifiAssessment.best24Channel(sorted)?.let {
                    Text("best: ch $it", style = ZdType.Mono.copy(fontWeight = ZdType.Label.fontWeight), color = ZdColors.Accent)
                }
            }
            ZdBarChart(
                bars = load.map { ZdBar(it.channel.toString(), it.networks.toFloat(), congestionColor(it.networks)) },
                contentDescription = "Networks per 2.4 GHz channel"
            )
        }
    }
    item {
        ZdChipRow(Modifier.padding(horizontal = 0.dp), contentPadding = 0.dp) {
            NetworkFilter.entries.forEach { f ->
                val count = when (f) {
                    NetworkFilter.ALL -> sorted.size
                    NetworkFilter.BAND_24 -> sorted.count { it.band == WifiBand.BAND_2_4GHZ }
                    NetworkFilter.BAND_5 -> sorted.count { it.band == WifiBand.BAND_5GHZ }
                    NetworkFilter.WEAK -> weak
                }
                ZdChip("${f.label} $count", selected = filter == f, onClick = { onFilter(f) })
            }
        }
    }
    val shown = sorted.filter {
        when (filter) {
            NetworkFilter.ALL -> true
            NetworkFilter.BAND_24 -> it.band == WifiBand.BAND_2_4GHZ
            NetworkFilter.BAND_5 -> it.band == WifiBand.BAND_5GHZ
            NetworkFilter.WEAK -> WifiAssessment.isWeak(it.security)
        }
    }
    item {
        if (shown.isEmpty()) {
            ZdFootnote("No networks match this filter.")
        } else {
            ZdListCard(shown) { ap -> NetworkRow(ap, sorted, onClick = { onOpen(ap) }) }
        }
    }
}

private fun LazyListScope.channelsTab(accessPoints: List<WifiAccessPoint>) {
    val on24 = accessPoints.filter { it.band == WifiBand.BAND_2_4GHZ }
    val on5 = accessPoints.filter { it.band == WifiBand.BAND_5GHZ }
    val best = WifiAssessment.best24Channel(accessPoints)
    val busy24 = WifiAssessment.busiestChannel(accessPoints, WifiBand.BAND_2_4GHZ)
    val busy5 = WifiAssessment.busiestChannel(accessPoints, WifiBand.BAND_5GHZ)

    item {
        ChannelCard(
            title = "2.4 GHz",
            trailing = "${on24.size} networks",
            bars = WifiAssessment.load(on24, WifiAssessment.CHANNELS_24),
            note = buildString {
                append("Channels 1, 6 and 11 don’t overlap.")
                if (busy24 != null) append(" Channel ${busy24.channel} is crowded here")
                if (best != null && busy24 != null) append("; a router would do better on $best.") else if (busy24 != null) append(".")
            }
        )
    }
    item {
        ChannelCard(
            title = "5 GHz",
            trailing = "${on5.size} networks",
            bars = WifiAssessment.load(on5, WifiAssessment.CHANNELS_5),
            note = if (busy5 == null) "Plenty of room. 5 GHz has many non-overlapping channels, so it rarely gets crowded."
            else "Channel ${busy5.channel} is shared by ${busy5.networks} networks. 5 GHz reaches less far through walls, which also keeps it quieter."
        )
    }
    if (best != null && busy24 != null) {
        item {
            ZdCard(background = ZdColors.AccentBg, borderColor = ZdColors.AccentBorder) {
                Text(
                    "Recommendation: move your 2.4 GHz router to channel $best. Change it in your router’s admin page.",
                    style = ZdType.BodySmall,
                    color = ZdColors.Text
                )
            }
        }
    }
}

@Composable
private fun ChannelCard(title: String, trailing: String, bars: List<ChannelLoad>, note: String) {
    ZdCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = ZdType.Label, color = ZdColors.Text, modifier = Modifier.weight(1f))
            Text(trailing, style = ZdType.Mono, color = ZdColors.Text3)
        }
        ZdBarChart(
            bars = bars.map { ZdBar(it.channel.toString(), it.networks.toFloat(), congestionColor(it.networks)) },
            height = 96.dp,
            contentDescription = "Networks per $title channel"
        )
        Text(note, style = ZdType.BodySmall, color = ZdColors.Text2)
    }
}

private fun LazyListScope.securityTab(
    sorted: List<WifiAccessPoint>,
    weak: Int,
    onOpen: (WifiAccessPoint) -> Unit
) {
    item {
        ZdCard {
            Text(
                if (weak == 0) "All ${sorted.size} networks are encrypted" else "$weak of ${sorted.size} need attention",
                style = ZdType.Heading,
                color = if (weak == 0) ZdColors.Accent else ZdColors.Text
            )
            Text("Open and WEP networks send data others nearby can read.", style = ZdType.BodySmall, color = ZdColors.Text2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                listOf(SecurityType.WPA3, SecurityType.WPA2, SecurityType.WPA, SecurityType.WEP, SecurityType.OPEN).forEach { type ->
                    ZdProportionRow(
                        label = type.label.uppercase(),
                        count = sorted.count { it.security == type },
                        total = sorted.size,
                        color = securityColor(type)
                    )
                }
            }
        }
    }
    val risky = sorted.filter { WifiAssessment.isWeak(it.security) }
    if (risky.isNotEmpty()) {
        item { ZdSectionLabel("Risky networks") }
        item { ZdListCard(risky) { ap -> NetworkRow(ap, sorted, onClick = { onOpen(ap) }) } }
    }
}

@Composable
private fun NetworkRow(ap: WifiAccessPoint, all: List<WifiAccessPoint>, onClick: () -> Unit) {
    val twin = !WifiAssessment.isHidden(ap) && all.any {
        it.ssid == ap.ssid && it.bssid != ap.bssid && WifiAssessment.ouiOf(it.bssid) != WifiAssessment.ouiOf(ap.bssid)
    }
    ZdListRow(
        title = WifiAssessment.displayName(ap),
        leading = { ZdSignal(ap.rssi) },
        showChevron = true,
        onClick = onClick,
        supporting = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SecurityTag(ap.security)
                    Text("ch ${ap.channel} · ${WifiAssessment.hardwareLabel(ap.bssid)}", style = ZdType.Caption, color = ZdColors.Text3)
                }
                if (twin) {
                    ZdTag("Same name, other hardware", color = ZdColors.Medium, background = ZdColors.MediumBg, border = ZdColors.MediumBorder)
                }
            }
        }
    )
}

@Composable
private fun SecurityTag(type: SecurityType) {
    val color = securityColor(type)
    val bg = when (type) {
        SecurityType.WPA3, SecurityType.WPA2 -> ZdColors.AccentBg
        SecurityType.WPA -> ZdColors.MediumBg
        SecurityType.WEP, SecurityType.OPEN -> ZdColors.CriticalBg
        SecurityType.UNKNOWN -> ZdColors.Surface2
    }
    ZdTag(type.label.uppercase(), color = color, background = bg, border = bg)
}

private fun securityColor(type: SecurityType): Color = when (type) {
    SecurityType.WPA3, SecurityType.WPA2 -> ZdColors.Accent
    SecurityType.WPA -> ZdColors.Medium
    SecurityType.WEP, SecurityType.OPEN -> ZdColors.Critical
    SecurityType.UNKNOWN -> ZdColors.Text3
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkDetailSheet(ap: WifiAccessPoint, all: List<WifiAccessPoint>, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ZdColors.Surface,
        scrimColor = ZdColors.Scrim,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ZdSignal(ap.rssi)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(WifiAssessment.displayName(ap), style = ZdType.Title, color = ZdColors.Text)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SecurityTag(ap.security)
                        ZdTag(ap.band.label)
                    }
                }
                Text(
                    WifiAssessment.grade(ap),
                    style = ZdType.Display.copy(fontSize = ZdType.Title.fontSize.times(1.4f)),
                    color = securityColor(ap.security)
                )
            }
            ZdCard {
                Row {
                    ZdStat("BSSID", ap.bssid, Modifier.weight(1f))
                    ZdStat("Hardware", WifiAssessment.hardwareLabel(ap.bssid).removePrefix("OUI "), Modifier.weight(1f))
                }
                Row {
                    ZdStat("Channel", "${ap.channel} (${ap.frequency} MHz)", Modifier.weight(1f))
                    ZdStat("Width", if (ap.channelWidth > 0) "${ap.channelWidth} MHz" else "—", Modifier.weight(1f))
                }
                Row {
                    ZdStat("Signal", "${ap.rssi} dBm · ${ap.signalPercent}%", Modifier.weight(1f))
                    ZdStat("Co-channel APs", WifiAssessment.coChannel(ap, all).toString(), Modifier.weight(1f))
                }
            }
            ZdSectionLabel("Security rating")
            ZdListCard(WifiAssessment.checks(ap, all)) { check ->
                ZdCheckRow(
                    title = check.title,
                    detail = check.detail,
                    status = when (check.outcome) {
                        CheckOutcome.PASS -> ZdCheckStatus.PASS
                        CheckOutcome.WARN -> ZdCheckStatus.WARN
                        CheckOutcome.FAIL -> ZdCheckStatus.FAIL
                    }
                )
            }
            ZdDivider()
            ZdFootnote("Grades weigh the encryption first, then extras like WPS. A = WPA3 without WPS; F = open or WEP.")
        }
    }
}

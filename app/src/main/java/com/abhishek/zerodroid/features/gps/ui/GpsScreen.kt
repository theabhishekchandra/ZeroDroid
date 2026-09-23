package com.abhishek.zerodroid.features.gps.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdCardShape
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdScanControlBar
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTabs
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.features.gps.domain.GpsState
import com.abhishek.zerodroid.features.gps.domain.SatelliteInfo
import com.abhishek.zerodroid.features.gps.viewmodel.GpsViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.util.Locale
import kotlin.math.abs

@Composable
fun GpsScreen(
    viewModel: GpsViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.gpsPermissions(),
        rationale = "Position, satellites and raw NMEA all come from the location system, which Android gates."
    ) {
        GpsContent(viewModel)
    }
}

@Composable
private fun GpsContent(viewModel: GpsViewModel) {
    val state by viewModel.state.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    HardwareLifecycleEffect(
        isActive = state.isTracking,
        onPause = viewModel::stopTracking,
        onResume = viewModel::startTracking
    )

    val used = state.satellites.count { it.usedInFix }
    val hasFix = state.lastUpdateTime > 0L
    Column(Modifier.fillMaxSize()) {
        ZdScanControlBar(
            running = state.isTracking,
            onStart = viewModel::startTracking,
            onStop = viewModel::stopTracking,
            runningLabel = when {
                !hasFix -> "Searching"
                used >= 4 -> "Fix · 3D"
                else -> "Fix · 2D"
            },
            runningDetail = "$used of ${state.satellites.size} satellites used",
            idleDetail = if (hasFix) "Last fix kept" else "Best outdoors or by a window"
        )

        if (!state.isTracking && !hasFix) {
            ZdStatePanel(
                kicker = if (state.error != null) "Couldn’t start" else "Ready",
                kickerColor = if (state.error != null) ZdColors.Critical else ZdColors.Text3,
                icon = ZdIcons.Crosshair,
                iconTint = ZdColors.Accent,
                iconBackground = ZdColors.AccentBg,
                title = "Where the satellites think you are",
                body = state.error ?: "Live position, every satellite your phone can hear across GPS, Galileo, GLONASS and BeiDou, and the raw NMEA sentences receivers speak.",
                primaryAction = "Start tracking" to viewModel::startTracking,
                primaryIcon = ZdIcons.Play
            )
            return@Column
        }

        ZdTabs(
            tabs = listOf("Position", "Satellites ${state.satellites.size}", "NMEA"),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
        )
        when (tab) {
            0 -> PositionTab(state)
            1 -> SatellitesTab(state)
            else -> NmeaTab(state)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PositionTab(state: GpsState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ZdCard {
                Row {
                    ZdStat("Latitude", coord(state.latitude, "N", "S"), Modifier.weight(1f))
                    ZdStat("Longitude", coord(state.longitude, "E", "W"), Modifier.weight(1f))
                }
                Row {
                    ZdStat("Accuracy", "±%.1f m".format(Locale.US, state.accuracy), Modifier.weight(1f))
                    ZdStat("Altitude", "${state.altitude.toInt()} m", Modifier.weight(1f))
                }
                Row {
                    ZdStat("Speed", "%.1f km/h".format(Locale.US, state.speed * 3.6f), Modifier.weight(1f))
                    ZdStat("Bearing", if (state.speed > 0.5f) "${state.bearing.toInt()}° ${bearingToDirection(state.bearing)}" else "—", Modifier.weight(1f))
                }
            }
        }
        item {
            val byConstellation = state.satellites.groupBy { it.constellationName }
            ZdCard {
                Text("Constellations", style = ZdType.Label, color = ZdColors.Text)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    byConstellation.entries.sortedByDescending { it.value.size }.forEach { (name, sats) ->
                        val used = sats.count { it.usedInFix }
                        ZdTag(
                            "$name $used/${sats.size}",
                            color = if (used > 0) ZdColors.Accent else ZdColors.Text2,
                            background = if (used > 0) ZdColors.AccentBg else ZdColors.Surface2,
                            border = if (used > 0) ZdColors.AccentBorder else ZdColors.Border
                        )
                    }
                }
                Text("used / visible. A fix needs 4+ satellites; more constellations make it faster and steadier.", style = ZdType.Caption, color = ZdColors.Text3)
            }
        }
        state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
        item { ZdFootnote("Provider: ${state.provider.ifEmpty { "—" }}. Accuracy drops sharply indoors.") }
    }
}

@Composable
private fun SatellitesTab(state: GpsState) {
    var constellation by rememberSaveable { mutableStateOf<String?>(null) }
    val groups = state.satellites.groupingBy { it.constellationName }.eachCount()
    val shown = state.satellites
        .filter { constellation == null || it.constellationName == constellation }
        .sortedWith(compareByDescending<SatelliteInfo> { it.usedInFix }.thenByDescending { it.cn0DbHz })
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ZdChipRow(contentPadding = 0.dp) {
                ZdChip("All ${state.satellites.size}", selected = constellation == null, onClick = { constellation = null })
                groups.entries.sortedByDescending { it.value }.forEach { (name, n) ->
                    ZdChip("$name $n", selected = constellation == name, onClick = { constellation = name })
                }
            }
        }
        item {
            if (shown.isEmpty()) ZdFootnote("No satellites heard yet. Move near a window.")
            else ZdListCard(shown) { SatelliteRow(it) }
        }
        item { ZdFootnote("Bars show signal quality (C/N0, dB-Hz); 35+ is strong. Grey = visible but not used in the fix.") }
    }
}

@Composable
private fun SatelliteRow(sat: SatelliteInfo) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.width(92.dp)) {
            Text("${sat.constellationName.take(1)}${sat.svid}", style = ZdType.Label, color = if (sat.usedInFix) ZdColors.Text else ZdColors.Text3)
            Text(listOfNotNull(sat.constellationName, sat.frequencyBand).joinToString(" · "), style = ZdType.Path, color = ZdColors.Text3)
        }
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ZdColors.Surface3)
        ) {
            Box(
                Modifier
                    .fillMaxWidth((sat.cn0DbHz / 50f).coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (sat.usedInFix) ZdColors.Accent else ZdColors.BorderStrong)
            )
        }
        Text("${sat.cn0DbHz.toInt()}", style = ZdType.Label, color = ZdColors.Text2, modifier = Modifier.width(24.dp))
        Text("${sat.elevationDeg.toInt()}°/${sat.azimuthDeg.toInt()}°", style = ZdType.Path, color = ZdColors.Text3, modifier = Modifier.width(64.dp))
    }
}

@Composable
private fun NmeaTab(state: GpsState) {
    val context = LocalContext.current
    var type by rememberSaveable { mutableStateOf<String?>(null) }
    val types = listOf("GGA", "RMC", "GSV", "GSA")
    val shown = state.nmeaSentences.filter { s -> type == null || s.drop(3).startsWith(type!!) }.takeLast(60)
    Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
        ZdChipRow {
            ZdChip("All", selected = type == null, onClick = { type = null })
            types.forEach { t -> ZdChip(t, selected = type == t, onClick = { type = t }) }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(ZdCardShape)
                .background(ZdColors.Bg)
                .border(1.dp, ZdColors.Border, ZdCardShape),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (shown.isEmpty()) item { Text("Waiting for sentences…", style = ZdType.Mono, color = ZdColors.Text3) }
            shown.forEach { line -> item { Text(line.trim(), style = ZdType.Path, color = ZdColors.Text2) } }
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            ZdButton(
                "Copy all",
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("NMEA", state.nmeaSentences.joinToString("\n")))
                },
                variant = ZdButtonVariant.Secondary,
                icon = ZdIcons.Copy,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun coord(value: Double, positive: String, negative: String): String =
    "%.5f° %s".format(Locale.US, abs(value), if (value >= 0) positive else negative)

private fun bearingToDirection(bearing: Float): String {
    val normalized = ((bearing % 360) + 360) % 360
    return when {
        normalized < 22.5f || normalized >= 337.5f -> "N"
        normalized < 67.5f -> "NE"
        normalized < 112.5f -> "E"
        normalized < 157.5f -> "SE"
        normalized < 202.5f -> "S"
        normalized < 247.5f -> "SW"
        normalized < 292.5f -> "W"
        else -> "NW"
    }
}

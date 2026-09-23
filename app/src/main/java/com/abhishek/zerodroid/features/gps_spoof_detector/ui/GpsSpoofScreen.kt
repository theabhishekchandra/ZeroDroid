package com.abhishek.zerodroid.features.gps_spoof_detector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdCheckRow
import com.abhishek.zerodroid.core.ui.zd.ZdCheckStatus
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdToolScanBar
import com.abhishek.zerodroid.features.gps_spoof_detector.viewmodel.GpsSpoofViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.util.Locale

@Composable
fun GpsSpoofScreen(
    viewModel: GpsSpoofViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.gpsSpoofPermissions(),
        rationale = "Cross-checking your position needs GPS, cell and WiFi readings, which Android only shares with permission."
    ) {
        GpsSpoofContent(viewModel)
    }
}

@Composable
private fun GpsSpoofContent(viewModel: GpsSpoofViewModel) {
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
            verb = "Checking",
            runningNote = "GPS + cell + WiFi + sensors",
            idleNote = if (state.results.isEmpty()) "Best outdoors or by a window" else "Results kept"
        )

        val latest = state.results.lastOrNull()
        if (latest == null) {
            ZdStatePanel(
                kicker = when {
                    state.error != null -> "Couldn’t start"
                    state.isMonitoring -> "Waiting for a fix"
                    else -> "Ready"
                },
                kickerColor = if (state.error != null) ZdColors.Critical else ZdColors.Text3,
                icon = ZdIcons.Crosshair,
                iconTint = ZdColors.Accent,
                iconBackground = ZdColors.AccentBg,
                title = if (state.isMonitoring) "Gathering location sources…" else "Is your GPS telling the truth?",
                body = state.error ?: "Compares GPS with the cell tower, nearby WiFi, the barometer and motion sensors. A spoofer can fake GPS, but rarely all of these at once.",
                chipsLabel = if (state.isMonitoring) "Sources" else null,
                chips = if (state.isMonitoring) listOf("GPS: ${state.gpsStatus}", "Cell: ${state.cellStatus}", "WiFi: ${state.wifiStatus}", "Sensors: ${state.sensorStatus}") else emptyList(),
                primaryAction = if (state.isMonitoring) null else "Start checking" to viewModel::startMonitoring,
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
                val passed = latest.checks.count { it.passed }
                val total = latest.checks.size
                val spoofPct = (state.confidence * 100).toInt()
                ZdCard(
                    background = if (state.spoofDetected) ZdColors.CriticalBg else ZdColors.Surface,
                    borderColor = if (state.spoofDetected) ZdColors.CriticalBorder else ZdColors.Border
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ZdIconTile(
                            if (state.spoofDetected) ZdIcons.Warning else ZdIcons.ShieldCheck,
                            tint = if (state.spoofDetected) ZdColors.Critical else ZdColors.Accent,
                            background = if (state.spoofDetected) ZdColors.Bg else ZdColors.AccentBg,
                            size = 44.dp,
                            iconSize = 22.dp
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (state.spoofDetected) "Location may be spoofed" else "Location looks real",
                                style = ZdType.Heading,
                                color = ZdColors.Text
                            )
                            Text("$passed of $total checks agree · spoof likelihood $spoofPct%", style = ZdType.BodySmall, color = ZdColors.Text2)
                        }
                    }
                }
            }
            item {
                ZdCard {
                    Row {
                        ZdStat("GPS vs cell", latest.gpsVsCellDistanceKm?.let { km(it) } ?: "—", Modifier.weight(1f))
                        ZdStat("GPS vs WiFi", latest.gpsVsWifiDistanceKm?.let { km(it) } ?: "—", Modifier.weight(1f))
                    }
                    Row {
                        ZdStat("Satellites", latest.gpsSatelliteCount?.toString() ?: "—", Modifier.weight(1f))
                        ZdStat("Altitude", latest.gpsAltitudeM?.let { "${it.toInt()} m" } ?: "—", Modifier.weight(1f))
                    }
                }
            }
            item { ZdSectionLabel("Checks", trailingText = "${latest.checks.size}") }
            item {
                ZdListCard(latest.checks) { check ->
                    ZdCheckRow(
                        title = check.name,
                        detail = check.detail,
                        status = if (check.passed) ZdCheckStatus.PASS else ZdCheckStatus.WARN
                    )
                }
            }
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
            item {
                ZdFootnote("One warning is common indoors. Spoofing usually trips several checks at once, especially speed and cell distance.")
            }
        }
    }
}

private fun km(value: Double): String = if (value < 1.0) "${(value * 1000).toInt()} m" else String.format(Locale.US, "%.1f km", value)

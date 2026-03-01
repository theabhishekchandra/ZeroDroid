package com.abhishek.zerodroid.features.gps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.gps.domain.GpsState
import com.abhishek.zerodroid.features.gps.domain.SatelliteInfo
import com.abhishek.zerodroid.features.gps.viewmodel.GpsViewModel
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TextDim

@Composable
fun GpsScreen(
    viewModel: GpsViewModel = viewModel(factory = GpsViewModel.Factory)
) {
    PermissionGate(
        permissions = PermissionUtils.gpsPermissions(),
        rationale = "Location permission is needed to track GPS position and satellites."
    ) {
        GpsContent(viewModel)
    }
}

@Composable
private fun GpsContent(viewModel: GpsViewModel) {
    val state by viewModel.state.collectAsState()
    var satellitesExpanded by remember { mutableStateOf(false) }
    var nmeaExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> GPS Tracker",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (state.isTracking) {
                    OutlinedButton(
                        onClick = { viewModel.toggleTracking() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Stop") }
                } else {
                    Button(
                        onClick = { viewModel.toggleTracking() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) { Text("Track") }
                }
            }
        }

        // Scanning indicator
        if (state.isTracking) {
            item {
                ScanningIndicator(isScanning = true, label = "Tracking GPS...")
            }
        }

        // Error
        state.error?.let { error ->
            item {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Empty state
        if (!state.isTracking && state.lastUpdateTime == 0L) {
            item {
                EmptyState(
                    icon = Icons.Default.LocationOff,
                    title = "GPS Inactive",
                    subtitle = "Tap Track to start receiving GPS data."
                )
            }
        }

        // Position card
        if (state.lastUpdateTime != 0L) {
            item { PositionCard(state) }
            item { MotionCard(state) }
            item { SatelliteSummaryCard(state) }

            // Satellite list (expandable)
            if (state.satellites.isNotEmpty()) {
                item {
                    Text(
                        text = if (satellitesExpanded)
                            "> Satellites (${state.satellites.size}) [-]"
                        else
                            "> Satellites (${state.satellites.size}) [+]",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { satellitesExpanded = !satellitesExpanded }
                    )
                }
                if (satellitesExpanded) {
                    items(state.satellites) { sat -> SatelliteRow(sat) }
                }
            }

            // NMEA section (expandable)
            if (state.nmeaSentences.isNotEmpty()) {
                item {
                    Text(
                        text = if (nmeaExpanded)
                            "> NMEA Sentences (${state.nmeaSentences.size.coerceAtMost(20)}) [-]"
                        else
                            "> NMEA Sentences (${state.nmeaSentences.size.coerceAtMost(20)}) [+]",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { nmeaExpanded = !nmeaExpanded }
                    )
                }
                if (nmeaExpanded) {
                    item { NmeaCard(state.nmeaSentences.take(20)) }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun PositionCard(state: GpsState) {
    TerminalCard {
        Text(
            text = "> Position",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        DataRow("Latitude", String.format("%.6f", state.latitude))
        DataRow("Longitude", String.format("%.6f", state.longitude))
        DataRow("Altitude", String.format("%.1f m", state.altitude))
        DataRow("Provider", state.provider.uppercase())
    }
}

@Composable
private fun MotionCard(state: GpsState) {
    val speedKmh = state.speed * 3.6f
    val direction = bearingToDirection(state.bearing)

    TerminalCard {
        Text(
            text = "> Motion",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        DataRow("Speed", String.format("%.1f m/s (%.1f km/h)", state.speed, speedKmh))
        DataRow("Bearing", String.format("%.1f\u00B0 %s", state.bearing, direction))
        DataRow("Accuracy", String.format("\u00B1%.1f m", state.accuracy))
    }
}

@Composable
private fun SatelliteSummaryCard(state: GpsState) {
    val usedCount = state.satellites.count { it.usedInFix }
    val totalCount = state.satellites.size
    val constellationCounts = state.satellites
        .groupBy { it.constellationName }
        .mapValues { it.value.size }
        .toSortedMap()

    TerminalCard {
        Text(
            text = "> Satellite Summary",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        DataRow("In Fix / Total", "$usedCount / $totalCount")
        constellationCounts.forEach { (name, count) ->
            val usedInConstellation = state.satellites.count {
                it.constellationName == name && it.usedInFix
            }
            DataRow(name, "$usedInConstellation / $count")
        }
    }
}

@Composable
private fun SatelliteRow(sat: SatelliteInfo) {
    val statusColor = if (sat.usedInFix) TerminalGreen else TerminalRed
    val signalColor = when {
        sat.cn0DbHz >= 35f -> TerminalGreen
        sat.cn0DbHz >= 25f -> TerminalAmber
        else -> TerminalRed
    }
    val fillFraction = (sat.cn0DbHz / 50f).coerceIn(0f, 1f)

    TerminalCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${sat.constellationName} #${sat.svid}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (sat.usedInFix) "IN FIX" else "NO FIX",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "CN0: ${String.format("%.1f", sat.cn0DbHz)} dB-Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                    Text(
                        text = "El: ${String.format("%.0f", sat.elevationDeg)}\u00B0",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                    Text(
                        text = "Az: ${String.format("%.0f", sat.azimuthDeg)}\u00B0",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Signal bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                ) {
                    // Background track
                    drawRoundRect(
                        color = signalColor.copy(alpha = 0.2f),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    // Filled portion
                    drawRoundRect(
                        color = signalColor,
                        size = Size(size.width * fillFraction, size.height),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NmeaCard(sentences: List<String>) {
    TerminalCard {
        sentences.forEach { sentence ->
            Text(
                text = sentence,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = TerminalCyan,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextDim
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TerminalGreen
        )
    }
}

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

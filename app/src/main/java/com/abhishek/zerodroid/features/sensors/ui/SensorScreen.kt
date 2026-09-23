package com.abhishek.zerodroid.features.sensors.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTabs
import com.abhishek.zerodroid.core.ui.zd.ZdToolScanBar
import com.abhishek.zerodroid.features.sensors.domain.SensorReading
import com.abhishek.zerodroid.features.sensors.viewmodel.SensorViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.util.Locale
import kotlin.math.sqrt

private const val AUTO_STOP_MS = 60_000L

private val instruments = listOf("Metal", "Level", "Compass", "Vibration")

@Composable
fun SensorScreen(
    viewModel: SensorViewModel = hiltViewModel()
) {
    val isMonitoring by viewModel.isMonitoring.collectAsState()

    HardwareLifecycleEffect(
        isActive = isMonitoring,
        onPause = viewModel::stopSensors,
        onResume = viewModel::startSensors
    )
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var instrument by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        ZdToolScanBar(
            running = isMonitoring,
            onStart = viewModel::startSensors,
            onStop = viewModel::stopSensors,
            verb = "Monitoring",
            runningNote = "5 Hz",
            autoStopMs = AUTO_STOP_MS,
            idleNote = "Sensors refresh 5 times a second"
        )

        if (!isMonitoring) {
            ZdStatePanel(
                kicker = "Ready",
                icon = ZdIcons.Sensors,
                iconTint = ZdColors.Accent,
                iconBackground = ZdColors.AccentBg,
                title = "What your phone can feel",
                body = "Live readings from the accelerometer, gyroscope, magnetometer, barometer and light sensor, plus instruments built on them: a metal detector, spirit level, compass and vibration meter.",
                primaryAction = "Start monitoring" to viewModel::startSensors,
                primaryIcon = ZdIcons.Play
            )
            return@Column
        }

        ZdTabs(
            tabs = listOf("Readings", "Instruments"),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (tab == 0) {
            ReadingsTab(viewModel)
        } else {
            Column(Modifier.fillMaxSize()) {
                ZdChipRow(Modifier.padding(top = 12.dp)) {
                    instruments.forEachIndexed { i, label -> ZdChip(label, selected = instrument == i, onClick = { instrument = i }) }
                }
                InstrumentTab(viewModel, instrument)
            }
        }
    }
}

@Composable
private fun ReadingsTab(viewModel: SensorViewModel) {
    val accelerometer by viewModel.accelerometer.collectAsState()
    val gyroscope by viewModel.gyroscope.collectAsState()
    val magnetometer by viewModel.magnetometer.collectAsState()
    val barometer by viewModel.barometer.collectAsState()
    val light by viewModel.light.collectAsState()
    val proximity by viewModel.proximity.collectAsState()
    val floorState by viewModel.floorState.collectAsState()
    val heading by viewModel.compassHeading.collectAsState()
    val tilt by viewModel.tiltState.collectAsState()
    val vibration by viewModel.vibrationState.collectAsState()
    val metal by viewModel.metalDetectorState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ZdSectionLabel("Motion") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReadingTile("Accelerometer", accelerometer, "%.2f", Modifier.weight(1f))
                ReadingTile("Gyroscope", gyroscope, "%.2f", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ValueTile("Vibration", vibration.severity.label.uppercase(), "", "peak %.1f m/s²".format(Locale.US, vibration.peakMagnitude), Modifier.weight(1f))
                ValueTile("Level", "%.1f°".format(Locale.US, maxOf(kotlin.math.abs(tilt.pitch), kotlin.math.abs(tilt.roll))), "", "pitch %.1f · roll %.1f".format(Locale.US, tilt.pitch, tilt.roll), Modifier.weight(1f), if (tilt.isLevel) ZdColors.Accent else ZdColors.Text)
            }
        }
        item { ZdSectionLabel("Magnetic") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReadingTile("Magnetometer", magnetometer, "%.1f", Modifier.weight(1f), detail = "baseline %.1f · %+.1f".format(Locale.US, metal.baseline, metal.deviation))
                ValueTile("Compass", "${heading.toInt()}°", compassPoint(heading), "magnetic north", Modifier.weight(1f))
            }
        }
        item { ZdSectionLabel("Environment") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (barometer.isAvailable) {
                    ValueTile("Pressure", "%.0f".format(Locale.US, floorState.pressureHpa), "hPa", "floor ~${floorState.estimatedFloor}", Modifier.weight(1f))
                } else {
                    ValueTile("Pressure", "—", "", "no barometer", Modifier.weight(1f), ZdColors.Text3)
                }
                ReadingTile("Light", light, "%.0f", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReadingTile("Proximity", proximity, "%.1f", Modifier.weight(1f))
                Column(Modifier.weight(1f)) {}
            }
        }
        item { ZdFootnote("Readings stop after 60 s to save battery. Open Instruments for the metal detector, level, compass and vibration meter.") }
    }
}

@Composable
private fun InstrumentTab(viewModel: SensorViewModel, index: Int) {
    val metal by viewModel.metalDetectorState.collectAsState()
    val heading by viewModel.compassHeading.collectAsState()
    val tilt by viewModel.tiltState.collectAsState()
    val vibration by viewModel.vibrationState.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (index) {
            0 -> {
                item { MetalDetectorView(state = metal, onReset = viewModel::resetMetalDetector) }
                item { ZdFootnote("Move the top of the phone slowly along the wall. Press Reset first, away from metal, to set the baseline.") }
            }
            1 -> {
                item { LevelMeterView(tiltState = tilt) }
                item { ZdFootnote("Lay the phone on its back for surfaces, or on its long edge for walls and frames.") }
            }
            2 -> {
                item { CompassView(heading = heading) }
                item { ZdFootnote("Keep away from speakers and metal cases; accuracy drops near magnets. Wave the phone in a figure 8 to calibrate.") }
            }
            else -> {
                item { VibrationDetectorView(state = vibration, onReset = viewModel::resetVibrationPeak) }
                item { ZdFootnote("Below 0.5 m/s² is normal building hum; 0.5–2 is appliances and traffic; above 2 is machinery.") }
            }
        }
    }
}

@Composable
private fun ReadingTile(label: String, reading: SensorReading, format: String, modifier: Modifier = Modifier, detail: String? = null) {
    if (!reading.isAvailable || reading.values.isEmpty()) {
        ValueTile(label, "—", "", "not on this phone", modifier, ZdColors.Text3)
        return
    }
    val v = reading.values
    val value = if (v.size >= 3) sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]) else v[0]
    val axes = if (v.size >= 3) "x %.2f · y %.2f · z %.2f".format(Locale.US, v[0], v[1], v[2]) else null
    ValueTile(label, format.format(Locale.US, value), reading.unit, detail ?: axes ?: reading.name, modifier)
}

@Composable
private fun ValueTile(
    label: String,
    value: String,
    unit: String,
    detail: String,
    modifier: Modifier = Modifier,
    valueColor: Color = ZdColors.Text
) {
    ZdCard(modifier = modifier.heightIn(min = 104.dp), verticalSpacing = 4.dp) {
        Text(label.uppercase(), style = ZdType.Path.copy(letterSpacing = ZdType.Section.letterSpacing.times(0.7f)), color = ZdColors.Text3)
        Row {
            Text(value, style = ZdType.Heading.copy(fontSize = ZdType.Title.fontSize.times(1.2f)), color = valueColor)
            if (unit.isNotEmpty()) Text(" $unit", style = ZdType.Mono, color = ZdColors.Text3, modifier = Modifier.padding(top = 8.dp))
        }
        Text(detail, style = ZdType.Caption, color = ZdColors.Text3, maxLines = 2)
    }
}

private fun compassPoint(deg: Float): String {
    val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return points[(((deg % 360) + 360) % 360 / 45f + 0.5f).toInt() % 8]
}

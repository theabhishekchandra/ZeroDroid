package com.abhishek.zerodroid.features.ultrasonic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdMetric
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdTabs
import com.abhishek.zerodroid.core.ui.zd.ZdToolScanBar
import com.abhishek.zerodroid.features.ultrasonic.domain.UltrasonicScreenTab
import com.abhishek.zerodroid.features.ultrasonic.domain.UltrasonicState
import com.abhishek.zerodroid.features.ultrasonic.viewmodel.UltrasonicViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.util.Locale

@Composable
fun UltrasonicScreen(viewModel: UltrasonicViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    HardwareLifecycleEffect(
        isActive = state.isRecording,
        onPause = viewModel::stopAnalysis,
        onResume = viewModel::startAnalysis,
        key = "microphone"
    )
    HardwareLifecycleEffect(
        isActive = state.isTonePlaying,
        onPause = viewModel::stopTone,
        onResume = viewModel::startTone,
        key = "speaker"
    )

    Column(Modifier.fillMaxSize()) {
        ZdTabs(
            tabs = listOf("Analyzer", "Tone generator"),
            selectedIndex = if (state.activeTab == UltrasonicScreenTab.DETECT) 0 else 1,
            onSelect = { viewModel.setActiveTab(if (it == 0) UltrasonicScreenTab.DETECT else UltrasonicScreenTab.GENERATE) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        when (state.activeTab) {
            UltrasonicScreenTab.DETECT -> PermissionGate(
                permissions = PermissionUtils.audioPermissions(),
                rationale = "The analyzer measures sound frequencies through the microphone. Audio is never saved."
            ) {
                AnalyzerTab(state, viewModel)
            }
            UltrasonicScreenTab.GENERATE -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                ToneGeneratorPanel(
                    frequency = state.toneFrequency,
                    isPlaying = state.isTonePlaying,
                    onFrequencyChange = viewModel::setToneFrequency,
                    onPlay = viewModel::startTone,
                    onStop = viewModel::stopTone
                )
            }
        }
    }
}

@Composable
private fun AnalyzerTab(state: UltrasonicState, viewModel: UltrasonicViewModel) {
    Column(Modifier.fillMaxSize()) {
        ZdToolScanBar(
            running = state.isRecording,
            onStart = viewModel::startAnalysis,
            onStop = viewModel::stopAnalysis,
            verb = "Listening",
            runningNote = "Mic in use · audio never saved",
            idleNote = "Keep the room quiet while it listens"
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ZdCard {
                    Row {
                        Text("Spectrum · 18–24 kHz", style = ZdType.Mono.copy(fontWeight = ZdType.Label.fontWeight), color = ZdColors.Text2, modifier = Modifier.weight(1f))
                        if (state.detectedBeacons.isNotEmpty()) ZdSeverityBadge(ZdSeverity.MEDIUM, label = "BEACON?")
                    }
                    SpectrumChart(bins = state.spectrumData)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("18 kHz", style = ZdType.Path, color = ZdColors.Text3)
                        Text("21 kHz", style = ZdType.Path, color = ZdColors.Text3)
                        Text("24 kHz", style = ZdType.Path, color = ZdColors.Text3)
                    }
                }
            }
            item {
                ZdCard {
                    Row {
                        ZdMetric("${state.detectedBeacons.size}", "Peaks", Modifier.weight(1f), valueColor = if (state.detectedBeacons.isNotEmpty()) ZdColors.Medium else ZdColors.Text)
                        ZdMetric(if (state.peakFrequency > 0) "%.2f".format(Locale.US, state.peakFrequency / 1000f) else "—", "Peak kHz", Modifier.weight(1f))
                    }
                }
            }
            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
            if (state.detectedBeacons.isNotEmpty()) {
                item { ZdSectionLabel("Possible beacons", trailingText = "${state.detectedBeacons.size}") }
                items(state.detectedBeacons) { beacon ->
                    ZdCard(borderColor = ZdColors.MediumBorder) {
                        ZdSeverityBadge(ZdSeverity.MEDIUM, label = "BEACON?")
                        Text("%.2f kHz".format(Locale.US, beacon.centerFrequencyHz / 1000f), style = ZdType.Heading, color = ZdColors.Text)
                        Text(
                            "A narrow, steady tone above human hearing. Common sources: TV ads, retail speakers and some apps, but also chargers and old screens.",
                            style = ZdType.BodySmall,
                            color = ZdColors.Text2
                        )
                        Row {
                            ZdStat("Bandwidth", "%.0f Hz".format(Locale.US, beacon.bandwidth), Modifier.weight(1f))
                            ZdStat("Level", "%.4f".format(Locale.US, beacon.magnitude), Modifier.weight(1f))
                        }
                    }
                }
            }
            item { ZdFootnote("Some phone mics roll off above 20 kHz, so “quiet” doesn’t prove nothing is there.") }
        }
    }
}

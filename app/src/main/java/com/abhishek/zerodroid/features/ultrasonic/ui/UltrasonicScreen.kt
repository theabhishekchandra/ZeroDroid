package com.abhishek.zerodroid.features.ultrasonic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ultrasonic.domain.UltrasonicScreenTab
import com.abhishek.zerodroid.features.ultrasonic.viewmodel.UltrasonicViewModel

@Composable
fun UltrasonicScreen(viewModel: UltrasonicViewModel = viewModel(factory = UltrasonicViewModel.Factory)) {
    val state by viewModel.state.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.activeTab == UltrasonicScreenTab.DETECT, onClick = { viewModel.setActiveTab(UltrasonicScreenTab.DETECT) },
                    label = { Text("Detect", style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = { Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary, selectedLeadingIconColor = MaterialTheme.colorScheme.primary))
                FilterChip(selected = state.activeTab == UltrasonicScreenTab.GENERATE, onClick = { viewModel.setActiveTab(UltrasonicScreenTab.GENERATE) },
                    label = { Text("Generate", style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Speaker, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary, selectedLeadingIconColor = MaterialTheme.colorScheme.primary))
            }
        }

        when (state.activeTab) {
            UltrasonicScreenTab.DETECT -> {
                item {
                    PermissionGate(permissions = PermissionUtils.audioPermissions(),
                        rationale = "Microphone permission is needed to analyze ultrasonic frequencies."
                    ) { UltrasonicDetectContent(viewModel) }
                }
            }
            UltrasonicScreenTab.GENERATE -> {
                item {
                    ToneGeneratorPanel(frequency = state.toneFrequency, isPlaying = state.isTonePlaying,
                        onFrequencyChange = viewModel::setToneFrequency, onPlay = viewModel::startTone, onStop = viewModel::stopTone)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun UltrasonicDetectContent(viewModel: UltrasonicViewModel) {
    val state by viewModel.state.collectAsState()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "> Ultrasonic (18-24kHz)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        if (state.isRecording) {
            OutlinedButton(onClick = { viewModel.stopAnalysis() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Stop") }
        } else {
            Button(onClick = { viewModel.startAnalysis() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Analyze") }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    state.error?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error); Spacer(modifier = Modifier.height(8.dp)) }
    TerminalCard {
        Text(text = "> Frequency Spectrum", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        SpectrumChart(bins = state.spectrumData)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("18kHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("21kHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("24kHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (state.peakFrequency > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        TerminalCard { Text(text = "Peak: %.1f Hz (%.4f)".format(state.peakFrequency, state.peakMagnitude), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface) }
    }
    if (state.detectedBeacons.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "> Beacons Detected (${state.detectedBeacons.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(4.dp))
        state.detectedBeacons.forEach { beacon -> BeaconAlertCard(beacon = beacon); Spacer(modifier = Modifier.height(4.dp)) }
    }
}

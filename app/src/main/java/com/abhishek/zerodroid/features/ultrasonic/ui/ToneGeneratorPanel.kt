package com.abhishek.zerodroid.features.ultrasonic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ultrasonic.domain.ToneGenerator
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalGreenDark
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun ToneGeneratorPanel(
    frequency: Int, isPlaying: Boolean,
    onFrequencyChange: (Int) -> Unit, onPlay: () -> Unit, onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TerminalCard {
            Text(text = "> Tone Generator", style = MaterialTheme.typography.labelSmall, color = TerminalCyan)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                Text(text = "%,d".format(frequency), style = MaterialTheme.typography.displayLarge,
                    color = if (isPlaying) TerminalGreen else MaterialTheme.colorScheme.onSurface)
                Text(text = " Hz", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = frequencyDescription(frequency), style = MaterialTheme.typography.labelSmall,
                color = TerminalAmber, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = frequency.toFloat(),
                onValueChange = { onFrequencyChange(it.toInt()) },
                valueRange = ToneGenerator.MIN_FREQUENCY.toFloat()..ToneGenerator.MAX_FREQUENCY.toFloat(),
                steps = 59,
                colors = SliderDefaults.colors(thumbColor = TerminalGreen, activeTrackColor = TerminalGreen,
                    inactiveTrackColor = TerminalGreenDark),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("18 kHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("21 kHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("24 kHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Presets:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(18000, 19000, 20000, 21000, 22000).forEach { preset ->
                    OutlinedButton(onClick = { onFrequencyChange(preset) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (frequency == preset) TerminalGreen else MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) { Text("${preset / 1000}k", style = MaterialTheme.typography.labelSmall) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (isPlaying) {
                Button(onClick = onStop, colors = ButtonDefaults.buttonColors(containerColor = TerminalRed), modifier = Modifier.fillMaxWidth())
                { Text("Stop Transmission") }
            } else {
                Button(onClick = onPlay, colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen), modifier = Modifier.fillMaxWidth())
                { Text("Start Transmission", color = MaterialTheme.colorScheme.surface) }
            }
        }

        // Waveform preview
        TerminalCard {
            Text(text = "> Waveform Preview", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            WaveformPreview(isPlaying = isPlaying, modifier = Modifier.fillMaxWidth().height(100.dp))
        }
    }
}

@Composable
private fun WaveformPreview(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val waveColor = if (isPlaying) TerminalGreen else TerminalGreen.copy(alpha = 0.4f)
    val gridColor = TerminalGreenDark
    val toneGenerator = remember { ToneGenerator() }
    val samples = remember { toneGenerator.generatePreviewSamples(20000, 200) }

    Canvas(modifier = modifier) {
        val centerY = size.height / 2
        drawLine(gridColor, Offset(0f, centerY), Offset(size.width, centerY), strokeWidth = 0.5f)
        drawLine(gridColor, Offset(0f, size.height * 0.25f), Offset(size.width, size.height * 0.25f), strokeWidth = 0.3f)
        drawLine(gridColor, Offset(0f, size.height * 0.75f), Offset(size.width, size.height * 0.75f), strokeWidth = 0.3f)
        for (i in 1..7) { val x = size.width * i / 8f; drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.3f) }

        if (samples.isNotEmpty()) {
            val path = Path()
            val stepX = size.width / (samples.size - 1).coerceAtLeast(1)
            val amplitude = size.height * 0.4f
            samples.forEachIndexed { index, sample ->
                val x = index * stepX; val y = centerY - (sample * amplitude)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, waveColor, style = Stroke(width = 2f))
            if (isPlaying) drawPath(path, waveColor.copy(alpha = 0.2f), style = Stroke(width = 6f))
        }
    }
}

private fun frequencyDescription(hz: Int): String = when {
    hz < 18500 -> "Near-ultrasonic threshold"
    hz < 19500 -> "Low ultrasonic range"
    hz < 20500 -> "Standard ultrasonic (inaudible to most adults)"
    hz < 22000 -> "Mid ultrasonic range"
    hz < 23000 -> "High ultrasonic range"
    else -> "Near maximum ultrasonic"
}

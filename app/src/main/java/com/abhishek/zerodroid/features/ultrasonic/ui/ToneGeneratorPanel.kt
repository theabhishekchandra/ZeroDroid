package com.abhishek.zerodroid.features.ultrasonic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.features.ultrasonic.domain.ToneGenerator
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalGreenDark

@Composable
fun ToneGeneratorPanel(
    frequency: Int, isPlaying: Boolean,
    onFrequencyChange: (Int) -> Unit, onPlay: () -> Unit, onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ZdCard(verticalSpacing = 8.dp) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                Text("%,d".format(frequency), style = ZdType.Display, color = if (isPlaying) ZdColors.Accent else ZdColors.Text)
                Text(" Hz", style = ZdType.Label, color = ZdColors.Text3, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(
                frequencyDescription(frequency),
                style = ZdType.Caption,
                color = ZdColors.Text3,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Slider(
                value = frequency.toFloat(),
                onValueChange = { onFrequencyChange(it.toInt()) },
                valueRange = ToneGenerator.MIN_FREQUENCY.toFloat()..ToneGenerator.MAX_FREQUENCY.toFloat(),
                steps = 59,
                colors = SliderDefaults.colors(
                    thumbColor = ZdColors.Accent,
                    activeTrackColor = ZdColors.Accent,
                    inactiveTrackColor = ZdColors.Surface3,
                    activeTickColor = ZdColors.Accent,
                    inactiveTickColor = ZdColors.BorderStrong
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${ToneGenerator.MIN_FREQUENCY / 1000} kHz", style = ZdType.Path, color = ZdColors.Text3)
                Text("${ToneGenerator.MAX_FREQUENCY / 1000} kHz", style = ZdType.Path, color = ZdColors.Text3)
            }
        }
        ZdChipRow(contentPadding = 0.dp) {
            listOf(18000, 19000, 20000, 21000, 22000).forEach { preset ->
                ZdChip("${preset / 1000} kHz", selected = frequency == preset, onClick = { onFrequencyChange(preset) })
            }
        }
        ZdCard {
            Text("Waveform", style = ZdType.Label, color = ZdColors.Text2)
            WaveformPreview(isPlaying = isPlaying, modifier = Modifier.fillMaxWidth().height(100.dp))
        }
        if (isPlaying) {
            ZdButton("Stop tone", onClick = onStop, variant = ZdButtonVariant.Danger, icon = ZdIcons.Stop, modifier = Modifier.fillMaxWidth())
        } else {
            ZdButton("Play tone", onClick = onPlay, icon = ZdIcons.Play, modifier = Modifier.fillMaxWidth())
        }
        ZdFootnote("Use this to test whether another device’s microphone picks up ultrasound. Keep the volume low: pets can hear it.")
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

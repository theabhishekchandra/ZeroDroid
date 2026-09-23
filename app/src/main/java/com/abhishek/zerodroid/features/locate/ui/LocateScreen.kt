package com.abhishek.zerodroid.features.locate.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdSwitchRow
import com.abhishek.zerodroid.features.locate.domain.LocateTracker
import com.abhishek.zerodroid.features.locate.domain.LocateTrend
import com.abhishek.zerodroid.features.locate.viewmodel.LocateUiState
import com.abhishek.zerodroid.features.locate.viewmodel.LocateViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun LocateScreen(viewModel: LocateViewModel = hiltViewModel()) {
    PermissionGate(
        permissions = PermissionUtils.blePermissions(),
        rationale = "Locate follows one Bluetooth device’s signal strength as you move."
    ) {
        val state by viewModel.state.collectAsState()
        LaunchedEffect(Unit) { viewModel.start() }
        Feedback(state)
        LocateContent(state, viewModel)
    }
}

/** Beeps and ticks faster as the signal gets stronger, so you can walk without watching the screen. */
@Composable
private fun Feedback(state: LocateUiState) {
    val view = LocalView.current
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 60) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    val current by rememberUpdatedState(state)
    val active = state.running && !state.lost && state.reading != null && (state.beep || state.haptics)
    LaunchedEffect(active) {
        while (active && isActive) {
            val s = current
            val proximity = s.reading?.proximity ?: 0f
            if (s.beep) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
            if (s.haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            delay(LocateTracker.beepIntervalMs(proximity))
        }
    }
}

@Composable
private fun LocateContent(state: LocateUiState, viewModel: LocateViewModel) {
    val reading = state.reading
    val trend = if (state.lost) LocateTrend.LOST else reading?.trend
    val trendColor by animateColorAsState(
        when (trend) {
            LocateTrend.WARMER -> ZdColors.Accent
            LocateTrend.COLDER -> ZdColors.Info
            LocateTrend.LOST -> ZdColors.Medium
            else -> ZdColors.Text2
        },
        label = "trend"
    )
    val proximity by animateFloatAsState(reading?.proximity ?: 0f, label = "proximity")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            state.label.ifBlank { "Unnamed device" } + " · " + state.address,
            style = ZdType.Mono,
            color = ZdColors.Text3
        )
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = when {
                        reading == null -> "Listening for the device"
                        else -> "${trend?.label?.lowercase()}, ${reading.distance}, ${reading.rssi} dBm"
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val track = ZdColors.Surface3
            val accent = trendColor
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2 - 12.dp.toPx()
                val c = Offset(size.width / 2, size.height / 2)
                for (i in 1..3) {
                    drawCircle(track, radius = r * i / 3, center = c, style = Stroke(1.dp.toPx()))
                }
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * proximity,
                    useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                    style = Stroke(8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(accent.copy(alpha = 0.18f), radius = r * (0.25f + 0.6f * proximity), center = c)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(trend?.label ?: "LISTENING", style = ZdType.Label, color = trendColor)
                Text(reading?.let { "${it.rssi}" } ?: "--", style = ZdType.Display, color = ZdColors.Text)
                Text("dBm", style = ZdType.Path, color = ZdColors.Text3)
            }
        }
        Text(
            when {
                state.lost -> "Lost the signal. Step back to where it was last strong."
                reading == null -> "Listening… the device must be advertising."
                else -> reading.distance
            },
            style = ZdType.Heading,
            color = ZdColors.Text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.trail.size > 1) Trail(state.trail)
        state.error?.let { ZdFootnote(it, icon = ZdIcons.Info) }

        ZdCard(verticalSpacing = 0.dp) {
            ZdSwitchRow(label = "Beep", description = "Faster when closer", checked = state.beep, onCheckedChange = { viewModel.toggleBeep() })
            ZdSwitchRow(label = "Vibrate", description = "A tick with each beep", checked = state.haptics, onCheckedChange = { viewModel.toggleHaptics() })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.running) {
                ZdButton("Stop", onClick = viewModel::stop, variant = ZdButtonVariant.Danger, icon = ZdIcons.Stop, modifier = Modifier.weight(1f))
            } else {
                ZdButton("Locate again", onClick = viewModel::start, icon = ZdIcons.Play, modifier = Modifier.weight(1f))
            }
        }
        ZdFootnote("Walk slowly and pause every few steps; the reading is smoothed over a few seconds. Your body blocks Bluetooth, so turn around once to find the strongest direction.")
    }
}

/** The last minute of smoothed signal as thin bars. */
@Composable
private fun Trail(trail: List<Int>) {
    val bar = ZdColors.Accent
    val track = ZdColors.Surface3
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        val n = LocateViewModel.TRAIL
        val w = size.width / n
        trail.takeLast(n).forEachIndexed { i, v ->
            val p = LocateTracker.proximity(v.toFloat())
            val h = (size.height * p).coerceAtLeast(2f)
            val x = (n - trail.size.coerceAtMost(n) + i) * w
            drawLine(track, Offset(x + w / 2, size.height), Offset(x + w / 2, 0f), strokeWidth = w * 0.5f)
            drawLine(bar, Offset(x + w / 2, size.height), Offset(x + w / 2, size.height - h), strokeWidth = w * 0.5f)
        }
    }
}

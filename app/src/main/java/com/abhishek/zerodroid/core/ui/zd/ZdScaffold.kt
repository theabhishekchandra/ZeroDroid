package com.abhishek.zerodroid.core.ui.zd

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import kotlinx.coroutines.delay

// ── Header ───────────────────────────────────────────────────────────────────

/**
 * The terminal-path header on every screen: `zd:~/tools/wifi $▌` over the screen title.
 * Pass [onBack] on pushed screens; top-level tabs have no back button.
 */
@Composable
fun ZdHeader(
    path: String,
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = if (onBack != null) 8.dp else 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (onBack != null) {
            ZdIconButton(ZdIcons.Back, contentDescription = "Back", onClick = onBack, tint = ZdColors.Text)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clearAndSetSemantics { }) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = ZdColors.Accent)) { append("zd:~") }
                        append(path)
                        withStyle(SpanStyle(color = ZdColors.Accent)) { append(" $") }
                    },
                    style = ZdType.Path,
                    color = ZdColors.Text3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                BlinkingCursor(Modifier.padding(start = 3.dp))
            }
            Text(
                text = title,
                style = ZdType.Title,
                color = ZdColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
        }
        actions()
    }
}

@Composable
private fun BlinkingCursor(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                1f at 0
                1f at 549
                0f at 550
                0f at 1100
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "cursorAlpha"
    )
    Box(
        modifier
            .width(6.dp)
            .height(11.dp)
            .alpha(alpha)
            .background(ZdColors.Accent)
    )
}

// ── Scan control bar ─────────────────────────────────────────────────────────

/**
 * The same Start/Stop bar on every tool. Running shows a live dot, what is happening and
 * when it auto-stops, so battery use is never a surprise; stopped says results were kept.
 */
@Composable
fun ZdScanControlBar(
    running: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    runningLabel: String = "Scanning",
    runningDetail: String? = null,
    idleLabel: String = "Stopped",
    idleDetail: String? = null,
    startLabel: String = "Start",
    stopLabel: String = "Stop"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(ZdCardShape)
            .background(ZdColors.Surface)
            .border(1.dp, ZdColors.Border, ZdCardShape)
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (running) LiveDot() else Box(Modifier.size(8.dp).clip(CircleShape).background(ZdColors.Text3))
                Text(
                    text = if (running) runningLabel else idleLabel,
                    style = ZdType.Label,
                    color = if (running) ZdColors.Accent else ZdColors.Text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val detail = if (running) runningDetail else idleDetail
            if (detail != null) {
                Text(detail, style = ZdType.Caption, color = ZdColors.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (running) {
            ZdButton(stopLabel, onClick = onStop, variant = ZdButtonVariant.Danger, icon = ZdIcons.Stop, height = 40.dp)
        } else {
            ZdButton(startLabel, onClick = onStart, variant = ZdButtonVariant.Primary, icon = ZdIcons.Play, height = 40.dp)
        }
    }
}

/**
 * Elapsed milliseconds since [running] last became true, ticking once a second; 0 when stopped.
 * Keeps scan timers in the UI so tools don't each need their own clock.
 */
@Composable
fun rememberRunClock(running: Boolean): Long {
    var startedAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running) {
        if (running) {
            if (startedAt == null) startedAt = System.currentTimeMillis()
            while (true) {
                now = System.currentTimeMillis()
                delay(1_000)
            }
        } else {
            startedAt = null
        }
    }
    return startedAt?.let { (now - it).coerceAtLeast(0L) } ?: 0L
}

/** "auto-stops in 0:18" style countdown text, never negative. */
fun autoStopText(elapsedMs: Long, autoStopMs: Long): String {
    val remaining = ((autoStopMs - elapsedMs).coerceAtLeast(0L) + 999) / 1000
    return "auto-stops in %d:%02d".format(remaining / 60, remaining % 60)
}

/**
 * The standard tool scan bar with a live clock: "Scanning · 00:12" over
 * "Recording · auto-stops in 0:18" while running, and what was kept once stopped.
 */
@Composable
fun ZdToolScanBar(
    running: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    verb: String = "Scanning",
    runningNote: String? = "Recording",
    autoStopMs: Long? = null,
    idleLabel: String = "Stopped",
    idleNote: String? = null,
    startLabel: String = "Start",
    stopLabel: String = "Stop"
) {
    val elapsed = rememberRunClock(running)
    val detail = listOfNotNull(runningNote, autoStopMs?.let { autoStopText(elapsed, it) })
        .joinToString(" · ")
        .ifEmpty { null }
    ZdScanControlBar(
        running = running,
        onStart = onStart,
        onStop = onStop,
        modifier = modifier,
        runningLabel = "$verb · ${formatElapsed(elapsed)}",
        runningDetail = detail,
        idleLabel = idleLabel,
        idleDetail = idleNote,
        startLabel = startLabel,
        stopLabel = stopLabel
    )
}

/** Pulsing accent dot that marks something as live. */
@Composable
fun LiveDot(modifier: Modifier = Modifier, color: Color = ZdColors.Accent) {
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Restart, StartOffset(0)),
        label = "livePulse"
    )
    Box(
        modifier
            .size(8.dp)
            .drawBehind {
                val ring = (1f - pulse).coerceIn(0f, 1f) * 0.55f
                drawCircle(color.copy(alpha = ring), radius = size.minDimension / 2 + 7.dp.toPx() * pulse)
                drawCircle(color)
            }
    )
}

/** mm:ss, or h:mm:ss past an hour; used by every scan timer. */
fun formatElapsed(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) / 1000)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

// ── Full-screen states ───────────────────────────────────────────────────────

/**
 * Honest full-screen states: not on this phone, nothing recorded yet, paused by Android,
 * radio off. Says what's wrong, offers the fix, and points at what still works.
 */
@Composable
fun ZdStatePanel(
    kicker: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    kickerColor: Color = ZdColors.Text3,
    iconTint: Color = ZdColors.Text3,
    iconBackground: Color = ZdColors.Surface2,
    note: String? = null,
    noteIcon: ImageVector = ZdIcons.Info,
    chipsLabel: String? = null,
    chips: List<String> = emptyList(),
    primaryAction: Pair<String, () -> Unit>? = null,
    primaryIcon: ImageVector? = null,
    secondaryAction: Pair<String, () -> Unit>? = null,
    linkAction: Pair<String, () -> Unit>? = null,
    /** False when placed inside a list: no fill, no own scrolling. */
    fullScreen: Boolean = true,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .then(if (fullScreen) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth())
            .padding(horizontal = if (fullScreen) 28.dp else 12.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        if (icon != null) {
            ZdIconTile(icon, tint = iconTint, background = iconBackground, size = 56.dp, iconSize = 28.dp)
        }
        Text(kicker.uppercase(), style = ZdType.Tag.copy(letterSpacing = ZdType.Section.letterSpacing), color = kickerColor)
        Text(title, style = ZdType.Title, color = ZdColors.Text, modifier = Modifier.semantics { heading() })
        Text(body, style = ZdType.BodySmall.copy(fontSize = ZdType.Body.fontSize.times(0.94f)), color = ZdColors.Text2)
        if (note != null || chips.isNotEmpty()) {
            ZdCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                if (note != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(noteIcon, contentDescription = null, tint = ZdColors.Info, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                        Text(note, style = ZdType.BodySmall, color = ZdColors.Text2)
                    }
                }
                if (chips.isNotEmpty()) {
                    if (chipsLabel != null) {
                        Text(chipsLabel.uppercase(), style = ZdType.Tag.copy(fontSize = ZdType.Tag.fontSize.times(0.91f), letterSpacing = ZdType.Section.letterSpacing.times(0.7f)), color = ZdColors.Text3)
                    }
                    ZdTagFlow(chips)
                }
            }
        }
        extra?.invoke(this)
        if (primaryAction != null || secondaryAction != null || linkAction != null) {
            Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                primaryAction?.let { (label, action) ->
                    ZdButton(label, onClick = action, icon = primaryIcon, modifier = Modifier.fillMaxWidth())
                }
                secondaryAction?.let { (label, action) ->
                    ZdButton(label, onClick = action, variant = ZdButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
                }
                linkAction?.let { (label, action) ->
                    ZdButton(label, onClick = action, variant = ZdButtonVariant.Ghost, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZdTagFlow(tags: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { ZdTag(it) }
    }
}

// ── Screen body ──────────────────────────────────────────────────────────────

/** Scrollable screen body with the standard 16 dp gutters and section rhythm. */
@Composable
fun ZdScreenColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

/** Short explanatory footnote in the caption style, used under lists. */
@Composable
fun ZdFootnote(text: String, modifier: Modifier = Modifier, icon: ImageVector? = ZdIcons.Info) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = ZdColors.Text3, modifier = Modifier.size(14.dp).padding(top = 2.dp))
        Text(text, style = ZdType.Caption.copy(lineHeight = ZdType.BodySmall.lineHeight), color = ZdColors.Text3)
    }
}

// ── Dialog ───────────────────────────────────────────────────────────────────

/** Styled dialog: mono title, body slot, primary confirm and a quiet dismiss. */
@Composable
fun ZdDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissLabel: String = "Cancel",
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZdColors.Surface,
        shape = RoundedCornerShape(22.dp),
        title = { Text(title, style = ZdType.Title, color = ZdColors.Text) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) },
        confirmButton = { ZdButton(confirmLabel, onClick = onConfirm, enabled = confirmEnabled) },
        dismissButton = { ZdButton(dismissLabel, onClick = onDismiss, variant = ZdButtonVariant.Ghost) }
    )
}

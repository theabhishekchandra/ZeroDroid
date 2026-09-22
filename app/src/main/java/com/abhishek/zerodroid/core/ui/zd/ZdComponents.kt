package com.abhishek.zerodroid.core.ui.zd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

val ZdCardShape = RoundedCornerShape(14.dp)
val ZdButtonShape = RoundedCornerShape(10.dp)
val ZdTagShape = RoundedCornerShape(6.dp)

// ── Surfaces ─────────────────────────────────────────────────────────────────

/** The standard card: surface fill, hairline border, 14 dp corners. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZdCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    borderColor: Color = ZdColors.Border,
    background: Color = ZdColors.Surface,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    verticalSpacing: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickable = when {
        onClick != null || onLongClick != null -> Modifier.combinedClickable(
            onClick = onClick ?: {},
            onLongClick = onLongClick
        )
        else -> Modifier
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ZdCardShape)
            .background(background)
            .border(1.dp, borderColor, ZdCardShape)
            .then(clickable)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content
    )
}

/** A card holding rows separated by hairlines, as used for every list in the redesign. */
@Composable
fun <T> ZdListCard(
    items: List<T>,
    modifier: Modifier = Modifier,
    row: @Composable (T) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ZdCardShape)
            .background(ZdColors.Surface)
            .border(1.dp, ZdColors.Border, ZdCardShape)
    ) {
        items.forEachIndexed { index, item ->
            row(item)
            if (index < items.lastIndex) ZdDivider()
        }
    }
}

@Composable
fun ZdDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ZdColors.Border)
    )
}

/**
 * A list row: optional leading slot, title + subtitle, optional trailing slot and chevron.
 * Minimum height is 60 dp so every row is a comfortable touch target.
 */
@Composable
fun ZdListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleMono: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = if (titleMono) ZdType.Label else ZdType.BodySmall.copy(fontSize = ZdType.Body.fontSize),
                color = ZdColors.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(text = subtitle, style = ZdType.Caption, color = ZdColors.Text3)
            }
        }
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = trailing
            )
        }
        if (showChevron) {
            Icon(ZdIcons.Chevron, contentDescription = null, tint = ZdColors.Text3, modifier = Modifier.size(16.dp))
        }
    }
}

/** Rounded square holding a tool or device icon. */
@Composable
fun ZdIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = ZdColors.Accent,
    background: Color = ZdColors.AccentBg,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

// ── Text ─────────────────────────────────────────────────────────────────────

/** `// SECTION` label with an optional link-style action on the right. */
@Composable
fun ZdSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = ZdColors.Accent)) { append("//") }
                append(" ")
                append(text.uppercase())
            },
            style = ZdType.Section,
            color = ZdColors.Text3,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (trailingText != null) {
            Text(trailingText, style = ZdType.Path, color = ZdColors.Text3)
        }
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(actionLabel, style = ZdType.Mono, color = ZdColors.Accent)
            }
        }
    }
}

// ── Buttons ──────────────────────────────────────────────────────────────────

enum class ZdButtonVariant { Primary, Secondary, Danger, Ghost }

/** 48 dp action button. Primary is the single most important action on a screen. */
@Composable
fun ZdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ZdButtonVariant = ZdButtonVariant.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp
) {
    val (bg, fg, border) = when (variant) {
        ZdButtonVariant.Primary -> Triple(ZdColors.Accent, ZdColors.OnAccent, ZdColors.Accent)
        ZdButtonVariant.Secondary -> Triple(ZdColors.Surface2, ZdColors.Text, ZdColors.BorderStrong)
        ZdButtonVariant.Danger -> Triple(ZdColors.CriticalBg, ZdColors.Critical, ZdColors.CriticalBorder)
        ZdButtonVariant.Ghost -> Triple(Color.Transparent, ZdColors.Accent, Color.Transparent)
    }
    Row(
        modifier = modifier
            .heightIn(min = height)
            .clip(ZdButtonShape)
            .background(if (enabled) bg else ZdColors.Surface2)
            .border(1.dp, if (enabled) border else ZdColors.Border, ZdButtonShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        val tint = if (enabled) fg else ZdColors.Text3
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Text(text, style = ZdType.Button, color = tint, maxLines = 1)
    }
}

/** 44 dp icon-only button used in headers. */
@Composable
fun ZdIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ZdColors.Text2,
    badge: Int = 0
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        if (badge > 0) ZdCountBadge(badge, Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 6.dp))
    }
}

@Composable
fun ZdCountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ZdColors.Critical)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = ZdType.Tag.copy(fontSize = ZdType.Path.fontSize.times(0.91f), letterSpacing = ZdType.Path.letterSpacing),
            color = ZdColors.OnCritical
        )
    }
}

// ── Tags, chips, badges ──────────────────────────────────────────────────────

/** Small uppercase-ish pill: WPA3, PAIRED, NEW, ON. */
@Composable
fun ZdTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ZdColors.Text2,
    background: Color = ZdColors.Surface2,
    border: Color = ZdColors.Border
) {
    Box(
        modifier = modifier
            .height(22.dp)
            .clip(ZdTagShape)
            .background(background)
            .border(1.dp, border, ZdTagShape)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = ZdType.Path.copy(fontSize = ZdType.Tag.fontSize, letterSpacing = ZdType.Path.letterSpacing), color = color, maxLines = 1)
    }
}

/** Hardware requirement tag on a tool row: BLE, WIFI, OTG. */
@Composable
fun ZdHardwareTag(text: String, available: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .border(1.dp, ZdColors.BorderStrong, RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (available) text else "NO $text",
            style = ZdType.Tag.copy(fontSize = ZdType.Tag.fontSize.times(0.91f)),
            color = if (available) ZdColors.Text2 else ZdColors.Critical
        )
    }
}

enum class ZdSeverity(val label: String, val color: Color, val background: Color) {
    CRITICAL("CRITICAL", ZdColors.Critical, ZdColors.CriticalBg),
    HIGH("HIGH", ZdColors.High, ZdColors.HighBg),
    MEDIUM("MEDIUM", ZdColors.Medium, ZdColors.MediumBg),
    LOW("LOW", ZdColors.Info, ZdColors.InfoBg),
    CLEAN("CLEAN", ZdColors.Accent, ZdColors.AccentBg);

    val icon: ImageVector
        get() = when (this) {
            CRITICAL, HIGH, MEDIUM -> ZdIcons.Warning
            LOW -> ZdIcons.Info
            CLEAN -> ZdIcons.Check
        }
}

/** Severity is always an icon plus a word, never colour alone. */
@Composable
fun ZdSeverityBadge(severity: ZdSeverity, modifier: Modifier = Modifier, label: String = severity.label) {
    Row(
        modifier = modifier
            .height(22.dp)
            .clip(ZdTagShape)
            .background(severity.background)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(severity.icon, contentDescription = null, tint = severity.color, modifier = Modifier.size(12.dp))
        Text(label, style = ZdType.Tag, color = severity.color)
    }
}

/** Filter chip (36 dp pill). */
@Composable
fun ZdChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) ZdColors.Accent else ZdColors.Surface)
            .border(1.dp, if (selected) ZdColors.Accent else ZdColors.BorderStrong, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = ZdType.Mono.copy(fontWeight = ZdType.Label.fontWeight), color = if (selected) ZdColors.OnAccent else ZdColors.Text2, maxLines = 1)
    }
}

/** Horizontally scrolling row of chips, bleeding to the screen edges. */
@Composable
fun ZdChipRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

/** Underlined tab strip used inside a tool (Networks · Channels · Security). */
@Composable
fun ZdTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            style = ZdType.Mono.copy(fontWeight = ZdType.Label.fontWeight),
                            color = if (selected) ZdColors.Accent else ZdColors.Text3,
                            maxLines = 1
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (selected) ZdColors.Accent else Color.Transparent)
                    )
                }
            }
        }
        ZdDivider()
    }
}

/** Switch row: label, optional trailing note, custom 36×22 switch. */
@Composable
fun ZdSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    trailingText: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ZdSwitchVisual(checked)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = ZdType.BodySmall, color = ZdColors.Text2)
            if (description != null) Text(description, style = ZdType.Caption, color = ZdColors.Text3)
        }
        if (trailingText != null) Text(trailingText, style = ZdType.Mono, color = ZdColors.Text3)
    }
}

@Composable
fun ZdSwitchVisual(checked: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(36.dp)
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (checked) ZdColors.Accent else ZdColors.BorderStrong)
    ) {
        Box(
            Modifier
                .padding(start = if (checked) 17.dp else 3.dp, top = 3.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(ZdColors.Bg)
        )
    }
}

// ── Checks ───────────────────────────────────────────────────────────────────

enum class ZdCheckStatus(val label: String, val color: Color) {
    PASS("PASS", ZdColors.Accent),
    WARN("WARN", ZdColors.Medium),
    FAIL("FAIL", ZdColors.Critical),
    NA("N/A", ZdColors.Text3);

    val icon: ImageVector
        get() = when (this) {
            PASS -> ZdIcons.Check
            WARN, FAIL -> ZdIcons.Warning
            NA -> ZdIcons.Info
        }
}

/** A check line: status icon, what was checked, the result, and a PASS/WARN/FAIL word. */
@Composable
fun ZdCheckRow(
    title: String,
    status: ZdCheckStatus,
    modifier: Modifier = Modifier,
    detail: String? = null,
    icon: ImageVector = status.icon
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = status.color, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = ZdType.BodySmall.copy(fontSize = ZdType.Body.fontSize.times(0.94f)), color = ZdColors.Text)
            if (detail != null) Text(detail, style = ZdType.Caption, color = ZdColors.Text3)
        }
        Text(status.label, style = ZdType.Tag.copy(letterSpacing = ZdType.Path.letterSpacing.times(1.6f)), color = status.color)
    }
}

// ── Signal ───────────────────────────────────────────────────────────────────

/** Number of lit bars (0–4) for an RSSI in dBm. */
fun signalBarsFor(dbm: Int): Int = when {
    dbm >= -55 -> 4
    dbm >= -67 -> 3
    dbm >= -78 -> 2
    dbm >= -90 -> 1
    else -> 0
}

@Composable
fun ZdSignalBars(bars: Int, modifier: Modifier = Modifier) {
    val lit = if (bars >= 3) ZdColors.Accent else ZdColors.Medium
    Row(
        modifier = modifier
            .height(14.dp)
            .clearAndSetSemantics { contentDescription = "Signal $bars of 4" },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(5, 8, 11, 14).forEachIndexed { i, h ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i < bars) lit else ZdColors.BorderStrong)
            )
        }
    }
}

/** Bars over the dBm value; the leading column of a network or device row. */
@Composable
fun ZdSignal(dbm: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ZdSignalBars(signalBarsFor(dbm))
        Text(dbm.toString(), style = ZdType.Path.copy(fontSize = ZdType.Tag.fontSize), color = ZdColors.Text3)
    }
}

// ── Stats ────────────────────────────────────────────────────────────────────

/** Label-over-value cell, e.g. BSSID / A4:2B:…:7F. */
@Composable
fun ZdStat(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = ZdColors.Text) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label.uppercase(), style = ZdType.Path.copy(letterSpacing = ZdType.Section.letterSpacing.times(0.7f)), color = ZdColors.Text3)
        Text(value, style = ZdType.Label, color = valueColor)
    }
}

/** Big number over a small caption, e.g. 412 / NETWORKS. */
@Composable
fun ZdMetric(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = ZdColors.Text) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = ZdType.Heading.copy(fontSize = ZdType.Title.fontSize), color = valueColor)
        Text(label.uppercase(), style = ZdType.Path, color = ZdColors.Text3)
    }
}

@Composable
fun ZdSpacer(height: Dp) = Spacer(Modifier.height(height))

// ── Feedback ─────────────────────────────────────────────────────────────────

/** Toast-style confirmation: accent dot plus a short mono message. */
@Composable
fun ZdSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shape)
            .background(ZdColors.Surface3)
            .border(1.dp, ZdColors.BorderStrong, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(ZdColors.Accent))
        Text(data.visuals.message, style = ZdType.Label.copy(fontWeight = FontWeight.Medium), color = ZdColors.Text, modifier = Modifier.weight(1f))
        data.visuals.actionLabel?.let { label ->
            Text(
                label,
                style = ZdType.Label,
                color = ZdColors.Accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button) { data.performAction() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

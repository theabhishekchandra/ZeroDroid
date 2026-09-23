package com.abhishek.zerodroid.features.ir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.features.ir.domain.IrRemoteButton
import com.abhishek.zerodroid.features.ir.domain.IrRemoteDatabase
import com.abhishek.zerodroid.features.ir.domain.IrRemoteProfile
import com.abhishek.zerodroid.features.ir.domain.TransmitResult
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

/** A TV remote for the built-in brand profiles: pick a brand, then press keys like a real remote. */
@Composable
fun IrRemoteGridPanel(
    selectedProfile: IrRemoteProfile?,
    lastTransmitResult: TransmitResult?,
    onProfileSelected: (IrRemoteProfile) -> Unit,
    onButtonPress: (IrRemoteButton) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastSent by remember(selectedProfile) { mutableStateOf<IrRemoteButton?>(null) }
    val press: (IrRemoteButton) -> Unit = { lastSent = it; onButtonPress(it) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ZdChipRow(contentPadding = 0.dp) {
            IrRemoteDatabase.profiles.forEach { profile ->
                ZdChip(
                    "${profile.brand} ${profile.deviceType}",
                    selected = profile == selectedProfile,
                    onClick = { onProfileSelected(profile) }
                )
            }
        }

        if (selectedProfile == null) {
            ZdCard {
                Text("Pick your TV’s brand", style = ZdType.Label, color = ZdColors.Text)
                Text("Each brand uses its own codes; the remote appears once one is chosen.", style = ZdType.BodySmall, color = ZdColors.Text2)
            }
            return@Column
        }

        ZdCard(verticalSpacing = 18.dp) {
            val keys = selectedProfile.buttons.associateBy { it.icon }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${selectedProfile.brand} ${selectedProfile.deviceType}", style = ZdType.Label, color = ZdColors.Text)
                    Text(
                        selectedProfile.buttons.firstOrNull()?.protocol?.displayName.orEmpty(),
                        style = ZdType.Path,
                        color = ZdColors.Text3
                    )
                }
                keys["power"]?.let {
                    RoundKey(it, press, size = 52.dp, tint = ZdColors.Critical, background = ZdColors.CriticalBg, icon = ZdIcons.Power)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("input" to "Source", "menu" to "Menu", "back" to "Back", "exit" to "Exit").forEach { (id, label) ->
                    keys[id]?.let { PillKey(it, label, press, Modifier.weight(1f)) }
                }
            }

            DPad(keys, press)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RockerKey("Vol", keys["volume_up"], keys["volume_down"], press, Modifier.weight(1f))
                keys["mute"]?.let {
                    Box(Modifier.align(Alignment.CenterVertically)) {
                        RoundKey(it, press, size = 52.dp, icon = ZdIcons.Volume, tint = ZdColors.Medium, background = ZdColors.MediumBg)
                    }
                }
                RockerKey("Ch", keys["channel_up"], keys["channel_down"], press, Modifier.weight(1f))
            }
        }

        LastSent(lastSent, lastTransmitResult)
    }
}

@Composable
private fun LastSent(button: IrRemoteButton?, result: TransmitResult?) {
    if (button == null || result == null) return
    val ok = result is TransmitResult.Success
    ZdCard(
        background = if (ok) ZdColors.Surface else ZdColors.CriticalBg,
        borderColor = if (ok) ZdColors.Border else ZdColors.Critical.copy(alpha = 0.4f),
        verticalSpacing = 4.dp
    ) {
        Text(if (ok) "Last sent · ${button.label}" else "Couldn’t send ${button.label}", style = ZdType.Label, color = if (ok) ZdColors.Text else ZdColors.Critical)
        Text(
            if (result is TransmitResult.Error) result.message
            else "${button.protocol.displayName} · 0x${button.code.toString(16).uppercase()} · ${button.frequency / 1000} kHz",
            style = ZdType.Mono,
            color = ZdColors.Text3
        )
    }
}

/** Up / left / OK / right / down, laid out as a cross. */
@Composable
private fun DPad(keys: Map<String, IrRemoteButton>, press: (IrRemoteButton) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys["nav_up"]?.let { RoundKey(it, press, icon = ZdIcons.ChevronDown, iconRotation = 180f) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            keys["nav_left"]?.let { RoundKey(it, press, icon = ZdIcons.Back) }
            keys["nav_ok"]?.let { RoundKey(it, press, size = 64.dp, label = "OK", tint = ZdColors.Bg, background = ZdColors.Accent) }
            keys["nav_right"]?.let { RoundKey(it, press, icon = ZdIcons.Chevron) }
        }
        keys["nav_down"]?.let { RoundKey(it, press, icon = ZdIcons.ChevronDown) }
    }
}

/** A tall two-ended key like Vol +/− on a real remote. */
@Composable
private fun RockerKey(label: String, up: IrRemoteButton?, down: IrRemoteButton?, press: (IrRemoteButton) -> Unit, modifier: Modifier) {
    if (up == null && down == null) return
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(ZdColors.Surface2)
            .border(1.dp, ZdColors.BorderStrong, shape),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        up?.let { KeyArea(it, "+", press, "$label up") }
        Text(label.uppercase(), style = ZdType.Path, color = ZdColors.Text3)
        down?.let { KeyArea(it, "−", press, "$label down") }
    }
}

@Composable
private fun KeyArea(button: IrRemoteButton, glyph: String, press: (IrRemoteButton) -> Unit, description: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { press(button) }
            .semantics { contentDescription = description; role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, style = ZdType.Heading, color = ZdColors.Text)
    }
}

@Composable
private fun PillKey(button: IrRemoteButton, label: String, press: (IrRemoteButton) -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .height(40.dp)
            .clip(shape)
            .background(ZdColors.Surface2)
            .border(1.dp, ZdColors.BorderStrong, shape)
            .clickable { press(button) }
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = ZdType.Label, color = ZdColors.Text2)
    }
}

@Composable
private fun RoundKey(
    button: IrRemoteButton,
    press: (IrRemoteButton) -> Unit,
    size: Dp = 48.dp,
    icon: ImageVector? = null,
    iconRotation: Float = 0f,
    label: String? = null,
    tint: Color = ZdColors.Text,
    background: Color = ZdColors.Surface2,
    shape: Shape = CircleShape
) {
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(background)
            .border(1.dp, if (background == ZdColors.Surface2) ZdColors.BorderStrong else Color.Transparent, shape)
            .clickable { press(button) }
            .semantics { contentDescription = button.label; role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        when {
            icon != null -> Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp).rotate(iconRotation))
            label != null -> Text(label, style = ZdType.Label, color = tint)
        }
    }
}


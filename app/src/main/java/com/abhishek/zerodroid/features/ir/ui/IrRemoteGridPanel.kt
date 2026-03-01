package com.abhishek.zerodroid.features.ir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ir.domain.IrRemoteButton
import com.abhishek.zerodroid.features.ir.domain.IrRemoteDatabase
import com.abhishek.zerodroid.features.ir.domain.IrRemoteProfile
import com.abhishek.zerodroid.features.ir.domain.TransmitResult
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrRemoteGridPanel(
    selectedProfile: IrRemoteProfile?,
    lastTransmitResult: TransmitResult?,
    onProfileSelected: (IrRemoteProfile) -> Unit,
    onButtonPress: (IrRemoteButton) -> Unit,
    modifier: Modifier = Modifier
) {
    var brandExpanded by remember { mutableStateOf(false) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )

    TerminalCard(modifier = modifier) {
        Text(text = "> Remote Control", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = brandExpanded, onExpandedChange = { brandExpanded = it }) {
            OutlinedTextField(
                value = if (selectedProfile != null) "${selectedProfile.brand} ${selectedProfile.deviceType}" else "Select Device",
                onValueChange = {}, readOnly = true, label = { Text("Device") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = brandExpanded, onDismissRequest = { brandExpanded = false }) {
                IrRemoteDatabase.profiles.forEach { profile ->
                    DropdownMenuItem(
                        text = { Text("${profile.brand} ${profile.deviceType}") },
                        onClick = { onProfileSelected(profile); brandExpanded = false }
                    )
                }
            }
        }

        if (selectedProfile != null) {
            Spacer(modifier = Modifier.height(16.dp))
            RemoteControlLayout(profile = selectedProfile, onButtonPress = onButtonPress)
            lastTransmitResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                val (text, color) = when (result) {
                    is TransmitResult.Success -> "Signal transmitted" to TerminalGreen
                    is TransmitResult.Error -> result.message to TerminalRed
                }
                Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}

@Composable
private fun RemoteControlLayout(profile: IrRemoteProfile, onButtonPress: (IrRemoteButton) -> Unit) {
    val buttonMap = profile.buttons.associateBy { it.icon }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Top row: Power, Input, Menu
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            buttonMap["power"]?.let { RemoteButton(Icons.Default.Power, it.label, TerminalRed) { onButtonPress(it) } }
            buttonMap["input"]?.let { RemoteButton(Icons.Default.Input, it.label, TerminalCyan) { onButtonPress(it) } }
            buttonMap["menu"]?.let { RemoteButton(Icons.Default.Menu, it.label, TerminalCyan) { onButtonPress(it) } }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Middle: Vol | D-Pad | Channel
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            // Volume
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("VOL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                buttonMap["volume_up"]?.let { RemoteButton(Icons.AutoMirrored.Filled.VolumeUp, "+", TerminalGreen, true) { onButtonPress(it) } }
                buttonMap["mute"]?.let { RemoteButton(Icons.Default.VolumeOff, "Mute", TerminalAmber, true) { onButtonPress(it) } }
                buttonMap["volume_down"]?.let { RemoteButton(Icons.AutoMirrored.Filled.VolumeDown, "-", TerminalGreen, true) { onButtonPress(it) } }
            }
            // D-Pad
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                buttonMap["nav_up"]?.let { RemoteButton(Icons.Default.ArrowUpward, "", TerminalCyan, true) { onButtonPress(it) } }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    buttonMap["nav_left"]?.let { RemoteButton(Icons.Default.ArrowBack, "", TerminalCyan, true) { onButtonPress(it) } }
                    buttonMap["nav_ok"]?.let { RemoteButton(Icons.Default.CheckCircle, "OK", TerminalGreen) { onButtonPress(it) } }
                    buttonMap["nav_right"]?.let { RemoteButton(Icons.Default.ArrowForward, "", TerminalCyan, true) { onButtonPress(it) } }
                }
                buttonMap["nav_down"]?.let { RemoteButton(Icons.Default.ArrowDownward, "", TerminalCyan, true) { onButtonPress(it) } }
            }
            // Channel
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                buttonMap["channel_up"]?.let { RemoteButton(Icons.Default.KeyboardArrowUp, "+", TerminalAmber, true) { onButtonPress(it) } }
                Spacer(modifier = Modifier.height(40.dp))
                buttonMap["channel_down"]?.let { RemoteButton(Icons.Default.KeyboardArrowDown, "-", TerminalAmber, true) { onButtonPress(it) } }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Bottom: Back, Exit
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            buttonMap["back"]?.let { RemoteButton(Icons.Default.Undo, it.label, TerminalCyan) { onButtonPress(it) } }
            buttonMap["exit"]?.let { RemoteButton(Icons.AutoMirrored.Filled.ExitToApp, it.label, TerminalAmber) { onButtonPress(it) } }
        }
    }
}

@Composable
private fun RemoteButton(icon: ImageVector, label: String, color: Color, isSmall: Boolean = false, onClick: () -> Unit) {
    val buttonSize = if (isSmall) 40.dp else 52.dp
    val iconSize = if (isSmall) 18.dp else 22.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(2.dp)) {
        Box(
            modifier = Modifier.size(buttonSize).clip(CircleShape)
                .background(color = color.copy(alpha = 0.1f), shape = CircleShape)
                .border(width = 1.dp, color = color.copy(alpha = 0.5f), shape = CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(iconSize)) }
        if (label.isNotEmpty() && !isSmall) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
        }
    }
}

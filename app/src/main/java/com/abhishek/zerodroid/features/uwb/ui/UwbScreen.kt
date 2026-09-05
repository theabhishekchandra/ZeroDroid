package com.abhishek.zerodroid.features.uwb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.StatusIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.uwb.domain.UwbRole
import com.abhishek.zerodroid.features.uwb.domain.UwbState
import com.abhishek.zerodroid.features.uwb.viewmodel.UwbViewModel
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun UwbScreen(
    viewModel: UwbViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.uwbPermissions(),
        rationale = "UWB ranging permission is needed to range with nearby UWB devices."
    ) {
        UwbContent(viewModel = viewModel)
    }
}

@Composable
private fun UwbContent(viewModel: UwbViewModel) {
    val state by viewModel.state.collectAsState()

    // A ranging session needs fresh keys shared with the peer, so it is not auto-resumed.
    HardwareLifecycleEffect(
        isActive = state.isRanging,
        onPause = viewModel::stopRanging,
        resumeOnForeground = false
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            StatusIndicator(isAvailable = state.isHardwareAvailable)
        }

        if (!state.isHardwareAvailable) {
            item {
                TerminalCard {
                    Text(
                        text = "> UWB hardware not detected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ultra-Wideband requires specific hardware support (e.g., Google Pixel 6 Pro+, Samsung Galaxy S21+)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        state.deviceInfo?.let { info ->
            item { UwbCapabilitiesCard(info = info) }
        }

        if (state.isHardwareAvailable) {
            item {
                TerminalCard {
                    Text(
                        text = "> Ranging Radar",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    UwbRangingView(measurement = state.measurement)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Ranging needs a peer UWB device also running this screen. " +
                            "There is no auto-discovery - pick a role below, then copy the " +
                            "session values between the two devices by hand.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.isRanging) {
                        RangingActiveContent(state = state, onStop = viewModel::stopRanging)
                    } else {
                        RoleSetupContent(viewModel = viewModel)
                    }

                    state.statusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "> $msg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    state.error?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "! $err",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalRed
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun RoleSetupContent(viewModel: UwbViewModel) {
    val state by viewModel.state.collectAsState()

    Text(
        text = "Controller (has a fixed session, shares it) or Controlee (enters the controller's session)?",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.peerAddressInput,
        onValueChange = viewModel::updatePeerAddressInput,
        label = { Text("Peer UWB address (hex)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = viewModel::startAsController,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Start as Controller")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Controlee: fill these in from the controller's displayed session",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.sessionIdInput,
        onValueChange = viewModel::updateSessionIdInput,
        label = { Text("Session ID") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = state.sessionKeyInput,
        onValueChange = viewModel::updateSessionKeyInput,
        label = { Text("Session key (16 hex chars)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.channelInput,
            onValueChange = viewModel::updateChannelInput,
            label = { Text("Channel") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = state.preambleInput,
            onValueChange = viewModel::updatePreambleInput,
            label = { Text("Preamble idx") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = viewModel::startAsControlee) {
        Text("Start as Controlee")
    }
}

@Composable
private fun RangingActiveContent(
    state: UwbState,
    onStop: () -> Unit
) {
    Text(
        text = "Role: ${if (state.role == UwbRole.CONTROLLER) "Controller" else "Controlee"}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    state.localSession?.let { session ->
        Spacer(modifier = Modifier.height(4.dp))
        if (state.role == UwbRole.CONTROLLER) {
            Text(
                text = "Share these with the peer's Controlee inputs:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("Your address: ${session.localAddressHex}", style = MaterialTheme.typography.labelSmall)
        if (state.role == UwbRole.CONTROLLER) {
            Text("Session ID: ${session.sessionId}", style = MaterialTheme.typography.labelSmall)
            Text("Session key: ${session.sessionKeyHex}", style = MaterialTheme.typography.labelSmall)
            Text(
                "Channel: ${session.channel}  Preamble: ${session.preambleIndex}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

    state.measurement?.let { m ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Distance: ${m.distanceMeters?.let { "%.2f m".format(it) } ?: "--"}" +
                (m.azimuthDegrees?.let { "  Azimuth: %.0f°".format(it) } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onStop) {
        Text("Stop Ranging")
    }
}

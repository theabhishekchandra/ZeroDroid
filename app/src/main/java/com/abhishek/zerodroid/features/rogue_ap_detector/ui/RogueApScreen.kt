package com.abhishek.zerodroid.features.rogue_ap_detector.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.ApThreatType
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RiskLevel
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RogueApAlert
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RogueApState
import com.abhishek.zerodroid.features.rogue_ap_detector.viewmodel.RogueApViewModel
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.ui.theme.SeverityCritical
import com.abhishek.zerodroid.ui.theme.SeverityHigh
import com.abhishek.zerodroid.ui.theme.SeverityLow
import com.abhishek.zerodroid.ui.theme.SeverityMedium
import com.abhishek.zerodroid.ui.theme.SurfaceVariantDark
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TerminalRedGlow
import com.abhishek.zerodroid.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RogueApScreen(
    viewModel: RogueApViewModel = viewModel(factory = RogueApViewModel.Factory)
) {
    PermissionGate(
        permissions = PermissionUtils.wifiPermissions(),
        rationale = "Location permission is required by Android to scan WiFi networks and detect rogue access points."
    ) {
        RogueApContent(viewModel)
    }
}

@Composable
private fun RogueApContent(viewModel: RogueApViewModel) {
    DisposableEffect(Unit) {
        onDispose { viewModel.stopScan() }
    }

    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. Scan Controls
        item { ScanControls(state, viewModel) }

        // 2. Threat Summary
        if (state.alerts.isNotEmpty() || state.isScanning) {
            item { ThreatSummaryCard(state) }
        }

        // 3. Known/Trusted SSIDs
        item { KnownSsidsSection(state, viewModel) }

        // 4. Empty State
        if (state.alerts.isEmpty() && !state.isScanning) {
            item {
                EmptyState(
                    icon = Icons.Default.Shield,
                    title = "No threats detected",
                    subtitle = "Tap SCAN to analyze nearby WiFi networks for evil twins, rogue APs, and spoofed SSIDs"
                )
            }
        }

        // 5. Alert Cards
        items(state.alerts, key = { it.id }) { alert ->
            AlertCard(alert)
        }

        // 6. Safe Networks Section
        if (state.safeAps > 0 && state.totalAps > 0) {
            item { SafeNetworksSection(state) }
        }

        // 7. Error Display
        state.error?.let { error ->
            item {
                TerminalCard(glowColor = TerminalRed, borderColor = TerminalRed) {
                    Text(
                        text = "> ERROR: $error",
                        color = TerminalRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// -- Scan Controls --

@Composable
private fun ScanControls(state: RogueApState, viewModel: RogueApViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { if (state.isScanning) viewModel.stopScan() else viewModel.startScan() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isScanning) TerminalRed else TerminalGreen
            ),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (state.isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.isScanning) "STOP" else "SCAN",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (state.totalAps > 0) {
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${state.totalAps} APs",
                    color = TerminalGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (state.isScanning) {
                    ScanningIndicator(
                        isScanning = true,
                        label = "live",
                        color = TerminalGreen
                    )
                }
            }
        }

        if (state.alerts.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { viewModel.clearAlerts() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear alerts",
                    tint = TextSecondary
                )
            }
        }
    }
}

// -- Threat Summary Card --

@Composable
private fun ThreatSummaryCard(state: RogueApState) {
    val criticalCount = state.alerts.count { it.riskLevel == RiskLevel.CRITICAL }
    val highCount = state.alerts.count { it.riskLevel == RiskLevel.HIGH }
    val mediumCount = state.alerts.count { it.riskLevel == RiskLevel.MEDIUM }
    val lowCount = state.alerts.count { it.riskLevel == RiskLevel.LOW }
    val total = state.alerts.size

    val summaryColor = when {
        criticalCount > 0 -> SeverityCritical
        highCount > 0 -> SeverityHigh
        mediumCount > 0 -> SeverityMedium
        total > 0 -> SeverityLow
        else -> TerminalGreen
    }

    val animatedColor by animateColorAsState(targetValue = summaryColor, label = "summaryColor")

    TerminalCard(
        glowColor = animatedColor,
        borderColor = animatedColor,
        animated = criticalCount > 0
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (total == 0) "> Scanning..."
                    else "> $total Alert${if (total != 1) "s" else ""} Detected",
                    color = animatedColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (state.isScanning) {
                    ScanningIndicator(isScanning = true, label = "", color = animatedColor)
                }
            }

            if (total > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (criticalCount > 0) SeverityBadge("CRIT: $criticalCount", SeverityCritical)
                    if (highCount > 0) SeverityBadge("HIGH: $highCount", SeverityHigh)
                    if (mediumCount > 0) SeverityBadge("MED: $mediumCount", SeverityMedium)
                    if (lowCount > 0) SeverityBadge("LOW: $lowCount", SeverityLow)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.safeAps} safe / ${state.suspiciousAps} suspicious / ${state.totalAps} total",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SeverityBadge(label: String, color: Color) {
    Text(
        text = "[$label]",
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

// -- Alert Card --

@Composable
private fun AlertCard(alert: RogueApAlert) {
    val color = when (alert.riskLevel) {
        RiskLevel.CRITICAL -> SeverityCritical
        RiskLevel.HIGH -> SeverityHigh
        RiskLevel.MEDIUM -> SeverityMedium
        RiskLevel.LOW -> SeverityLow
        RiskLevel.SAFE -> TerminalGreen
    }

    val isCritical = alert.riskLevel == RiskLevel.CRITICAL

    TerminalCard(
        glowColor = color,
        borderColor = color,
        animated = isCritical
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Threat type badge + Risk level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Threat type badge
                    Text(
                        text = "[${alert.threatType.label}]",
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    // Risk level
                    Text(
                        text = alert.riskLevel.label,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Timestamp
                Text(
                    text = formatTime(alert.timestamp),
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = "> ${alert.title}",
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Suspicious AP details
            ApDetailRow(label = "SSID", value = alert.suspiciousAp.ssid, color = color)
            ApDetailRow(label = "BSSID", value = alert.suspiciousAp.bssid, color = TextSecondary)
            ApDetailRow(
                label = "Signal",
                value = "${alert.suspiciousAp.rssi}dBm (${alert.suspiciousAp.signalPercent}%)",
                color = TextSecondary
            )
            ApDetailRow(
                label = "Ch/Band",
                value = "${alert.suspiciousAp.channel} / ${alert.suspiciousAp.band.label}",
                color = TextSecondary
            )
            ApDetailRow(
                label = "Security",
                value = alert.suspiciousAp.security.label,
                color = if (alert.suspiciousAp.security.label == "Open") SeverityHigh else TextSecondary
            )

            // Evil twin comparison
            alert.legitimateAp?.let { legit ->
                Spacer(modifier = Modifier.height(8.dp))
                EvilTwinComparison(suspect = alert.suspiciousAp, legitimate = legit, color = color)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = alert.description,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )

            // Signal bar
            Spacer(modifier = Modifier.height(4.dp))
            SignalBar(rssi = alert.suspiciousAp.rssi, color = color)
        }
    }
}

@Composable
private fun ApDetailRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            color = TextSecondary.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.width(64.dp)
        )
        Text(
            text = value,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EvilTwinComparison(
    suspect: WifiAccessPoint,
    legitimate: WifiAccessPoint,
    color: Color
) {
    TerminalCard(
        borderColor = color.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "> Side-by-Side Comparison",
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Suspicious column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "[SUSPECT]",
                        color = SeverityCritical,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    ComparisonField("BSSID", suspect.bssid.takeLast(8))
                    ComparisonField("Signal", "${suspect.rssi}dBm")
                    ComparisonField("Security", suspect.security.label)
                    ComparisonField("Channel", "${suspect.channel}")
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(70.dp)
                        .background(color.copy(alpha = 0.3f))
                )

                // Legitimate column
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "[LEGIT]",
                        color = TerminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    ComparisonField("BSSID", legitimate.bssid.takeLast(8), Alignment.End)
                    ComparisonField("Signal", "${legitimate.rssi}dBm", Alignment.End)
                    ComparisonField("Security", legitimate.security.label, Alignment.End)
                    ComparisonField("Channel", "${legitimate.channel}", Alignment.End)
                }
            }
        }
    }
}

@Composable
private fun ComparisonField(
    label: String,
    value: String,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = "$label: $value",
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}

// -- Signal Bar --

@Composable
private fun SignalBar(rssi: Int, color: Color) {
    val bars = when {
        rssi >= -50 -> 4
        rssi >= -65 -> 3
        rssi >= -75 -> 2
        else -> 1
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "${rssi}dBm",
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((4 + i * 3).dp)
                    .background(
                        if (i <= bars) color else color.copy(alpha = 0.2f),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

// -- Safe Networks Section --

@Composable
private fun SafeNetworksSection(state: RogueApState) {
    var expanded by remember { mutableStateOf(false) }

    TerminalCard(
        glowColor = TerminalGreen,
        borderColor = TerminalGreen.copy(alpha = 0.4f),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = TerminalGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "> ${state.safeAps} Safe Network${if (state.safeAps != 1) "s" else ""}",
                        color = TerminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TerminalGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Networks that passed all detection checks are considered safe. " +
                            "Add trusted SSIDs below to reduce false positives.",
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// -- Known/Trusted SSIDs --

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KnownSsidsSection(state: RogueApState, viewModel: RogueApViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }

    TerminalCard(borderColor = TerminalCyan.copy(alpha = 0.3f)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> Trusted SSIDs",
                    color = TerminalCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add trusted SSID",
                        tint = TerminalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (state.knownSsids.isEmpty()) {
                Text(
                    text = "No trusted SSIDs. Add your home/work networks to reduce false positives.",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.knownSsids.forEach { ssid ->
                        InputChip(
                            selected = false,
                            onClick = { },
                            label = {
                                Text(
                                    text = ssid,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove $ssid",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.removeKnownSsid(ssid) }
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = TerminalCyan.copy(alpha = 0.1f),
                                labelColor = TerminalCyan,
                                leadingIconColor = TerminalCyan,
                                trailingIconColor = TerminalCyan.copy(alpha = 0.6f)
                            ),
                            border = InputChipDefaults.inputChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = TerminalCyan.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSsidDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { ssid ->
                viewModel.addKnownSsid(ssid)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddSsidDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var ssidInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "> Add Trusted SSID",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Networks matching this SSID will be excluded from weak security alerts.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = ssidInput,
                    onValueChange = { ssidInput = it },
                    label = {
                        Text(
                            text = "SSID",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (ssidInput.isNotBlank()) onAdd(ssidInput)
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TerminalCyan,
                        cursorColor = TerminalCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (ssidInput.isNotBlank()) onAdd(ssidInput) },
                enabled = ssidInput.isNotBlank()
            ) {
                Text(
                    text = "ADD",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (ssidInput.isNotBlank()) TerminalCyan else TextSecondary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            }
        },
        containerColor = SurfaceVariantDark
    )
}

// -- Utilities --

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

package com.abhishek.zerodroid.features.network_scanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.network_scanner.domain.NetworkDevice
import com.abhishek.zerodroid.features.network_scanner.domain.NetworkScanState
import com.abhishek.zerodroid.features.network_scanner.domain.OpenPort
import com.abhishek.zerodroid.features.network_scanner.domain.Vulnerability
import com.abhishek.zerodroid.features.network_scanner.domain.VulnerabilityLevel
import com.abhishek.zerodroid.features.network_scanner.viewmodel.NetworkScannerViewModel
import com.abhishek.zerodroid.ui.theme.CardBorderGreen
import com.abhishek.zerodroid.ui.theme.SeverityCritical
import com.abhishek.zerodroid.ui.theme.SeverityHigh
import com.abhishek.zerodroid.ui.theme.SeverityInfo
import com.abhishek.zerodroid.ui.theme.SeverityLow
import com.abhishek.zerodroid.ui.theme.SeverityMedium
import com.abhishek.zerodroid.ui.theme.SurfaceVariantDark
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TextSecondary

@Composable
fun NetworkScannerScreen(
    viewModel: NetworkScannerViewModel = viewModel(factory = NetworkScannerViewModel.Factory)
) {
    PermissionGate(
        permissions = PermissionUtils.wifiPermissions(),
        rationale = "Location permission is required to access WiFi network information for vulnerability scanning."
    ) {
        NetworkScannerContent(viewModel)
    }
}

@Composable
private fun NetworkScannerContent(viewModel: NetworkScannerViewModel) {
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

        // 1. Scan Controls + Subnet + Progress
        item { ScanControlSection(state, viewModel) }

        // 2. Scan Phase Indicator
        if (state.isScanning) {
            item { ScanPhaseIndicator(state) }
        }

        // 3. Summary Card
        if (state.devices.isNotEmpty() || state.isScanning) {
            item { SummaryCard(state) }
        }

        // 4. Empty State
        if (state.devices.isEmpty() && !state.isScanning && state.scanPhase == "Idle") {
            item {
                EmptyState(
                    icon = Icons.Default.Security,
                    title = "No scan results",
                    subtitle = "Tap SCAN to discover devices on your network and identify potential vulnerabilities"
                )
            }
        }

        // 5. Device List
        items(state.devices, key = { it.ip }) { device ->
            DeviceCard(device = device, onRescan = { viewModel.rescanDevice(device.ip) })
        }

        // 6. Error Display
        state.error?.let { error ->
            item {
                TerminalCard(glowColor = TerminalRed, borderColor = TerminalRed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "> ERROR: $error",
                            color = TerminalRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "[DISMISS]",
                            color = TerminalRed.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.clickable { viewModel.clearError() }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// --- Scan Controls ---

@Composable
private fun ScanControlSection(state: NetworkScanState, viewModel: NetworkScannerViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main scan button
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
                    text = if (state.isScanning) "STOP SCAN" else "SCAN NETWORK",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Subnet display
        state.subnet?.let { subnet ->
            Text(
                text = "> Subnet: $subnet.0/24",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        // Progress bar
        if (state.isScanning) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = TerminalGreen,
                trackColor = SurfaceVariantDark,
            )
            Text(
                text = "> ${(state.progress * 100).toInt()}% - ${state.scanPhase}",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}

// --- Scan Phase Indicator ---

@Composable
private fun ScanPhaseIndicator(state: NetworkScanState) {
    val phases = listOf("Host Discovery", "Port Scanning", "Service Detection")
    val currentPhaseIndex = phases.indexOf(state.scanPhase).coerceAtLeast(0)

    TerminalCard(glowColor = TerminalCyan, borderColor = TerminalCyan) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ScanningIndicator(
                    isScanning = true,
                    label = "",
                    color = TerminalCyan
                )
                Text(
                    text = "> Scan Pipeline",
                    color = TerminalCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                phases.forEachIndexed { index, phase ->
                    val isActive = state.scanPhase == phase
                    val isDone = currentPhaseIndex > index ||
                            (state.scanPhase == "Finalizing" || state.scanPhase == "Complete")

                    val phaseColor = when {
                        isActive -> TerminalCyan
                        isDone -> TerminalGreen
                        else -> TextSecondary.copy(alpha = 0.5f)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isDone || isActive) phaseColor else phaseColor.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        )
                        Text(
                            text = phase.split(" ").first(),
                            color = phaseColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    if (index < phases.lastIndex) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Summary Card ---

@Composable
private fun SummaryCard(state: NetworkScanState) {
    val deviceCount = state.devices.size
    val totalVulns = state.totalVulnerabilities
    val criticalCount = state.criticalCount
    val highCount = state.devices.sumOf { d ->
        d.vulnerabilities.count { it.level == VulnerabilityLevel.HIGH }
    }
    val mediumCount = state.devices.sumOf { d ->
        d.vulnerabilities.count { it.level == VulnerabilityLevel.MEDIUM }
    }
    val lowCount = state.devices.sumOf { d ->
        d.vulnerabilities.count { it.level == VulnerabilityLevel.LOW }
    }
    val infoCount = state.devices.sumOf { d ->
        d.vulnerabilities.count { it.level == VulnerabilityLevel.INFO }
    }

    val summaryColor = when {
        criticalCount > 0 -> SeverityCritical
        highCount > 0 -> SeverityHigh
        mediumCount > 0 -> SeverityMedium
        totalVulns > 0 -> TerminalCyan
        deviceCount > 0 -> TerminalGreen
        else -> TerminalGreen
    }

    val animatedColor by animateColorAsState(targetValue = summaryColor, label = "summaryColor")

    TerminalCard(
        glowColor = animatedColor,
        borderColor = animatedColor,
        animated = criticalCount > 0
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.isScanning) "> Scanning..." else "> Scan Summary",
                    color = animatedColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (state.isScanning) {
                    ScanningIndicator(isScanning = true, label = "", color = animatedColor)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Device count + vulnerability count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$deviceCount",
                        color = TerminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Text(
                        text = "Device${if (deviceCount != 1) "s" else ""} Found",
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$totalVulns",
                        color = if (totalVulns > 0) animatedColor else TerminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Text(
                        text = "Vulnerabilit${if (totalVulns != 1) "ies" else "y"}",
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            // Severity breakdown
            if (totalVulns > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (criticalCount > 0) SeverityBadge("CRIT: $criticalCount", SeverityCritical)
                    if (highCount > 0) SeverityBadge("HIGH: $highCount", SeverityHigh)
                    if (mediumCount > 0) SeverityBadge("MED: $mediumCount", SeverityMedium)
                    if (lowCount > 0) SeverityBadge("LOW: $lowCount", SeverityLow)
                    if (infoCount > 0) SeverityBadge("INFO: $infoCount", SeverityInfo)
                }
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
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// --- Device Card ---

@Composable
private fun DeviceCard(device: NetworkDevice, onRescan: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    val hasCritical = device.vulnerabilities.any { it.level == VulnerabilityLevel.CRITICAL }
    val hasHigh = device.vulnerabilities.any { it.level == VulnerabilityLevel.HIGH }
    val hasMedium = device.vulnerabilities.any { it.level == VulnerabilityLevel.MEDIUM }

    val borderColor = when {
        hasCritical -> SeverityCritical
        hasHigh -> SeverityHigh
        hasMedium -> SeverityMedium
        device.vulnerabilities.isNotEmpty() -> TerminalCyan
        else -> CardBorderGreen
    }

    val glowColor = when {
        hasCritical -> SeverityCritical
        hasHigh -> SeverityHigh
        hasMedium -> TerminalAmber
        else -> TerminalGreen
    }

    TerminalCard(
        glowColor = glowColor,
        borderColor = borderColor,
        animated = hasCritical,
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            // Header: IP + Device Type + Expand
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Device type badge
                    Text(
                        text = "[${device.deviceType}]",
                        color = TerminalCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(TerminalCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Column {
                        Text(
                            text = device.ip,
                            color = TerminalGreen,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        device.hostname?.let { hostname ->
                            Text(
                                text = hostname,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Vulnerability count
                    if (device.vulnerabilities.isNotEmpty()) {
                        val vulnColor = when {
                            hasCritical -> SeverityCritical
                            hasHigh -> SeverityHigh
                            hasMedium -> SeverityMedium
                            else -> SeverityLow
                        }
                        Text(
                            text = "${device.vulnerabilities.size}",
                            color = vulnColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = vulnColor,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = TerminalGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Port summary (always visible)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${device.openPorts.size} open port${if (device.openPorts.size != 1) "s" else ""}: " +
                        device.openPorts.take(5).joinToString(", ") { "${it.port}/${it.service.name.lowercase()}" } +
                        if (device.openPorts.size > 5) " ..." else "",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Rescan button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onRescan,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan device",
                                tint = TerminalCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Open Ports Section
                    Text(
                        text = "> OPEN PORTS",
                        color = TerminalGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    device.openPorts.forEach { port ->
                        PortRow(port)
                    }

                    // Vulnerabilities Section
                    if (device.vulnerabilities.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "> VULNERABILITIES (${device.vulnerabilities.size})",
                            color = SeverityCritical,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        device.vulnerabilities.forEach { vuln ->
                            VulnerabilityRow(vuln)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- Port Row ---

@Composable
private fun PortRow(port: OpenPort) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${port.port}",
            color = TerminalAmber,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = port.service.name,
            color = TerminalCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.width(72.dp)
        )
        port.banner?.let { banner ->
            Text(
                text = banner,
                color = TextSecondary.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

// --- Vulnerability Row ---

@Composable
private fun VulnerabilityRow(vuln: Vulnerability) {
    val vulnColor = vulnerabilityColor(vuln.level)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(vulnColor.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
            .border(1.dp, vulnColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        // Title + severity badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = vuln.title,
                color = vulnColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "[${vuln.level.name}]",
                color = vulnColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(vulnColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Description
        Text(
            text = vuln.description,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )

        // Port reference
        Text(
            text = "Port: ${vuln.port}",
            color = TextSecondary.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Recommendation
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "FIX:",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
            Text(
                text = vuln.recommendation,
                color = TerminalGreen.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        }
    }
}

// --- Helpers ---

@Composable
private fun vulnerabilityColor(level: VulnerabilityLevel): Color {
    return when (level) {
        VulnerabilityLevel.CRITICAL -> SeverityCritical
        VulnerabilityLevel.HIGH -> SeverityHigh
        VulnerabilityLevel.MEDIUM -> SeverityMedium
        VulnerabilityLevel.LOW -> SeverityLow
        VulnerabilityLevel.INFO -> SeverityInfo
    }
}

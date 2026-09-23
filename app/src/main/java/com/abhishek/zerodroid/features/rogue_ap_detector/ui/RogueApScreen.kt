package com.abhishek.zerodroid.features.rogue_ap_detector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdCheckRow
import com.abhishek.zerodroid.core.ui.zd.ZdCheckStatus
import com.abhishek.zerodroid.core.ui.zd.ZdDialog
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconButton
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.ZdTextField
import com.abhishek.zerodroid.core.ui.zd.ZdToolScanBar
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.ApThreatType
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RiskLevel
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RogueApAlert
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RogueApState
import com.abhishek.zerodroid.features.rogue_ap_detector.viewmodel.RogueApViewModel
import com.abhishek.zerodroid.features.wifi.domain.WifiAssessment
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

@Composable
fun RogueApScreen(
    viewModel: RogueApViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.wifiPermissions(),
        rationale = "Android needs location access before any app can list WiFi networks."
    ) {
        RogueApContent(viewModel = viewModel)
    }
}

internal fun RiskLevel.severity(): ZdSeverity = when (this) {
    RiskLevel.CRITICAL -> ZdSeverity.CRITICAL
    RiskLevel.HIGH -> ZdSeverity.HIGH
    RiskLevel.MEDIUM -> ZdSeverity.MEDIUM
    RiskLevel.LOW -> ZdSeverity.LOW
    RiskLevel.SAFE -> ZdSeverity.CLEAN
}

/** The detection algorithms, in the order they're explained on screen. */
private val algorithms = listOf(
    ApThreatType.EVIL_TWIN to "Evil twin",
    ApThreatType.SSID_SPOOF to "SSID spoofing",
    ApThreatType.KARMA_ATTACK to "Karma (many SSIDs, one radio)",
    ApThreatType.OPEN_IMPERSONATOR to "Open impersonator",
    ApThreatType.WEAK_SECURITY to "Weak security (WEP/open)",
    ApThreatType.HIDDEN_SUSPICIOUS to "Hidden AP, strong signal"
)

@Composable
private fun RogueApContent(viewModel: RogueApViewModel) {
    val state by viewModel.state.collectAsState()
    var showAdd by rememberSaveable { mutableStateOf(false) }

    HardwareLifecycleEffect(
        isActive = state.isScanning,
        onPause = viewModel::stopScan,
        onResume = viewModel::startScan
    )

    Column(Modifier.fillMaxSize()) {
        ZdToolScanBar(
            running = state.isScanning,
            onStart = viewModel::startScan,
            onStop = viewModel::stopScan,
            runningNote = "WiFi · 4 scans / 2 min limit",
            idleNote = if (state.totalAps == 0) "Checks every network in range" else "Results kept"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { TrustedCard(state, onAdd = { showAdd = true }, onRemove = viewModel::removeKnownSsid) }

            state.error?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }

            if (state.totalAps == 0 && state.alerts.isEmpty()) {
                item {
                    ZdStatePanel(
                        kicker = if (state.isScanning) "Scanning" else "Ready",
                        icon = ZdIcons.Wifi,
                        iconTint = ZdColors.Accent,
                        iconBackground = ZdColors.AccentBg,
                        title = "Is a network pretending to be one you trust?",
                        body = "Compares every access point in range for copied names, look-alike names, mismatched hardware and suspicious open networks.",
                        primaryAction = if (state.isScanning) null else "Start scan" to viewModel::startScan,
                        primaryIcon = ZdIcons.Play,
                        fullScreen = false
                    )
                }
            } else {
                item {
                    ZdCard {
                        Row {
                            ZdStat("Networks", "${state.totalAps}", Modifier.weight(1f))
                            ZdStat("Safe", "${state.safeAps}", Modifier.weight(1f), valueColor = ZdColors.Accent)
                            ZdStat("Suspicious", "${state.suspiciousAps}", Modifier.weight(1f), valueColor = if (state.suspiciousAps > 0) ZdColors.Medium else ZdColors.Text)
                        }
                    }
                }
                if (state.alerts.isNotEmpty()) {
                    item { ZdSectionLabel("Findings", trailingText = "${state.alerts.size}") }
                    items(state.alerts, key = { it.id }) { alert -> FindingCard(alert) }
                } else {
                    item {
                        ZdCard(background = ZdColors.AccentBg, borderColor = ZdColors.AccentBorder) {
                            Text("No rogue access points found", style = ZdType.Label, color = ZdColors.Accent)
                            Text("Every network passed the checks below.", style = ZdType.BodySmall, color = ZdColors.Text2)
                        }
                    }
                }
                item { ZdSectionLabel("Algorithms") }
                item {
                    ZdListCard(algorithms) { (type, label) ->
                        val found = state.alerts.count { it.threatType == type }
                        ZdCheckRow(
                            title = label,
                            detail = if (found == 0) "None" else "$found found",
                            status = if (found == 0) ZdCheckStatus.PASS else ZdCheckStatus.WARN
                        )
                    }
                }
                if (state.alerts.isNotEmpty()) {
                    item { ZdButton("Clear findings", onClick = viewModel::clearAlerts, variant = ZdButtonVariant.Ghost, icon = ZdIcons.Trash) }
                }
            }
        }
    }

    if (showAdd) {
        var ssid by rememberSaveable { mutableStateOf("") }
        ZdDialog(
            title = "Trust a network",
            onDismiss = { showAdd = false },
            confirmLabel = "Trust",
            confirmEnabled = ssid.isNotBlank(),
            onConfirm = {
                viewModel.addKnownSsid(ssid.trim())
                showAdd = false
            }
        ) {
            Text("Twins and look-alikes of trusted networks are flagged first, and weak-security warnings are skipped for them.", style = ZdType.BodySmall, color = ZdColors.Text2)
            ZdTextField(value = ssid, onValueChange = { ssid = it }, label = "NETWORK NAME (SSID)", placeholder = "Home_5G")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrustedCard(state: RogueApState, onAdd: () -> Unit, onRemove: (String) -> Unit) {
    ZdCard(verticalSpacing = 8.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Trusted networks · ${state.knownSsids.size}", style = ZdType.Label, color = ZdColors.Text)
                Text(
                    if (state.knownSsids.isEmpty()) "Add your home and work WiFi so their twins are caught first"
                    else "${state.knownSsids.joinToString(" · ")} — twins of these are flagged first",
                    style = ZdType.Caption,
                    color = ZdColors.Text3
                )
            }
            ZdIconButton(ZdIcons.Plus, contentDescription = "Add trusted network", onClick = onAdd, tint = ZdColors.Accent)
        }
        if (state.knownSsids.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.knownSsids.forEach { ssid ->
                    ZdButton(
                        "$ssid  ×",
                        onClick = { onRemove(ssid) },
                        variant = ZdButtonVariant.Secondary,
                        height = 32.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun FindingCard(alert: RogueApAlert) {
    val severity = alert.riskLevel.severity()
    ZdCard(borderColor = severity.color.copy(alpha = 0.4f)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ZdSeverityBadge(severity)
            ZdTag(alert.threatType.label.uppercase())
        }
        Text(WifiAssessment.displayName(alert.suspiciousAp), style = ZdType.Heading, color = ZdColors.Text)
        Text(alert.description, style = ZdType.BodySmall, color = ZdColors.Text2)
        val legit = alert.legitimateAp
        if (legit != null) {
            ZdListCard(listOf("Suspect" to alert.suspiciousAp, "Original" to legit)) { (label, ap) ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    ZdStat(label, WifiAssessment.hardwareLabel(ap.bssid), Modifier.weight(1.3f), valueColor = if (label == "Suspect") ZdColors.Medium else ZdColors.Text)
                    ZdStat("Signal", "${ap.rssi} dBm", Modifier.weight(1f))
                    ZdStat("Security", ap.security.label, Modifier.weight(1f))
                }
            }
        }
    }
}

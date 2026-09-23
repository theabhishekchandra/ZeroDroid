package com.abhishek.zerodroid.features.watch.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.abhishek.zerodroid.core.notify.ZdNotifier
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.prefs.AppSettings
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconButton
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.features.watch.data.WatchRuleStore
import com.abhishek.zerodroid.features.watch.domain.RuleSensor
import com.abhishek.zerodroid.features.watch.domain.RuleKind
import com.abhishek.zerodroid.features.watch.domain.WatchRule
import com.abhishek.zerodroid.features.watch.service.WatchService
import com.abhishek.zerodroid.navigation.locateRoute
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val store: WatchRuleStore,
    val settings: AppSettings,
    private val notifier: ZdNotifier
) : ViewModel() {

    fun setEnabled(rule: WatchRule, on: Boolean) {
        store.setEnabled(rule.id, on)
        if (on) store.setPaused(false)
        WatchService.sync(context, store)
    }

    fun resume() {
        store.setPaused(false)
        WatchService.sync(context, store)
    }

    fun pause() {
        store.setPaused(true)
        WatchService.sync(context, store)
    }

    fun addCustom(rssi: Int, minutes: Int) {
        store.addCustom(rssi, minutes)
        store.setPaused(false)
        WatchService.sync(context, store)
    }

    fun remove(rule: WatchRule) {
        store.remove(rule.id)
        WatchService.sync(context, store)
    }

    /** Posts what an alert from this rule would look like, so people can check notifications work. */
    fun test(rssi: Int, minutes: Int) {
        notifier.postAlert(
            "rule-test",
            "Test: Unknown BLE device nearby",
            "This is how a rule alert looks: a device stronger than $rssi dBm for $minutes min. Nothing was actually detected.",
            locateRoute("00:00:00:00:00:00", "Test device"),
            "Device nearby"
        )
    }

    val canNotify: Boolean get() = notifier.canNotify
}

/** Runtime permissions a rule needs before it can run in the background. */
internal fun permissionsFor(rule: WatchRule): List<String> = buildList {
    rule.kind.sensors.forEach { s ->
        when (s) {
            RuleSensor.BLE -> addAll(PermissionUtils.blePermissions())
            RuleSensor.GPS, RuleSensor.WIFI -> add(Manifest.permission.ACCESS_FINE_LOCATION)
            RuleSensor.CELL -> add(Manifest.permission.READ_PHONE_STATE)
        }
    }
    if (rule.kind.sensors.contains(RuleSensor.WIFI)) addAll(PermissionUtils.wifiPermissions())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.distinct()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesScreen(
    onOpenSettings: () -> Unit,
    viewModel: RulesViewModel = hiltViewModel()
) {
    val rules by viewModel.store.rules.collectAsState()
    val paused by viewModel.store.paused.collectAsState()
    val trusted by viewModel.settings.trustedNetworks.collectAsState()

    var rssi by rememberSaveable { mutableIntStateOf(WatchRule.DEFAULT_RSSI) }
    var minutes by rememberSaveable { mutableIntStateOf(WatchRule.DEFAULT_MINUTES) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val anyEnabled = rules.any { it.enabled }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Rules keep watching after you leave the app, using short low-power scans. Android shows a notification while any rule is on.",
            style = ZdType.BodySmall,
            color = ZdColors.Text2
        )
        if (anyEnabled) {
            ZdCard(
                background = if (paused) ZdColors.MediumBg else ZdColors.AccentBg,
                borderColor = if (paused) ZdColors.Medium.copy(alpha = 0.4f) else ZdColors.AccentBorder
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(if (paused) "Paused" else "Watching", style = ZdType.Label, color = if (paused) ZdColors.Medium else ZdColors.Accent)
                        Text(
                            if (paused) "Rules are kept but not running." else WatchService.summary(viewModel.store.active),
                            style = ZdType.Caption,
                            color = ZdColors.Text2
                        )
                    }
                    if (paused) ZdButton("Resume", onClick = viewModel::resume, height = 40.dp)
                    else ZdButton("Pause all", onClick = viewModel::pause, variant = ZdButtonVariant.Secondary, height = 40.dp)
                }
            }
        }

        rules.forEach { rule ->
            RuleCard(
                rule = rule,
                trusted = trusted,
                onToggle = { on, granted -> if (granted) viewModel.setEnabled(rule, on) else message = "“${rule.title}” needs the permissions it asked for." },
                onRemove = { viewModel.remove(rule) },
                onOpenSettings = onOpenSettings
            )
        }
        message?.let { ZdFootnote(it, icon = ZdIcons.Warning) }

        ZdSectionLabel("New rule")
        ZdCard(verticalSpacing = 10.dp) {
            Step("WHEN") { ZdTag("any unknown BLE device", color = ZdColors.Text) }
            Step("IS") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("stronger than", style = ZdType.Caption, color = ZdColors.Text2)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WatchRule.RSSI_CHOICES.forEach { v -> ZdChip("$v dBm", selected = rssi == v, onClick = { rssi = v }) }
                    }
                }
            }
            Step("FOR") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WatchRule.MINUTE_CHOICES.forEach { v -> ZdChip("$v min", selected = minutes == v, onClick = { minutes = v }) }
                }
            }
            Step("THEN") { ZdTag("notify + log", color = ZdColors.Text) }
            val newRule = WatchRule("new", RuleKind.CUSTOM_BLE)
            val perms = rememberMultiplePermissionsStateCompat(permissionsFor(newRule)) { granted ->
                if (granted) viewModel.addCustom(rssi, minutes).also { message = null }
                else message = "Custom rules need Bluetooth, location and notification permissions."
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZdButton(
                    "Test now",
                    onClick = {
                        if (viewModel.canNotify) viewModel.test(rssi, minutes)
                        else message = "Notifications are off for ZeroDroid. Turn them on to get rule alerts."
                    },
                    variant = ZdButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
                ZdButton("Save rule", onClick = perms, modifier = Modifier.weight(1f))
            }
        }
        ZdFootnote("Devices you mark “It’s mine” from an alert are ignored. Some trackers change their address every few hours, so the same tag can alert again.")
        ZdFootnote("Rules resume when you open the app after a restart; Android doesn’t let apps start background scanning on their own.")
    }
}

@Composable
private fun Step(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = ZdType.Path, color = ZdColors.Text3, modifier = Modifier.width(44.dp).padding(top = 6.dp))
        Column(Modifier.weight(1f)) { content() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleCard(
    rule: WatchRule,
    trusted: Set<String>,
    onToggle: (Boolean, Boolean) -> Unit,
    onRemove: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val blocked = rule.kind == RuleKind.ROGUE_TRUSTED && trusted.isEmpty()
    val request = rememberMultiplePermissionsStateCompat(permissionsFor(rule)) { granted -> onToggle(true, granted) }
    ZdCard(borderColor = if (rule.enabled) ZdColors.AccentBorder else ZdColors.Border) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(rule.title, style = ZdType.Label, color = ZdColors.Text, modifier = Modifier.weight(1f))
            if (!rule.kind.builtIn) ZdIconButton(ZdIcons.Trash, contentDescription = "Delete rule", onClick = onRemove)
            Switch(
                checked = rule.enabled,
                enabled = !blocked,
                onCheckedChange = { on -> if (on) request() else onToggle(false, true) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ZdColors.Bg,
                    checkedTrackColor = ZdColors.Accent,
                    uncheckedThumbColor = ZdColors.Text3,
                    uncheckedTrackColor = ZdColors.Surface2,
                    uncheckedBorderColor = ZdColors.Border
                ),
                modifier = Modifier.semantics { contentDescription = rule.title }
            )
        }
        Text(rule.description(trusted), style = ZdType.BodySmall, color = ZdColors.Text2)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rule.kind.sensors.forEach { ZdTag(it.label) }
            ZdTag(rule.kind.battery.label)
        }
        if (blocked) ZdButton("Add trusted networks", onClick = onOpenSettings, variant = ZdButtonVariant.Secondary, height = 40.dp, modifier = Modifier.fillMaxWidth())
    }
}

/** Returns a click handler that asks for [permissions] only if needed, then reports whether all were granted. */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun rememberMultiplePermissionsStateCompat(permissions: List<String>, onResult: (Boolean) -> Unit): () -> Unit {
    val state = rememberMultiplePermissionsState(permissions) { results -> onResult(results.values.all { it }) }
    return { if (state.allPermissionsGranted) onResult(true) else state.launchMultiplePermissionRequest() }
}

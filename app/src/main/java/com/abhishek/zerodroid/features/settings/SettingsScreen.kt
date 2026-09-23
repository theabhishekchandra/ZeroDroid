package com.abhishek.zerodroid.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.BuildConfig
import com.abhishek.zerodroid.R
import com.abhishek.zerodroid.core.database.AppDatabase
import com.abhishek.zerodroid.core.prefs.AppSettings
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdDialog
import com.abhishek.zerodroid.core.ui.zd.ZdIconButton
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSwitchRow
import com.abhishek.zerodroid.core.ui.zd.ZdTextField
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val settings: AppSettings,
    private val database: AppDatabase
) : ViewModel() {

    fun addTrusted(ssid: String) = settings.setTrustedNetworks(settings.trustedNetworks.value + ssid.trim())
    fun removeTrusted(ssid: String) = settings.setTrustedNetworks(settings.trustedNetworks.value - ssid)

    /** Wipes every scan, session, alert and log on this phone; settings stay. */
    fun deleteAllData(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            launch(Dispatchers.Main) { onDone() }
        }
    }

    fun redoOnboarding() = settings.setOnboardingDone(false)
}

@Composable
fun SettingsScreen(
    onDataDeleted: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val s = viewModel.settings
    val redact by s.redactExports.collectAsState()
    val retention by s.retentionDays.collectAsState()
    val keepOn by s.keepScreenOnDuringSweeps.collectAsState()
    val trusted by s.trustedNetworks.collectAsState()
    var newSsid by rememberSaveable { mutableStateOf("") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showAgreement by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ZdSectionLabel("Privacy")
        ZdCard(verticalSpacing = 0.dp) {
            ZdSwitchRow(
                label = "Redact MAC addresses in exports",
                description = "The default for every export; you can still change it per export",
                checked = redact,
                onCheckedChange = s::setRedactExports
            )
        }
        ZdCard(verticalSpacing = 8.dp) {
            Text("Keep sessions for", style = ZdType.Label, color = ZdColors.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppSettings.RETENTION_CHOICES.forEach { days ->
                    ZdChip("$days days", selected = retention == days, onClick = { s.setRetentionDays(days) })
                }
            }
            Text("Older sessions are deleted automatically. Everything stays on this phone.", style = ZdType.Caption, color = ZdColors.Text3)
        }

        ZdSectionLabel("Room sweep")
        ZdCard(verticalSpacing = 0.dp) {
            ZdSwitchRow(
                label = "Keep screen on during sweeps",
                description = "Android slows scanning when the screen turns off",
                checked = keepOn,
                onCheckedChange = s::setKeepScreenOn
            )
        }

        ZdSectionLabel("Trusted networks", trailingText = "${trusted.size}")
        Text(
            "Your own WiFi names. Rogue AP and sweeps warn you first when a look-alike appears.",
            style = ZdType.Caption,
            color = ZdColors.Text3
        )
        if (trusted.isNotEmpty()) {
            ZdListCard(trusted.sorted()) { ssid ->
                ZdListRow(
                    title = ssid,
                    leading = null,
                    trailing = { ZdIconButton(ZdIcons.Close, contentDescription = "Remove $ssid", onClick = { viewModel.removeTrusted(ssid) }) }
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val add = { if (newSsid.isNotBlank()) { viewModel.addTrusted(newSsid); newSsid = "" } }
            ZdTextField(
                value = newSsid,
                onValueChange = { newSsid = it },
                label = "ADD NETWORK",
                placeholder = "Home WiFi name",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.weight(1f)
            )
            ZdButton("Add", onClick = add, enabled = newSsid.isNotBlank(), icon = ZdIcons.Plus, variant = ZdButtonVariant.Secondary)
        }

        ZdSectionLabel("About")
        ZdListCard(listOf(0, 1, 2)) { row ->
            when (row) {
                0 -> ZdListRow(title = "Pick my goals again", titleMono = false, subtitle = "Re-pins tools on Home", showChevron = true, onClick = viewModel::redoOnboarding)
                1 -> ZdListRow(title = "Responsible use agreement", titleMono = false, showChevron = true, onClick = { showAgreement = true })
                else -> ZdListRow(title = "ZeroDroid ${BuildConfig.VERSION_NAME}", titleMono = false, subtitle = "Open source · for learning about the radios around you")
            }
        }

        ZdSectionLabel("Data")
        ZdButton("Delete all saved data", onClick = { confirmDelete = true }, variant = ZdButtonVariant.Danger, icon = ZdIcons.Trash)
    }

    if (confirmDelete) {
        ZdDialog(
            title = "Delete everything?",
            onDismiss = { confirmDelete = false },
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                viewModel.deleteAllData(onDataDeleted)
            }
        ) {
            Text(
                "Removes every session, alert, saved BLE device, NFC tag, QR scan and wardriving log from this phone. Settings are kept. This can’t be undone.",
                style = ZdType.BodySmall,
                color = ZdColors.Text2
            )
        }
    }
    if (showAgreement) {
        ZdDialog(
            title = stringResource(R.string.ethical_title),
            onDismiss = { showAgreement = false },
            confirmLabel = "Close",
            onConfirm = { showAgreement = false },
            dismissLabel = ""
        ) {
            Text(stringResource(R.string.ethical_message), style = ZdType.BodySmall, color = ZdColors.Text2)
        }
    }
}

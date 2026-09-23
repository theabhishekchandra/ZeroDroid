package com.abhishek.zerodroid.features.nfc.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.ui.zd.LiveDot
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTabs
import com.abhishek.zerodroid.core.ui.zd.ZdTextField
import com.abhishek.zerodroid.features.nfc.domain.NfcState
import com.abhishek.zerodroid.features.nfc.domain.NfcTab
import com.abhishek.zerodroid.features.nfc.viewmodel.NfcViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

@Composable
fun NfcScreen(
    viewModel: NfcViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    if (!state.isNfcAvailable) {
        ZdStatePanel(
            kicker = "Not on this phone",
            icon = ZdIcons.Nfc,
            title = "Your phone has no NFC chip",
            body = "NFC reads tags and cards over a few centimetres at 13.56 MHz. This phone doesn’t report an NFC controller."
        )
        return
    }
    if (!state.isNfcEnabled) {
        ZdStatePanel(
            kicker = "NFC is off",
            kickerColor = ZdColors.Medium,
            icon = ZdIcons.Nfc,
            title = "Turn on NFC to read tags",
            body = "NFC is switched off in Android settings. Turn it on, then come back.",
            primaryAction = "Open NFC settings" to {
                context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            primaryIcon = ZdIcons.Settings
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        ZdTabs(
            tabs = NfcTab.entries.map { it.label },
            selectedIndex = state.tab.ordinal,
            onSelect = { viewModel.setTab(NfcTab.entries[it]) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state.tab) {
                NfcTab.READ -> readTab(state, onClear = viewModel::clearHistory)
                NfcTab.WRITE -> item {
                    NfcWritePanel(
                        writeResult = state.writeResult,
                        onWriteText = viewModel::writeText,
                        onWriteUri = viewModel::writeUri
                    )
                }
                NfcTab.MIFARE -> mifareTab(state, viewModel, context)
                NfcTab.EMULATE -> item { EmulatePanel(state, viewModel) }
            }
        }
    }
}

@Composable
private fun HoldTagCard(title: String, detail: String) {
    ZdCard(borderColor = ZdColors.AccentBorder) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ZdIconTile(ZdIcons.Nfc, size = 44.dp, iconSize = 22.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiveDot()
                    Text(title, style = ZdType.Label, color = ZdColors.Text)
                }
                Text(detail, style = ZdType.Caption, color = ZdColors.Text3)
            }
        }
    }
}

private fun LazyListScope.readTab(state: NfcState, onClear: () -> Unit) {
    item { HoldTagCard("Hold a tag to the back of the phone", "Reading starts automatically. Keep still for a second.") }
    state.lastTag?.let { tag ->
        item { ZdSectionLabel("Last tag") }
        item { NfcTagCard(tag = tag) }
    }
    val last = state.lastTag
    val older = state.tagHistory.filterNot { last != null && it.uid == last.uid && it.timestamp == last.timestamp }
    if (older.isNotEmpty()) {
        item { ZdSectionLabel("History", trailingText = "${older.size}", actionLabel = "Clear", onAction = onClear) }
        items(older) { NfcTagCard(tag = it) }
    }
}

private fun LazyListScope.mifareTab(state: NfcState, viewModel: NfcViewModel, context: Context) {
    item {
        HoldTagCard(
            if (state.isReadingMifare) "Reading sectors…" else "Hold a MIFARE Classic card",
            state.mifareMessage ?: "Each sector is tried with ${10 + state.customKeys.size} keys: the common factory keys plus any you add."
        )
    }
    if (state.isReadingMifare) {
        item { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = ZdColors.Accent) }
    }
    item {
        NfcMifareEnhancedPanel(
            sectors = state.mifareSectors,
            onAddCustomKey = viewModel::addCustomKey,
            onRemoveCustomKey = viewModel::removeCustomKey,
            customKeys = state.customKeys,
            onCopyDump = { dump ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("MIFARE dump", dump))
            },
            onWriteBlock = viewModel::writeMifareBlock
        )
    }
    item { ZdFootnote("Only read or clone cards you own. Many phones can’t read MIFARE Classic at all, because it needs an NXP NFC chip.") }
}

@Composable
private fun EmulatePanel(state: NfcState, viewModel: NfcViewModel) {
    var isUrl by rememberSaveable { mutableStateOf(state.emulatedIsUrl) }
    var value by rememberSaveable { mutableStateOf(state.emulatedPayload) }
    var tooLong by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HoldTagCard(
            "Emulating a tag",
            "Another phone or reader can tap this phone now. The screen must stay on and unlocked."
        )
        ZdCard(verticalSpacing = 12.dp) {
            Text("Payload · Type 4 tag", style = ZdType.Label, color = ZdColors.Text2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZdChip("Text", selected = !isUrl, onClick = { isUrl = false })
                ZdChip("URL", selected = isUrl, onClick = { isUrl = true })
            }
            ZdTextField(value = value, onValueChange = { value = it; tooLong = false }, label = if (isUrl) "URL" else "TEXT")
            if (tooLong) Text("Too long: emulated tags hold about 250 bytes.", style = ZdType.Caption, color = ZdColors.Critical)
            ZdButton(
                "Serve this payload",
                onClick = { tooLong = !viewModel.setEmulatedPayload(value, isUrl) },
                enabled = value.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Now serving: ${if (state.emulatedIsUrl) "URL" else "Text"} “${state.emulatedPayload}”",
                style = ZdType.Caption,
                color = ZdColors.Text3
            )
        }
        ZdFootnote("Host card emulation answers readers as an NFC Forum Type 4 tag. It can’t copy payment or access cards: those rely on secure hardware.")
    }
}

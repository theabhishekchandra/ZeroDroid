package com.abhishek.zerodroid.features.ir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTabs
import com.abhishek.zerodroid.features.ir.domain.IrScreenTab
import com.abhishek.zerodroid.features.ir.viewmodel.IrViewModel

@Composable
fun IrScreen(viewModel: IrViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    // Without an IR blaster, Flipper files can still be opened to read and keep codes.
    var browseAnyway by rememberSaveable { mutableStateOf(false) }

    if (!state.isIrAvailable && !browseAnyway) {
        ZdStatePanel(
            kicker = "Not on this phone",
            icon = ZdIcons.Remote,
            title = "Your phone has no IR blaster",
            body = "IR Remote sends infrared codes, which needs an IR emitter. Most phones since 2020 don’t have one.",
            note = "You can still open Flipper Zero .ir files to read and save codes.",
            secondaryAction = "Browse & import .ir files anyway" to {
                viewModel.setActiveTab(IrScreenTab.IMPORT)
                browseAnyway = true
            }
        )
        return
    }

    val tabs = if (state.isIrAvailable) listOf(IrScreenTab.REMOTE, IrScreenTab.CUSTOM, IrScreenTab.IMPORT) else listOf(IrScreenTab.IMPORT)
    Column(Modifier.fillMaxSize()) {
        ZdTabs(
            tabs = tabs.map {
                when (it) {
                    IrScreenTab.REMOTE -> "Remotes"
                    IrScreenTab.CUSTOM -> "Custom"
                    IrScreenTab.IMPORT -> "Imported"
                }
            },
            selectedIndex = tabs.indexOf(state.activeTab).coerceAtLeast(0),
            onSelect = { viewModel.setActiveTab(tabs[it]) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (if (state.isIrAvailable) state.activeTab else IrScreenTab.IMPORT) {
                IrScreenTab.REMOTE -> {
                    item {
                        IrRemoteGridPanel(
                            selectedProfile = state.selectedProfile,
                            lastTransmitResult = state.lastTransmitResult,
                            onProfileSelected = viewModel::selectProfile,
                            onButtonPress = viewModel::transmitRemoteButton
                        )
                    }
                    item { ZdFootnote("Sends standard codes for the selected brand. IR is one-way: there’s no pairing, so point the top of the phone at the device.") }
                }
                IrScreenTab.CUSTOM -> item {
                    IrTransmitPanel(
                        state = state,
                        onProtocolChange = viewModel::setProtocol,
                        onFrequencyChange = viewModel::setFrequency,
                        onCodeChange = viewModel::setCode,
                        onTransmit = viewModel::transmit
                    )
                }
                IrScreenTab.IMPORT -> {
                    item {
                        IrImportPanel(
                            signals = state.importedSignals,
                            onImportFile = viewModel::importFlipperFile,
                            onTransmitSignal = viewModel::transmitSignal
                        )
                    }
                    if (!state.isIrAvailable) {
                        item { ZdFootnote("This phone can read and list codes but can’t transmit them.", icon = ZdIcons.Info) }
                    }
                }
            }
        }
    }
}

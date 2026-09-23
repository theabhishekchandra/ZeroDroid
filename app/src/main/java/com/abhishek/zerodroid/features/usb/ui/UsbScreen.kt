package com.abhishek.zerodroid.features.usb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdCheckRow
import com.abhishek.zerodroid.core.ui.zd.ZdCheckStatus
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.features.usb.domain.BadUsbIndicator
import com.abhishek.zerodroid.features.usb.domain.UsbDeviceInfo
import com.abhishek.zerodroid.features.usb.viewmodel.UsbViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

@Composable
fun UsbScreen(
    viewModel: UsbViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    if (!viewModel.hasUsbHost) {
        ZdStatePanel(
            kicker = "Not on this phone",
            icon = ZdIcons.Usb,
            title = "Your phone can’t host USB devices",
            body = "Inspecting keyboards, drives and dongles needs USB host (OTG) support, which this phone doesn’t report."
        )
        return
    }

    if (state.devices.isEmpty()) {
        ZdStatePanel(
            kicker = "Watching USB",
            icon = ZdIcons.Usb,
            iconTint = ZdColors.Accent,
            iconBackground = ZdColors.AccentBg,
            title = "Plug something in",
            body = "Connect a device through a USB-OTG adapter. ZeroDroid lists what it claims to be and flags combinations used by BadUSB attacks, like a “flash drive” that is also a keyboard."
        )
        return
    }

    val flagged = state.devices.filter { it.badUsbIndicators.isNotEmpty() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        flagged.forEach { device ->
            item {
                ZdCard(background = ZdColors.HighBg, borderColor = ZdColors.High.copy(alpha = 0.45f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(ZdIcons.Warning, contentDescription = null, tint = ZdColors.High, modifier = Modifier.size(20.dp))
                        Text("Check this: “${device.productName ?: device.deviceName}”", style = ZdType.Heading, color = ZdColors.Text)
                    }
                    Text(
                        "It says it’s ${device.deviceClassName.lowercase()} but also acts as a keyboard. That combination is how BadUSB devices type commands. Unplug it unless you know what it is.",
                        style = ZdType.BodySmall,
                        color = ZdColors.Text2
                    )
                    ZdButton("Details", onClick = { viewModel.selectDevice(device) }, variant = ZdButtonVariant.Secondary, height = 40.dp)
                }
            }
        }
        item { ZdSectionLabel("Connected", trailingText = "${state.devices.size}") }
        item {
            ZdListCard(state.devices) { device ->
                ZdListRow(
                    title = device.productName ?: device.deviceName,
                    subtitle = "${device.interfaces.joinToString(" + ") { it.className }.ifEmpty { device.deviceClassName }} · ${device.vidPid}",
                    leading = { ZdIconTile(ZdIcons.Usb) },
                    showChevron = true,
                    onClick = { viewModel.selectDevice(device) },
                    trailing = if (device.badUsbIndicators.isNotEmpty()) {
                        { ZdSeverityBadge(ZdSeverity.HIGH, label = "CHECK") }
                    } else null
                )
            }
        }
        item { ZdFootnote("Android accepts typing from any USB keyboard. Unplugging is always the safest action.") }
    }

    state.selectedDevice?.let { device ->
        DeviceSheet(device, onDismiss = { viewModel.selectDevice(null) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSheet(device: UsbDeviceInfo, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ZdColors.Surface,
        scrimColor = ZdColors.Scrim,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(device.productName ?: device.deviceName, style = ZdType.Title, color = ZdColors.Text)
            ZdCard {
                Row {
                    ZdStat("Vendor ID", "0x%04X".format(device.vendorId), Modifier.weight(1f))
                    ZdStat("Product ID", "0x%04X".format(device.productId), Modifier.weight(1f))
                }
                Row {
                    ZdStat("Manufacturer", device.manufacturerName ?: "(blank)", Modifier.weight(1f))
                    ZdStat("Class", device.deviceClassName, Modifier.weight(1f))
                }
            }
            ZdSectionLabel("Interfaces", trailingText = "${device.interfaces.size}")
            ZdListCard(device.interfaces) { iface ->
                ZdListRow(
                    title = "Interface ${iface.id} · ${iface.className}",
                    subtitle = "${iface.endpointCount} endpoint${if (iface.endpointCount == 1) "" else "s"}" +
                        iface.endpoints.joinToString("") { " · ${it.direction} ${it.type}" },
                    trailing = {
                        when (iface.interfaceClass) {
                            UsbDeviceInfo.USB_CLASS_HID -> ZdTag("INPUT", color = ZdColors.Medium, background = ZdColors.MediumBg, border = ZdColors.MediumBorder)
                            UsbDeviceInfo.USB_CLASS_MASS_STORAGE -> ZdTag("STORAGE", color = ZdColors.Info, background = ZdColors.InfoBg, border = ZdColors.InfoBorder)
                            else -> {}
                        }
                    }
                )
            }
            ZdSectionLabel("Why it was flagged")
            ZdListCard(BadUsbIndicator.entries) { indicator ->
                val hit = indicator in device.badUsbIndicators
                ZdCheckRow(
                    title = when (indicator) {
                        BadUsbIndicator.HID_PLUS_STORAGE -> "Keyboard + storage in one device"
                        BadUsbIndicator.HID_NO_IDENTITY -> "Keyboard with no maker or product name"
                    },
                    detail = if (hit) indicator.description else "Not seen",
                    status = if (hit) ZdCheckStatus.WARN else ZdCheckStatus.PASS
                )
            }
        }
    }
}

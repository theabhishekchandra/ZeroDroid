package com.abhishek.zerodroid.features.usb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.StatusIndicator
import com.abhishek.zerodroid.features.usb.viewmodel.UsbViewModel

@Composable
fun UsbScreen(
    viewModel: UsbViewModel = viewModel(factory = UsbViewModel.Factory)
) {
    val app = LocalContext.current.applicationContext as ZeroDroidApp
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            StatusIndicator(isAvailable = app.container.hardwareChecker.hasUsbHost())
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "> ${state.devices.size} USB device(s) connected",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (state.devices.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Usb,
                    title = "No USB devices detected",
                    subtitle = "Connect a USB device via OTG cable"
                )
            }
        }

        items(state.devices, key = { it.vidPid + it.deviceName }) { device ->
            UsbDeviceItem(
                device = device,
                onClick = { viewModel.selectDevice(device) }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    state.selectedDevice?.let { device ->
        UsbDeviceDetailSheet(
            device = device,
            onDismiss = { viewModel.selectDevice(null) }
        )
    }
}

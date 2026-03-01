package com.abhishek.zerodroid.features.usbcamera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.StatusIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.usbcamera.viewmodel.UsbCameraViewModel

@Composable
fun UsbCameraScreen(
    viewModel: UsbCameraViewModel = viewModel(factory = UsbCameraViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            StatusIndicator(isAvailable = state.hasUsbHost)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.refresh() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Rescan") }
        }

        // USB Video Class devices
        if (state.usbVideoDevices.isNotEmpty()) {
            item {
                Text(
                    text = "> USB Video Devices (${state.usbVideoDevices.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(state.usbVideoDevices) { device ->
                TerminalCard {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "VID:PID ${device.vidPid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    device.manufacturerName?.let {
                        Text(
                            text = "Manufacturer: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Camera2 EXTERNAL cameras
        if (state.camera2ExternalCameras.isNotEmpty()) {
            item {
                Text(
                    text = "> Camera2 External Cameras",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(state.camera2ExternalCameras) { camera ->
                UsbCameraInfoCard(camera = camera)
            }

            // Preview for the first available external camera
            state.camera2ExternalCameras.firstOrNull()?.let { camera ->
                item {
                    TerminalCard {
                        Text(
                            text = "> Preview",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        UsbCameraPreview(cameraId = camera.cameraId)
                    }
                }
            }
        }

        if (state.usbVideoDevices.isEmpty() && state.camera2ExternalCameras.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Videocam,
                    title = "No USB cameras detected",
                    subtitle = "Connect a USB camera via OTG cable. Full UVC support requires native JNI libraries."
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

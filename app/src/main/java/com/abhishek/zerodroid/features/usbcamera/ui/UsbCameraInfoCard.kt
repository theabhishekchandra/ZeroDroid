package com.abhishek.zerodroid.features.usbcamera.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.usbcamera.domain.UsbCameraInfo

@Composable
fun UsbCameraInfoCard(
    camera: UsbCameraInfo,
    modifier: Modifier = Modifier
) {
    TerminalCard(modifier = modifier) {
        Text(
            text = camera.deviceName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Camera ID: ${camera.cameraId} | External: ${camera.isExternal}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        if (camera.resolutions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Resolutions: ${camera.resolutions.take(5).joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

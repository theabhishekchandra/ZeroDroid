package com.abhishek.zerodroid.features.usbcamera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdStatePanel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.ZdTagFlow
import com.abhishek.zerodroid.features.usbcamera.viewmodel.UsbCameraViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

@Composable
fun UsbCameraScreen(
    viewModel: UsbCameraViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.cameraPermissions(),
        rationale = "Android treats an external camera like any camera, so the preview needs camera access."
    ) {
        UsbCameraContent(viewModel = viewModel)
    }
}

@Composable
private fun UsbCameraContent(viewModel: UsbCameraViewModel) {
    val state by viewModel.state.collectAsState()

    if (!state.hasUsbHost) {
        ZdStatePanel(
            kicker = "Not on this phone",
            icon = ZdIcons.Usb,
            title = "Your phone can’t host USB devices",
            body = "USB cameras connect through USB-OTG, which needs USB host support that this phone doesn’t report."
        )
        return
    }

    if (state.usbVideoDevices.isEmpty() && state.camera2ExternalCameras.isEmpty()) {
        ZdStatePanel(
            kicker = "No camera connected",
            icon = ZdIcons.Camera,
            iconTint = ZdColors.Accent,
            iconBackground = ZdColors.AccentBg,
            title = "Plug in a USB camera",
            body = "Most webcams and endoscopes follow the USB Video Class (UVC) standard. Connect one with a USB-OTG adapter, then rescan.",
            primaryAction = "Rescan" to viewModel::refresh,
            primaryIcon = ZdIcons.Refresh
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.camera2ExternalCameras.firstOrNull()?.let { camera ->
            item {
                ZdCard {
                    Text("Live preview · ${camera.deviceName}", style = ZdType.Label, color = ZdColors.Text)
                    UsbCameraPreview(cameraId = camera.cameraId)
                }
            }
        }
        if (state.usbVideoDevices.isNotEmpty()) {
            item { ZdSectionLabel("USB video devices", trailingText = "${state.usbVideoDevices.size}") }
            item {
                ZdListCard(state.usbVideoDevices) { device ->
                    ZdListRow(
                        title = device.deviceName,
                        subtitle = listOfNotNull(device.manufacturerName, device.vidPid).joinToString(" · "),
                        leading = { ZdIconTile(ZdIcons.Camera) },
                        trailing = {
                            when {
                                state.connectingVidPid == device.vidPid ->
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ZdColors.Accent)
                                state.connectedVidPid == device.vidPid ->
                                    ZdButton("Disconnect", onClick = viewModel::disconnect, variant = ZdButtonVariant.Danger, height = 36.dp)
                                else ->
                                    ZdButton("Connect", onClick = { viewModel.connect(device) }, variant = ZdButtonVariant.Secondary, height = 36.dp)
                            }
                        }
                    )
                }
            }
        }
        if (state.camera2ExternalCameras.isNotEmpty()) {
            item { ZdSectionLabel("Camera2 external cameras", trailingText = "${state.camera2ExternalCameras.size}") }
            state.camera2ExternalCameras.forEach { camera ->
                item {
                    ZdCard {
                        Text(camera.deviceName, style = ZdType.Label, color = ZdColors.Text)
                        Text("Camera ID ${camera.cameraId}${camera.vidPid?.let { " · $it" } ?: ""}", style = ZdType.Caption, color = ZdColors.Text3)
                        if (camera.resolutions.isNotEmpty()) {
                            ZdTagFlow(camera.resolutions.take(8))
                        } else {
                            ZdTag("No resolutions reported")
                        }
                    }
                }
            }
        }
        state.connectionError?.let { item { ZdFootnote(it, icon = ZdIcons.Warning) } }
        item { ZdButton("Rescan", onClick = viewModel::refresh, variant = ZdButtonVariant.Ghost, icon = ZdIcons.Refresh) }
        item { ZdFootnote("Needs a USB-OTG adapter and a UVC-class camera. Some cameras need a powered hub.") }
    }
}

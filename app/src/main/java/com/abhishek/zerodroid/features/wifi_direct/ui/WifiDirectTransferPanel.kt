package com.abhishek.zerodroid.features.wifi_direct.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.wifi_direct.domain.TransferHistoryEntry
import com.abhishek.zerodroid.features.wifi_direct.domain.TransferState
import com.abhishek.zerodroid.features.wifi_direct.domain.WifiDirectFileTransfer
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val GROUP_OWNER_IP = "192.168.49.1"

@Composable
fun WifiDirectTransferPanel(
    isGroupOwner: Boolean,
    groupOwnerAddress: String?,
    transfer: WifiDirectFileTransfer,
    modifier: Modifier = Modifier
) {
    val progress by transfer.progress.collectAsState()
    val history by transfer.history.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var targetIp by remember { mutableStateOf(if (!isGroupOwner) GROUP_OWNER_IP else "") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        selectedFileUri = uri
        selectedFileName = uri?.lastPathSegment?.substringAfterLast('/') ?: uri?.toString()
    }

    val isTransferActive = progress.state == TransferState.Transferring ||
            progress.state == TransferState.WaitingForConnection ||
            progress.state == TransferState.Connecting

    Column(modifier = modifier.fillMaxWidth()) {
        // Role Indicator
        RoleIndicatorSection(isGroupOwner = isGroupOwner)

        Spacer(modifier = Modifier.height(12.dp))

        // Transfer Progress (shown when active or completed/failed)
        AnimatedVisibility(
            visible = progress.state != TransferState.Idle,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                TransferProgressSection(
                    progress = progress,
                    onCancel = { transfer.cancel() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Send File Section
        AnimatedVisibility(visible = !isTransferActive) {
            Column {
                SendFileSection(
                    selectedFileName = selectedFileName,
                    targetIp = targetIp,
                    isGroupOwner = isGroupOwner,
                    onSelectFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                    onTargetIpChanged = { targetIp = it },
                    onSend = {
                        val uri = selectedFileUri ?: return@SendFileSection
                        val address = targetIp.ifBlank { return@SendFileSection }
                        scope.launch {
                            transfer.sendFile(address, uri)
                        }
                    },
                    canSend = selectedFileUri != null && targetIp.isNotBlank()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Receive File Section
        AnimatedVisibility(visible = !isTransferActive) {
            Column {
                ReceiveFileSection(
                    onStartListening = {
                        scope.launch { transfer.startReceiving() }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Transfer History
        if (history.isNotEmpty()) {
            TransferHistorySection(history = history)
        }
    }
}

@Composable
private fun RoleIndicatorSection(isGroupOwner: Boolean) {
    TerminalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> DEVICE ROLE:",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isGroupOwner) "GROUP OWNER (SERVER)" else "CLIENT",
                color = if (isGroupOwner) TerminalAmber else TerminalCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SendFileSection(
    selectedFileName: String?,
    targetIp: String,
    isGroupOwner: Boolean,
    onSelectFile: () -> Unit,
    onTargetIpChanged: (String) -> Unit,
    onSend: () -> Unit,
    canSend: Boolean
) {
    TerminalCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> SEND FILE",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // File selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSelectFile,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilePresent,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Select File",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = selectedFileName ?: "No file selected",
                    color = if (selectedFileName != null) TerminalGreen else TerminalAmber.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Target IP
            OutlinedTextField(
                value = targetIp,
                onValueChange = onTargetIpChanged,
                label = {
                    Text(
                        text = "Target IP Address",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                },
                placeholder = {
                    Text(
                        text = if (isGroupOwner) "Enter client IP" else GROUP_OWNER_IP,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TerminalGreen,
                    unfocusedTextColor = TerminalGreen,
                    cursorColor = TerminalGreen,
                    focusedBorderColor = TerminalCyan,
                    unfocusedBorderColor = TerminalGreen.copy(alpha = 0.3f),
                    focusedLabelColor = TerminalCyan,
                    unfocusedLabelColor = TerminalGreen.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Send button
            Button(
                onClick = onSend,
                enabled = canSend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerminalGreen,
                    disabledContainerColor = TerminalGreen.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SEND",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ReceiveFileSection(onStartListening: () -> Unit) {
    TerminalCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> RECEIVE FILE",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Open a server socket to accept incoming file transfers from the connected peer.",
                color = TerminalGreen.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStartListening,
                colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START LISTENING",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun TransferProgressSection(
    progress: com.abhishek.zerodroid.features.wifi_direct.domain.TransferProgress,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waiting_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val stateColor = when (progress.state) {
        TransferState.Completed -> TerminalGreen
        TransferState.Failed -> TerminalRed
        TransferState.WaitingForConnection -> TerminalAmber
        TransferState.Connecting -> TerminalCyan
        TransferState.Transferring -> TerminalCyan
        TransferState.Idle -> TerminalGreen
    }

    val stateText = when (progress.state) {
        TransferState.Idle -> "IDLE"
        TransferState.WaitingForConnection -> "WAITING FOR CONNECTION..."
        TransferState.Connecting -> "CONNECTING..."
        TransferState.Transferring -> "TRANSFERRING"
        TransferState.Completed -> "TRANSFER COMPLETE"
        TransferState.Failed -> "TRANSFER FAILED"
    }

    val isWaiting = progress.state == TransferState.WaitingForConnection ||
            progress.state == TransferState.Connecting

    val isActive = progress.state == TransferState.Transferring ||
            progress.state == TransferState.WaitingForConnection ||
            progress.state == TransferState.Connecting

    TerminalCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> TRANSFER STATUS",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (isWaiting) Modifier.alpha(pulseAlpha) else Modifier
            ) {
                Icon(
                    imageVector = when (progress.state) {
                        TransferState.Completed -> Icons.Default.CheckCircle
                        TransferState.Failed -> Icons.Default.Error
                        TransferState.Transferring -> Icons.Default.CloudUpload
                        else -> Icons.Default.CloudDownload
                    },
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stateText,
                    color = stateColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // File name
            if (progress.fileName.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "File: ${progress.fileName}",
                    color = TerminalGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Progress bar for active transfer
            if (progress.state == TransferState.Transferring) {
                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { progress.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = TerminalCyan,
                    trackColor = TerminalGreen.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatBytes(progress.bytesTransferred)} / ${formatBytes(progress.totalBytes)}",
                        color = TerminalGreen.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${(progress.progressPercent * 100).toInt()}%",
                        color = TerminalCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Speed
                if (progress.speedBytesPerSec > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Speed: ${formatBytes(progress.speedBytesPerSec)}/s",
                        color = TerminalAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            // Error message
            if (progress.error != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Error: ${progress.error}",
                    color = TerminalRed,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            // Cancel button
            if (isActive) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CANCEL",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferHistorySection(history: List<TransferHistoryEntry>) {
    TerminalCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> TRANSFER HISTORY",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.height((history.size.coerceAtMost(5) * 56).dp)
            ) {
                items(history) { entry ->
                    TransferHistoryItem(entry = entry)
                    HorizontalDivider(
                        color = TerminalGreen.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferHistoryItem(entry: TransferHistoryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (entry.isSent) Icons.Default.CloudUpload else Icons.Default.CloudDownload,
            contentDescription = null,
            tint = if (entry.success) TerminalGreen else TerminalRed,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.fileName.ifBlank { "Unknown" },
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row {
                Text(
                    text = if (entry.isSent) "Sent" else "Received",
                    color = TerminalCyan.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatBytes(entry.fileSize),
                    color = TerminalAmber.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimestamp(entry.timestamp),
                    color = TerminalGreen.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }

        Icon(
            imageVector = if (entry.success) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (entry.success) TerminalGreen else TerminalRed,
            modifier = Modifier.size(14.dp)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

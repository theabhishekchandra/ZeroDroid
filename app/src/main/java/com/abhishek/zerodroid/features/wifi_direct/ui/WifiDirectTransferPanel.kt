package com.abhishek.zerodroid.features.wifi_direct.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdTextField
import com.abhishek.zerodroid.features.wifi_direct.domain.TransferHistoryEntry
import com.abhishek.zerodroid.features.wifi_direct.domain.TransferProgress
import com.abhishek.zerodroid.features.wifi_direct.domain.TransferState
import com.abhishek.zerodroid.features.wifi_direct.domain.WifiDirectFileTransfer
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The group owner's address on every Wi-Fi Direct group Android creates. */
private const val GROUP_OWNER_IP = "192.168.49.1"

private data class PickedFile(val uri: Uri, val name: String, val size: Long?)

/** Send one file to the other device in the group, or wait to receive one. */
@Composable
fun WifiDirectTransferPanel(
    isGroupOwner: Boolean,
    @Suppress("UNUSED_PARAMETER") groupOwnerAddress: String?,
    transfer: WifiDirectFileTransfer,
    modifier: Modifier = Modifier
) {
    val progress by transfer.progress.collectAsState()
    val history by transfer.history.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var picked by remember { mutableStateOf<PickedFile?>(null) }
    // Clients always reach the owner at .49.1; the owner has to be told the client's address.
    var targetIp by remember(isGroupOwner) { mutableStateOf(if (isGroupOwner) "" else GROUP_OWNER_IP) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        picked = uri?.let { describe(context, it) }
    }
    val active = progress.state in setOf(TransferState.WaitingForConnection, TransferState.Connecting, TransferState.Transferring)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (progress.state != TransferState.Idle) {
            ProgressCard(progress, onCancel = transfer::cancel)
        }

        if (!active) {
            ZdCard(verticalSpacing = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ZdIconTile(ZdIcons.Document, tint = ZdColors.Text2, background = ZdColors.Surface2)
                    Column(Modifier.weight(1f)) {
                        Text(picked?.name ?: "No file chosen", style = ZdType.Label, color = ZdColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            picked?.size?.let { formatBytes(it) } ?: "Any file; it goes straight to the other phone",
                            style = ZdType.Caption,
                            color = ZdColors.Text3
                        )
                    }
                    ZdButton(if (picked == null) "Choose" else "Change", onClick = { picker.launch(arrayOf("*/*")) }, variant = ZdButtonVariant.Ghost, height = 36.dp)
                }
                if (isGroupOwner) {
                    ZdTextField(
                        value = targetIp,
                        onValueChange = { targetIp = it.trim() },
                        label = "SEND TO (CLIENT IP)",
                        placeholder = "192.168.49.x",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZdButton(
                        "Send file",
                        onClick = {
                            val file = picked ?: return@ZdButton
                            scope.launch { transfer.sendFile(targetIp, file.uri) }
                        },
                        enabled = picked != null && targetIp.isNotBlank(),
                        icon = ZdIcons.Send,
                        modifier = Modifier.weight(1f)
                    )
                    ZdButton(
                        "Receive",
                        onClick = { scope.launch { transfer.startReceiving() } },
                        variant = ZdButtonVariant.Secondary,
                        icon = ZdIcons.Download,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            ZdFootnote(
                if (isGroupOwner) "You’re the group owner. Tap Receive to wait for a file, or enter the client’s IP (shown on its screen) to send."
                else "Tap Receive on the other phone first, then Send here."
            )
        }

        if (history.isNotEmpty()) {
            ZdSectionLabel("This session", trailingText = "${history.size}")
            ZdListCard(history.reversed()) { HistoryRow(it) }
        }
    }
}

@Composable
private fun ProgressCard(progress: TransferProgress, onCancel: () -> Unit) {
    val failed = progress.state == TransferState.Failed
    val done = progress.state == TransferState.Completed
    ZdCard(
        borderColor = when {
            failed -> ZdColors.Critical.copy(alpha = 0.4f)
            done -> ZdColors.AccentBorder
            else -> ZdColors.Border
        },
        verticalSpacing = 10.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(ZdIcons.Document, contentDescription = null, tint = ZdColors.Text2, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    progress.fileName.ifBlank {
                        when (progress.state) {
                            TransferState.WaitingForConnection -> "Waiting for the other phone…"
                            TransferState.Connecting -> "Connecting…"
                            else -> "Transfer"
                        }
                    },
                    style = ZdType.Label,
                    color = ZdColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(progressLine(progress), style = ZdType.Caption, color = if (failed) ZdColors.Critical else ZdColors.Text3)
            }
            when {
                done -> ZdSeverityBadge(ZdSeverity.CLEAN, label = "DONE")
                failed -> ZdSeverityBadge(ZdSeverity.HIGH, label = "FAILED")
                else -> ZdButton("Cancel", onClick = onCancel, variant = ZdButtonVariant.Ghost, height = 36.dp)
            }
        }
        if (progress.state == TransferState.Transferring || done) {
            LinearProgressIndicator(
                progress = { if (done) 1f else progress.progressPercent },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)),
                color = ZdColors.Accent,
                trackColor = ZdColors.Surface3
            )
        }
    }
}

/** "42.1 of 68.0 MB · 9.8 MB/s · 3 s left". */
internal fun progressLine(p: TransferProgress): String = when (p.state) {
    TransferState.Idle -> ""
    TransferState.WaitingForConnection -> "Listening on port ${WifiDirectFileTransfer.PORT}"
    TransferState.Connecting -> "Opening a connection"
    TransferState.Failed -> p.error ?: "The connection dropped"
    TransferState.Completed -> formatBytes(p.totalBytes.takeIf { it > 0 } ?: p.bytesTransferred)
    TransferState.Transferring -> buildList {
        add(if (p.totalBytes > 0) "${formatBytes(p.bytesTransferred)} of ${formatBytes(p.totalBytes)}" else formatBytes(p.bytesTransferred))
        if (p.speedBytesPerSec > 0) {
            add("${formatBytes(p.speedBytesPerSec)}/s")
            if (p.totalBytes > p.bytesTransferred) add("${(p.totalBytes - p.bytesTransferred) / p.speedBytesPerSec} s left")
        }
    }.joinToString(" · ")
}

@Composable
private fun HistoryRow(entry: TransferHistoryEntry) {
    ZdListRow(
        title = entry.fileName.ifBlank { "Unnamed file" },
        titleMono = false,
        subtitle = "${if (entry.isSent) "Sent" else "Received"} · ${formatBytes(entry.fileSize)} · ${timeFormat.format(Date(entry.timestamp))}",
        leading = {
            ZdIconTile(
                if (entry.isSent) ZdIcons.Send else ZdIcons.Download,
                tint = if (entry.success) ZdColors.Accent else ZdColors.Critical,
                background = if (entry.success) ZdColors.AccentBg else ZdColors.CriticalBg
            )
        },
        trailing = { if (!entry.success) ZdSeverityBadge(ZdSeverity.HIGH, label = "FAILED") }
    )
}

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

internal fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
}

/** The picked file's real name and size, from the document provider. */
private fun describe(context: Context, uri: Uri): PickedFile {
    var name: String? = null
    var size: Long? = null
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                name = c.getString(0)
                size = if (c.isNull(1)) null else c.getLong(1)
            }
        }
    }
    return PickedFile(uri, name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file", size)
}

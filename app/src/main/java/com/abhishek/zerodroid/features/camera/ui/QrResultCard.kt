package com.abhishek.zerodroid.features.camera.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.features.camera.domain.QrContentType
import com.abhishek.zerodroid.features.camera.domain.QrScanResult
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

/**
 * A decoded code with its verdict first: the link is shown but never opened until the user
 * chooses to, and dangerous ones say why.
 */
@Composable
fun QrResultCard(
    result: QrScanResult,
    modifier: Modifier = Modifier,
    showActions: Boolean = true
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    ZdCard(
        modifier = modifier,
        borderColor = if (result.isThreat) ZdColors.CriticalBorder else ZdColors.Border
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (result.isThreat) ZdSeverityBadge(ZdSeverity.CRITICAL, label = "DANGEROUS")
            else ZdSeverityBadge(ZdSeverity.CLEAN, label = "NO WARNINGS")
            ZdTag(result.contentType.displayName.uppercase())
            ZdTag(result.format)
        }
        Text(result.parsedContent, style = ZdType.Label, color = ZdColors.Text, maxLines = 5)
        if (result.isThreat) {
            Text(result.threatReason ?: "Suspicious content", style = ZdType.BodySmall, color = ZdColors.Critical)
        }
        if (result.rawValue != result.parsedContent) {
            Text(result.rawValue, style = ZdType.Path, color = ZdColors.Text3, maxLines = 2)
        }
        if (showActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZdButton(
                    "Copy",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("QR", result.rawValue))
                    },
                    variant = ZdButtonVariant.Secondary,
                    icon = ZdIcons.Copy,
                    modifier = Modifier.weight(1f)
                )
                if (result.contentType == QrContentType.URL) {
                    ZdButton(
                        if (result.isThreat) "Open anyway" else "Open",
                        onClick = { runCatching { uriHandler.openUri(result.rawValue) } },
                        variant = if (result.isThreat) ZdButtonVariant.Danger else ZdButtonVariant.Primary,
                        icon = ZdIcons.Globe,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

package com.abhishek.zerodroid.features.camera.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconButton
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdTextField
import com.abhishek.zerodroid.features.camera.domain.QrGenerator
import com.abhishek.zerodroid.features.camera.viewmodel.QrGeneratorInputType
import com.abhishek.zerodroid.features.camera.viewmodel.QrScannerViewModel
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.io.File

/** Make a QR code for text, a link or a WiFi network, then save or share it as an image. */
@Composable
fun QrGeneratorPanel(
    viewModel: QrScannerViewModel,
    modifier: Modifier = Modifier
) {
    val gen by viewModel.generatorState.collectAsState()
    val context = LocalContext.current
    var note by remember { mutableStateOf<String?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ZdChipRow(contentPadding = 0.dp) {
            QrGeneratorInputType.entries.forEach { type ->
                ZdChip(type.displayName, selected = gen.inputType == type, onClick = { viewModel.setGeneratorInputType(type) })
            }
        }

        ZdCard(verticalSpacing = 12.dp) {
            when (gen.inputType) {
                QrGeneratorInputType.TEXT -> ZdTextField(
                    value = gen.textInput,
                    onValueChange = viewModel::setGeneratorText,
                    label = "TEXT",
                    placeholder = "Anything up to a few hundred characters",
                    minLines = 3
                )
                QrGeneratorInputType.URL -> ZdTextField(
                    value = gen.textInput,
                    onValueChange = viewModel::setGeneratorText,
                    label = "LINK",
                    placeholder = "https://example.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                QrGeneratorInputType.WIFI -> {
                    ZdTextField(value = gen.wifiSsid, onValueChange = viewModel::setWifiSsid, label = "NETWORK NAME", placeholder = "Guest_Net")
                    if (gen.wifiSecurity != QrGenerator.WifiSecurity.NONE) {
                        ZdTextField(
                            value = gen.wifiPassword,
                            onValueChange = viewModel::setWifiPassword,
                            label = "PASSWORD",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }
                    Text("SECURITY", style = ZdType.Path, color = ZdColors.Text3)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        QrGenerator.WifiSecurity.entries.forEach { sec ->
                            ZdChip(sec.displayName, selected = gen.wifiSecurity == sec, onClick = { viewModel.setWifiSecurity(sec) })
                        }
                    }
                }
            }
            ZdButton("Make QR code", onClick = viewModel::generateQrCode, enabled = gen.canGenerate, icon = ZdIcons.Qr, modifier = Modifier.fillMaxWidth())
            gen.errorMessage?.let { Text(it, style = ZdType.Caption, color = ZdColors.Critical) }
        }

        gen.generatedBitmap?.let { bitmap ->
            ZdCard(verticalSpacing = 12.dp) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR code for ${gen.encodedContent}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(14.dp)
                    )
                }
                Text(
                    if (gen.inputType == QrGeneratorInputType.WIFI) "Scan to join ${gen.wifiSsid}" else gen.encodedContent,
                    style = ZdType.Mono,
                    color = ZdColors.Text2,
                    maxLines = 3
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ZdButton(
                            "Save image",
                            onClick = { note = if (saveToPictures(context, bitmap)) "Saved to Pictures/ZeroDroid" else "Couldn’t save the image" },
                            variant = ZdButtonVariant.Secondary,
                            icon = ZdIcons.Download,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ZdButton("Share", onClick = { shareImage(context, bitmap) }, icon = ZdIcons.Share, modifier = Modifier.weight(1f))
                    ZdIconButton(ZdIcons.Copy, contentDescription = "Copy text", onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("QR content", gen.encodedContent))
                        note = "Text copied"
                    })
                }
                note?.let { Text(it, style = ZdType.Caption, color = ZdColors.Accent) }
            }
        }

        if (gen.inputType == QrGeneratorInputType.WIFI) {
            ZdFootnote("WiFi codes include the password in plain text: anyone who photographs the code can join.")
        }
    }
}

private fun qrFile(context: Context): File =
    File(context.cacheDir, "exports").apply { mkdirs() }.resolve("qr_${System.currentTimeMillis()}.png")

private fun shareImage(context: Context, bitmap: Bitmap) {
    val file = qrFile(context)
    runCatching {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Share QR code").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Android 10+ only: MediaStore needs no storage permission there. */
private fun saveToPictures(context: Context, bitmap: Bitmap): Boolean = runCatching {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "zerodroid_qr_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ZeroDroid")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } != null
}.getOrDefault(false)

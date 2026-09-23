package com.abhishek.zerodroid.features.nfc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdTextField
import com.abhishek.zerodroid.features.nfc.domain.WriteResult
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

/** Write a Text or URL NDEF record to the last tag held to the phone. */
@Composable
fun NfcWritePanel(
    writeResult: WriteResult?,
    onWriteText: (String) -> Unit,
    onWriteUri: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isUrl by rememberSaveable { mutableStateOf(true) }
    var value by rememberSaveable { mutableStateOf("") }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ZdCard(verticalSpacing = 12.dp) {
            Text("Record type", style = ZdType.Label, color = ZdColors.Text2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZdChip("URL", selected = isUrl, onClick = { isUrl = true })
                ZdChip("Text", selected = !isUrl, onClick = { isUrl = false })
            }
            ZdTextField(
                value = value,
                onValueChange = { value = it },
                label = if (isUrl) "URL" else "TEXT",
                placeholder = if (isUrl) "https://example.com" else "Hello from ZeroDroid"
            )
            Text("${value.toByteArray().size} bytes", style = ZdType.Path, color = ZdColors.Text3)
            ZdButton(
                "Write to tag",
                onClick = { if (isUrl) onWriteUri(value) else onWriteText(value) },
                enabled = value.isNotBlank(),
                icon = ZdIcons.Nfc,
                modifier = Modifier.fillMaxWidth()
            )
        }
        when (writeResult) {
            WriteResult.Success -> ZdFootnote("Written. Tap the tag with another phone to check it.", icon = ZdIcons.Check)
            is WriteResult.Error -> ZdFootnote(writeResult.message, icon = ZdIcons.Warning)
            null -> ZdFootnote("Hold a writable tag to the back of the phone first, then tap Write while it’s still there.")
        }
    }
}

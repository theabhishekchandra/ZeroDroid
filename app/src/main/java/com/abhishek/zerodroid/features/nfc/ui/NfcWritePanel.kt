package com.abhishek.zerodroid.features.nfc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.nfc.domain.WriteResult

@Composable
fun NfcWritePanel(
    writeResult: WriteResult?,
    onWriteText: (String) -> Unit,
    onWriteUri: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember { mutableStateOf("") }
    var uriValue by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )

    TerminalCard(modifier = modifier) {
        Text(
            text = "> Write NDEF",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Tap an NFC tag to write",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = textValue,
            onValueChange = { textValue = it },
            label = { Text("Text content") },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onWriteText(textValue) },
            enabled = textValue.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Write Text") }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uriValue,
            onValueChange = { uriValue = it },
            label = { Text("URI") },
            placeholder = { Text("https://") },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onWriteUri(uriValue) },
            enabled = uriValue.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Write URI") }

        writeResult?.let { result ->
            Spacer(modifier = Modifier.height(4.dp))
            val (text, color) = when (result) {
                is WriteResult.Success -> "Written successfully" to MaterialTheme.colorScheme.primary
                is WriteResult.Error -> result.message to MaterialTheme.colorScheme.error
            }
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

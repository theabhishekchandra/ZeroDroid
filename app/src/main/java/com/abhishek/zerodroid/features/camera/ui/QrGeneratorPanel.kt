package com.abhishek.zerodroid.features.camera.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.camera.domain.QrGenerator
import com.abhishek.zerodroid.features.camera.viewmodel.QrGeneratorInputType
import com.abhishek.zerodroid.features.camera.viewmodel.QrScannerViewModel
import com.abhishek.zerodroid.ui.theme.TerminalCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrGeneratorPanel(
    viewModel: QrScannerViewModel,
    modifier: Modifier = Modifier
) {
    val genState by viewModel.generatorState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "> QR Generator", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        // Input type selector chips
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QrGeneratorInputType.entries.forEach { inputType ->
                FilterChip(
                    selected = genState.inputType == inputType,
                    onClick = { viewModel.setGeneratorInputType(inputType) },
                    label = { Text(inputType.displayName, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (inputType) {
                                QrGeneratorInputType.TEXT -> Icons.Default.TextFields
                                QrGeneratorInputType.URL -> Icons.Default.Link
                                QrGeneratorInputType.WIFI -> Icons.Default.Wifi
                            },
                            contentDescription = null, modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TerminalCard {
            when (genState.inputType) {
                QrGeneratorInputType.TEXT -> {
                    OutlinedTextField(
                        value = genState.textInput, onValueChange = { viewModel.setGeneratorText(it) },
                        label = { Text("Text content") }, placeholder = { Text("Enter text to encode...") },
                        colors = fieldColors, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6
                    )
                }
                QrGeneratorInputType.URL -> {
                    OutlinedTextField(
                        value = genState.textInput, onValueChange = { viewModel.setGeneratorText(it) },
                        label = { Text("URL") }, placeholder = { Text("https://example.com") },
                        colors = fieldColors, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                QrGeneratorInputType.WIFI -> {
                    val securityExpanded = remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = genState.wifiSsid, onValueChange = { viewModel.setWifiSsid(it) },
                        label = { Text("SSID (Network Name)") }, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = genState.wifiPassword, onValueChange = { viewModel.setWifiPassword(it) },
                        label = { Text("Password") }, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = securityExpanded.value,
                        onExpandedChange = { securityExpanded.value = it }
                    ) {
                        OutlinedTextField(
                            value = genState.wifiSecurity.displayName, onValueChange = {}, readOnly = true,
                            label = { Text("Security") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = securityExpanded.value) },
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = securityExpanded.value,
                            onDismissRequest = { securityExpanded.value = false }
                        ) {
                            QrGenerator.WifiSecurity.entries.forEach { security ->
                                DropdownMenuItem(
                                    text = { Text(security.displayName) },
                                    onClick = { viewModel.setWifiSecurity(security); securityExpanded.value = false }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.generateQrCode() }, enabled = genState.canGenerate,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Generate QR Code") }

            genState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        genState.generatedBitmap?.let { bitmap ->
            TerminalCard {
                Text(text = "> Generated QR Code", style = MaterialTheme.typography.labelSmall, color = TerminalCyan)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        .background(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(), contentDescription = "Generated QR Code",
                        modifier = Modifier.fillMaxSize().padding(16.dp), contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = genState.encodedContent, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(genState.encodedContent)) }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, genState.encodedContent)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share QR content")
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

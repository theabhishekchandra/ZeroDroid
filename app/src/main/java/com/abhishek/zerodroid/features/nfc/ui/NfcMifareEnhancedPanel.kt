package com.abhishek.zerodroid.features.nfc.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.nfc.domain.MifareClassicReader
import com.abhishek.zerodroid.features.nfc.domain.MifareSectorData
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

private val MonoFont = FontFamily.Monospace

private val HexCharRegex = Regex("^[0-9A-Fa-f]*$")

private fun ByteArray.toHexDisplay(): String = joinToString("") { "%02X".format(it) }

private fun String.hexToByteArray(): ByteArray? {
    val clean = replace(" ", "").uppercase()
    if (clean.length % 2 != 0) return null
    if (!clean.matches(Regex("^[0-9A-F]*$"))) return null
    return ByteArray(clean.length / 2) { i ->
        clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

private fun byteToAscii(b: Byte): Char {
    val c = b.toInt() and 0xFF
    return if (c in 0x20..0x7E) c.toChar() else '.'
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NfcMifareEnhancedPanel(
    sectors: List<MifareSectorData>,
    onAddCustomKey: (ByteArray) -> Unit,
    onRemoveCustomKey: (Int) -> Unit,
    customKeys: List<ByteArray>,
    onCopyDump: (String) -> Unit,
    onWriteBlock: (blockIndex: Int, data: ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    val reader = remember { MifareClassicReader() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ==================== Custom Keys Input ====================
        CustomKeysSection(
            customKeys = customKeys,
            onAddCustomKey = onAddCustomKey,
            onRemoveCustomKey = onRemoveCustomKey
        )

        // ==================== Key Dictionary ====================
        KeyDictionarySection(customKeys = customKeys)

        // ==================== Export Button ====================
        if (sectors.isNotEmpty()) {
            ExportSection(
                sectors = sectors,
                reader = reader,
                onCopyDump = onCopyDump
            )
        }

        // ==================== Sector Dump Display ====================
        if (sectors.isNotEmpty()) {
            SectorDumpSection(
                sectors = sectors,
                reader = reader,
                onWriteBlock = onWriteBlock
            )
        }
    }
}

// ===================================================================
// Custom Keys Section
// ===================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomKeysSection(
    customKeys: List<ByteArray>,
    onAddCustomKey: (ByteArray) -> Unit,
    onRemoveCustomKey: (Int) -> Unit
) {
    var keyInput by remember { mutableStateOf("") }
    var keyError by remember { mutableStateOf<String?>(null) }

    TerminalCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = TerminalAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CUSTOM KEYS",
                    color = TerminalAmber,
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isLetterOrDigit() }.uppercase()
                        if (filtered.length <= 12 && (filtered.isEmpty() || HexCharRegex.matches(filtered))) {
                            keyInput = filtered
                            keyError = null
                        }
                    },
                    placeholder = {
                        Text(
                            text = "FFFFFFFFFFFF",
                            color = TerminalGreen.copy(alpha = 0.3f),
                            fontFamily = MonoFont,
                            fontSize = 13.sp
                        )
                    },
                    textStyle = TextStyle(
                        color = TerminalGreen,
                        fontFamily = MonoFont,
                        fontSize = 13.sp
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TerminalGreen,
                        unfocusedBorderColor = TerminalGreen.copy(alpha = 0.4f),
                        cursorColor = TerminalGreen
                    ),
                    isError = keyError != null,
                    supportingText = if (keyError != null) {
                        {
                            Text(
                                text = keyError!!,
                                color = TerminalRed,
                                fontFamily = MonoFont,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        {
                            Text(
                                text = "${keyInput.length}/12 hex chars (6 bytes)",
                                color = TerminalGreen.copy(alpha = 0.5f),
                                fontFamily = MonoFont,
                                fontSize = 11.sp
                            )
                        }
                    }
                )

                Button(
                    onClick = {
                        if (keyInput.length != 12) {
                            keyError = "Key must be 12 hex chars (6 bytes)"
                            return@Button
                        }
                        val bytes = keyInput.hexToByteArray()
                        if (bytes == null || bytes.size != 6) {
                            keyError = "Invalid hex key"
                            return@Button
                        }
                        // Check for duplicates
                        val hex = bytes.toHexDisplay()
                        val isDuplicate = customKeys.any { it.toHexDisplay() == hex }
                        if (isDuplicate) {
                            keyError = "Key already added"
                            return@Button
                        }
                        onAddCustomKey(bytes)
                        keyInput = ""
                        keyError = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalGreen.copy(alpha = 0.15f),
                        contentColor = TerminalGreen
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Key",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ADD",
                        fontFamily = MonoFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Custom keys list
            if (customKeys.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    customKeys.forEachIndexed { index, key ->
                        CustomKeyChip(
                            keyHex = key.toHexDisplay(),
                            onRemove = { onRemoveCustomKey(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomKeyChip(
    keyHex: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = TerminalCyan.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = TerminalCyan.copy(alpha = 0.4f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = keyHex,
            color = TerminalCyan,
            fontFamily = MonoFont,
            fontSize = 12.sp
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove key",
            tint = TerminalRed.copy(alpha = 0.7f),
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .clickable { onRemove() }
        )
    }
}

// ===================================================================
// Key Dictionary Section
// ===================================================================

@Composable
private fun KeyDictionarySection(
    customKeys: List<ByteArray>
) {
    var showAllKeys by remember { mutableStateOf(false) }
    val defaultCount = MifareClassicReader.DEFAULT_KEYS.size
    val customCount = customKeys.size
    val totalCount = defaultCount + customCount

    TerminalCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAllKeys = !showAllKeys },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showAllKeys) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = TerminalGreen.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KEY DICTIONARY",
                        color = TerminalGreen,
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$totalCount keys ($defaultCount default + $customCount custom)",
                        color = TerminalGreen.copy(alpha = 0.6f),
                        fontFamily = MonoFont,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showAllKeys) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showAllKeys) "Hide keys" else "Show keys",
                        tint = TerminalGreen.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = showAllKeys,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (customKeys.isNotEmpty()) {
                        Text(
                            text = "-- Custom Keys --",
                            color = TerminalCyan.copy(alpha = 0.7f),
                            fontFamily = MonoFont,
                            fontSize = 11.sp
                        )
                        customKeys.forEachIndexed { index, key ->
                            Text(
                                text = "  [${index + 1}] ${key.toHexDisplay()}",
                                color = TerminalCyan,
                                fontFamily = MonoFont,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = "-- Default Keys --",
                        color = TerminalGreen.copy(alpha = 0.7f),
                        fontFamily = MonoFont,
                        fontSize = 11.sp
                    )
                    MifareClassicReader.DEFAULT_KEYS.forEachIndexed { index, key ->
                        Text(
                            text = "  [${index + 1}] ${key.toHexDisplay()}",
                            color = TerminalGreen.copy(alpha = 0.8f),
                            fontFamily = MonoFont,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ===================================================================
// Export Section
// ===================================================================

@Composable
private fun ExportSection(
    sectors: List<MifareSectorData>,
    reader: MifareClassicReader,
    onCopyDump: (String) -> Unit
) {
    val authCount = sectors.count { it.isAuthenticated }

    TerminalCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "DUMP SUMMARY",
                    color = TerminalGreen,
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "$authCount/${sectors.size} sectors authenticated",
                    color = if (authCount == sectors.size) TerminalGreen else TerminalAmber,
                    fontFamily = MonoFont,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    val dump = reader.formatDump(sectors)
                    onCopyDump(dump)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerminalCyan.copy(alpha = 0.15f),
                    contentColor = TerminalCyan
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "COPY DUMP",
                    fontFamily = MonoFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ===================================================================
// Sector Dump Display
// ===================================================================

@Composable
private fun SectorDumpSection(
    sectors: List<MifareSectorData>,
    reader: MifareClassicReader,
    onWriteBlock: (blockIndex: Int, data: ByteArray) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sectors.forEach { sector ->
            SectorCard(
                sector = sector,
                reader = reader,
                onWriteBlock = onWriteBlock
            )
        }
    }
}

@Composable
private fun SectorCard(
    sector: MifareSectorData,
    reader: MifareClassicReader,
    onWriteBlock: (blockIndex: Int, data: ByteArray) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAccessBits by remember { mutableStateOf(false) }

    TerminalCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Sector header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (sector.isAuthenticated) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (sector.isAuthenticated) TerminalGreen else TerminalRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SECTOR ${sector.sectorIndex}",
                        color = if (sector.isAuthenticated) TerminalGreen else TerminalRed,
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sector.isAuthenticated) {
                        Text(
                            text = "Key ${sector.keyType}: ${sector.keyUsed}",
                            color = TerminalGreen.copy(alpha = 0.6f),
                            fontFamily = MonoFont,
                            fontSize = 10.sp
                        )
                    } else {
                        Text(
                            text = "NO AUTH",
                            color = TerminalRed.copy(alpha = 0.7f),
                            fontFamily = MonoFont,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = TerminalGreen.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Expanded block data
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (!sector.isAuthenticated) {
                        Text(
                            text = "Authentication failed - no keys matched",
                            color = TerminalRed.copy(alpha = 0.7f),
                            fontFamily = MonoFont,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        // Column headers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "BLK",
                                color = TerminalGreen.copy(alpha = 0.5f),
                                fontFamily = MonoFont,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp)
                            )
                            Text(
                                text = "HEX DATA",
                                color = TerminalGreen.copy(alpha = 0.5f),
                                fontFamily = MonoFont,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(290.dp)
                            )
                            Text(
                                text = "ASCII",
                                color = TerminalGreen.copy(alpha = 0.5f),
                                fontFamily = MonoFont,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(
                            color = TerminalGreen.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        sector.blocks.forEachIndexed { index, block ->
                            val isTrailer = index == sector.blocks.lastIndex

                            BlockRow(
                                block = block,
                                isTrailer = isTrailer,
                                isAuthenticated = sector.isAuthenticated,
                                onWriteBlock = onWriteBlock
                            )
                        }

                        // Access bits interpretation for trailer block
                        val trailerBlock = sector.blocks.lastOrNull()
                        if (trailerBlock != null && trailerBlock.data.size >= 10) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAccessBits = !showAccessBits },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showAccessBits) "[-] Access Bits" else "[+] Access Bits",
                                    color = TerminalAmber.copy(alpha = 0.8f),
                                    fontFamily = MonoFont,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            AnimatedVisibility(
                                visible = showAccessBits,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .background(
                                            color = TerminalAmber.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    val accessBitsInfo = reader.interpretAccessBits(trailerBlock.data)
                                    accessBitsInfo.forEach { line ->
                                        Text(
                                            text = line,
                                            color = TerminalAmber.copy(alpha = 0.9f),
                                            fontFamily = MonoFont,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockRow(
    block: com.abhishek.zerodroid.features.nfc.domain.MifareBlockData,
    isTrailer: Boolean,
    isAuthenticated: Boolean,
    onWriteBlock: (blockIndex: Int, data: ByteArray) -> Unit
) {
    var showWriteDialog by remember { mutableStateOf(false) }

    val blockColor = if (isTrailer) TerminalAmber else TerminalGreen
    val blockLabel = if (isTrailer) "[T]" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Block index
        Text(
            text = "%3d".format(block.blockIndex),
            color = blockColor.copy(alpha = 0.7f),
            fontFamily = MonoFont,
            fontSize = 11.sp,
            modifier = Modifier.width(36.dp)
        )

        if (block.hexString == "READ ERROR") {
            Text(
                text = "[READ ERROR]",
                color = TerminalRed,
                fontFamily = MonoFont,
                fontSize = 11.sp
            )
        } else {
            // Hex data
            Text(
                text = buildAnnotatedString {
                    if (isTrailer && block.data.size >= 16) {
                        // Color-code trailer: Key A (6 bytes) | Access bits (4 bytes) | Key B (6 bytes)
                        withStyle(SpanStyle(color = TerminalCyan)) {
                            append(block.data.take(6).joinToString(" ") { "%02X".format(it) })
                        }
                        append(" ")
                        withStyle(SpanStyle(color = TerminalAmber)) {
                            append(block.data.drop(6).take(4).joinToString(" ") { "%02X".format(it) })
                        }
                        append(" ")
                        withStyle(SpanStyle(color = TerminalCyan)) {
                            append(block.data.drop(10).take(6).joinToString(" ") { "%02X".format(it) })
                        }
                    } else {
                        withStyle(SpanStyle(color = blockColor)) {
                            append(block.hexString)
                        }
                    }
                },
                fontFamily = MonoFont,
                fontSize = 11.sp,
                modifier = Modifier.width(290.dp)
            )

            // ASCII column
            Text(
                text = block.data.map { byteToAscii(it) }.joinToString(""),
                color = blockColor.copy(alpha = 0.5f),
                fontFamily = MonoFont,
                fontSize = 11.sp,
                modifier = Modifier.width(100.dp)
            )

            // Trailer label
            if (isTrailer) {
                Text(
                    text = blockLabel,
                    color = TerminalAmber,
                    fontFamily = MonoFont,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Write button for data blocks (not trailer) if authenticated
            if (!isTrailer && isAuthenticated) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { showWriteDialog = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Write to block ${block.blockIndex}",
                        tint = TerminalAmber.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }

    // Write dialog
    if (showWriteDialog) {
        WriteBlockDialog(
            blockIndex = block.blockIndex,
            currentData = block.hexString.replace(" ", ""),
            onDismiss = { showWriteDialog = false },
            onConfirmWrite = { data ->
                onWriteBlock(block.blockIndex, data)
                showWriteDialog = false
            }
        )
    }
}

// ===================================================================
// Write Block Dialog
// ===================================================================

@Composable
private fun WriteBlockDialog(
    blockIndex: Int,
    currentData: String,
    onDismiss: () -> Unit,
    onConfirmWrite: (ByteArray) -> Unit
) {
    var hexInput by remember { mutableStateOf(currentData) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmStep by remember { mutableIntStateOf(0) } // 0=input, 1=confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D0D0D),
        titleContentColor = TerminalAmber,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = TerminalAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (confirmStep == 0) "WRITE BLOCK $blockIndex" else "CONFIRM WRITE",
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        text = {
            Column {
                if (confirmStep == 0) {
                    Text(
                        text = "Enter 32 hex characters (16 bytes) to write:",
                        color = TerminalGreen.copy(alpha = 0.8f),
                        fontFamily = MonoFont,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isLetterOrDigit() }.uppercase()
                            if (filtered.length <= 32 && (filtered.isEmpty() || HexCharRegex.matches(filtered))) {
                                hexInput = filtered
                                error = null
                            }
                        },
                        textStyle = TextStyle(
                            color = TerminalGreen,
                            fontFamily = MonoFont,
                            fontSize = 12.sp
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerminalAmber,
                            unfocusedBorderColor = TerminalAmber.copy(alpha = 0.4f),
                            cursorColor = TerminalAmber
                        ),
                        isError = error != null,
                        supportingText = {
                            Text(
                                text = error ?: "${hexInput.length}/32 hex chars",
                                color = if (error != null) TerminalRed else TerminalGreen.copy(alpha = 0.5f),
                                fontFamily = MonoFont,
                                fontSize = 10.sp
                            )
                        }
                    )
                } else {
                    // Confirmation step
                    Text(
                        text = "WARNING: This will permanently overwrite block $blockIndex.",
                        color = TerminalRed,
                        fontFamily = MonoFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Data to write:",
                        color = TerminalGreen.copy(alpha = 0.7f),
                        fontFamily = MonoFont,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = TerminalAmber.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = TerminalAmber.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    ) {
                        // Format hex with spaces for readability
                        val formatted = hexInput.chunked(2).joinToString(" ")
                        Text(
                            text = formatted,
                            color = TerminalAmber,
                            fontFamily = MonoFont,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (confirmStep == 0) {
                Button(
                    onClick = {
                        if (hexInput.length != 32) {
                            error = "Must be exactly 32 hex chars"
                            return@Button
                        }
                        val bytes = hexInput.hexToByteArray()
                        if (bytes == null || bytes.size != 16) {
                            error = "Invalid hex data"
                            return@Button
                        }
                        confirmStep = 1
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalAmber.copy(alpha = 0.2f),
                        contentColor = TerminalAmber
                    )
                ) {
                    Text(
                        text = "NEXT",
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            } else {
                Button(
                    onClick = {
                        val bytes = hexInput.hexToByteArray()
                        if (bytes != null && bytes.size == 16) {
                            onConfirmWrite(bytes)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalRed.copy(alpha = 0.3f),
                        contentColor = TerminalRed
                    )
                ) {
                    Text(
                        text = "WRITE",
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (confirmStep == 1) {
                    confirmStep = 0
                } else {
                    onDismiss()
                }
            }) {
                Text(
                    text = if (confirmStep == 1) "BACK" else "CANCEL",
                    color = TerminalGreen.copy(alpha = 0.7f),
                    fontFamily = MonoFont,
                    fontSize = 12.sp
                )
            }
        }
    )
}

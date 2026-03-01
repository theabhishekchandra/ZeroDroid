package com.abhishek.zerodroid.features.ble.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishek.zerodroid.R
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ble.domain.HciPacket
import com.abhishek.zerodroid.features.ble.domain.HciPacketType
import com.abhishek.zerodroid.features.ble.domain.HciSnoopLog
import com.abhishek.zerodroid.features.ble.domain.toHexDump
import com.abhishek.zerodroid.features.ble.viewmodel.HciSnoopState
import com.abhishek.zerodroid.features.ble.viewmodel.HciSnoopViewModel
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun HciSnoopPanel(viewModel: HciSnoopViewModel) {
    val state by viewModel.state.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.loadFromUri(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Instructions card
        item {
            TerminalCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "BLE HCI Snoop Log Analyzer",
                        color = TerminalGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Go to Settings > Developer Options\n" +
                                "2. Enable \"Bluetooth HCI snoop log\"\n" +
                                "3. Toggle Bluetooth OFF then ON\n" +
                                "4. Reproduce BLE activity, then load the log",
                        color = TerminalGreen.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Action buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.loadLog() },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading
                ) {
                    Text(
                        text = "Load Log",
                        color = TerminalGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
                OutlinedButton(
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading
                ) {
                    Text(
                        text = "Select File",
                        color = TerminalCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Loading indicator
        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = TerminalGreen,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Parsing HCI snoop log...",
                            color = TerminalGreen.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Error display
        state.error?.let { error ->
            item {
                TerminalCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ERROR",
                            color = TerminalRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error,
                            color = TerminalRed.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Log info + filters + packets
        state.log?.let { log ->
            // File info card
            item {
                LogInfoCard(log = log, loadedFrom = state.loadedFromPath)
            }

            // Filter chips
            item {
                FilterChipRow(
                    currentFilter = state.filter,
                    onFilterSelected = { viewModel.setFilter(it) },
                    log = log
                )
            }

            // Packet list
            val filteredPackets = if (state.filter != null) {
                log.packets.filter { it.packetType == state.filter }
            } else {
                log.packets
            }

            items(
                items = filteredPackets,
                key = { it.index }
            ) { packet ->
                PacketCard(packet = packet)
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LogInfoCard(log: HciSnoopLog, loadedFrom: String?) {
    TerminalCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "LOG INFO",
                color = TerminalAmber,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))

            val fileSizeStr = when {
                log.fileSize < 0 -> "unknown"
                log.fileSize < 1024 -> "${log.fileSize} B"
                log.fileSize < 1024 * 1024 -> "${log.fileSize / 1024} KB"
                else -> String.format("%.1f MB", log.fileSize / (1024.0 * 1024.0))
            }

            val cmdCount = log.packets.count { it.packetType == HciPacketType.Command }
            val evtCount = log.packets.count { it.packetType == HciPacketType.Event }
            val aclCount = log.packets.count { it.packetType == HciPacketType.AclData }

            val infoLines = buildString {
                loadedFrom?.let { append("Source: $it\n") }
                append("Packets: ${log.packetCount}  |  Size: $fileSizeStr\n")
                append("Version: ${log.version}  |  Datalink: ${log.datalinkType}\n")
                append("CMD: $cmdCount  |  EVT: $evtCount  |  ACL: $aclCount")
            }

            Text(
                text = infoLines,
                color = TerminalGreen.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    currentFilter: HciPacketType?,
    onFilterSelected: (HciPacketType?) -> Unit,
    log: HciSnoopLog
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        data class ChipInfo(
            val label: String,
            val filter: HciPacketType?,
            val color: Color,
            val count: Int
        )

        val chips = listOf(
            ChipInfo("All", null, TerminalGreen, log.packetCount),
            ChipInfo("CMD", HciPacketType.Command, TerminalAmber,
                log.packets.count { it.packetType == HciPacketType.Command }),
            ChipInfo("ACL", HciPacketType.AclData, TerminalCyan,
                log.packets.count { it.packetType == HciPacketType.AclData }),
            ChipInfo("EVT", HciPacketType.Event, TerminalAmber,
                log.packets.count { it.packetType == HciPacketType.Event })
        )

        chips.forEach { chip ->
            FilterChip(
                selected = currentFilter == chip.filter,
                onClick = { onFilterSelected(chip.filter) },
                label = {
                    Text(
                        text = "${chip.label} (${chip.count})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (currentFilter == chip.filter) Color.Black else chip.color
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chip.color,
                    containerColor = Color.Transparent
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = chip.color.copy(alpha = 0.5f),
                    enabled = true,
                    selected = currentFilter == chip.filter
                )
            )
        }
    }
}

@Composable
private fun PacketCard(packet: HciPacket) {
    var expanded by remember { mutableStateOf(false) }

    val accentColor = when {
        packet.summary.contains("ATT Error") || packet.summary.startsWith("ATT Error") -> TerminalRed
        packet.packetType == HciPacketType.Command || packet.packetType == HciPacketType.Event -> TerminalAmber
        packet.isSent -> TerminalCyan
        else -> TerminalGreen
    }

    val directionArrow = if (packet.isSent) "\u2192" else "\u2190"
    val directionLabel = if (packet.isSent) "TX" else "RX"

    // Format timestamp: btsnoop timestamps are microseconds since 0000-01-01
    // We display relative time as HH:mm:ss.SSS from the raw micros
    val totalSeconds = packet.timestampMicros / 1_000_000
    val millis = (packet.timestampMicros % 1_000_000) / 1_000
    val hours = (totalSeconds / 3600) % 24
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val timeStr = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)

    TerminalCard {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(10.dp)
        ) {
            // Top row: index, direction, type badge, timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Packet index
                Text(
                    text = "#${packet.index}",
                    color = TerminalGreen.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))

                // Direction arrow
                Text(
                    text = "$directionArrow $directionLabel",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))

                // Packet type badge
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = packet.packetType.label,
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Timestamp
                Text(
                    text = timeStr,
                    color = TerminalGreen.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Summary line
            Text(
                text = packet.summary,
                color = accentColor.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )

            // Expanded hex dump
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Size info
                    Text(
                        text = "orig=${packet.originalLength} incl=${packet.includedLength} bytes",
                        color = TerminalGreen.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Hex dump
                    Surface(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Text(
                            text = packet.data.toHexDump(),
                            color = TerminalGreen.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 13.sp,
                            modifier = Modifier
                                .padding(8.dp)
                                .horizontalScroll(scrollState)
                        )
                    }
                }
            }
        }
    }
}

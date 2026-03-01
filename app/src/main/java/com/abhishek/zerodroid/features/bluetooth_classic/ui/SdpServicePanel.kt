package com.abhishek.zerodroid.features.bluetooth_classic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SdpServiceInfo
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

/**
 * Panel that displays SDP service discovery results for a Bluetooth Classic device.
 *
 * Shows a header with the device name, a button to trigger live SDP queries,
 * and a scrollable list of discovered services with profile names, UUIDs,
 * and descriptions.
 *
 * @param deviceAddress MAC address of the target device
 * @param deviceName Display name of the target device, or null if unknown
 * @param services List of discovered SDP services to display
 * @param isQuerying Whether an SDP query is currently in progress
 * @param isCached Whether the currently displayed results are from cache
 * @param onQuerySdp Callback invoked when the user taps "Query SDP"
 * @param onDismiss Callback invoked when the user dismisses the panel
 * @param modifier Modifier applied to the root composable
 */
@Composable
fun SdpServicePanel(
    deviceAddress: String,
    deviceName: String?,
    services: List<SdpServiceInfo>,
    isQuerying: Boolean,
    isCached: Boolean = false,
    onQuerySdp: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    TerminalCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // -- Header --
            SdpHeader(
                deviceName = deviceName,
                deviceAddress = deviceAddress,
                onDismiss = onDismiss
            )

            Spacer(modifier = Modifier.height(12.dp))

            // -- Query Button --
            SdpQueryButton(
                isQuerying = isQuerying,
                onQuerySdp = onQuerySdp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // -- Source indicator --
            AnimatedVisibility(
                visible = services.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = if (isCached) "[cached results]" else "[live SDP results]",
                    color = if (isCached) TerminalAmber else TerminalCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // -- Service List --
            if (services.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(services, key = { it.uuid }) { service ->
                        SdpServiceCard(service = service)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // -- Summary --
                HorizontalDivider(color = TerminalGreen.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "> Found ${services.size} service${if (services.size != 1) "s" else ""}",
                    color = TerminalGreen,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            } else if (!isQuerying) {
                Text(
                    text = "> No services discovered. Tap \"Query SDP\" to scan.",
                    color = TerminalAmber,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SdpHeader(
    deviceName: String?,
    deviceAddress: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SDP Services",
                color = TerminalGreen,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = deviceName ?: "Unknown Device",
                color = TerminalCyan,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = deviceAddress,
                color = TerminalAmber.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        TextButton(onClick = onDismiss) {
            Text(
                text = "[X]",
                color = TerminalRed,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SdpQueryButton(
    isQuerying: Boolean,
    onQuerySdp: () -> Unit
) {
    TextButton(
        onClick = onQuerySdp,
        enabled = !isQuerying,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isQuerying) TerminalAmber.copy(alpha = 0.5f) else TerminalGreen,
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isQuerying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = TerminalAmber,
                    strokeWidth = 1.5.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Querying SDP...",
                    color = TerminalAmber,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text(
                    text = "> Query SDP",
                    color = TerminalGreen,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SdpServiceCard(service: SdpServiceInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = TerminalGreen.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile name
            Text(
                text = service.profileName,
                color = TerminalGreen,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Short UUID badge
            Box(
                modifier = Modifier
                    .background(
                        color = TerminalCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = TerminalCyan.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = service.shortUuid,
                    color = TerminalCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Full UUID
        Text(
            text = service.uuid,
            color = TerminalAmber.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Description
        Text(
            text = service.description,
            color = TerminalGreen.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

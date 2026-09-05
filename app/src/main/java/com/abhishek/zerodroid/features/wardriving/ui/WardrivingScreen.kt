package com.abhishek.zerodroid.features.wardriving.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.features.wardriving.viewmodel.WardrivingViewModel

@Composable
fun WardrivingScreen(
    viewModel: WardrivingViewModel = hiltViewModel()
) {
    PermissionGate(
        permissions = PermissionUtils.wardrivingPermissions(),
        rationale = "Location and Bluetooth permissions are needed to log WiFi networks with GPS coordinates."
    ) {
        WardrivingContent(viewModel)
    }
}

@Composable
private fun WardrivingContent(viewModel: WardrivingViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> Wardriving",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.isScanning) {
                        OutlinedButton(
                            onClick = { viewModel.stopSession() },
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Stop", maxLines = 1, softWrap = false) }
                    } else {
                        Button(
                            onClick = { viewModel.startSession() },
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text("Start", maxLines = 1, softWrap = false) }
                    }

                    if (state.session != null && !state.isScanning) {
                        Button(
                            onClick = {
                                viewModel.exportCsv { uri ->
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "wardriving_export.csv")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Export CSV"))
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text("Export", maxLines = 1, softWrap = false) }
                    }
                }
            }
        }

        state.error?.let { error ->
            item {
                Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        state.session?.let { session ->
            item { WardrivingStatsCard(session = session) }
        }

        state.stats?.let { stats ->
            if (stats.totalRecords > 0) {
                item { WardrivingDashboard(stats = stats) }
            }
        }

        state.exportStatus?.let { status ->
            item {
                Text(text = status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (state.records.isNotEmpty()) {
            item {
                Text(
                    text = "> Recent Records",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(state.records.takeLast(50).reversed()) { record ->
                WardrivingRecordItem(record = record)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

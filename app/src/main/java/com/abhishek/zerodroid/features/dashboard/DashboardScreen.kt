package com.abhishek.zerodroid.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.navigation.ScreenCategory
import com.abhishek.zerodroid.navigation.ZeroDroidScreen
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TextDim

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val hardwareItems by viewModel.hardwareItems.collectAsState()
    val lastUsed by viewModel.lastUsedFeature.collectAsState()
    val deviceInfo = viewModel.deviceInfo

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Device info
        item {
            SectionHeader(title = "> DEVICE")
        }
        item {
            TerminalCard {
                InfoRow("Model", deviceInfo.model)
                InfoRow("Android", deviceInfo.androidVersion)
                InfoRow("Device", deviceInfo.device)
                InfoRow("Board", deviceInfo.board)
            }
        }

        // Hardware capabilities — compact 2-column grid inside one card
        item {
            SectionHeader(title = "> HARDWARE")
        }
        item {
            TerminalCard {
                val rows = hardwareItems.chunked(2)
                rows.forEachIndexed { index, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        pair.forEach { item ->
                            HardwareChipRow(
                                name = item.name,
                                isAvailable = item.isAvailable,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Pad with empty space if odd count on last row
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Quick scan (last used feature)
        item {
            SectionHeader(title = "> QUICK SCAN")
        }
        item {
            val feature = lastUsed
            TerminalCard(
                animated = feature != null,
                onClick = if (feature != null) {
                    { onNavigate(feature.route) }
                } else null
            ) {
                if (feature != null) {
                    Text(
                        text = "Last used: ${feature.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to resume \u2192",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerminalGreen
                    )
                } else {
                    Text(
                        text = "No recent scans",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDim
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a tool below to get started",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim
                    )
                }
            }
        }

        // Toolkit - feature grid grouped by category
        item {
            SectionHeader(title = "> TOOLKIT")
        }

        val orderedCategories = listOf(
            ScreenCategory.WIRELESS,
            ScreenCategory.RF,
            ScreenCategory.SENSORS,
            ScreenCategory.NETWORK,
            ScreenCategory.SECURITY
        )

        orderedCategories.forEach { category ->
            val screens = (ZeroDroidScreen.byCategory[category] ?: return@forEach)
                .filter { it != ZeroDroidScreen.Dashboard }
            item {
                Text(
                    text = "/* ${category.label.uppercase()} */",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim,
                    letterSpacing = 2.sp
                )
            }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    screens.forEach { screen ->
                        FeatureTile(
                            screen = screen,
                            onClick = {
                                viewModel.saveLastUsed(screen.route, screen.title)
                                onNavigate(screen.route)
                            }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = TextDim,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HardwareChipRow(
    name: String,
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val statusText = if (isAvailable) "ONLINE" else "OFFLINE"
    val statusColor = if (isAvailable) TerminalGreen else TerminalRed

    Row(
        modifier = modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "[$statusText]",
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun FeatureTile(
    screen: ZeroDroidScreen,
    onClick: () -> Unit
) {
    TerminalCard(
        modifier = Modifier.width(100.dp),
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = screen.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                fontSize = 10.sp
            )
        }
    }
}

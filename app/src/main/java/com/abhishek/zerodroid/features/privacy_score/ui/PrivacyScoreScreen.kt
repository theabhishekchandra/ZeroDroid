package com.abhishek.zerodroid.features.privacy_score.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.privacy_score.domain.CheckCategory
import com.abhishek.zerodroid.features.privacy_score.domain.CheckStatus
import com.abhishek.zerodroid.features.privacy_score.domain.PrivacyCheck
import com.abhishek.zerodroid.features.privacy_score.domain.PrivacyScoreState
import com.abhishek.zerodroid.features.privacy_score.viewmodel.PrivacyScoreViewModel
import com.abhishek.zerodroid.ui.theme.SurfaceVariantDark
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TextPrimary
import com.abhishek.zerodroid.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Entry Point ────────────────────────────────────────────────────────

@Composable
fun PrivacyScoreScreen(
    viewModel: PrivacyScoreViewModel = viewModel(factory = PrivacyScoreViewModel.Factory)
) {
    val permissions = buildList {
        addAll(PermissionUtils.wifiPermissions())
        addAll(PermissionUtils.blePermissions())
    }.distinct()

    PermissionGate(
        permissions = permissions,
        rationale = "WiFi, Bluetooth, and location permissions are required to scan your environment and calculate a privacy score."
    ) {
        PrivacyScoreContent(viewModel)
    }
}

// ── Main Content ───────────────────────────────────────────────────────

@Composable
private fun PrivacyScoreContent(viewModel: PrivacyScoreViewModel) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Score circle
        item {
            ScoreCircleSection(state)
        }

        // Scan button
        item {
            ScanButton(state, viewModel)
        }

        // Status summary
        if (state.score >= 0) {
            item {
                StatusSummaryCard(state)
            }
        }

        // Category breakdown
        if (state.categoryScores.isNotEmpty()) {
            item {
                CategoryBreakdownCard(state)
            }
        }

        // Check details grouped by category
        if (state.checks.isNotEmpty()) {
            val groupedChecks = state.checks.groupBy { it.category }
            CheckCategory.entries.forEach { category ->
                val checks = groupedChecks[category] ?: return@forEach
                item {
                    CategoryHeader(category)
                }
                items(checks, key = { "${it.category.name}_${it.name}" }) { check ->
                    CheckRow(check)
                }
            }
        }

        // Empty state
        if (state.score < 0 && !state.isScanning) {
            item {
                EmptyState(
                    icon = Icons.Default.Shield,
                    title = "Privacy Score",
                    subtitle = "Tap SCAN to analyze your device security and environment"
                )
            }
        }

        // Error display
        state.error?.let { error ->
            item {
                TerminalCard(glowColor = TerminalRed, borderColor = TerminalRed) {
                    Text(
                        text = "> ERROR: $error",
                        color = TerminalRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Last scanned timestamp
        state.lastScanTime?.let { timestamp ->
            item {
                Text(
                    text = "Last scanned: ${formatTimestamp(timestamp)}",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ── Score Circle ───────────────────────────────────────────────────────

@Composable
private fun ScoreCircleSection(state: PrivacyScoreState) {
    val displayScore = if (state.score >= 0) state.score else 0
    val animatedScore by animateFloatAsState(
        targetValue = displayScore.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "scoreAnim"
    )

    val scoreColor by animateColorAsState(
        targetValue = scoreToColor(displayScore),
        animationSpec = tween(durationMillis = 500),
        label = "scoreColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            // Arc background and score arc
            val strokeWidth = 16.dp
            val arcColor = scoreColor
            val bgColor = SurfaceVariantDark

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .drawBehind {
                        val sw = strokeWidth.toPx()
                        val arcSize = Size(size.width - sw, size.height - sw)
                        val arcOffset = Offset(sw / 2f, sw / 2f)

                        // Background arc
                        drawArc(
                            color = bgColor,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = arcOffset,
                            size = arcSize,
                            style = Stroke(width = sw, cap = StrokeCap.Round)
                        )

                        // Score arc
                        if (state.score >= 0) {
                            val scoreAngle = (animatedScore / 100f) * 270f
                            drawArc(
                                color = arcColor,
                                startAngle = 135f,
                                sweepAngle = scoreAngle,
                                useCenter = false,
                                topLeft = arcOffset,
                                size = arcSize,
                                style = Stroke(width = sw, cap = StrokeCap.Round)
                            )
                        }
                    }
            )

            // Center text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.isScanning && state.score < 0) {
                    ScanningIndicator(
                        isScanning = true,
                        label = "Scanning",
                        color = TerminalGreen
                    )
                } else {
                    Text(
                        text = if (state.score >= 0) "${animatedScore.toInt()}" else "--",
                        color = if (state.score >= 0) scoreColor else TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp
                    )
                    Text(
                        text = state.grade,
                        color = if (state.score >= 0) scoreColor else TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

// ── Scan Button ────────────────────────────────────────────────────────

@Composable
private fun ScanButton(state: PrivacyScoreState, viewModel: PrivacyScoreViewModel) {
    val hasScanned = state.lastScanTime != null

    Button(
        onClick = {
            if (hasScanned) viewModel.rescan() else viewModel.startScan()
        },
        enabled = !state.isScanning,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (state.isScanning) TextSecondary else TerminalGreen,
            disabledContainerColor = TextSecondary.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (hasScanned) Icons.Default.Refresh else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.Black
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when {
                state.isScanning -> "SCANNING..."
                hasScanned -> "RESCAN"
                else -> "SCAN"
            },
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ── Status Summary ─────────────────────────────────────────────────────

@Composable
private fun StatusSummaryCard(state: PrivacyScoreState) {
    val borderColor = scoreToColor(state.score)

    TerminalCard(
        glowColor = borderColor,
        borderColor = borderColor,
        animated = state.isScanning
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusBadge(
                count = state.passCount,
                label = "PASS",
                color = TerminalGreen
            )
            StatusBadge(
                count = state.warnCount,
                label = "WARN",
                color = TerminalAmber
            )
            StatusBadge(
                count = state.failCount,
                label = "FAIL",
                color = TerminalRed
            )
        }

        if (state.isScanning) {
            ScanningIndicator(
                isScanning = true,
                label = "Analyzing ${state.checks.size}/17 checks...",
                color = TerminalGreen,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun StatusBadge(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Text(
            text = label,
            color = color.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Category Breakdown ─────────────────────────────────────────────────

@Composable
private fun CategoryBreakdownCard(state: PrivacyScoreState) {
    TerminalCard {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "> Category Breakdown",
                color = TerminalGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            CheckCategory.entries.forEach { category ->
                val categoryScore = state.categoryScores[category] ?: 0
                CategoryBar(
                    label = category.label,
                    score = categoryScore,
                    weight = category.weightPercent
                )
            }
        }
    }
}

@Composable
private fun CategoryBar(label: String, score: Int, weight: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "catBar_$label"
    )
    val barColor = scoreToColor(score)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label
        Text(
            text = label,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.width(72.dp)
        )

        // Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .background(SurfaceVariantDark, RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(12.dp)
                    .background(barColor, RoundedCornerShape(6.dp))
            )
        }

        // Score & weight
        Text(
            text = "${score}%",
            color = barColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = "(${weight}%)",
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.width(36.dp)
        )
    }
}

// ── Category Header ────────────────────────────────────────────────────

@Composable
private fun CategoryHeader(category: CheckCategory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(TerminalGreen, CircleShape)
        )
        Text(
            text = "> ${category.label} Checks",
            color = TerminalGreen,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

// ── Check Row ──────────────────────────────────────────────────────────

@Composable
private fun CheckRow(check: PrivacyCheck) {
    val statusColor = when (check.status) {
        CheckStatus.PASS -> TerminalGreen
        CheckStatus.WARNING -> TerminalAmber
        CheckStatus.FAIL -> TerminalRed
    }
    val statusIcon = when (check.status) {
        CheckStatus.PASS -> Icons.Default.CheckCircle
        CheckStatus.WARNING -> Icons.Default.Warning
        CheckStatus.FAIL -> Icons.Default.Error
    }

    TerminalCard(
        glowColor = statusColor,
        borderColor = statusColor.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = check.status.name,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = check.name,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = check.status.name,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Detail
            Text(
                text = check.detail,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )

            // Recommendation
            check.recommendation?.let { rec ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            statusColor.copy(alpha = 0.08f),
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp,
                            statusColor.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = ">>",
                        color = statusColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = rec,
                        color = statusColor.copy(alpha = 0.9f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ── Utilities ──────────────────────────────────────────────────────────

private fun scoreToColor(score: Int): Color = when {
    score >= 70 -> TerminalGreen
    score >= 40 -> TerminalAmber
    else -> TerminalRed
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

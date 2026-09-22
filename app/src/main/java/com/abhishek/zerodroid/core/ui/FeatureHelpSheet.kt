package com.abhishek.zerodroid.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdDivider
import com.abhishek.zerodroid.core.ui.zd.ZdIconButton
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSignalBars
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import kotlinx.coroutines.launch

private const val FULL_GUIDE_URL = "https://github.com/theabhishekchandra/ZeroDroid/blob/main/docs/TOOLS.md"

/**
 * "How this tool works": what to do, how to read what you see, and the limits worth knowing.
 * Opened from the help button in every tool header.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureHelpSheet(
    featureKey: String,
    onDismiss: () -> Unit,
    icon: ImageVector = ZdIcons.Help
) {
    val help = HelpContent.features[featureKey] ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ZdColors.Surface,
        scrimColor = ZdColors.Scrim,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ZdColors.BorderStrong)
            )
        }
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ZdIconTile(icon, size = 40.dp, iconSize = 20.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(help.title, style = ZdType.Heading, color = ZdColors.Text)
                    Text(help.description, style = ZdType.Caption, color = ZdColors.Text3)
                }
                ZdIconButton(ZdIcons.Close, contentDescription = "Close help", onClick = close)
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (help.steps.isNotEmpty()) {
                    HelpSection("How to use") {
                        help.steps.forEachIndexed { i, step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${i + 1}", style = ZdType.Label.copy(fontWeight = ZdType.Display.fontWeight), color = ZdColors.Accent, modifier = Modifier.width(20.dp))
                                Text(step, style = ZdType.BodySmall.copy(fontSize = ZdType.Body.fontSize.times(0.94f)), color = ZdColors.Text2)
                            }
                        }
                    }
                } else if (help.capabilities.isNotEmpty()) {
                    HelpSection("What it does") {
                        help.capabilities.forEach { line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(ZdIcons.Check, contentDescription = null, tint = ZdColors.Accent, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                Text(line, style = ZdType.BodySmall.copy(fontSize = ZdType.Body.fontSize.times(0.94f)), color = ZdColors.Text2)
                            }
                        }
                    }
                }

                if (help.legend.isNotEmpty()) {
                    HelpSection("Reading the results") {
                        help.legend.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.width(64.dp)) { LegendMarkView(item) }
                                Text(item.meaning, style = ZdType.BodySmall, color = ZdColors.Text2)
                            }
                        }
                    }
                }

                val facts = help.facts.ifEmpty { help.tips.map { HelpFact(it, "") } }
                if (facts.isNotEmpty()) {
                    HelpSection("Good to know") {
                        ZdListCard(facts) { fact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(ZdIcons.Info, contentDescription = null, tint = ZdColors.Info, modifier = Modifier.size(18.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(fact.title, style = ZdType.BodySmall.copy(fontSize = ZdType.Body.fontSize.times(0.94f), fontWeight = ZdType.Label.fontWeight), color = ZdColors.Text)
                                    if (fact.detail.isNotEmpty()) Text(fact.detail, style = ZdType.Caption, color = ZdColors.Text3)
                                }
                            }
                        }
                    }
                }
            }

            ZdDivider()
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ZdButton(
                    "Full guide",
                    onClick = { uriHandler.openUri(FULL_GUIDE_URL) },
                    variant = ZdButtonVariant.Secondary,
                    icon = ZdIcons.Document,
                    modifier = Modifier.weight(1f)
                )
                ZdButton("Got it", onClick = close, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HelpSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ZdSectionLabel(label)
        content()
    }
}

@Composable
private fun LegendMarkView(item: HelpLegend) {
    when (item.mark) {
        LegendMark.SIGNAL_STRONG -> ZdSignalBars(4)
        LegendMark.SIGNAL_WEAK -> ZdSignalBars(1)
        LegendMark.TAG_GOOD -> ZdTag(item.label, color = ZdColors.Accent, background = ZdColors.AccentBg, border = ZdColors.AccentBg)
        LegendMark.TAG_WARN -> ZdTag(item.label, color = ZdColors.Medium, background = ZdColors.MediumBg, border = ZdColors.MediumBg)
        LegendMark.TAG_BAD -> ZdTag(item.label, color = ZdColors.Critical, background = ZdColors.CriticalBg, border = ZdColors.CriticalBg)
        LegendMark.TAG_INFO -> ZdTag(item.label, color = ZdColors.Info, background = ZdColors.InfoBg, border = ZdColors.InfoBg)
    }
}

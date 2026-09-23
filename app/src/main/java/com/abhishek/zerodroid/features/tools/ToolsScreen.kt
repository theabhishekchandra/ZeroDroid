package com.abhishek.zerodroid.features.tools

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.ui.zd.ZdChip
import com.abhishek.zerodroid.core.ui.zd.ZdChipRow
import com.abhishek.zerodroid.core.ui.zd.ZdDivider
import com.abhishek.zerodroid.core.ui.zd.ZdHardwareTag
import com.abhishek.zerodroid.core.ui.zd.ZdHeader
import com.abhishek.zerodroid.core.ui.zd.ZdIconButton
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSwitchRow
import com.abhishek.zerodroid.core.ui.zd.ZdCardShape
import com.abhishek.zerodroid.navigation.ToolGroup
import com.abhishek.zerodroid.navigation.ToolInfo
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType

/**
 * Every tool, grouped by the job it does. Search, filter by goal, hide what this phone can't
 * run, and long-press to pin a tool to Home.
 */
@Composable
fun ToolsScreen(
    onOpenTool: (ToolInfo) -> Unit,
    onPinChanged: (ToolInfo, Boolean) -> Unit,
    onSearch: () -> Unit = {},
    onSettings: () -> Unit = {},
    /** On tablets the list sits beside the open tool, which is highlighted here. */
    selectedRoute: String? = null,
    viewModel: ToolsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        ZdHeader(path = "/tools", title = "Tools") {
            ZdIconButton(ZdIcons.Search, contentDescription = "Search everything", onClick = onSearch)
            ZdIconButton(ZdIcons.Settings, contentDescription = "Settings", onClick = onSettings)
        }

        Column(
            modifier = Modifier.padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToolSearchField(
                query = state.query,
                total = state.totalCount,
                onQueryChange = viewModel::setQuery,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            ZdChipRow {
                ZdChip("All", selected = state.group == null, onClick = { viewModel.setGroup(null) })
                ToolGroup.entries.forEach { group ->
                    ZdChip(group.chip, selected = state.group == group, onClick = { viewModel.setGroup(group) })
                }
            }
            ZdSwitchRow(
                label = "Only tools this phone supports",
                checked = state.onlySupported,
                onCheckedChange = viewModel::setOnlySupported,
                trailingText = "${state.shownCount} shown",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(state.sections, key = { it.group.name }) { section ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val n = section.rows.size
                    ZdSectionLabel(section.group.label, trailingText = if (n == 1) "1 tool" else "$n tools")
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(ZdCardShape)
                            .background(ZdColors.Surface)
                            .border(1.dp, ZdColors.Border, ZdCardShape)
                    ) {
                        section.rows.forEachIndexed { index, row ->
                            ToolListRow(
                                row = row,
                                selected = row.tool.route == selectedRoute,
                                onClick = {
                                    viewModel.onToolOpened(row.tool)
                                    onOpenTool(row.tool)
                                },
                                onLongClick = {
                                    val pinned = viewModel.togglePin(row.tool)
                                    onPinChanged(row.tool, pinned)
                                }
                            )
                            if (index < section.rows.lastIndex) ZdDivider()
                        }
                    }
                }
            }
            if (state.sections.isEmpty()) {
                item {
                    Text(
                        "No tools match. Try “tracker”, “wifi” or “nfc”.",
                        style = ZdType.BodySmall,
                        color = ZdColors.Text3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 16.dp)
                    )
                }
            } else {
                item {
                    Text(
                        "Long-press a tool to pin it to Home.",
                        style = ZdType.Caption,
                        color = ZdColors.Text3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolSearchField(
    query: String,
    total: Int,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(ZdColors.Surface)
            .border(1.dp, ZdColors.BorderStrong, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(ZdIcons.Search, contentDescription = null, tint = ZdColors.Text3, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Search $total tools…", style = ZdType.Button.copy(fontWeight = ZdType.Mono.fontWeight), color = ZdColors.Text3)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = ZdType.Button.copy(fontWeight = ZdType.Mono.fontWeight, color = ZdColors.Text),
                cursorBrush = SolidColor(ZdColors.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search tools" }
            )
        }
        if (query.isNotEmpty()) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { onQueryChange("") }
                    .semantics { contentDescription = "Clear search" },
                contentAlignment = Alignment.Center
            ) {
                Icon(ZdIcons.Close, contentDescription = null, tint = ZdColors.Text3, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolListRow(row: ToolRow, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val tool = row.tool
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(if (selected) ZdColors.AccentBg else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics {
                onLongClick(label = if (row.pinned) "Unpin from Home" else "Pin to Home") { onLongClick(); true }
            }
            .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
            .alpha(if (row.available) 1f else 0.55f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ZdIconTile(
            tool.icon,
            tint = if (row.available) ZdColors.Accent else ZdColors.Text3,
            background = if (row.available) ZdColors.AccentBg else ZdColors.Surface2,
            size = 38.dp
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tool.name, style = ZdType.Label, color = ZdColors.Text)
                if (row.pinned) {
                    Icon(ZdIcons.Pin, contentDescription = "Pinned", tint = ZdColors.Accent, modifier = Modifier.size(12.dp))
                }
            }
            Text(
                if (row.available) tool.job else "Not available on this phone",
                style = ZdType.Caption,
                color = ZdColors.Text3
            )
        }
        ZdHardwareTag(tool.requirement.tag, available = row.available)
    }
}

package com.abhishek.zerodroid.features.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.database.dao.SeenDeviceRow
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIconButton
import com.abhishek.zerodroid.core.ui.zd.ZdIconTile
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.navigation.ToolCatalog
import com.abhishek.zerodroid.navigation.ToolInfo
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dayFormat = SimpleDateFormat("d MMM", Locale.US)

/** Suggested tools shown before anything is typed. */
private val SUGGESTED = listOf("bluetooth_tracker", "hidden_camera", "wifi", "ble")

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenTool: (ToolInfo) -> Unit,
    onOpenDevice: (SeenDeviceRow) -> Unit,
    onOpenRoute: (String) -> Unit,
    onLearn: (ToolInfo) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    // Close the keyboard before leaving so its animation can't dismiss a help sheet on arrival.
    val go: (() -> Unit) -> Unit = { action -> keyboard?.hide(); action() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ZdIconButton(ZdIcons.Back, contentDescription = "Back", onClick = onBack)
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(shape)
                    .background(ZdColors.Surface)
                    .border(1.dp, ZdColors.Accent, shape)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(ZdIcons.Search, contentDescription = null, tint = ZdColors.Text3, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Tools, devices, actions, help…", style = ZdType.Button.copy(fontWeight = ZdType.Mono.fontWeight), color = ZdColors.Text3)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        singleLine = true,
                        textStyle = ZdType.Button.copy(fontWeight = ZdType.Mono.fontWeight, color = ZdColors.Text),
                        cursorBrush = SolidColor(ZdColors.Accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                            .semantics { contentDescription = "Search everything" }
                    )
                }
                if (query.isNotEmpty()) {
                    ZdIconButton(ZdIcons.Close, contentDescription = "Clear search", onClick = { viewModel.setQuery("") })
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (query.isBlank()) {
                item { ZdSectionLabel("Quick actions") }
                item {
                    ZdListCard(SearchViewModel.ACTIONS.take(3)) { action -> ActionRow(action) { go { onOpenRoute(action.route) } } }
                }
                item { ZdSectionLabel("Suggested tools") }
                item {
                    ZdListCard(SUGGESTED.mapNotNull { ToolCatalog.forRoute(it) }) { tool -> ToolRow(tool) { go { onOpenTool(tool) } } }
                }
                item { ZdFootnote("Search looks through tool names, every device saved in a session, and the help for all tools.") }
                return@LazyColumn
            }
            if (results.isEmpty) {
                item { ZdFootnote("Nothing matches “${query.trim()}”. Try a device name, a MAC prefix or a word like “tracker”.", icon = ZdIcons.Search) }
            }
            if (results.tools.isNotEmpty()) {
                item { ZdSectionLabel("Tools", trailingText = "${results.tools.size}") }
                item { ZdListCard(results.tools) { tool -> ToolRow(tool) { go { onOpenTool(tool) } } } }
            }
            if (results.devices.isNotEmpty()) {
                item { ZdSectionLabel("Devices seen", trailingText = "${results.devices.size}") }
                item {
                    ZdListCard(results.devices) { d ->
                        ZdListRow(
                            title = d.label.ifBlank { d.itemKey },
                            subtitle = "${d.itemKey} · last ${dayFormat.format(Date(d.lastSeen))}",
                            titleMono = false,
                            leading = { ZdIconTile(if (d.kind == "WIFI") ZdIcons.Wifi else ZdIcons.Bluetooth) },
                            trailing = { ZdTag("${d.sessionCount}×") },
                            onClick = { go { onOpenDevice(d) } }
                        )
                    }
                }
            }
            if (results.actions.isNotEmpty()) {
                item { ZdSectionLabel("Actions") }
                item { ZdListCard(results.actions) { action -> ActionRow(action) { go { onOpenRoute(action.route) } } } }
            }
            if (results.learn.isNotEmpty()) {
                item { ZdSectionLabel("Learn") }
                item {
                    ZdListCard(results.learn) { hit ->
                        ZdListRow(
                            title = hit.title,
                            titleMono = false,
                            supporting = { Text(hit.snippet, style = ZdType.Caption, color = ZdColors.Text2, maxLines = 3) },
                            leading = { ZdIconTile(ZdIcons.Help) },
                            showChevron = true,
                            onClick = { go { onLearn(hit.tool) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolRow(tool: ToolInfo, onClick: () -> Unit) {
    ZdListRow(
        title = tool.name,
        subtitle = tool.job,
        titleMono = false,
        leading = { ZdIconTile(tool.icon) },
        showChevron = true,
        onClick = onClick
    )
}

@Composable
private fun ActionRow(action: SearchAction, onClick: () -> Unit) {
    ZdListRow(
        title = action.label,
        subtitle = action.detail,
        titleMono = false,
        leading = { ZdIconTile(ZdIcons.Bolt, tint = ZdColors.Accent, background = ZdColors.AccentBg) },
        showChevron = true,
        onClick = onClick
    )
}

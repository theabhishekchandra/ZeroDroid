package com.abhishek.zerodroid.features.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.sessions.ItemKind
import com.abhishek.zerodroid.core.sessions.Session
import com.abhishek.zerodroid.core.sessions.SessionItem
import com.abhishek.zerodroid.core.sessions.SessionRepository
import com.abhishek.zerodroid.core.ui.zd.ZdBar
import com.abhishek.zerodroid.core.ui.zd.ZdBarChart
import com.abhishek.zerodroid.core.ui.zd.ZdButton
import com.abhishek.zerodroid.core.ui.zd.ZdButtonVariant
import com.abhishek.zerodroid.core.ui.zd.ZdCard
import com.abhishek.zerodroid.core.ui.zd.ZdCheckRow
import com.abhishek.zerodroid.core.ui.zd.ZdCheckStatus
import com.abhishek.zerodroid.core.ui.zd.ZdFootnote
import com.abhishek.zerodroid.core.ui.zd.ZdIcons
import com.abhishek.zerodroid.core.ui.zd.ZdListCard
import com.abhishek.zerodroid.core.ui.zd.ZdListRow
import com.abhishek.zerodroid.core.ui.zd.ZdSectionLabel
import com.abhishek.zerodroid.core.ui.zd.ZdSeverity
import com.abhishek.zerodroid.core.ui.zd.ZdSeverityBadge
import com.abhishek.zerodroid.core.ui.zd.ZdStat
import com.abhishek.zerodroid.core.ui.zd.ZdTag
import com.abhishek.zerodroid.core.ui.zd.signalBarsFor
import com.abhishek.zerodroid.core.util.formatAgo
import com.abhishek.zerodroid.ui.theme.ZdColors
import com.abhishek.zerodroid.ui.theme.ZdType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DeviceDetailState(
    val key: String = "",
    val label: String = "",
    val kind: ItemKind = ItemKind.BLE,
    /** Newest first. */
    val sightings: List<Pair<Session, SessionItem>> = emptyList()
)

/** One device or network across every session it appeared in. */
@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        DeviceDetailState(
            key = checkNotNull(savedStateHandle["key"]),
            label = savedStateHandle.get<String>("label").orEmpty(),
            kind = runCatching { ItemKind.valueOf(savedStateHandle.get<String>("kind").orEmpty()) }.getOrDefault(ItemKind.BLE)
        )
    )
    val state: StateFlow<DeviceDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(sightings = repository.sightings(_state.value.key))
        }
    }
}

private val whenFormat = SimpleDateFormat("EEE d MMM · HH:mm", Locale.US)

@Composable
fun DeviceDetailScreen(
    onOpenGatt: (address: String, name: String?) -> Unit,
    onOpenSession: (String) -> Unit,
    viewModel: DeviceDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val latest = state.sightings.firstOrNull()?.second
    val flagged = state.sightings.any { it.second.flagged }
    val isBle = state.kind == ItemKind.BLE || state.kind == ItemKind.TRACKER

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ZdCard {
                Row {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(state.label.ifBlank { state.key }, style = ZdType.Title, color = ZdColors.Text)
                        Text(state.key, style = ZdType.Path, color = ZdColors.Text3)
                    }
                    if (flagged) ZdSeverityBadge(ZdSeverity.HIGH, label = "FLAGGED") else ZdTag(state.kind.name)
                }
                if (state.sightings.isNotEmpty()) {
                    val first = state.sightings.last().first
                    Text(
                        "Seen in ${state.sightings.size} session${if (state.sightings.size == 1) "" else "s"}, first ${formatAgo(first.startedAt)}.",
                        style = ZdType.BodySmall,
                        color = ZdColors.Text2
                    )
                }
            }
        }
        val withSignal = state.sightings.filter { it.second.rssi != null }.reversed()
        if (withSignal.size > 1) {
            item {
                ZdCard {
                    Row {
                        Text("Signal across sessions", style = ZdType.Mono.copy(fontWeight = ZdType.Label.fontWeight), color = ZdColors.Text2, modifier = Modifier.weight(1f))
                        Text("${withSignal.last().second.rssi} dBm", style = ZdType.Label, color = ZdColors.Accent)
                    }
                    ZdBarChart(
                        bars = withSignal.takeLast(12).mapIndexed { i, (_, item) ->
                            val rssi = item.rssi ?: -100
                            ZdBar("${i + 1}", (rssi + 100).coerceAtLeast(1).toFloat(), if (signalBarsFor(rssi) >= 3) ZdColors.Accent else ZdColors.Medium)
                        },
                        contentDescription = "Signal strength in each session, oldest first"
                    )
                    Text("Taller is stronger. Getting stronger across sessions means it’s getting closer to where you scan.", style = ZdType.Caption, color = ZdColors.Text3)
                }
            }
        }
        if (latest != null) {
            item {
                ZdCard {
                    Row {
                        ZdStat("Last seen", formatAgo(state.sightings.first().first.startedAt), Modifier.weight(1f))
                        ZdStat("Last signal", latest.rssi?.let { "$it dBm" } ?: "—", Modifier.weight(1f))
                    }
                    Row {
                        ZdStat("Type", latest.detail.ifBlank { state.kind.name }, Modifier.weight(1f))
                        ZdStat("Sessions", "${state.sightings.size}", Modifier.weight(1f))
                    }
                }
            }
        }
        if (flagged) {
            item { ZdSectionLabel("Why it was flagged") }
            item {
                ZdListCard(state.sightings.filter { it.second.flagged }.map { it.second.detail }.distinct()) { reason ->
                    ZdCheckRow(title = reason.ifBlank { "Flagged by ${latest?.kind?.name ?: "a tool"}" }, status = ZdCheckStatus.WARN)
                }
            }
            if (state.kind == ItemKind.TRACKER) {
                item {
                    ZdCard(background = ZdColors.HighBg, borderColor = ZdColors.High.copy(alpha = 0.4f)) {
                        Text("If it isn’t yours", style = ZdType.Label, color = ZdColors.Text)
                        Text(
                            "Check bags, pockets, coat linings and your car. Most trackers can be made to play a sound from their owner’s app, and Android can scan for unknown trackers in Settings → Safety & emergency. If you feel unsafe, contact local authorities.",
                            style = ZdType.BodySmall,
                            color = ZdColors.Text2
                        )
                    }
                }
            }
        }
        if (isBle) {
            item {
                ZdButton(
                    "Open in GATT Explorer",
                    onClick = { onOpenGatt(state.key, state.label.takeIf { it.isNotBlank() && it != "[no name]" }) },
                    variant = ZdButtonVariant.Secondary,
                    icon = ZdIcons.Bluetooth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (state.sightings.isNotEmpty()) {
            item { ZdSectionLabel("Sessions", trailingText = "${state.sightings.size}") }
            item {
                ZdListCard(state.sightings) { (session, item) ->
                    ZdListRow(
                        title = listOfNotNull(session.title, session.place).joinToString(" · "),
                        subtitle = "${whenFormat.format(Date(session.startedAt))}${item.rssi?.let { " · $it dBm" } ?: ""}",
                        showChevron = true,
                        onClick = { onOpenSession(session.id) }
                    )
                }
            }
        } else {
            item { ZdFootnote("Not in any saved session yet. Sessions are saved when a scan stops.") }
        }
    }
}

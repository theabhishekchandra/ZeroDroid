package com.abhishek.zerodroid.features.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhishek.zerodroid.core.hardware.HardwareChecker
import com.abhishek.zerodroid.core.prefs.ToolPreferences
import com.abhishek.zerodroid.navigation.ToolCatalog
import com.abhishek.zerodroid.navigation.ToolGroup
import com.abhishek.zerodroid.navigation.ToolInfo
import com.abhishek.zerodroid.navigation.filterTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ToolRow(
    val tool: ToolInfo,
    val available: Boolean,
    val pinned: Boolean
)

data class ToolSection(
    val group: ToolGroup,
    val rows: List<ToolRow>
)

data class ToolsUiState(
    val query: String = "",
    val group: ToolGroup? = null,
    val onlySupported: Boolean = false,
    val sections: List<ToolSection> = emptyList(),
    val shownCount: Int = 0,
    val totalCount: Int = ToolCatalog.tools.size
)

@HiltViewModel
class ToolsViewModel @Inject constructor(
    hardwareChecker: HardwareChecker,
    private val toolPreferences: ToolPreferences
) : ViewModel() {

    private val availability: Map<String, Boolean> =
        ToolCatalog.tools.associate { it.route to it.requirement.isAvailable(hardwareChecker) }

    private data class Filters(val query: String = "", val group: ToolGroup? = null, val onlySupported: Boolean = false)

    private val filters = MutableStateFlow(Filters())

    val uiState: StateFlow<ToolsUiState> = combine(filters, toolPreferences.pinnedRoutes) { f, pinned ->
        buildState(f, pinned)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        buildState(Filters(), toolPreferences.pinnedRoutes.value)
    )

    fun setQuery(query: String) = filters.update { it.copy(query = query) }

    fun setGroup(group: ToolGroup?) = filters.update { it.copy(group = group) }

    fun setOnlySupported(only: Boolean) = filters.update { it.copy(onlySupported = only) }

    /** Returns true if the tool is pinned afterwards. */
    fun togglePin(tool: ToolInfo): Boolean = toolPreferences.togglePin(tool.route)

    fun onToolOpened(tool: ToolInfo) = toolPreferences.recordOpened(tool.route, tool.name)

    private fun buildState(f: Filters, pinned: List<String>): ToolsUiState {
        val visible = filterTools(ToolCatalog.tools, f.group, f.query, f.onlySupported) { availability[it.route] == true }
        val sections = ToolGroup.entries.mapNotNull { group ->
            val rows = visible.filter { it.group == group }.map {
                ToolRow(it, available = availability[it.route] == true, pinned = it.route in pinned)
            }
            if (rows.isEmpty()) null else ToolSection(group, rows)
        }
        return ToolsUiState(
            query = f.query,
            group = f.group,
            onlySupported = f.onlySupported,
            sections = sections,
            shownCount = visible.size
        )
    }
}

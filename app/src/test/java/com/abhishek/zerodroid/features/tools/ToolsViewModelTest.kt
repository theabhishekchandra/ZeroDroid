package com.abhishek.zerodroid.features.tools

import android.content.SharedPreferences
import com.abhishek.zerodroid.core.hardware.HardwareChecker
import com.abhishek.zerodroid.core.prefs.ToolPreferences
import com.abhishek.zerodroid.core.testing.MainDispatcherRule
import com.abhishek.zerodroid.navigation.ToolCatalog
import com.abhishek.zerodroid.navigation.ToolGroup
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ToolsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val hardware = mockk<HardwareChecker>(relaxed = true) {
        every { hasWifi() } returns true
        every { hasBluetoothLe() } returns true
        every { hasIr() } returns false
        every { hasNfc() } returns true
    }
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val prefs = mockk<SharedPreferences> {
        every { getString(any(), any()) } returns null
        every { edit() } returns editor
    }

    private fun vm() = ToolsViewModel(hardware, ToolPreferences(prefs))

    @Test
    fun `initial state lists every tool in group order`() {
        val state = vm().uiState.value
        assertEquals(ToolCatalog.tools.size, state.shownCount)
        assertEquals(ToolGroup.entries.toList(), state.sections.map { it.group })
    }

    @Test
    fun `rows carry availability and pin state`() {
        val rows = vm().uiState.value.sections.flatMap { it.rows }
        assertFalse(rows.first { it.tool.name == "IR Remote" }.available)
        assertTrue(rows.first { it.tool.name == "WiFi Analyzer" }.available)
        assertTrue(rows.first { it.tool.name == "WiFi Analyzer" }.pinned)
        assertFalse(rows.first { it.tool.name == "GPS Tracker" }.pinned)
    }

    @Test
    fun `filters update the visible sections`() = runTest(mainRule.dispatcher) {
        val viewModel = vm()
        val subscriber = launch { viewModel.uiState.collect { } }

        viewModel.setGroup(ToolGroup.INTERACT)
        viewModel.setOnlySupported(true)
        val state = viewModel.uiState.value

        assertEquals(listOf(ToolGroup.INTERACT), state.sections.map { it.group })
        assertTrue(state.sections.single().rows.none { it.tool.name == "IR Remote" })

        viewModel.setGroup(null)
        viewModel.setOnlySupported(false)
        viewModel.setQuery("zzz-no-match")
        assertTrue(viewModel.uiState.value.sections.isEmpty())
        assertEquals(0, viewModel.uiState.value.shownCount)
        subscriber.cancel()
    }

    @Test
    fun `toggling a pin is reflected in the rows`() = runTest(mainRule.dispatcher) {
        val viewModel = vm()
        val subscriber = launch { viewModel.uiState.collect { } }
        val gps = ToolCatalog.forRoute("gps")!!

        assertTrue(viewModel.togglePin(gps))
        assertTrue(viewModel.uiState.value.sections.flatMap { it.rows }.first { it.tool == gps }.pinned)
        subscriber.cancel()
    }
}

package com.abhishek.zerodroid.features.celltower.viewmodel

import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.testing.MainDispatcherRule
import com.abhishek.zerodroid.features.celltower.domain.CellTowerAnalyzer
import com.abhishek.zerodroid.features.celltower.domain.CellTowerInfo
import com.abhishek.zerodroid.features.celltower.domain.CellTowerState
import com.abhishek.zerodroid.features.celltower.domain.CellType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CellTowerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val analyzer = mockk<CellTowerAnalyzer>()

    private fun cell(rssi: Int) = CellTowerInfo(CellType.LTE, 404, 45, 1, 100L, rssi, 1800, isRegistered = true)
    private fun state(rssi: Int) = CellTowerState(currentCell = cell(rssi), isMonitoring = true)

    @Test
    fun `monitoring publishes the analyzer state and builds a signal history`() {
        every { analyzer.monitor() } returns flowOf(state(-80), state(-85), state(-90))
        val vm = CellTowerViewModel(analyzer, DemoDataBus())

        vm.startMonitoring()

        assertTrue(vm.state.value.isMonitoring)
        assertEquals(-90, vm.state.value.currentCell?.rssi)
        assertEquals(listOf(-80, -85, -90), vm.state.value.signalHistory)
    }

    @Test
    fun `signal history keeps only the last sixty samples`() {
        every { analyzer.monitor() } returns flow { repeat(75) { emit(state(-60 - it)) } }
        val vm = CellTowerViewModel(analyzer, DemoDataBus())

        vm.startMonitoring()

        assertEquals(60, vm.state.value.signalHistory.size)
        assertEquals(-75, vm.state.value.signalHistory.first())
        assertEquals(-134, vm.state.value.signalHistory.last())
    }

    @Test
    fun `startMonitoring is idempotent while active`() {
        every { analyzer.monitor() } returns MutableSharedFlow()
        val vm = CellTowerViewModel(analyzer, DemoDataBus())

        vm.startMonitoring()
        vm.startMonitoring()

        verify(exactly = 1) { analyzer.monitor() }
    }

    @Test
    fun `stopMonitoring clears the flag and telephony errors are reported`() {
        every { analyzer.monitor() } returns flow { throw SecurityException("READ_PHONE_STATE") }
        val vm = CellTowerViewModel(analyzer, DemoDataBus())

        vm.startMonitoring()
        assertFalse(vm.state.value.isMonitoring)
        assertEquals("READ_PHONE_STATE", vm.state.value.error)

        vm.stopMonitoring()
        assertFalse(vm.state.value.isMonitoring)
    }
}

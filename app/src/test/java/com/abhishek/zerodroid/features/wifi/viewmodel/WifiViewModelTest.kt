package com.abhishek.zerodroid.features.wifi.viewmodel

import com.abhishek.zerodroid.core.testing.MainDispatcherRule
import com.abhishek.zerodroid.core.util.WifiBand
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WifiViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val scanner = mockk<WifiScanner>()

    private fun ap(bssid: String, freq: Int = 2437, rssi: Int = -50) =
        WifiAccessPoint("Net-$bssid", bssid, rssi, freq, "[WPA2-PSK-CCMP][ESS]")

    @Test
    fun `startScan publishes access points and channel scores`() {
        every { scanner.scan() } returns flowOf(listOf(ap("A"), ap("B", freq = 5180)))
        val vm = WifiViewModel(scanner)

        vm.startScan()

        assertTrue(vm.isScanning.value)
        assertEquals(listOf("A", "B"), vm.accessPoints.value.map { it.bssid })
        assertEquals(setOf(6, 36), vm.channelScores.value.map { it.channel }.toSet())
    }

    @Test
    fun `stopScan clears the scanning flag but keeps results`() {
        every { scanner.scan() } returns flowOf(listOf(ap("A")))
        val vm = WifiViewModel(scanner)
        vm.startScan()

        vm.stopScan()

        assertFalse(vm.isScanning.value)
        assertEquals(1, vm.accessPoints.value.size)
    }

    @Test
    fun `startScan is ignored while a scan is already running`() {
        val live = MutableSharedFlow<List<WifiAccessPoint>>()
        every { scanner.scan() } returns live
        val vm = WifiViewModel(scanner)

        vm.startScan()
        vm.startScan()

        verify(exactly = 1) { scanner.scan() }
    }

    @Test
    fun `scan auto-stops after thirty seconds`() = runTest(mainRule.dispatcher) {
        every { scanner.scan() } returns MutableSharedFlow()
        val vm = WifiViewModel(scanner)
        vm.startScan()

        advanceTimeBy(29_999); runCurrent()
        assertTrue(vm.isScanning.value)
        advanceTimeBy(2); runCurrent()
        assertFalse(vm.isScanning.value)
    }

    @Test
    fun `band selection is independent of scanning`() {
        val vm = WifiViewModel(scanner)
        assertNull(vm.selectedBand.value)

        vm.selectBand(WifiBand.BAND_5GHZ)

        assertEquals(WifiBand.BAND_5GHZ, vm.selectedBand.value)
        vm.selectBand(null)
        assertNull(vm.selectedBand.value)
    }
}

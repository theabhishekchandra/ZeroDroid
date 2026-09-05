package com.abhishek.zerodroid.features.bluetooth_tracker.viewmodel

import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.alerts.AlertSource
import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.testing.MainDispatcherRule
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackerType
import com.abhishek.zerodroid.features.bluetooth_tracker.domain.TrackingRisk
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BluetoothTrackerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val scanner = mockk<BleScanner>()
    private val alerts = mockk<AlertCenterRepository>(relaxed = true)

    private val airTag = BleDevice(null, "7D:1E:AA:40:9C:02", -60, listOf("7dfc9000-7d1c-4951-86aa-8d9728f8d66c"))
    private val tile = BleDevice("Tile Pro", "E4:B0:21:77:0A:1F", -80)
    private val buds = BleDevice("Galaxy Buds", "5C:F3:70:A1:02:9B", -48)

    private fun vm() = BluetoothTrackerViewModel(scanner, alerts, DemoDataBus())

    @Test
    fun `only tracker devices are kept and counted`() {
        every { scanner.scan() } returns flowOf(listOf(airTag, tile, buds))
        val vm = vm()

        vm.startScan()

        val s = vm.state.value
        assertTrue(s.isScanning)
        assertEquals(3, s.totalDevicesScanned)
        assertEquals(setOf(TrackerType.AIRTAG, TrackerType.TILE), s.trackers.map { it.type }.toSet())
        assertTrue(s.trackers.all { it.risk == TrackingRisk.LOW && it.seenCount == 1 })
        assertEquals(0, s.highRiskCount)
    }

    @Test
    fun `repeated sightings raise seen count and risk and alert once per tier`() {
        every { scanner.scan() } returns flow { repeat(5) { emit(listOf(airTag)) } }
        val vm = vm()

        vm.startScan()

        val tracker = vm.state.value.trackers.single()
        assertEquals(5, tracker.seenCount)
        assertEquals(TrackingRisk.MEDIUM, tracker.risk)
        coVerify(exactly = 1) {
            alerts.record(AlertSource.BLUETOOTH_TRACKER, AlertSeverity.MEDIUM, any(), any(), any())
        }
    }

    @Test
    fun `low risk trackers are not forwarded to the alert center`() {
        every { scanner.scan() } returns flowOf(listOf(tile))
        vm().startScan()

        coVerify(exactly = 0) { alerts.record(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `stop and clear reset the scan`() {
        every { scanner.scan() } returns flowOf(listOf(airTag))
        val vm = vm()
        vm.startScan()

        vm.stopScan()
        assertFalse(vm.state.value.isScanning)
        assertEquals(1, vm.state.value.trackers.size)

        vm.clearTrackers()
        assertTrue(vm.state.value.trackers.isEmpty())
        assertEquals(0, vm.state.value.totalDevicesScanned)
    }

    @Test
    fun `scanner failure stops the scan with an error`() {
        every { scanner.scan() } returns flow { throw IllegalStateException("adapter off") }
        val vm = vm()

        vm.startScan()

        assertFalse(vm.state.value.isScanning)
        assertEquals("Scan failed: adapter off", vm.state.value.error)
    }
}

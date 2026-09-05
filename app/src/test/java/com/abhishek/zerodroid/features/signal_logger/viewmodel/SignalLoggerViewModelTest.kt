package com.abhishek.zerodroid.features.signal_logger.viewmodel

import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.testing.MainDispatcherRule
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.signal_logger.domain.SignalType
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignalLoggerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val wifi = mockk<WifiScanner>()
    private val ble = mockk<BleScanner>()

    private fun ap(bssid: String, rssi: Int = -60) = WifiAccessPoint("Net-$bssid", bssid, rssi, 2437, "[WPA2-PSK-CCMP][ESS]")
    private fun dev(addr: String, rssi: Int = -60) = BleDevice("Dev-$addr", addr, rssi)

    private fun vm() = SignalLoggerViewModel(wifi, ble, DemoDataBus())

    @Test
    fun `first sighting of each device is logged once and not repeated`() {
        every { wifi.scan() } returns flow { repeat(3) { emit(listOf(ap("A"), ap("B"))) } }
        every { ble.scan() } returns flow { repeat(3) { emit(listOf(dev("X"))) } }
        val vm = vm()

        vm.startLogging()

        val s = vm.state.value
        assertTrue(s.isLogging)
        assertEquals(3, s.totalEntries)
        assertEquals(mapOf(SignalType.WIFI_AP to 2, SignalType.BLE_DEVICE to 1), s.entries.groupingBy { it.type }.eachCount())
        assertEquals(2, s.wifiApCount)
        assertEquals(1, s.bleDeviceCount)
    }

    @Test
    fun `new and lost devices are logged and counted`() {
        every { wifi.scan() } returns flow { emit(listOf(ap("A"))); emit(listOf(ap("B"))) }
        every { ble.scan() } returns MutableSharedFlow()
        val vm = vm()

        vm.startLogging()

        val types = vm.state.value.entries.map { it.type }
        assertEquals(listOf(SignalType.WIFI_AP, SignalType.WIFI_NEW, SignalType.WIFI_LOST), types)
        assertEquals(1, vm.state.value.newDevicesCount)
        assertEquals(1, vm.state.value.lostDevicesCount)
    }

    @Test
    fun `a large signal jump is an anomaly`() {
        every { wifi.scan() } returns flow { emit(listOf(ap("A", -60))); emit(listOf(ap("A", -90))) }
        every { ble.scan() } returns MutableSharedFlow()
        val vm = vm()

        vm.startLogging()

        assertEquals(1, vm.state.value.anomalyCount)
        assertTrue(vm.state.value.entries.any { it.type == SignalType.ANOMALY && it.detail.contains("Signal spike") })
    }

    @Test
    fun `export contains a header and one line per entry`() {
        every { wifi.scan() } returns flowOf(listOf(ap("A")))
        every { ble.scan() } returns flowOf(listOf(dev("X")))
        val vm = vm()
        vm.startLogging()

        val export = vm.exportLog().lines()

        assertEquals("Timestamp | Type | Source | Address | RSSI | Detail", export[0])
        assertEquals(2 + 2, export.size)
        assertTrue(export.drop(2).all { it.contains("|") })
    }

    @Test
    fun `stop keeps entries and clear resets everything`() {
        every { wifi.scan() } returns flowOf(listOf(ap("A")))
        every { ble.scan() } returns MutableSharedFlow()
        val vm = vm()
        vm.startLogging()

        vm.stopLogging()
        assertFalse(vm.state.value.isLogging)
        assertEquals(1, vm.state.value.totalEntries)

        vm.clearLog()
        assertEquals(0, vm.state.value.totalEntries)
        assertTrue(vm.state.value.entries.isEmpty())
    }

    @Test
    fun `scanner errors are surfaced without stopping the other source`() {
        every { wifi.scan() } returns flow { throw IllegalStateException("wifi off") }
        every { ble.scan() } returns flowOf(listOf(dev("X")))
        val vm = vm()

        vm.startLogging()

        assertEquals("WiFi scan error: wifi off", vm.state.value.error)
        assertEquals(1, vm.state.value.bleDeviceCount)
    }
}

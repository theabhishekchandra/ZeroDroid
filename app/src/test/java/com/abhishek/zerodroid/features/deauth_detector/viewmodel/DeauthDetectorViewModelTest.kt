package com.abhishek.zerodroid.features.deauth_detector.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import com.abhishek.zerodroid.core.alerts.AlertSource
import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.testing.MainDispatcherRule
import com.abhishek.zerodroid.features.deauth_detector.domain.AttackType
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DeauthDetectorViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val scanner = mockk<WifiScanner>()
    private val alerts = mockk<AlertCenterRepository>(relaxed = true)
    private val connectivity = mockk<ConnectivityManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private val home = "AA:BB:CC:11:22:33"
    private fun ap(freq: Int, rssi: Int = -55) = WifiAccessPoint("HomeNet", home, rssi, freq, "[WPA2-PSK-CCMP][ESS]")

    @Before
    fun setUp() {
        val info = mockk<WifiInfo> {
            every { ssid } returns "\"HomeNet\""
            every { bssid } returns home
            every { rssi } returns -55
        }
        val wifi = mockk<WifiManager> { every { connectionInfo } returns info }
        every { context.getSystemService(Context.WIFI_SERVICE) } returns wifi
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivity
        mockkConstructor(NetworkRequest.Builder::class)
        val builder = mockk<NetworkRequest.Builder>()
        every { anyConstructed<NetworkRequest.Builder>().addTransportType(any()) } returns builder
        every { builder.build() } returns mockk()
        every { connectivity.registerNetworkCallback(any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>()) } just runs
    }

    @After
    fun tearDown() = unmockkConstructor(NetworkRequest.Builder::class)

    private fun vm() = DeauthDetectorViewModel(scanner, context, alerts, DemoDataBus())

    @Test
    fun `monitoring reports the connected network and registers connectivity tracking`() {
        every { scanner.scan() } returns flowOf(listOf(ap(5180)))
        val vm = vm()

        vm.startMonitoring()

        val s = vm.state.value
        assertTrue(s.isMonitoring)
        assertEquals("HomeNet", s.connectedSsid)
        assertEquals(home, s.connectedBssid)
        assertEquals(36, s.connectedChannel)
        assertTrue(s.events.isEmpty())
        assertFalse(s.isUnderAttack)
        verify(exactly = 1) { connectivity.registerNetworkCallback(any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>()) }
    }

    @Test
    fun `an unexpected channel change becomes an event an attack flag and an alert`() {
        every { scanner.scan() } returns flow { emit(listOf(ap(5180))); emit(listOf(ap(5745))) }
        val vm = vm()

        vm.startMonitoring()

        val s = vm.state.value
        assertEquals(listOf(AttackType.CHANNEL_HOPPING), s.events.map { it.type })
        assertTrue(s.isUnderAttack)
        assertEquals(149, s.connectedChannel)
        coVerify(exactly = 1) { alerts.record(AlertSource.DEAUTH, any(), any(), any(), any()) }
    }

    @Test
    fun `clearEvents wipes history and stopMonitoring unregisters`() {
        every { scanner.scan() } returns flow { emit(listOf(ap(5180))); emit(listOf(ap(5745))) }
        val vm = vm()
        vm.startMonitoring()

        vm.clearEvents()
        assertTrue(vm.state.value.events.isEmpty())
        assertFalse(vm.state.value.isUnderAttack)

        vm.stopMonitoring()
        assertFalse(vm.state.value.isMonitoring)
        verify(exactly = 1) { connectivity.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) }
    }

    @Test
    fun `scan failure ends monitoring with an error`() {
        every { scanner.scan() } returns flow { throw IllegalStateException("wifi off") }
        val vm = vm()

        vm.startMonitoring()

        assertFalse(vm.state.value.isMonitoring)
        assertEquals("WiFi scan error: wifi off", vm.state.value.error)
    }
}

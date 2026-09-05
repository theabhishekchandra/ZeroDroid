package com.abhishek.zerodroid.features.rogue_ap_detector.viewmodel

import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import com.abhishek.zerodroid.core.alerts.AlertSource
import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.testing.MainDispatcherRule
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.ApThreatType
import com.abhishek.zerodroid.features.rogue_ap_detector.domain.RiskLevel
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
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

class RogueApViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val scanner = mockk<WifiScanner>()
    private val alerts = mockk<AlertCenterRepository>(relaxed = true)

    // The analyzer treats the strongest AP of an SSID as legitimate, so the open twin is weaker.
    // The SSID avoids the "common public SSID" list so only the evil-twin rule fires.
    private val legit = WifiAccessPoint("Chandra-Lab", "AA:BB:CC:11:22:33", -58, 5180, "[WPA2-PSK-CCMP][ESS]")
    private val twin = WifiAccessPoint("Chandra-Lab", "DE:AD:BE:EF:00:01", -70, 2437, "[ESS]")
    private val neighbour = WifiAccessPoint("Neighbour", "11:22:33:44:55:66", -70, 2437, "[WPA3-SAE-CCMP][ESS]")

    private fun vm() = RogueApViewModel(scanner, alerts, DemoDataBus())

    @Test
    fun `an open twin of a secured network is flagged and counted`() {
        every { scanner.scan() } returns flowOf(listOf(legit, twin, neighbour))
        val vm = vm()

        vm.startScan()

        val s = vm.state.value
        assertTrue(s.isScanning)
        assertEquals(3, s.totalAps)
        val alert = s.alerts.single()
        assertEquals(ApThreatType.EVIL_TWIN, alert.threatType)
        assertEquals(RiskLevel.CRITICAL, alert.riskLevel)
        assertEquals(twin.bssid, alert.suspiciousAp.bssid)
        assertEquals(legit.bssid, alert.legitimateAp?.bssid)
        assertEquals(1, s.suspiciousAps)
        assertEquals(2, s.safeAps)
    }

    @Test
    fun `a persistent rogue AP is reported to the alert center only once`() {
        every { scanner.scan() } returns flow { repeat(3) { emit(listOf(legit, twin)) } }
        vm().startScan()

        coVerify(exactly = 1) { alerts.record(AlertSource.ROGUE_AP, any(), match { it.contains("Chandra-Lab") }, any(), any()) }
    }

    @Test
    fun `clean scans produce no alerts`() {
        every { scanner.scan() } returns flowOf(listOf(legit, neighbour))
        val vm = vm()

        vm.startScan()

        assertTrue(vm.state.value.alerts.isEmpty())
        assertEquals(2, vm.state.value.safeAps)
        coVerify(exactly = 0) { alerts.record(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `known SSIDs can be added trimmed and removed`() {
        val vm = vm()

        vm.addKnownSsid("  HomeNet ")
        vm.addKnownSsid("   ")

        assertEquals(setOf("HomeNet"), vm.state.value.knownSsids)
        vm.removeKnownSsid("HomeNet")
        assertTrue(vm.state.value.knownSsids.isEmpty())
    }

    @Test
    fun `clearAlerts empties the list and stopScan ends scanning`() {
        every { scanner.scan() } returns flowOf(listOf(legit, twin))
        val vm = vm()
        vm.startScan()

        vm.clearAlerts()
        assertTrue(vm.state.value.alerts.isEmpty())
        assertEquals(0, vm.state.value.suspiciousAps)
        assertEquals(vm.state.value.totalAps, vm.state.value.safeAps)

        vm.stopScan()
        assertFalse(vm.state.value.isScanning)
    }

    @Test
    fun `scan failures are surfaced`() {
        every { scanner.scan() } returns flow { throw IllegalStateException("wifi off") }
        val vm = vm()

        vm.startScan()

        assertFalse(vm.state.value.isScanning)
        assertEquals("WiFi scan error: wifi off", vm.state.value.error)
    }
}

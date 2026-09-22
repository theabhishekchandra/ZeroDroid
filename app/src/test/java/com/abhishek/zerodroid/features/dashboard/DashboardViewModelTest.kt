package com.abhishek.zerodroid.features.dashboard

import android.content.SharedPreferences
import com.abhishek.zerodroid.core.alerts.AlertCenterRepository
import com.abhishek.zerodroid.core.alerts.AlertSeverity
import com.abhishek.zerodroid.core.alerts.AlertSource
import com.abhishek.zerodroid.core.alerts.UnifiedAlert
import com.abhishek.zerodroid.core.hardware.HardwareChecker
import com.abhishek.zerodroid.core.prefs.LastUsedFeature
import com.abhishek.zerodroid.core.prefs.ToolPreferences
import com.abhishek.zerodroid.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val hardware = mockk<HardwareChecker>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val prefs = mockk<SharedPreferences> {
        every { getString(any(), any()) } returns null
        every { edit() } returns editor
    }
    private val alerts = mockk<AlertCenterRepository>()
    private val info = DeviceInfo("VIVO V2036", "13 (API 33)", "2036", "bengal")

    private fun alert(i: Int, severity: AlertSeverity = AlertSeverity.HIGH) =
        UnifiedAlert("a$i", AlertSource.ROGUE_AP, severity, "t$i", "d$i", i.toLong())

    private fun vm(alertList: List<UnifiedAlert> = emptyList()): DashboardViewModel {
        every { alerts.alerts } returns flowOf(alertList)
        every { editor.putString(any(), any()) } returns editor
        return DashboardViewModel(hardware, ToolPreferences(prefs), alerts, info)
    }

    @Test
    fun `exposes the injected device info`() {
        assertEquals(info, vm().deviceInfo)
    }

    @Test
    fun `hardware list mirrors the checker in a fixed order`() {
        every { hardware.hasWifi() } returns true
        every { hardware.hasBluetoothLe() } returns true
        every { hardware.hasNfc() } returns false

        val items = vm().hardwareItems.value

        assertEquals(14, items.size)
        assertEquals(listOf("WiFi", "Bluetooth", "BLE", "NFC"), items.take(4).map { it.name })
        assertTrue(items.first { it.name == "WiFi" }.isAvailable)
        assertTrue(items.first { it.name == "BLE" }.isAvailable)
        assertFalse(items.first { it.name == "NFC" }.isAvailable)
        assertFalse(items.first { it.name == "Barometer" }.isAvailable)
    }

    @Test
    fun `last used feature is restored from preferences`() {
        every { prefs.getString("last_used_route", null) } returns "wifi"
        every { prefs.getString("last_used_title", null) } returns "WiFi Analyzer"

        assertEquals(LastUsedFeature("wifi", "WiFi Analyzer"), vm().lastUsedFeature.value)
    }

    @Test
    fun `missing or partial preferences mean no last used feature`() {
        every { prefs.getString("last_used_route", null) } returns "wifi"
        every { prefs.getString("last_used_title", null) } returns null

        assertNull(vm().lastUsedFeature.value)
    }

    @Test
    fun `saveLastUsed persists and updates state`() {
        val viewModel = vm()

        viewModel.saveLastUsed("ble", "BLE Scanner")

        assertEquals(LastUsedFeature("ble", "BLE Scanner"), viewModel.lastUsedFeature.value)
        verify { editor.putString("last_used_route", "ble") }
        verify { editor.putString("last_used_title", "BLE Scanner") }
        verify { editor.apply() }
    }

    @Test
    fun `recent alerts are capped at three and total count is unbounded`() = runTest(mainRule.dispatcher) {
        val viewModel = vm((1..5).map { alert(it) })
        val subscribers = launch {
            launch { viewModel.recentAlerts.collect { } }
            launch { viewModel.totalAlertCount.collect { } }
        }

        assertEquals(listOf("a1", "a2", "a3"), viewModel.recentAlerts.value.map { it.id })
        assertEquals(5, viewModel.totalAlertCount.value)
        subscribers.cancel()
    }

    @Test
    fun `device info from build never throws on the JVM`() {
        val built = DeviceInfo.fromBuild()
        assertTrue(built.androidVersion.contains("(API 0)"))
        assertEquals("", built.device)
    }

    @Test
    fun `alert summary counts each severity and orders the breakdown by severity`() = runTest(mainRule.dispatcher) {
        val viewModel = vm(
            listOf(
                alert(1, AlertSeverity.MEDIUM),
                alert(2, AlertSeverity.HIGH),
                alert(3, AlertSeverity.MEDIUM),
                alert(9, AlertSeverity.LOW)
            )
        )
        val subscriber = launch { viewModel.alertSummary.collect { } }

        val summary = viewModel.alertSummary.value
        assertEquals(4, summary.total)
        assertEquals(AlertSeverity.HIGH, summary.worst)
        assertEquals("1 high · 2 medium · 1 low", summary.breakdown)
        assertEquals(9L, summary.latestTimestamp)
        subscriber.cancel()
    }

    @Test
    fun `empty alert summary has no worst severity`() {
        val summary = AlertSummary.from(emptyList())
        assertNull(summary.worst)
        assertEquals("", summary.breakdown)
        assertNull(summary.latestTimestamp)
    }

    @Test
    fun `tool support counts catalog tools this phone can run and names what is missing`() {
        every { hardware.hasWifi() } returns true
        every { hardware.hasBluetoothLe() } returns true
        every { hardware.hasBluetooth() } returns true
        every { hardware.hasGps() } returns true
        every { hardware.hasIr() } returns false
        every { hardware.hasUwb() } returns false
        every { hardware.hasBarometer() } returns false

        val support = vm().toolSupport

        assertEquals(28, support.total)
        assertTrue(support.supported < support.total)
        assertTrue("no IR blaster" in support.missing)
        assertTrue("no UWB" in support.missing)
        assertTrue("no barometer" in support.missing)
        assertEquals(support.missing.distinct(), support.missing)
    }

    @Test
    fun `pinned tools default to the catalog defaults`() {
        assertEquals(
            listOf("wifi", "bluetooth_tracker", "ble", "nfc"),
            vm().pinnedTools.value.map { it.route }
        )
    }
}

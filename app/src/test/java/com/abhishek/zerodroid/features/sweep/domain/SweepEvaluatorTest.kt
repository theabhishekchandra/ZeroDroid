package com.abhishek.zerodroid.features.sweep.domain

import android.content.Context
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.hidden_camera.domain.HiddenCameraDetector
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SweepEvaluatorTest {

    private val evaluator = SweepEvaluator(HiddenCameraDetector(mockk<Context>(relaxed = true)))

    private fun ap(ssid: String, bssid: String, caps: String = "[WPA2-PSK-CCMP][ESS]", rssi: Int = -60) =
        WifiAccessPoint(ssid, bssid, rssi, 2437, caps)

    private val airTagUuid = "7dfc9000-7d1c-4951-86aa-8d9728f8d66c"

    @Test
    fun `open and WEP networks are low findings, secured ones are not`() {
        val result = evaluator.wifi(listOf(ap("Cafe", "AA:00:00:00:00:01", "[ESS]"), ap("Old", "AA:00:00:00:00:02", "[WEP][ESS]"), ap("Home", "AA:00:00:00:00:03")))
        assertEquals(2, result.findings.size)
        assertTrue(result.findings.all { it.level == FindingLevel.LOW && it.check == SweepCheck.WIFI })
        assertEquals("3 networks · 2 open or WEP", result.summary)
    }

    @Test
    fun `clean wifi summary says so`() {
        assertEquals("1 networks · none open or WEP", evaluator.wifi(listOf(ap("Home", "AA:00:00:00:00:03"))).summary)
    }

    @Test
    fun `a strong tracker is high and likely in the room, a weak one is medium`() {
        val near = BleDevice(null, "7D:1E:AA:40:9C:02", -60, listOf(airTagUuid))
        val far = BleDevice(null, "7D:1E:AA:40:9C:03", -85, listOf(airTagUuid))
        val findings = evaluator.trackers(listOf(near, far)).findings.associateBy { it.key }

        assertEquals(FindingLevel.HIGH, findings.getValue(near.address).level)
        assertTrue(findings.getValue(near.address).title.contains("likely in this room"))
        assertEquals(FindingLevel.MEDIUM, findings.getValue(far.address).level)
        assertTrue(findings.values.all { it.isBle })
    }

    @Test
    fun `the in-room threshold is inclusive`() {
        val edge = BleDevice(null, "7D:1E:AA:40:9C:04", SweepEvaluator.IN_ROOM_RSSI, listOf(airTagUuid))
        assertEquals(FindingLevel.HIGH, evaluator.trackers(listOf(edge)).findings.single().level)
    }

    @Test
    fun `ordinary bluetooth devices are not flagged`() {
        val result = evaluator.trackers(listOf(BleDevice("Galaxy Buds", "5C:F3:70:A1:02:9B", -48)))
        assertTrue(result.findings.isEmpty())
        assertEquals("1 Bluetooth devices · no trackers or radio modules", result.summary)
    }

    @Test
    fun `no ultrasonic beacon means no finding`() {
        val result = evaluator.ultrasonic(peakHz = 19_000f, peakMagnitude = 0.02f, beaconCount = 0)
        assertTrue(result.findings.isEmpty())
        assertEquals("Quiet from 18 to 24 kHz", result.summary)
    }

    @Test
    fun `a detected ultrasonic beacon is a medium finding`() {
        val result = evaluator.ultrasonic(peakHz = 19_500f, peakMagnitude = 0.2f, beaconCount = 1)
        assertEquals(FindingLevel.MEDIUM, result.findings.single().level)
        assertTrue(result.summary.startsWith("Steady tone near 19.5"))
    }

    @Test
    fun `small magnetic changes are within baseline, big ones are flagged`() {
        assertTrue(evaluator.magnetic(baseline = 45f, peak = 47f).findings.isEmpty())
        val spike = evaluator.magnetic(baseline = 45f, peak = 200f)
        assertEquals(SweepCheck.MAGNETIC, spike.findings.single().check)
        assertEquals("Peak 155 µT above baseline", spike.summary)
    }
}

package com.abhishek.zerodroid.features.rf_bug_sweeper.domain

import com.abhishek.zerodroid.features.ble.domain.BleDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RfBugDetectorTest {

    private val detector = RfBugDetector()

    private fun ble(name: String?, address: String = "AA:BB:CC:DD:EE:FF", rssi: Int = -70) =
        BleDevice(name = name, address = address, rssi = rssi)

    @Test
    fun `flags known transmitter module names`() {
        val d = detector.analyseBleDevices(listOf(ble("HC-05", rssi = -45))).single()

        assertEquals(BugType.SUSPICIOUS_BLE, d.type)
        assertEquals(ThreatSeverity.HIGH, d.severity)
        assertEquals("ble-name-AA:BB:CC:DD:EE:FF", d.id)
        assertEquals(-45, d.rssi)
    }

    @Test
    fun `weaker name matches are MEDIUM`() {
        assertEquals(ThreatSeverity.MEDIUM, detector.analyseBleDevices(listOf(ble("ESP32_CAM", rssi = -80))).single().severity)
    }

    @Test
    fun `flags cheap module OUIs as RF transmitters`() {
        val d = detector.analyseBleDevices(listOf(ble("Speaker", address = "20:91:48:11:22:33", rssi = -65))).single()

        assertEquals(BugType.RF_TRANSMITTER, d.type)
        assertEquals(ThreatSeverity.MEDIUM, d.severity)
        assertTrue(d.detail.contains("20:91:48"))
    }

    @Test
    fun `name match takes precedence over OUI match`() {
        val d = detector.analyseBleDevices(listOf(ble("JDY-31", address = "20:91:48:11:22:33"))).single()
        assertEquals(BugType.SUSPICIOUS_BLE, d.type)
    }

    @Test
    fun `strong unnamed devices are suspicious but weak ones are not`() {
        assertEquals(ThreatSeverity.MEDIUM, detector.analyseBleDevices(listOf(ble(null, rssi = -35))).single().severity)
        assertTrue(detector.analyseBleDevices(listOf(ble(null, rssi = -60))).isEmpty())
    }

    @Test
    fun `ordinary devices produce nothing`() {
        assertTrue(detector.analyseBleDevices(listOf(ble("Pixel 8"), ble("AirPods", rssi = -30))).isEmpty())
    }

    @Test
    fun `ultrasonic severity follows beacon count then magnitude`() {
        assertNull(detector.analyseUltrasonic(20_000f, 0.001f, 0))
        assertEquals(ThreatSeverity.LOW, detector.analyseUltrasonic(20_000f, 0.02f, 0)!!.severity)
        assertEquals(ThreatSeverity.MEDIUM, detector.analyseUltrasonic(20_000f, 0.06f, 0)!!.severity)
        assertEquals(ThreatSeverity.HIGH, detector.analyseUltrasonic(20_000f, 0.0f, 1)!!.severity)
        assertEquals(ThreatSeverity.CRITICAL, detector.analyseUltrasonic(20_000f, 0.0f, 2)!!.severity)
        val d = detector.analyseUltrasonic(21_500.4f, 0.1f, 1)!!
        assertEquals(BugType.ULTRASONIC_BEACON, d.type)
        assertEquals("ultra-21500", d.id)
        assertEquals(21_500.4f, d.frequency!!, 0f)
    }

    @Test
    fun `magnetic anomaly needs 25 microtesla and escalates`() {
        assertNull(detector.analyseMagnetic(50f, 70f, 20f))
        assertEquals(ThreatSeverity.LOW, detector.analyseMagnetic(50f, 80f, 30f)!!.severity)
        assertEquals(ThreatSeverity.MEDIUM, detector.analyseMagnetic(50f, 95f, 45f)!!.severity)
        assertEquals(ThreatSeverity.HIGH, detector.analyseMagnetic(50f, 120f, 70f)!!.severity)
        assertEquals(ThreatSeverity.CRITICAL, detector.analyseMagnetic(50f, 200f, 150f)!!.severity)
        // negative deviation is an anomaly too
        val d = detector.analyseMagnetic(50f, 10f, -40f)
        assertNotNull(d)
        assertEquals(BugType.MAGNETIC_ANOMALY, d!!.type)
        assertEquals(10f, d.fieldStrength!!, 0f)
    }
}

package com.abhishek.zerodroid.features.hidden_camera.domain

import android.content.Context
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenCameraDetectorTest {

    private val detector = HiddenCameraDetector(mockk<Context>(relaxed = true))

    private fun ap(ssid: String, bssid: String, rssi: Int = -60) =
        WifiAccessPoint(ssid = ssid, bssid = bssid, rssi = rssi, frequency = 2437, capabilities = "[ESS]")

    private fun ble(name: String?, address: String = "AA:BB:CC:DD:EE:FF") =
        BleDevice(name = name, address = address, rssi = -55)

    @Test
    fun `wifi OUI match is HIGH threat and case insensitive`() {
        val d = detector.matchWifiOui(ap("", "c0:56:e3:12:34:56"))

        assertNotNull(d)
        assertEquals(DetectionSource.WIFI, d!!.source)
        assertEquals(ThreatLevel.HIGH, d.threatLevel)
        assertTrue(d.detail.contains("(hidden)"))
        assertEquals(-60, d.rssi)
        assertNull(detector.matchWifiOui(ap("Home", "00:11:22:33:44:55")))
    }

    @Test
    fun `wifi SSID keywords are MEDIUM threat`() {
        assertEquals(ThreatLevel.MEDIUM, detector.matchWifiSsid(ap("Wyze-Cam-1234", "00:11:22:33:44:55"))!!.threatLevel)
        assertNotNull(detector.matchWifiSsid(ap("ESP32-CAM", "00:11:22:33:44:55")))
        assertNotNull(detector.matchWifiSsid(ap("yi home", "00:11:22:33:44:55")))
        assertNull(detector.matchWifiSsid(ap("Living Room", "00:11:22:33:44:55")))
        assertNull(detector.matchWifiSsid(ap("", "00:11:22:33:44:55")))
    }

    @Test
    fun `ble name and OUI matches`() {
        assertEquals(ThreatLevel.MEDIUM, detector.matchBleDevice(ble("Blink Mini"))!!.threatLevel)
        assertNull(detector.matchBleDevice(ble("Pixel Buds")))
        assertNull(detector.matchBleDevice(ble(null)))

        val oui = detector.matchBleOui(ble(null, address = "2C:AA:8E:01:02:03"))
        assertEquals(ThreatLevel.HIGH, oui!!.threatLevel)
        assertEquals(DetectionSource.BLE, oui.source)
        assertTrue(oui.detail.contains("Unknown Device"))
        assertNull(detector.matchBleOui(ble("x", address = "00:00:00:00:00:00")))
    }

    @Test
    fun `magnetic anomaly uses absolute deviation against 15 microtesla`() {
        assertNull(detector.checkMagneticAnomaly(15f))
        assertNull(detector.checkMagneticAnomaly(-10f))
        val d = detector.checkMagneticAnomaly(-22.4f)
        assertEquals(ThreatLevel.LOW, d!!.threatLevel)
        assertEquals(DetectionSource.MAGNETIC, d.source)
        assertTrue(d.detail.contains("22.4"))
    }

    @Test
    fun `camera port list covers RTSP ONVIF and web ports`() {
        assertTrue(HiddenCameraDetector.CAMERA_PORTS.containsAll(listOf(554, 8554, 3702, 80, 443)))
    }
}

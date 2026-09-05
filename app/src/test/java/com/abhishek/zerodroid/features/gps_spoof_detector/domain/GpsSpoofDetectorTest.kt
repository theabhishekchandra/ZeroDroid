package com.abhishek.zerodroid.features.gps_spoof_detector.domain

import android.content.Context
import android.location.LocationManager
import com.abhishek.zerodroid.features.celltower.domain.CellTowerInfo
import com.abhishek.zerodroid.features.celltower.domain.CellType
import com.abhishek.zerodroid.features.gps.domain.GpsState
import com.abhishek.zerodroid.features.sensors.domain.SensorReading
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GpsSpoofDetectorTest {

    private lateinit var detector: GpsSpoofDetector

    private val noAccel = SensorReading("Accelerometer", isAvailable = false)
    private val noBaro = SensorReading("Barometer", isAvailable = false)
    private val stillAccel = SensorReading("Accelerometer", floatArrayOf(0f, 0f, 9.81f), isAvailable = true)
    private val shakingAccel = SensorReading("Accelerometer", floatArrayOf(3f, 3f, 12f), isAvailable = true)

    private fun gps(lat: Double = 12.9716, lon: Double = 77.5946, alt: Double = 900.0, speed: Float = 0f, sats: Int = 10) =
        GpsState(isTracking = true, latitude = lat, longitude = lon, altitude = alt, speed = speed, satelliteCount = sats)

    private fun ap(bssid: String) = WifiAccessPoint("n", bssid, -60, 2437, "[ESS]")

    private fun analyze(
        g: GpsState = gps(),
        cell: CellTowerInfo? = null,
        wifi: List<WifiAccessPoint> = emptyList(),
        accel: SensorReading = noAccel,
        baro: SensorReading = noBaro
    ) = detector.analyze(g, cell, wifi, accel, baro)

    private fun SpoofCheckResult.check(name: String) = checks.first { it.name == name }

    @Before
    fun setUp() {
        val context = mockk<Context>(relaxed = true)
        val locationManager = mockk<LocationManager>(relaxed = true)
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
        every { locationManager.allProviders } returns listOf("gps", "network")
        every { locationManager.getLastKnownLocation(any()) } returns null
        detector = GpsSpoofDetector(context)
    }

    @Test
    fun `runs all seven checks and passes on a healthy fix`() {
        val result = analyze()

        assertEquals(7, result.checks.size)
        assertTrue(result.checks.all { it.passed })
        assertEquals(0f, result.spoofConfidence, 0f)
        assertEquals(Pair(12.9716, 77.5946), result.gpsLocation)
        assertEquals(10, result.gpsSatelliteCount)
    }

    @Test
    fun `no fix yet reports null location and passes gracefully`() {
        val result = analyze(GpsState())

        assertNull(result.gpsLocation)
        assertNull(result.gpsSatelliteCount)
        assertTrue(result.check("Satellite Count").passed)
        assertEquals("No GPS fix yet", result.check("Satellite Count").detail)
    }

    @Test
    fun `satellite count outside realistic bounds fails`() {
        assertFalse(analyze(gps(sats = 2)).check("Satellite Count").passed)
        assertFalse(analyze(gps(sats = 40)).check("Satellite Count").passed)
        assertTrue(analyze(gps(sats = 4)).check("Satellite Count").passed)
    }

    @Test
    fun `barometric altitude must agree with GPS altitude`() {
        // 1013.25 hPa is sea level; GPS says 900 m -> mismatch
        val seaLevel = SensorReading("Barometer", floatArrayOf(1013.25f), isAvailable = true)
        assertFalse(analyze(gps(alt = 900.0), baro = seaLevel).check("Altitude Consistency").passed)
        assertTrue(analyze(gps(alt = 50.0), baro = seaLevel).check("Altitude Consistency").passed)
        assertTrue(analyze(gps(), baro = SensorReading("Barometer", floatArrayOf(0f), isAvailable = true)).check("Altitude Consistency").passed)
    }

    @Test
    fun `accelerometer must show movement when GPS reports speed`() {
        assertFalse(analyze(gps(speed = 15f), accel = stillAccel).check("Accelerometer Correlation").passed)
        assertTrue(analyze(gps(speed = 15f), accel = shakingAccel).check("Accelerometer Correlation").passed)
        assertTrue(analyze(gps(speed = 0f), accel = shakingAccel).check("Accelerometer Correlation").passed)
        assertTrue(analyze(gps(speed = 15f), accel = noAccel).check("Accelerometer Correlation").passed)
    }

    @Test
    fun `wifi environment must overlap between scans`() {
        val first = analyze(wifi = listOf(ap("A"), ap("B"), ap("C")))
        assertTrue(first.check("WiFi BSSID Consistency").passed)

        val same = analyze(wifi = listOf(ap("A"), ap("B"), ap("D")))
        assertTrue(same.check("WiFi BSSID Consistency").passed)

        val replaced = analyze(wifi = listOf(ap("X"), ap("Y"), ap("Z")))
        assertFalse(replaced.check("WiFi BSSID Consistency").passed)
        assertTrue(replaced.check("WiFi BSSID Consistency").detail.startsWith("AP overlap: 0%"))
    }

    @Test
    fun `small baselines do not trigger the wifi check`() {
        analyze(wifi = listOf(ap("A"), ap("B")))
        assertTrue(analyze(wifi = listOf(ap("X"), ap("Y"))).check("WiFi BSSID Consistency").passed)
    }

    @Test
    fun `teleporting between fixes fails the speed check`() {
        analyze(gps(lat = 12.9716, lon = 77.5946))
        Thread.sleep(5)
        val result = analyze(gps(lat = 28.6139, lon = 77.2090)) // Bangalore -> Delhi in milliseconds

        assertFalse(result.check("Speed Anomaly").passed)
        assertTrue(result.spoofConfidence > 0f)
    }

    @Test
    fun `slow movement passes the speed check and reset forgets the previous fix`() {
        analyze(gps(lat = 12.9716, lon = 77.5946))
        Thread.sleep(20)
        // ~0.1 m in 20 ms is a few metres per second: walking pace, not teleportation.
        assertTrue(analyze(gps(lat = 12.971601, lon = 77.5946)).check("Speed Anomaly").passed)

        detector.reset()
        assertEquals("No previous fix to compare", analyze(gps(lat = 28.6139, lon = 77.2090)).check("Speed Anomaly").detail)
    }

    @Test
    fun `confidence is the fraction of failed checks`() {
        val result = analyze(gps(speed = 15f, sats = 1), accel = stillAccel)
        assertEquals(2f / 7f, result.spoofConfidence, 1e-6f)
    }

    @Test
    fun `cell check passes when no network location is available`() {
        val cell = CellTowerInfo(CellType.LTE, 404, 45, 1, 1L, -80, null)
        val result = analyze(cell = cell)

        assertTrue(result.check("GPS vs Cell Tower").passed)
        assertNull(result.cellLocation)
        assertNull(result.gpsVsCellDistanceKm)
    }

    @Test
    fun `mock provider detection passes with plain providers`() {
        assertTrue(analyze().check("Mock Location Provider").passed)
    }
}

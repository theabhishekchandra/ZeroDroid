package com.abhishek.zerodroid.features.deauth_detector.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import com.abhishek.zerodroid.features.wifi.domain.WifiAccessPoint
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class DeauthAnalyzerTest {

    private val wifiManager = mockk<WifiManager>(relaxed = true)
    private val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
    private lateinit var analyzer: DeauthAnalyzer

    private val home = "AA:AA:AA:AA:AA:01"
    private val neighbour1 = "BB:BB:BB:BB:BB:01"
    private val neighbour2 = "BB:BB:BB:BB:BB:02"

    private fun ap(bssid: String, rssi: Int, frequency: Int = 2437, ssid: String = "Net-$bssid") =
        WifiAccessPoint(ssid = ssid, bssid = bssid, rssi = rssi, frequency = frequency, capabilities = "[WPA2]")

    @Before
    fun setUp() {
        val context = mockk<Context>(relaxed = true)
        every { context.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { wifiManager.connectionInfo } returns null
        analyzer = DeauthAnalyzer(context)
    }

    @After
    fun tearDown() {
        unmockkConstructor(NetworkRequest.Builder::class)
    }

    /** Stubs the Android NetworkRequest builder and captures the callback the analyzer registers. */
    private fun trackConnectivity(): ConnectivityManager.NetworkCallback {
        mockkConstructor(NetworkRequest.Builder::class)
        val builder = mockk<NetworkRequest.Builder>()
        every { anyConstructed<NetworkRequest.Builder>().addTransportType(any()) } returns builder
        every { builder.build() } returns mockk()
        val callback = slot<ConnectivityManager.NetworkCallback>()
        every { connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callback)) } just runs
        analyzer.startConnectivityTracking(onDisconnect = { disconnectCount++ }, onReconnect = {})
        return callback.captured
    }

    private var disconnectCount = 0

    @Test
    fun `quiet network produces no events`() {
        val scan = listOf(ap(home, -50), ap(neighbour1, -70))

        assertTrue(analyzer.analyze(scan, "Home", home).isEmpty())
        assertTrue(analyzer.analyze(scan, "Home", home).isEmpty())
        assertEquals(setOf(home, neighbour1), analyzer.getApHistory().keys)
        assertEquals(2, analyzer.getApHistory()[home]!!.size)
    }

    @Test
    fun `signal jamming when connected AP drops while neighbours stay stable`() {
        analyzer.analyze(listOf(ap(home, -50), ap(neighbour1, -70), ap(neighbour2, -72)), "Home", home)

        val events = analyzer.analyze(listOf(ap(home, -85), ap(neighbour1, -71), ap(neighbour2, -73)), "Home", home)

        val jam = events.single()
        assertEquals(AttackType.SIGNAL_JAMMING, jam.type)
        assertEquals(AlertLevel.HIGH, jam.level)
        assertEquals(home, jam.affectedBssid)
        assertTrue(jam.detail.contains("35dBm"))
    }

    @Test
    fun `no jamming alert when every AP drops together`() {
        analyzer.analyze(listOf(ap(home, -50), ap(neighbour1, -60), ap(neighbour2, -62)), "Home", home)

        val events = analyzer.analyze(listOf(ap(home, -85), ap(neighbour1, -90), ap(neighbour2, -92)), "Home", home)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `AP disappearance fires after two consecutive missing cycles`() {
        analyzer.analyze(listOf(ap(home, -50), ap(neighbour1, -70)), "Home", home)

        assertTrue(analyzer.analyze(listOf(ap(neighbour1, -70)), "Home", home).isEmpty())
        val gone = analyzer.analyze(listOf(ap(neighbour1, -70)), "Home", home).single()

        assertEquals(AttackType.AP_DISAPPEARANCE, gone.type)
        assertEquals(AlertLevel.HIGH, gone.level)
        assertEquals("Home", gone.affectedSsid)
    }

    @Test
    fun `disappearance counter resets when the AP returns and empty scans are ignored`() {
        analyzer.analyze(listOf(ap(home, -50), ap(neighbour1, -70)), "Home", home)
        analyzer.analyze(listOf(ap(neighbour1, -70)), "Home", home)
        analyzer.analyze(listOf(ap(home, -50), ap(neighbour1, -70)), "Home", home) // back
        analyzer.analyze(emptyList(), "Home", home)                                  // empty scan: no evidence

        assertTrue(analyzer.analyze(listOf(ap(neighbour1, -70)), "Home", home).isEmpty())
    }

    @Test
    fun `rapid reconnect fires at five cycles within two minutes`() {
        repeat(4) { analyzer.recordReconnect(home) }
        assertTrue(analyzer.analyze(listOf(ap(home, -50)), "Home", home).isEmpty())

        analyzer.recordReconnect(home)
        val ev = analyzer.analyze(listOf(ap(home, -50)), "Home", home).single()

        assertEquals(AttackType.RAPID_RECONNECT, ev.type)
        assertEquals(AlertLevel.CRITICAL, ev.level)
        assertEquals(home, ev.affectedBssid)
    }

    @Test
    fun `channel hopping flags an unexpected channel change`() {
        analyzer.analyze(listOf(ap(home, -50, frequency = 2437)), "Home", home)
        val ev = analyzer.analyze(listOf(ap(home, -50, frequency = 2462)), "Home", home).single()

        assertEquals(AttackType.CHANNEL_HOPPING, ev.type)
        assertEquals(AlertLevel.MEDIUM, ev.level)
        assertTrue(ev.detail.contains("channel 6 to channel 11"))
    }

    @Test
    fun `deauth flood counts network losses reported by the connectivity callback`() {
        val callback = trackConnectivity()
        val network = mockk<Network>()

        repeat(4) { callback.onLost(network) }
        callback.onAvailable(network)

        assertEquals(4, disconnectCount)
        assertEquals(4, analyzer.getDisconnectCount())
        val flood = analyzer.analyze(listOf(ap(home, -50)), "Home", home).single()
        assertEquals(AttackType.DEAUTH_FLOOD, flood.type)
        assertEquals(AlertLevel.CRITICAL, flood.level)
        assertNull(flood.affectedBssid)
    }

    @Test
    fun `three disconnects are below the flood threshold`() {
        val callback = trackConnectivity()
        repeat(3) { callback.onLost(mockk()) }

        assertTrue(analyzer.analyze(listOf(ap(home, -50)), "Home", home).isEmpty())
    }

    @Test
    fun `nothing is detected without a connected network`() {
        analyzer.analyze(listOf(ap(home, -50)), null, null)
        repeat(6) { analyzer.recordReconnect(home) }

        assertTrue(analyzer.analyze(listOf(ap(home, -90)), null, null).isEmpty())
    }

    @Test
    fun `reset clears history and counters`() {
        analyzer.analyze(listOf(ap(home, -50)), "Home", home)
        analyzer.recordReconnect(home)

        analyzer.reset()

        assertTrue(analyzer.getApHistory().isEmpty())
        assertEquals(0, analyzer.getDisconnectCount())
        assertTrue(analyzer.analyze(listOf(ap(home, -50)), "Home", home).isEmpty())
    }
}

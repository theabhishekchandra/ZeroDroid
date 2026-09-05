package com.abhishek.zerodroid.features.bluetooth_tracker.domain

import com.abhishek.zerodroid.features.ble.domain.BleDevice
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackerIdentifierTest {

    private val identifier = TrackerIdentifier()

    private fun ble(name: String?, uuids: List<String> = emptyList(), rssi: Int = -60) =
        BleDevice(name = name, address = "AA:BB:CC:DD:EE:FF", rssi = rssi, serviceUuids = uuids)

    @Test
    fun `identifies brands by advertised name`() {
        assertEquals(TrackerType.AIRTAG, identifier.identify(ble("AirTag")))
        assertEquals(TrackerType.SMARTTAG, identifier.identify(ble("Galaxy SmartTag2")))
        assertEquals(TrackerType.TILE, identifier.identify(ble("Tile Mate")))
        assertEquals(TrackerType.CHIPOLO, identifier.identify(ble("chipolo one")))
        assertEquals(TrackerType.PEBBLEBEE, identifier.identify(ble("PB-1234")))
    }

    @Test
    fun `identifies AirTag and Tile by service uuid when unnamed`() {
        assertEquals(TrackerType.AIRTAG, identifier.identify(ble(null, listOf("7dfc9000-7d1c-4951-86aa-8d9728f8d66c"))))
        assertEquals(TrackerType.TILE, identifier.identify(ble(null, listOf("0000feed-0000-1000-8000-00805f9b34fb"))))
    }

    @Test
    fun `generic tracker names fall back to GENERIC_TRACKER`() {
        assertEquals(TrackerType.GENERIC_TRACKER, identifier.identify(ble("Find My Keys")))
        assertEquals(TrackerType.GENERIC_TRACKER, identifier.identify(ble("Tractive GPS")))
    }

    @Test
    fun `ordinary devices are UNKNOWN`() {
        assertEquals(TrackerType.UNKNOWN, identifier.identify(ble("JBL Flip 6")))
        assertEquals(TrackerType.UNKNOWN, identifier.identify(ble(null)))
        assertEquals(TrackerType.UNKNOWN, identifier.identifyByManufacturerHint("AA:BB", "x"))
    }

    private fun tracker(type: TrackerType, seen: Int, durationMs: Long) = DetectedTracker(
        address = "AA", name = null, type = type, rssi = -70,
        firstSeen = 1_000L, lastSeen = 1_000L + durationMs, seenCount = seen, risk = TrackingRisk.NONE
    )

    @Test
    fun `risk escalates with sightings and dwell time`() {
        assertEquals(TrackingRisk.NONE, identifier.assessRisk(tracker(TrackerType.UNKNOWN, 100, 3_600_000L)))
        assertEquals(TrackingRisk.LOW, identifier.assessRisk(tracker(TrackerType.AIRTAG, 1, 0L)))
        assertEquals(TrackingRisk.MEDIUM, identifier.assessRisk(tracker(TrackerType.AIRTAG, 4, 60_000L)))
        // many sightings but short dwell stays MEDIUM
        assertEquals(TrackingRisk.MEDIUM, identifier.assessRisk(tracker(TrackerType.AIRTAG, 10, 60_000L)))
        // long dwell but few sightings stays LOW
        assertEquals(TrackingRisk.LOW, identifier.assessRisk(tracker(TrackerType.TILE, 3, 20 * 60_000L)))
        assertEquals(TrackingRisk.HIGH, identifier.assessRisk(tracker(TrackerType.TILE, 6, 11 * 60_000L)))
    }

    @Test
    fun `detected tracker exposes display name and signal percent`() {
        val t = tracker(TrackerType.SMARTTAG, 1, 0L)
        assertEquals("SmartTag", t.displayName)
        assertEquals("Mine", t.copy(name = "Mine").displayName)
        assertEquals(60, t.signalPercent)
    }
}

package com.abhishek.zerodroid.features.ble.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BleDistanceEstimatorTest {

    @Test
    fun `rssi of zero is treated as unknown`() {
        assertEquals(-1.0, BleDistanceEstimator.estimateDistance(0), 0.0)
    }

    @Test
    fun `rssi equal to tx power estimates about 1 meter`() {
        val distance = BleDistanceEstimator.estimateDistance(rssi = -59, txPower = -59)
        assertEquals(1.0, distance, 0.01)
    }

    @Test
    fun `weaker rssi estimates a larger distance`() {
        val near = BleDistanceEstimator.estimateDistance(rssi = -60)
        val far = BleDistanceEstimator.estimateDistance(rssi = -90)
        assertTrue(far > near)
    }

    @Test
    fun `distance label for unknown distance`() {
        assertEquals("Unknown", BleDistanceEstimator.getDistanceLabel(-1.0))
    }

    @Test
    fun `distance label for immediate range`() {
        assertEquals("Immediate", BleDistanceEstimator.getDistanceLabel(0.2))
    }

    @Test
    fun `distance label for near range includes the value`() {
        assertEquals("Near (1.5m)", BleDistanceEstimator.getDistanceLabel(1.5))
    }

    @Test
    fun `distance label for medium range includes the value`() {
        assertEquals("Medium (5.0m)", BleDistanceEstimator.getDistanceLabel(5.0))
    }

    @Test
    fun `distance label for far range rounds to whole meters`() {
        assertEquals("Far (15m)", BleDistanceEstimator.getDistanceLabel(15.0))
    }

    @Test
    fun `proximity glyphs follow the same thresholds as labels`() {
        assertEquals("?", BleDistanceEstimator.getProximityLabel(-1.0))
        assertEquals("●", BleDistanceEstimator.getProximityLabel(0.2))
        assertEquals("◉", BleDistanceEstimator.getProximityLabel(1.5))
        assertEquals("○", BleDistanceEstimator.getProximityLabel(5.0))
        assertEquals("◌", BleDistanceEstimator.getProximityLabel(15.0))
    }

    @Test
    fun `range label is a span past a metre and capped far away`() {
        org.junit.Assert.assertEquals("~1 m", BleDistanceEstimator.rangeLabel(-55))
        val mid = BleDistanceEstimator.rangeLabel(-70)
        org.junit.Assert.assertTrue(mid, Regex("~\\d+–\\d+ m").matches(mid))
        org.junit.Assert.assertEquals("30 m+", BleDistanceEstimator.rangeLabel(-100))
        org.junit.Assert.assertEquals("range unknown", BleDistanceEstimator.rangeLabel(0))
    }
}

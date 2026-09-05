package com.abhishek.zerodroid.features.celltower.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarrierLookupTest {

    @Test
    fun `resolves Indian carriers across both country codes`() {
        assertEquals("Airtel", CarrierLookup.lookup(404, 45))
        assertEquals("Vi (Vodafone Idea)", CarrierLookup.lookup(404, 1))
        assertEquals("BSNL", CarrierLookup.lookup(404, 34))
        assertEquals("Jio", CarrierLookup.lookup(405, 857))
        assertEquals("Indian Railways (GSM-R)", CarrierLookup.lookup(405, 48))
    }

    @Test
    fun `resolves international carriers`() {
        assertEquals("T-Mobile", CarrierLookup.lookup(310, 260))
        assertEquals("EE", CarrierLookup.lookup(234, 30))
        assertEquals("Deutsche Telekom", CarrierLookup.lookup(262, 1))
        assertEquals("NTT docomo", CarrierLookup.lookup(440, 10))
        assertEquals("China Mobile", CarrierLookup.lookup(460, 0))
    }

    @Test
    fun `returns null for missing or unknown codes`() {
        assertNull(CarrierLookup.lookup(null, 45))
        assertNull(CarrierLookup.lookup(404, null))
        assertNull(CarrierLookup.lookup(999, 1))
        assertNull(CarrierLookup.lookup(404, 999))
    }

    @Test
    fun `cell tower info maps rssi to a percentage`() {
        fun cell(rssi: Int) = CellTowerInfo(CellType.LTE, 404, 45, 1, 1L, rssi, null)

        assertEquals(100, cell(-60).signalPercent)
        assertEquals(100, cell(-70).signalPercent)
        assertEquals(50, cell(-95).signalPercent)
        assertEquals(0, cell(-120).signalPercent)
        assertEquals(0, cell(-130).signalPercent)
    }

    @Test
    fun `cell types have display names`() {
        assertEquals("4G LTE", CellType.LTE.displayName)
        assertEquals("5G NR", CellType.NR.displayName)
        assertEquals("Unknown", CellType.UNKNOWN.displayName)
    }
}

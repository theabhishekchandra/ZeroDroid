package com.abhishek.zerodroid.features.nfc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcStateTest {

    @Test
    fun `write mode follows the write tab`() {
        assertFalse(NfcState().writeMode)
        assertTrue(NfcState(tab = NfcTab.WRITE).writeMode)
        assertFalse(NfcState(tab = NfcTab.MIFARE).writeMode)
    }

    @Test
    fun `tabs are in design order`() {
        assertEquals(listOf("Read", "Write", "MIFARE", "Emulate"), NfcTab.entries.map { it.label })
    }
}

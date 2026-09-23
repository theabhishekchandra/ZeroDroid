package com.abhishek.zerodroid.features.sweep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SweepPresetTest {

    @Test
    fun `full sweep runs every check`() {
        assertEquals(SweepCheck.entries.toList(), SweepPreset.FULL.checks)
    }

    @Test
    fun `hardware needs follow the checks`() {
        assertTrue(SweepPreset.RF_BUG.needsMicrophone)
        assertFalse(SweepPreset.HIDDEN_CAMERA.needsMicrophone)
        assertTrue(SweepPreset.TRACKER_CHECK.needsRadios)
        assertFalse(SweepPreset.TRACKER_CHECK.needsMicrophone)
    }
}

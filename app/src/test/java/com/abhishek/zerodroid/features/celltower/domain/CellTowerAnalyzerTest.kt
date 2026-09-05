package com.abhishek.zerodroid.features.celltower.domain

import android.telephony.TelephonyManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CellTowerAnalyzerTest {

    @Test
    fun `without a SIM the state says so and has no registered cell`() = runTest {
        val tm = mockk<TelephonyManager> {
            every { allCellInfo } returns emptyList()
            every { simState } returns TelephonyManager.SIM_STATE_ABSENT
        }

        val state = CellTowerAnalyzer(tm).monitor().first()

        assertTrue(state.isMonitoring)
        assertTrue(state.simAbsent)
        assertNull(state.currentCell)
        assertTrue(state.neighbors.isEmpty())
    }

    @Test
    fun `with a SIM but no cells yet the state is simply empty`() = runTest {
        val tm = mockk<TelephonyManager> {
            every { allCellInfo } returns emptyList()
            every { simState } returns TelephonyManager.SIM_STATE_READY
        }

        val state = CellTowerAnalyzer(tm).monitor().first()

        assertFalse(state.simAbsent)
        assertNull(state.currentCell)
    }

    @Test
    fun `missing telephony manager yields an empty state rather than a crash`() = runTest {
        val state = CellTowerAnalyzer(null).monitor().first()

        assertNull(state.currentCell)
        assertEquals(0, state.neighbors.size)
        assertFalse(state.simAbsent)
    }
}

package com.abhishek.zerodroid.features.gps.viewmodel

import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.core.testing.MainDispatcherRule
import com.abhishek.zerodroid.features.gps.domain.GpsState
import com.abhishek.zerodroid.features.gps.domain.GpsTracker
import com.abhishek.zerodroid.features.gps.domain.SatelliteInfo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GpsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val tracker = mockk<GpsTracker>()

    private fun sat(svid: Int, cn0: Float, inFix: Boolean) = SatelliteInfo(svid, 1, cn0, 40f, 90f, inFix)

    @Test
    fun `toggleTracking starts tracking and sorts satellites by fix then signal`() {
        val fix = GpsState(
            isTracking = true, latitude = 12.9, longitude = 77.5, satelliteCount = 3,
            satellites = listOf(sat(1, 20f, false), sat(2, 30f, true), sat(3, 45f, false), sat(4, 25f, true))
        )
        every { tracker.track() } returns flowOf(fix)
        val vm = GpsViewModel(tracker, DemoDataBus())

        vm.toggleTracking()

        assertTrue(vm.state.value.isTracking)
        assertEquals(12.9, vm.state.value.latitude, 0.0)
        assertEquals(listOf(2, 4, 3, 1), vm.state.value.satellites.map { it.svid })
    }

    @Test
    fun `toggleTracking again stops tracking`() {
        every { tracker.track() } returns MutableSharedFlow()
        val vm = GpsViewModel(tracker, DemoDataBus())
        vm.toggleTracking()
        assertTrue(vm.state.value.isTracking)

        vm.toggleTracking()

        assertFalse(vm.state.value.isTracking)
    }

    @Test
    fun `provider failure stops tracking with an error message`() {
        every { tracker.track() } returns flow { throw SecurityException("no location permission") }
        val vm = GpsViewModel(tracker, DemoDataBus())

        vm.startTracking()

        assertFalse(vm.state.value.isTracking)
        assertEquals("no location permission", vm.state.value.error)
    }

    @Test
    fun `restarting tracking resets stale state`() {
        every { tracker.track() } returns flow { throw IllegalStateException("boom") } andThen MutableSharedFlow()
        val vm = GpsViewModel(tracker, DemoDataBus())
        vm.startTracking()
        assertEquals("boom", vm.state.value.error)

        vm.startTracking()

        assertTrue(vm.state.value.isTracking)
        assertEquals(null, vm.state.value.error)
    }
}

package com.abhishek.zerodroid.features.wifiaware.viewmodel

import com.abhishek.zerodroid.core.debug.DemoDataBus
import com.abhishek.zerodroid.features.wifiaware.domain.WifiAwarePeer
import com.abhishek.zerodroid.features.wifiaware.domain.WifiAwareService
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WifiAwareViewModelTest {

    private val service = mockk<WifiAwareService>()
    private val attachCallback = slot<(Boolean) -> Unit>()
    private lateinit var viewModel: WifiAwareViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { service.isAvailable } returns true
        every { service.observeAvailability() } returns flowOf(true)
        every { service.attach(capture(attachCallback)) } answers { }
        justRun { service.detach() }
        justRun { service.publish(any(), any()) }
        justRun { service.subscribe(any(), any()) }
        justRun { service.stopPublish() }
        justRun { service.stopSubscribe() }
        viewModel = WifiAwareViewModel(service, DemoDataBus())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun attachAndAccept() {
        viewModel.attachSession()
        attachCallback.captured(true)
    }

    @Test
    fun `attach success marks the session attached`() {
        attachAndAccept()

        assertTrue(viewModel.state.value.isSessionAttached)
    }

    @Test
    fun `pause detaches and clears publish subscribe and peers`() {
        attachAndAccept()
        viewModel.togglePublish()
        viewModel.toggleSubscribe()

        viewModel.pauseSession()

        val state = viewModel.state.value
        assertFalse(state.isSessionAttached)
        assertFalse(state.isPublishing)
        assertFalse(state.isSubscribing)
        assertTrue(state.discoveredPeers.isEmpty())
        verify(exactly = 1) { service.detach() }
    }

    @Test
    fun `resume re-attaches and restores publish and subscribe`() {
        attachAndAccept()
        viewModel.togglePublish()
        viewModel.toggleSubscribe()
        viewModel.pauseSession()

        viewModel.resumeSession()
        attachCallback.captured(true)

        val state = viewModel.state.value
        assertTrue(state.isSessionAttached)
        assertTrue(state.isPublishing)
        assertTrue(state.isSubscribing)
        verify(exactly = 2) { service.publish("zerodroid", any()) }
        verify(exactly = 2) { service.subscribe("zerodroid", any()) }
    }

    @Test
    fun `resume only restores what was active before the pause`() {
        attachAndAccept()
        viewModel.togglePublish()
        viewModel.pauseSession()

        viewModel.resumeSession()
        attachCallback.captured(true)

        assertTrue(viewModel.state.value.isPublishing)
        assertFalse(viewModel.state.value.isSubscribing)
        verify(exactly = 0) { service.subscribe(any(), any()) }
    }

    @Test
    fun `failed re-attach reports an error and restores nothing`() {
        attachAndAccept()
        viewModel.togglePublish()
        viewModel.pauseSession()

        viewModel.resumeSession()
        attachCallback.captured(false)

        val state = viewModel.state.value
        assertFalse(state.isSessionAttached)
        assertFalse(state.isPublishing)
        assertNotNull(state.error)
        verify(exactly = 1) { service.publish(any(), any()) }
    }

    @Test
    fun `restore intent is consumed by a single resume`() {
        attachAndAccept()
        viewModel.togglePublish()
        viewModel.pauseSession()
        viewModel.resumeSession()
        attachCallback.captured(true)
        viewModel.togglePublish() // user stops publishing

        viewModel.pauseSession()
        viewModel.resumeSession()
        attachCallback.captured(true)

        assertFalse(viewModel.state.value.isPublishing)
    }

    @Test
    fun `discovered peers accumulate from publish callbacks`() {
        val peerCallback = slot<(WifiAwarePeer) -> Unit>()
        every { service.publish(any(), capture(peerCallback)) } answers { }
        attachAndAccept()
        viewModel.togglePublish()

        peerCallback.captured(WifiAwarePeer("1", "zerodroid", null))
        peerCallback.captured(WifiAwarePeer("2", "zerodroid", null))

        assertEquals(2, viewModel.state.value.discoveredPeers.size)
    }

    @Test
    fun `service name changes flow into publish`() {
        attachAndAccept()
        viewModel.setServiceName("lab")

        viewModel.togglePublish()

        verify { service.publish("lab", any()) }
    }
}

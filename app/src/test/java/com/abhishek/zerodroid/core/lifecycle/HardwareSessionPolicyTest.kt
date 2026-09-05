package com.abhishek.zerodroid.core.lifecycle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareSessionPolicyTest {

    @Test
    fun `background while active pauses and arms resume`() {
        val policy = HardwareSessionPolicy()

        assertEquals(HardwareSessionAction.PAUSE, policy.onBackground(isActive = true))
        assertTrue(policy.isPausedByLifecycle)
        assertEquals(HardwareSessionAction.RESUME, policy.onForeground())
        assertFalse(policy.isPausedByLifecycle)
    }

    @Test
    fun `background while idle does nothing and does not resume later`() {
        val policy = HardwareSessionPolicy()

        assertEquals(HardwareSessionAction.NONE, policy.onBackground(isActive = false))
        assertEquals(HardwareSessionAction.NONE, policy.onForeground())
    }

    @Test
    fun `foreground without a prior pause does nothing`() {
        // Adding an observer to an already-started lifecycle replays ON_START.
        val policy = HardwareSessionPolicy()

        assertEquals(HardwareSessionAction.NONE, policy.onForeground())
    }

    @Test
    fun `resume fires only once per pause`() {
        val policy = HardwareSessionPolicy()
        policy.onBackground(isActive = true)

        assertEquals(HardwareSessionAction.RESUME, policy.onForeground())
        assertEquals(HardwareSessionAction.NONE, policy.onForeground())
    }

    @Test
    fun `resumeOnForeground false pauses but never resumes`() {
        val policy = HardwareSessionPolicy(resumeOnForeground = false)

        assertEquals(HardwareSessionAction.PAUSE, policy.onBackground(isActive = true))
        assertFalse(policy.isPausedByLifecycle)
        assertEquals(HardwareSessionAction.NONE, policy.onForeground())
    }

    @Test
    fun `dispose always stops and clears pending resume`() {
        val policy = HardwareSessionPolicy()
        policy.onBackground(isActive = true)

        assertEquals(HardwareSessionAction.STOP, policy.onDispose())
        assertFalse(policy.isPausedByLifecycle)
        assertEquals(HardwareSessionAction.NONE, policy.onForeground())
    }

    @Test
    fun `dispose while idle still stops`() {
        val policy = HardwareSessionPolicy()

        assertEquals(HardwareSessionAction.STOP, policy.onDispose())
    }

    @Test
    fun `user stop during background cancels the pending resume`() {
        val policy = HardwareSessionPolicy()
        policy.onBackground(isActive = true)

        policy.onUserStopped()

        assertEquals(HardwareSessionAction.NONE, policy.onForeground())
    }

    @Test
    fun `repeated background and foreground cycles behave consistently`() {
        val policy = HardwareSessionPolicy()

        repeat(3) {
            assertEquals(HardwareSessionAction.PAUSE, policy.onBackground(isActive = true))
            assertEquals(HardwareSessionAction.RESUME, policy.onForeground())
        }
        assertEquals(HardwareSessionAction.NONE, policy.onBackground(isActive = false))
        assertEquals(HardwareSessionAction.NONE, policy.onForeground())
    }
}

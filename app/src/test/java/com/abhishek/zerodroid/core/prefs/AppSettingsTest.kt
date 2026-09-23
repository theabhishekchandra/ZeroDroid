package com.abhishek.zerodroid.core.prefs

import com.abhishek.zerodroid.core.testing.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `defaults favour privacy and a month of history`() {
        val s = AppSettings(FakeSharedPreferences())
        assertTrue(s.redactExports.value)
        assertEquals(30, s.retentionDays.value)
        assertTrue(s.keepScreenOnDuringSweeps.value)
        assertFalse(s.onboardingDone.value)
        assertEquals(30L * 24 * 60 * 60 * 1000, s.retentionMs)
    }

    @Test
    fun `changes persist across instances`() {
        val prefs = FakeSharedPreferences()
        AppSettings(prefs).apply {
            setRedactExports(false)
            setRetentionDays(7)
            setTrustedNetworks(setOf(" Home ", "", "Office"))
            setOnboardingDone(true)
        }
        val reloaded = AppSettings(prefs)
        assertFalse(reloaded.redactExports.value)
        assertEquals(7, reloaded.retentionDays.value)
        assertEquals(setOf("Home", "Office"), reloaded.trustedNetworks.value)
        assertTrue(reloaded.onboardingDone.value)
    }
}

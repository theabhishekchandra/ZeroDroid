package com.abhishek.zerodroid.core.prefs

import android.content.SharedPreferences
import com.abhishek.zerodroid.navigation.ToolCatalog
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolPreferencesTest {

    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val stored = mutableMapOf<String, String?>()
    private val prefs = mockk<SharedPreferences> {
        every { getString(any(), any()) } answers { stored[firstArg()] ?: secondArg() }
        every { edit() } returns editor
    }

    init {
        every { editor.putString(any(), any()) } answers {
            stored[firstArg()] = secondArg()
            editor
        }
    }

    @Test
    fun `pins fall back to catalog defaults when nothing is stored`() {
        assertEquals(ToolCatalog.defaultPinnedRoutes, ToolPreferences(prefs).pinnedRoutes.value)
    }

    @Test
    fun `stored pins are read back in order`() {
        stored[ToolPreferences.KEY_PINNED] = "gps,nfc"
        assertEquals(listOf("gps", "nfc"), ToolPreferences(prefs).pinnedRoutes.value)
    }

    @Test
    fun `an empty stored list means nothing pinned, not defaults`() {
        stored[ToolPreferences.KEY_PINNED] = ""
        assertEquals(emptyList<String>(), ToolPreferences(prefs).pinnedRoutes.value)
    }

    @Test
    fun `toggle pins then unpins and persists each time`() {
        val toolPrefs = ToolPreferences(prefs)

        assertTrue(toolPrefs.togglePin("gps"))
        assertTrue(toolPrefs.isPinned("gps"))
        assertEquals("wifi,bluetooth_tracker,ble,nfc,gps", stored[ToolPreferences.KEY_PINNED])

        assertFalse(toolPrefs.togglePin("wifi"))
        assertFalse(toolPrefs.isPinned("wifi"))
        assertEquals("bluetooth_tracker,ble,nfc,gps", stored[ToolPreferences.KEY_PINNED])
    }

    @Test
    fun `recording an opened tool updates last used`() {
        val toolPrefs = ToolPreferences(prefs)
        assertNull(toolPrefs.lastUsed.value)

        toolPrefs.recordOpened("gps", "GPS Tracker")

        assertEquals(LastUsedFeature("gps", "GPS Tracker"), toolPrefs.lastUsed.value)
        verify { editor.putString(ToolPreferences.KEY_LAST_ROUTE, "gps") }
    }
}

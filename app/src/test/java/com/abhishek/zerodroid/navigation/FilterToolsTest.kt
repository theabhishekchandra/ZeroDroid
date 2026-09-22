package com.abhishek.zerodroid.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterToolsTest {

    private val tools = ToolCatalog.tools
    private val allAvailable: (ToolInfo) -> Boolean = { true }

    @Test
    fun `no filters returns every tool`() {
        assertEquals(tools, filterTools(tools, null, "", false, allAvailable))
    }

    @Test
    fun `group filter keeps only that group`() {
        val result = filterTools(tools, ToolGroup.LOG, "", false, allAvailable)
        assertEquals(setOf("Signal Logger", "Wardriving"), result.map { it.name }.toSet())
    }

    @Test
    fun `query matches name, job and hardware tag case-insensitively`() {
        assertEquals(setOf("Tracker Scanner", "GPS Tracker"), filterTools(tools, null, "  TRACKER ", false, allAvailable).map { it.name }.toSet())
        assertTrue(filterTools(tools, null, "airtag", false, allAvailable).any { it.name == "Tracker Scanner" })
        assertTrue(filterTools(tools, null, "otg", false, allAvailable).all { it.requirement == HardwareRequirement.OTG })
    }

    @Test
    fun `only supported hides unavailable tools`() {
        val noIr: (ToolInfo) -> Boolean = { it.requirement != HardwareRequirement.IR }
        val result = filterTools(tools, null, "", true, noIr)
        assertTrue(result.none { it.name == "IR Remote" })
        assertEquals(tools.size - 1, result.size)
    }

    @Test
    fun `filters combine`() {
        val result = filterTools(tools, ToolGroup.DETECT, "wifi", false, allAvailable)
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.group == ToolGroup.DETECT })
    }
}

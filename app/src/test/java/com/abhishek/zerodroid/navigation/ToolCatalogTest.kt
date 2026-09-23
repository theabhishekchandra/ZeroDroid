package com.abhishek.zerodroid.navigation

import com.abhishek.zerodroid.core.ui.HelpContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCatalogTest {

    private val tools = ToolCatalog.tools

    @Test
    fun `every tool destination is in the catalog exactly once`() {
        val topLevel = setOf(ZeroDroidScreen.Dashboard, ZeroDroidScreen.Tools, ZeroDroidScreen.AlertCenter)
        val toolScreens = ZeroDroidScreen.all.filterNot { it in topLevel }

        assertEquals(28, tools.size)
        assertEquals(toolScreens.map { it.route }.toSet(), tools.map { it.route }.toSet())
        assertEquals(tools.size, tools.map { it.route }.distinct().size)
    }

    @Test
    fun `paths are unique terminal paths under tools`() {
        assertTrue(tools.all { it.path.startsWith("/tools/") })
        assertEquals(tools.size, tools.map { it.path }.distinct().size)
    }

    @Test
    fun `every group has at least one tool`() {
        ToolGroup.entries.forEach { group ->
            assertTrue("$group is empty", tools.any { it.group == group })
        }
    }

    @Test
    fun `lookup by route finds tools and ignores unknown routes`() {
        assertEquals("WiFi Analyzer", ToolCatalog.forRoute("wifi")?.name)
        assertNull(ToolCatalog.forRoute("dashboard"))
        assertNull(ToolCatalog.forRoute(null))
    }

    @Test
    fun `default pins all resolve to catalog tools`() {
        ToolCatalog.defaultPinnedRoutes.forEach { assertNotNull(it, ToolCatalog.forRoute(it)) }
    }

    @Test
    fun `every tool has complete help and nothing else does`() {
        assertEquals(tools.map { it.route }.toSet(), HelpContent.features.keys)
        HelpContent.features.forEach { (route, help) ->
            assertTrue("$route has no steps", help.steps.isNotEmpty())
            assertTrue("$route has no explanation", !help.howItWorks.isNullOrBlank())
            assertTrue("$route has no facts", help.facts.isNotEmpty())
        }
    }
}

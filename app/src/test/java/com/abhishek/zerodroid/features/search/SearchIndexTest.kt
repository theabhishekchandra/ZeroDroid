package com.abhishek.zerodroid.features.search

import com.abhishek.zerodroid.core.ui.HelpContent
import com.abhishek.zerodroid.navigation.ToolCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchIndexTest {

    private val tools = ToolCatalog.tools

    @Test
    fun `blank query finds nothing`() {
        assertTrue(SearchIndex.tools(tools, "  ").isEmpty())
        assertTrue(SearchIndex.actions(SearchViewModel.ACTIONS, "").isEmpty())
        assertTrue(SearchIndex.learn(tools, HelpContent.features, "").isEmpty())
    }

    @Test
    fun `name matches rank before description matches`() {
        val results = SearchIndex.tools(tools, "tracker")
        assertEquals("Tracker Scanner", results.first().name)
    }

    @Test
    fun `matching ignores case`() {
        assertEquals(SearchIndex.tools(tools, "wifi"), SearchIndex.tools(tools, "WiFi"))
    }

    @Test
    fun `actions match on keywords`() {
        assertEquals("Full room sweep", SearchIndex.actions(SearchViewModel.ACTIONS, "hotel").first().label)
    }

    @Test
    fun `learn returns the sentence that contains the query`() {
        val hits = SearchIndex.learn(tools, HelpContent.features, "airtag")
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.snippet.lowercase().contains("airtag") })
    }

    @Test
    fun `learn needs at least three characters`() {
        assertTrue(SearchIndex.learn(tools, HelpContent.features, "ai").isEmpty())
    }
}

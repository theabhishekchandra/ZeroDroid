package com.abhishek.zerodroid.features.search

import com.abhishek.zerodroid.core.ui.FeatureHelp
import com.abhishek.zerodroid.navigation.ToolInfo

/** Something the palette can run that isn't a single tool. */
data class SearchAction(val label: String, val detail: String, val route: String, val keywords: String = "")

/** A help passage that answers the query, and the tool whose help sheet it lives in. */
data class LearnHit(val tool: ToolInfo, val title: String, val snippet: String)

/** Pure matching for the search palette, kept apart from the UI so it can be tested. */
object SearchIndex {

    fun tools(all: List<ToolInfo>, query: String): List<ToolInfo> {
        val q = query.norm()
        if (q.isEmpty()) return emptyList()
        return all
            .filter { "${it.name} ${it.job} ${it.requirement.tag}".norm().contains(q) }
            // Name matches first, then prefix matches within them.
            .sortedWith(compareBy({ !it.name.norm().contains(q) }, { !it.name.norm().startsWith(q) }))
    }

    fun actions(all: List<SearchAction>, query: String): List<SearchAction> {
        val q = query.norm()
        if (q.isEmpty()) return emptyList()
        return all.filter { "${it.label} ${it.detail} ${it.keywords}".norm().contains(q) }
    }

    fun learn(tools: List<ToolInfo>, help: Map<String, FeatureHelp>, query: String, limit: Int = 5): List<LearnHit> {
        val q = query.norm()
        if (q.length < 3) return emptyList()
        return tools.mapNotNull { tool ->
            val h = help[tool.route] ?: return@mapNotNull null
            val passages = listOfNotNull(h.howItWorks) + h.steps + h.description
            val sentence = passages.flatMap { it.split(SENTENCE) }.firstOrNull { it.norm().contains(q) }
                ?: return@mapNotNull null
            LearnHit(tool, h.title, sentence.trim())
        }.take(limit)
    }

    private val SENTENCE = Regex("(?<=[.!?])\\s+")

    private fun String.norm() = trim().lowercase()
}

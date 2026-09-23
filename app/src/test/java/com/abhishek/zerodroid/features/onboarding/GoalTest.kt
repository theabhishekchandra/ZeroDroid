package com.abhishek.zerodroid.features.onboarding

import com.abhishek.zerodroid.navigation.ToolCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalTest {

    @Test
    fun `every goal pins real tools`() {
        val routes = ToolCatalog.tools.map { it.route }.toSet()
        Goal.entries.forEach { goal -> assertTrue(goal.name, routes.containsAll(goal.tools)) }
    }

    @Test
    fun `pins are deduplicated, in goal order, and capped`() {
        assertEquals(listOf("bluetooth_tracker", "ble"), Goal.pinsFor(setOf(Goal.TRACKED)))
        val all = Goal.pinsFor(Goal.entries.toSet())
        assertEquals(all.distinct(), all)
        assertEquals(Goal.MAX_PINS, all.size)
        assertEquals("hidden_camera", all.first())
    }

    @Test
    fun `no goals pins nothing`() {
        assertTrue(Goal.pinsFor(emptySet()).isEmpty())
    }
}

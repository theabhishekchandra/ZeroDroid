package com.abhishek.zerodroid.features.watch.data

import com.abhishek.zerodroid.core.testing.FakeSharedPreferences
import com.abhishek.zerodroid.features.watch.domain.RuleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchRuleStoreTest {

    @Test
    fun `enabled rules persist and pause hides them`() {
        val prefs = FakeSharedPreferences()
        val store = WatchRuleStore(prefs)
        store.setEnabled(RuleKind.FOLLOW_ME.name, true)
        assertEquals(listOf(RuleKind.FOLLOW_ME), store.active.map { it.kind })

        store.setPaused(true)
        assertTrue(store.active.isEmpty())

        val reopened = WatchRuleStore(prefs)
        assertTrue(reopened.paused.value)
        assertTrue(reopened.rules.value.first { it.kind == RuleKind.FOLLOW_ME }.enabled)
    }

    @Test
    fun `custom rules can be added and removed, built-ins cannot be removed`() {
        val store = WatchRuleStore(FakeSharedPreferences())
        val custom = store.addCustom(-55, 5)
        assertEquals(5, store.rules.value.size)
        store.remove(RuleKind.CELL_2G.name)
        assertEquals(5, store.rules.value.size)
        store.remove(custom.id)
        assertEquals(4, store.rules.value.size)
    }
}

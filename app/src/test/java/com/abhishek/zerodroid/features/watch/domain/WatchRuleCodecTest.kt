package com.abhishek.zerodroid.features.watch.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchRuleCodecTest {

    @Test
    fun `nothing stored gives every built-in, off`() {
        val rules = WatchRuleCodec.decode(null)
        assertEquals(RuleKind.entries.filter { it.builtIn }, rules.map { it.kind })
        assertTrue(rules.none { it.enabled })
    }

    @Test
    fun `round trip keeps state and custom rules`() {
        val rules = WatchRule.builtIns.map { if (it.kind == RuleKind.CELL_2G) it.copy(enabled = true) else it } +
            WatchRule("custom-1", RuleKind.CUSTOM_BLE, enabled = true, rssiThreshold = -55, minutes = 30)
        assertEquals(rules, WatchRuleCodec.decode(WatchRuleCodec.encode(rules)))
    }

    @Test
    fun `corrupt or unknown entries are dropped`() {
        assertEquals(WatchRule.builtIns, WatchRuleCodec.decode("not json"))
        val odd = """[{"id":"X","kind":"TELEPORT","enabled":true}]"""
        assertEquals(WatchRule.builtIns, WatchRuleCodec.decode(odd))
    }

    @Test
    fun `rogue rule explains when there are no trusted networks`() {
        val rule = WatchRule(RuleKind.ROGUE_TRUSTED.name, RuleKind.ROGUE_TRUSTED)
        assertTrue(rule.description(emptySet()).contains("Trusted networks"))
        assertEquals("Watch Home_5G and OFFICE-2G for evil twins.", rule.description(setOf("OFFICE-2G", "Home_5G")))
        assertFalse(rule.description(setOf("a", "b", "c", "d")).contains("d for"))
    }
}

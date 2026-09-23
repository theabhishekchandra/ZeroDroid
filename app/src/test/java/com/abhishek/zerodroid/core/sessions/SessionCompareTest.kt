package com.abhishek.zerodroid.core.sessions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCompareTest {

    private fun item(key: String, rssi: Int? = -50, detail: String = "WPA2", flagged: Boolean = false) =
        SessionItem(key, "label-$key", ItemKind.WIFI, rssi, detail, flagged)

    @Test
    fun `identical runs have no differences`() {
        val a = listOf(item("a"), item("b"))
        val diff = SessionCompare.diff(a, a)
        assertTrue(diff.isEmpty)
        assertEquals(2, diff.unchangedCount)
    }

    @Test
    fun `new, gone and changed are separated by key`() {
        val before = listOf(item("a"), item("b"), item("c", rssi = -60))
        val after = listOf(item("a"), item("c", rssi = -45), item("d", flagged = true), item("e"))
        val diff = SessionCompare.diff(before, after)
        assertEquals(listOf("d", "e"), diff.added.map { it.key })
        assertEquals(listOf("b"), diff.removed.map { it.key })
        assertEquals(listOf("c"), diff.changed.map { it.after.key })
        assertTrue(diff.changed.single().what.contains("-60 → -45"))
        assertEquals(1, diff.unchangedCount)
    }

    @Test
    fun `small signal moves are noise`() {
        val diff = SessionCompare.diff(listOf(item("a", rssi = -60)), listOf(item("a", rssi = -56)))
        assertTrue(diff.isEmpty)
    }

    @Test
    fun `detail and flag changes are reported`() {
        val diff = SessionCompare.diff(listOf(item("a", detail = "WPA2")), listOf(item("a", detail = "WPA3", flagged = true)))
        val what = diff.changed.single().what
        assertTrue(what, what.contains("WPA2 → WPA3"))
        assertTrue(what, what.contains("Now flagged"))
    }
}

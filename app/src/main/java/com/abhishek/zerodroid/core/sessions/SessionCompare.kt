package com.abhishek.zerodroid.core.sessions

import kotlin.math.abs

/** Diffs two sessions by item key. Pure, so Compare's logic is testable without a database. */
object SessionCompare {

    /** Signal moves smaller than this are noise, not a change worth showing. */
    const val SIGNAL_CHANGE_DB = 6

    fun diff(before: List<SessionItem>, after: List<SessionItem>): SessionDiff {
        val beforeByKey = before.associateBy { it.key }
        val afterByKey = after.associateBy { it.key }
        val added = after.filter { it.key !in beforeByKey }
        val removed = before.filter { it.key !in afterByKey }
        val changed = mutableListOf<SessionDiff.Change>()
        var unchanged = 0
        after.forEach { a ->
            val b = beforeByKey[a.key] ?: return@forEach
            val what = describeChange(b, a)
            if (what != null) changed += SessionDiff.Change(b, a, what) else unchanged++
        }
        return SessionDiff(
            added = added.sortedByDescending { it.flagged },
            removed = removed,
            changed = changed,
            unchangedCount = unchanged
        )
    }

    private fun describeChange(b: SessionItem, a: SessionItem): String? {
        val parts = buildList {
            if (b.detail != a.detail && b.detail.isNotBlank() && a.detail.isNotBlank()) add("${b.detail} → ${a.detail}")
            if (b.rssi != null && a.rssi != null && abs(b.rssi - a.rssi) >= SIGNAL_CHANGE_DB) add("Signal ${b.rssi} → ${a.rssi} dBm")
            if (!b.flagged && a.flagged) add("Now flagged")
            if (b.flagged && !a.flagged) add("No longer flagged")
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }
}

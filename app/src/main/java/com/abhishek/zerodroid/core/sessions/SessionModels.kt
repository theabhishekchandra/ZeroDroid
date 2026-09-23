package com.abhishek.zerodroid.core.sessions

/** What kind of thing a session item is; drives its tag and grouping. */
enum class ItemKind { WIFI, BLE, BT, LAN, CAMERA, TRACKER, RF }

data class SessionItem(
    val key: String,
    val label: String,
    val kind: ItemKind,
    val rssi: Int? = null,
    val detail: String = "",
    val flagged: Boolean = false
)

data class Session(
    val id: String,
    val tool: String,
    val title: String,
    val place: String?,
    val startedAt: Long,
    val endedAt: Long,
    val itemCount: Int,
    val findingCount: Int,
    val summary: String
) {
    val durationMs: Long get() = (endedAt - startedAt).coerceAtLeast(0L)
    val isClean: Boolean get() = findingCount == 0
}

/** What changed between two sessions of comparable items. */
data class SessionDiff(
    val added: List<SessionItem>,
    val removed: List<SessionItem>,
    val changed: List<Change>,
    val unchangedCount: Int
) {
    data class Change(val before: SessionItem, val after: SessionItem, val what: String)

    val isEmpty: Boolean get() = added.isEmpty() && removed.isEmpty() && changed.isEmpty()
}

package com.abhishek.zerodroid.core.sessions

import com.abhishek.zerodroid.core.database.dao.SeenDeviceRow
import com.abhishek.zerodroid.core.database.dao.SessionDao
import com.abhishek.zerodroid.core.database.entity.SessionEntity
import com.abhishek.zerodroid.core.database.entity.SessionItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every tool run that found something is kept as a session on this phone only, so results
 * survive leaving the screen and two runs can be compared or exported.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val dao: SessionDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sessions: Flow<List<Session>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /**
     * Saves a finished run. Runs that saw nothing are not worth a session and return null,
     * unless [keepEmpty]: a clean room sweep is itself the result.
     */
    suspend fun record(
        tool: String,
        title: String,
        startedAt: Long,
        endedAt: Long,
        items: List<SessionItem>,
        summary: String,
        findingCount: Int = items.count { it.flagged },
        place: String? = null,
        keepEmpty: Boolean = false
    ): String? {
        if (items.isEmpty() && !keepEmpty) return null
        val id = UUID.randomUUID().toString()
        dao.insertWithItems(
            SessionEntity(
                id = id,
                tool = tool,
                title = title,
                place = place,
                startedAt = startedAt,
                endedAt = endedAt,
                itemCount = items.size,
                findingCount = findingCount,
                summary = summary
            ),
            items.distinctBy { it.key }.map {
                SessionItemEntity(
                    sessionId = id,
                    itemKey = it.key,
                    label = it.label,
                    kind = it.kind.name,
                    rssi = it.rssi,
                    detail = it.detail,
                    flagged = it.flagged
                )
            }
        )
        return id
    }

    /**
     * Fire-and-forget [record] for ViewModels: a run often ends in `onCleared`, after the
     * ViewModel's own scope is cancelled, so saving runs on the repository's scope instead.
     */
    fun recordInBackground(
        tool: String,
        title: String,
        startedAt: Long,
        items: List<SessionItem>,
        summary: String,
        endedAt: Long = System.currentTimeMillis()
    ) {
        if (items.isEmpty()) return
        scope.launch { runCatching { record(tool, title, startedAt, endedAt, items, summary) } }
    }

    suspend fun get(id: String): Session? = dao.get(id)?.toDomain()

    suspend fun items(id: String): List<SessionItem> = dao.items(id).map { it.toDomain() }

    suspend fun rename(id: String, place: String?) = dao.rename(id, place?.trim()?.ifEmpty { null })

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun deleteAll() = dao.deleteAll()

    /** Devices and networks seen in any session, matched on name or address. */
    suspend fun searchSeen(query: String): List<SeenDeviceRow> =
        if (query.isBlank()) emptyList() else dao.searchSeen(query.trim())

    suspend fun prune(retentionMs: Long = DEFAULT_RETENTION_MS, now: Long = System.currentTimeMillis()) =
        dao.deleteOlderThan(now - retentionMs)

    /** Every time [key] was seen, newest first, paired with the session it was seen in. */
    suspend fun sightings(key: String): List<Pair<Session, SessionItem>> {
        val items = dao.sightings(key)
        if (items.isEmpty()) return emptyList()
        val sessions = dao.getAll(items.map { it.sessionId }.distinct()).associateBy { it.id }
        return items.mapNotNull { item -> sessions[item.sessionId]?.let { it.toDomain() to item.toDomain() } }
    }

    companion object {
        const val DEFAULT_RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }
}

internal fun SessionEntity.toDomain() = Session(
    id = id,
    tool = tool,
    title = title,
    place = place,
    startedAt = startedAt,
    endedAt = endedAt,
    itemCount = itemCount,
    findingCount = findingCount,
    summary = summary
)

internal fun SessionItemEntity.toDomain() = SessionItem(
    key = itemKey,
    label = label,
    kind = runCatching { ItemKind.valueOf(kind) }.getOrDefault(ItemKind.RF),
    rssi = rssi,
    detail = detail,
    flagged = flagged
)

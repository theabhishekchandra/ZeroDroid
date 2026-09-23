package com.abhishek.zerodroid.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.abhishek.zerodroid.core.database.entity.SessionEntity
import com.abhishek.zerodroid.core.database.entity.SessionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: String): SessionEntity?

    @Query("SELECT * FROM session_items WHERE sessionId = :sessionId ORDER BY flagged DESC, rssi DESC")
    suspend fun items(sessionId: String): List<SessionItemEntity>

    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Insert
    suspend fun insertItems(items: List<SessionItemEntity>)

    @Transaction
    suspend fun insertWithItems(session: SessionEntity, items: List<SessionItemEntity>) {
        insertSession(session)
        if (items.isNotEmpty()) insertItems(items)
    }

    @Query("UPDATE sessions SET place = :place WHERE id = :id")
    suspend fun rename(id: String, place: String?)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM sessions WHERE startedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    /** Every sighting of one device or network, newest first, with the session it came from. */
    @Query(
        """
        SELECT session_items.* FROM session_items
        INNER JOIN sessions ON sessions.id = session_items.sessionId
        WHERE itemKey = :key ORDER BY sessions.startedAt DESC
        """
    )
    suspend fun sightings(key: String): List<SessionItemEntity>

    @Query("SELECT * FROM sessions WHERE id IN (:ids)")
    suspend fun getAll(ids: List<String>): List<SessionEntity>

    /** Devices and networks ever saved in a session whose name or address matches [query]. */
    @Query(
        """
        SELECT session_items.itemKey AS itemKey, session_items.label AS label, session_items.kind AS kind,
               MAX(sessions.startedAt) AS lastSeen, COUNT(DISTINCT sessions.id) AS sessionCount
        FROM session_items
        INNER JOIN sessions ON sessions.id = session_items.sessionId
        WHERE session_items.label LIKE '%' || :query || '%' OR session_items.itemKey LIKE '%' || :query || '%'
        GROUP BY session_items.itemKey
        ORDER BY lastSeen DESC
        LIMIT :limit
        """
    )
    suspend fun searchSeen(query: String, limit: Int = 20): List<SeenDeviceRow>

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}

/** One device or network across every session it appeared in. */
data class SeenDeviceRow(
    val itemKey: String,
    val label: String,
    val kind: String,
    val lastSeen: Long,
    val sessionCount: Int
)

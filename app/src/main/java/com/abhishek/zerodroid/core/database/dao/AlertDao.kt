package com.abhishek.zerodroid.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abhishek.zerodroid.core.database.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 300): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(alert: AlertEntity)

    @Query("DELETE FROM alerts")
    suspend fun clearAll()

    @Query("UPDATE alerts SET status = :status, resolution = :resolution, resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun setStatus(id: String, status: String, resolution: String?, resolvedAt: Long?)

    @Query("DELETE FROM alerts WHERE status = 'RESOLVED'")
    suspend fun clearResolved()

    /** Resolved alerts older than the retention window are dropped. */
    @Query("DELETE FROM alerts WHERE status = 'RESOLVED' AND resolvedAt < :cutoff")
    suspend fun pruneResolved(cutoff: Long)
}

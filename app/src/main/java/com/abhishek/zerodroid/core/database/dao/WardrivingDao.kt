package com.abhishek.zerodroid.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.abhishek.zerodroid.core.database.entity.WardrivingRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrivingDao {

    @Query("SELECT * FROM wardriving_records WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getRecordsBySession(sessionId: String): Flow<List<WardrivingRecordEntity>>

    @Query("SELECT * FROM wardriving_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<WardrivingRecordEntity>>

    @Insert
    suspend fun insert(record: WardrivingRecordEntity)

    @Query("SELECT COUNT(DISTINCT bssid) FROM wardriving_records WHERE sessionId = :sessionId")
    suspend fun getUniqueBssidCount(sessionId: String): Int

    @Query("SELECT * FROM wardriving_records WHERE sessionId = :sessionId")
    suspend fun getRecordsBySessionList(sessionId: String): List<WardrivingRecordEntity>

    @Query("DELETE FROM wardriving_records WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}

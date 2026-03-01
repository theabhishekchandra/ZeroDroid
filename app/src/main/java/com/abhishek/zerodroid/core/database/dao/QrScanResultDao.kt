package com.abhishek.zerodroid.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.abhishek.zerodroid.core.database.entity.QrScanResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QrScanResultDao {

    @Query("SELECT * FROM qr_scan_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<QrScanResultEntity>>

    @Query("SELECT * FROM qr_scan_results ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentResults(limit: Int): Flow<List<QrScanResultEntity>>

    @Insert
    suspend fun insert(result: QrScanResultEntity)

    @Query("DELETE FROM qr_scan_results")
    suspend fun deleteAll()
}

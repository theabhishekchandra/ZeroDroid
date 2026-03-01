package com.abhishek.zerodroid.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abhishek.zerodroid.core.database.entity.BleDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BleDeviceDao {

    @Query("SELECT * FROM ble_devices WHERE isBookmarked = 1 ORDER BY lastSeen DESC")
    fun getBookmarkedDevices(): Flow<List<BleDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: BleDeviceEntity)

    @Query("UPDATE ble_devices SET isBookmarked = :bookmarked WHERE address = :address")
    suspend fun setBookmarked(address: String, bookmarked: Boolean)

    @Query("SELECT isBookmarked FROM ble_devices WHERE address = :address")
    suspend fun isBookmarked(address: String): Boolean?
}

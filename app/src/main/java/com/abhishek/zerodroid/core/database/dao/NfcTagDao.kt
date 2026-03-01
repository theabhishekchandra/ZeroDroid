package com.abhishek.zerodroid.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.abhishek.zerodroid.core.database.entity.NfcTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NfcTagDao {

    @Query("SELECT * FROM nfc_tags ORDER BY timestamp DESC")
    fun getAllTags(): Flow<List<NfcTagEntity>>

    @Insert
    suspend fun insert(tag: NfcTagEntity)

    @Query("DELETE FROM nfc_tags")
    suspend fun deleteAll()
}

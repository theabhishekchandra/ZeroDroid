package com.abhishek.zerodroid.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nfc_tags")
data class NfcTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uid: String,
    val techList: String,
    val tagType: String,
    val ndefPayload: String?,
    val timestamp: Long = System.currentTimeMillis()
)

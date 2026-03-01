package com.abhishek.zerodroid.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wardriving_records")
data class WardrivingRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val bssid: String,
    val ssid: String?,
    val rssi: Int,
    val frequency: Int = 0,
    val capabilities: String? = null,
    val lat: Double,
    val lng: Double,
    val timestamp: Long = System.currentTimeMillis()
)

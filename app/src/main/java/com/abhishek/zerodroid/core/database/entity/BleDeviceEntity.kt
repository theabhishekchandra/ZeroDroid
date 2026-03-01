package com.abhishek.zerodroid.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ble_devices")
data class BleDeviceEntity(
    @PrimaryKey
    val address: String,
    val name: String?,
    val rssi: Int,
    val serviceUuids: String?,
    val isBookmarked: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

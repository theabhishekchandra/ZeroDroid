package com.abhishek.zerodroid.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qr_scan_results")
data class QrScanResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawValue: String,
    val format: String,
    val contentType: String,
    val parsedContent: String?,
    val isThreat: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

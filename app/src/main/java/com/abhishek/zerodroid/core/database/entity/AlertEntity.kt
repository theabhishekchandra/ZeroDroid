package com.abhishek.zerodroid.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey
    val id: String,
    val source: String,
    val severity: String,
    val title: String,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** OPEN until the user triages it; see [com.abhishek.zerodroid.core.alerts.AlertStatus]. */
    @ColumnInfo(defaultValue = "OPEN")
    val status: String = "OPEN",
    /** Why it was resolved: MINE, SAFE, MUTED, CHECKED. */
    val resolution: String? = null,
    val resolvedAt: Long? = null
)

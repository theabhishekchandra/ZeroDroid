package com.abhishek.zerodroid.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One recorded run of a tool: when it ran, what it found, and a one-line summary. */
@Entity(tableName = "sessions", indices = [Index("startedAt")])
data class SessionEntity(
    @PrimaryKey
    val id: String,
    /** Route of the tool that recorded it, e.g. "wifi". */
    val tool: String,
    val title: String,
    /** Optional user label for where it ran, e.g. "Living room". */
    val place: String? = null,
    val startedAt: Long,
    val endedAt: Long,
    val itemCount: Int,
    val findingCount: Int,
    val summary: String
)

/** A device or network seen during a session; the unit Compare diffs on. */
@Entity(
    tableName = "session_items",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("itemKey")]
)
data class SessionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val rowId: Long = 0,
    val sessionId: String,
    /** Stable identity across sessions: a BSSID, BLE address or tracker signature. */
    val itemKey: String,
    val label: String,
    /** WIFI, BLE, BT, LAN, CAMERA, TRACKER, RF. */
    val kind: String,
    val rssi: Int? = null,
    val detail: String = "",
    val flagged: Boolean = false
)

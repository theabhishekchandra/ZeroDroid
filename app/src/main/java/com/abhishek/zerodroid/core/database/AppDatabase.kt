package com.abhishek.zerodroid.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.abhishek.zerodroid.core.database.dao.AlertDao
import com.abhishek.zerodroid.core.database.dao.SessionDao
import com.abhishek.zerodroid.core.database.dao.BleDeviceDao
import com.abhishek.zerodroid.core.database.dao.NfcTagDao
import com.abhishek.zerodroid.core.database.dao.QrScanResultDao
import com.abhishek.zerodroid.core.database.dao.WardrivingDao
import com.abhishek.zerodroid.core.database.entity.AlertEntity
import com.abhishek.zerodroid.core.database.entity.SessionEntity
import com.abhishek.zerodroid.core.database.entity.SessionItemEntity
import com.abhishek.zerodroid.core.database.entity.BleDeviceEntity
import com.abhishek.zerodroid.core.database.entity.NfcTagEntity
import com.abhishek.zerodroid.core.database.entity.QrScanResultEntity
import com.abhishek.zerodroid.core.database.entity.WardrivingRecordEntity

@Database(
    entities = [
        BleDeviceEntity::class,
        NfcTagEntity::class,
        WardrivingRecordEntity::class,
        QrScanResultEntity::class,
        AlertEntity::class,
        SessionEntity::class,
        SessionItemEntity::class
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bleDeviceDao(): BleDeviceDao
    abstract fun nfcTagDao(): NfcTagDao
    abstract fun wardrivingDao(): WardrivingDao
    abstract fun qrScanResultDao(): QrScanResultDao
    abstract fun alertDao(): AlertDao
    abstract fun sessionDao(): SessionDao
}

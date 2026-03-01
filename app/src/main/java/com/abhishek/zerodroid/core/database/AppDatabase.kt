package com.abhishek.zerodroid.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.abhishek.zerodroid.core.database.dao.BleDeviceDao
import com.abhishek.zerodroid.core.database.dao.NfcTagDao
import com.abhishek.zerodroid.core.database.dao.QrScanResultDao
import com.abhishek.zerodroid.core.database.dao.WardrivingDao
import com.abhishek.zerodroid.core.database.entity.BleDeviceEntity
import com.abhishek.zerodroid.core.database.entity.NfcTagEntity
import com.abhishek.zerodroid.core.database.entity.QrScanResultEntity
import com.abhishek.zerodroid.core.database.entity.WardrivingRecordEntity

@Database(
    entities = [
        BleDeviceEntity::class,
        NfcTagEntity::class,
        WardrivingRecordEntity::class,
        QrScanResultEntity::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bleDeviceDao(): BleDeviceDao
    abstract fun nfcTagDao(): NfcTagDao
    abstract fun wardrivingDao(): WardrivingDao
    abstract fun qrScanResultDao(): QrScanResultDao
}

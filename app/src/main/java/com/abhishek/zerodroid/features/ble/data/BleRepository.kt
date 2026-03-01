package com.abhishek.zerodroid.features.ble.data

import com.abhishek.zerodroid.core.database.dao.BleDeviceDao
import com.abhishek.zerodroid.core.database.entity.BleDeviceEntity
import com.abhishek.zerodroid.features.ble.domain.BleDevice
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BleRepository(
    private val bleScanner: BleScanner,
    private val bleDeviceDao: BleDeviceDao
) {

    fun scan(): Flow<List<BleDevice>> = bleScanner.scan()

    val isAvailable: Boolean get() = bleScanner.isAvailable

    fun getBookmarkedDevices(): Flow<List<BleDevice>> {
        return bleDeviceDao.getBookmarkedDevices().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun toggleBookmark(device: BleDevice) {
        val current = bleDeviceDao.isBookmarked(device.address) ?: false
        val entity = BleDeviceEntity(
            address = device.address,
            name = device.name,
            rssi = device.rssi,
            serviceUuids = device.serviceUuids.joinToString(","),
            isBookmarked = !current,
            lastSeen = device.lastSeen
        )
        bleDeviceDao.upsert(entity)
    }

    private fun BleDeviceEntity.toDomain() = BleDevice(
        name = name,
        address = address,
        rssi = rssi,
        serviceUuids = serviceUuids?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        isBookmarked = isBookmarked,
        lastSeen = lastSeen
    )
}

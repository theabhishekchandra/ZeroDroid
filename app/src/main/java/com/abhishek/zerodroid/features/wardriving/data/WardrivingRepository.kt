package com.abhishek.zerodroid.features.wardriving.data

import com.abhishek.zerodroid.core.database.dao.WardrivingDao
import com.abhishek.zerodroid.core.database.entity.WardrivingRecordEntity
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingCollector
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingRecord
import com.abhishek.zerodroid.features.wardriving.domain.WigleExporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WardrivingRepository(
    private val collector: WardrivingCollector,
    private val dao: WardrivingDao
) {
    fun collect() = collector.collect()

    suspend fun saveRecords(sessionId: String, records: List<WardrivingRecord>) {
        records.forEach { record ->
            dao.insert(
                WardrivingRecordEntity(
                    sessionId = sessionId,
                    bssid = record.bssid,
                    ssid = record.ssid,
                    rssi = record.rssi,
                    frequency = record.frequency,
                    capabilities = record.capabilities,
                    lat = record.lat,
                    lng = record.lng,
                    timestamp = record.timestamp
                )
            )
        }
    }

    fun getSessionRecords(sessionId: String): Flow<List<WardrivingRecord>> {
        return dao.getRecordsBySession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getUniqueBssidCount(sessionId: String): Int {
        return dao.getUniqueBssidCount(sessionId)
    }

    suspend fun exportSession(sessionId: String): String {
        val records = dao.getRecordsBySessionList(sessionId)
        return WigleExporter.export(records)
    }

    private fun WardrivingRecordEntity.toDomain() = WardrivingRecord(
        bssid = bssid,
        ssid = ssid,
        rssi = rssi,
        frequency = frequency,
        capabilities = capabilities,
        lat = lat,
        lng = lng,
        timestamp = timestamp
    )
}

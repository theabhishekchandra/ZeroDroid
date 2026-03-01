package com.abhishek.zerodroid.features.nfc.data

import com.abhishek.zerodroid.core.database.dao.NfcTagDao
import com.abhishek.zerodroid.core.database.entity.NfcTagEntity
import com.abhishek.zerodroid.features.nfc.domain.NfcTagInfo
import com.abhishek.zerodroid.features.nfc.domain.NfcTagManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NfcRepository(
    private val nfcTagManager: NfcTagManager,
    private val nfcTagDao: NfcTagDao
) {
    fun getTagHistory(): Flow<List<NfcTagInfo>> {
        return nfcTagDao.getAllTags().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveTag(tag: NfcTagInfo) {
        nfcTagDao.insert(
            NfcTagEntity(
                uid = tag.uid,
                techList = tag.techList.joinToString(","),
                tagType = tag.tagType,
                ndefPayload = tag.ndefMessages.joinToString("\n") { "${it.type}: ${it.payload}" },
                timestamp = tag.timestamp
            )
        )
    }

    suspend fun clearHistory() {
        nfcTagDao.deleteAll()
    }

    private fun NfcTagEntity.toDomain() = NfcTagInfo(
        uid = uid,
        techList = techList.split(",").filter { it.isNotBlank() },
        atqa = null,
        sak = null,
        tagType = tagType,
        timestamp = timestamp
    )
}

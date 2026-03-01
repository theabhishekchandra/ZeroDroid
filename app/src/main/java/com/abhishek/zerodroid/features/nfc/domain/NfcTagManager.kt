package com.abhishek.zerodroid.features.nfc.domain

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA

class NfcTagManager {

    fun parseTag(tag: Tag): NfcTagInfo {
        val uid = tag.id.joinToString(":") { "%02X".format(it) }
        val techList = tag.techList.map { it.substringAfterLast('.') }
        val tagType = detectTagType(tag)

        var atqa: String? = null
        var sak: String? = null
        try {
            val nfcA = NfcA.get(tag)
            if (nfcA != null) {
                atqa = nfcA.atqa.joinToString(" ") { "%02X".format(it) }
                sak = "%02X".format(nfcA.sak.toInt())
            }
        } catch (_: Exception) {}

        val ndefMessages = try {
            val ndef = Ndef.get(tag)
            ndef?.connect()
            val msg = ndef?.ndefMessage
            ndef?.close()
            msg?.let { NdefParser.parse(it) } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return NfcTagInfo(
            uid = uid,
            techList = techList,
            atqa = atqa,
            sak = sak,
            tagType = tagType,
            ndefMessages = ndefMessages
        )
    }

    fun writeNdefText(tag: Tag, text: String): WriteResult {
        return try {
            val ndef = Ndef.get(tag) ?: return WriteResult.Error("Tag is not NDEF formatted")
            ndef.connect()
            if (!ndef.isWritable) {
                ndef.close()
                return WriteResult.Error("Tag is read-only")
            }
            val record = NdefRecord.createTextRecord("en", text)
            val message = NdefMessage(arrayOf(record))
            if (message.toByteArray().size > ndef.maxSize) {
                ndef.close()
                return WriteResult.Error("Data too large (${message.toByteArray().size}/${ndef.maxSize} bytes)")
            }
            ndef.writeNdefMessage(message)
            ndef.close()
            WriteResult.Success
        } catch (e: Exception) {
            WriteResult.Error("Write failed: ${e.message}")
        }
    }

    fun writeNdefUri(tag: Tag, uri: String): WriteResult {
        return try {
            val ndef = Ndef.get(tag) ?: return WriteResult.Error("Tag is not NDEF formatted")
            ndef.connect()
            if (!ndef.isWritable) {
                ndef.close()
                return WriteResult.Error("Tag is read-only")
            }
            val record = NdefRecord.createUri(uri)
            val message = NdefMessage(arrayOf(record))
            if (message.toByteArray().size > ndef.maxSize) {
                ndef.close()
                return WriteResult.Error("Data too large")
            }
            ndef.writeNdefMessage(message)
            ndef.close()
            WriteResult.Success
        } catch (e: Exception) {
            WriteResult.Error("Write failed: ${e.message}")
        }
    }

    private fun detectTagType(tag: Tag): String {
        val techList = tag.techList.toList()
        return when {
            techList.any { it.contains("MifareClassic") } -> {
                val mc = MifareClassic.get(tag)
                when (mc?.type) {
                    MifareClassic.TYPE_CLASSIC -> "MIFARE Classic ${mc.size}B"
                    MifareClassic.TYPE_PLUS -> "MIFARE Plus"
                    MifareClassic.TYPE_PRO -> "MIFARE Pro"
                    else -> "MIFARE Classic"
                }
            }
            techList.any { it.contains("MifareUltralight") } -> {
                val mu = MifareUltralight.get(tag)
                when (mu?.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> "NTAG/Ultralight"
                    MifareUltralight.TYPE_ULTRALIGHT_C -> "Ultralight C"
                    else -> "NTAG/Ultralight"
                }
            }
            techList.any { it.contains("IsoDep") } -> "ISO 14443-4 (ISO-DEP)"
            techList.any { it.contains("NfcA") } -> "NFC-A (ISO 14443-3A)"
            techList.any { it.contains("NfcB") } -> "NFC-B (ISO 14443-3B)"
            techList.any { it.contains("NfcF") } -> "NFC-F (FeliCa)"
            techList.any { it.contains("NfcV") } -> "NFC-V (ISO 15693)"
            else -> "Unknown"
        }
    }
}

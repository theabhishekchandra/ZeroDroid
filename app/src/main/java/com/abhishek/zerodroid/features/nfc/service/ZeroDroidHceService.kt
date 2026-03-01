package com.abhishek.zerodroid.features.nfc.service

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class ZeroDroidHceService : HostApduService() {

    companion object {
        // NDEF Tag Application AID
        private val NDEF_AID = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )

        // APDU response codes
        private val OK_SW = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val UNKNOWN_CMD_SW = byteArrayOf(0x6D.toByte(), 0x00.toByte())
        private val FILE_NOT_FOUND_SW = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        // CC file for NDEF Type 4 Tag
        private val CC_FILE = byteArrayOf(
            0x00, 0x0F, // CC length
            0x20,       // Version 2.0
            0x00, 0x3B, // Max read size
            0x00, 0x34, // Max write size
            0x04, 0x06, // NDEF TLV
            0xE1.toByte(), 0x04, // NDEF file ID
            0x00, 0xFF.toByte(), // Max NDEF size
            0x00,       // Read access
            0xFF.toByte() // Write access (deny)
        )

        // Shared NDEF data
        @Volatile
        var ndefData: ByteArray = createDefaultNdef()
            set(value) {
                field = value
                ndefFileContent = buildNdefFile(value)
            }

        @Volatile
        private var ndefFileContent: ByteArray = buildNdefFile(ndefData)

        private fun createDefaultNdef(): ByteArray {
            val text = "ZeroDroid HCE"
            val langBytes = "en".toByteArray(Charsets.US_ASCII)
            val textBytes = text.toByteArray(Charsets.UTF_8)
            val payload = ByteArray(1 + langBytes.size + textBytes.size)
            payload[0] = langBytes.size.toByte()
            langBytes.copyInto(payload, 1)
            textBytes.copyInto(payload, 1 + langBytes.size)

            // NDEF record: TNF=1 (Well-Known), Type="T" (Text)
            val record = byteArrayOf(
                0xD1.toByte(), // MB=1, ME=1, CF=0, SR=1, IL=0, TNF=001
                0x01,          // Type length = 1
                payload.size.toByte(), // Payload length
                0x54           // Type = 'T'
            ) + payload

            return record
        }

        private fun buildNdefFile(ndefMessage: ByteArray): ByteArray {
            val length = ndefMessage.size
            return byteArrayOf(
                (length shr 8).toByte(),
                (length and 0xFF).toByte()
            ) + ndefMessage
        }
    }

    private var selectedFile: ByteArray? = null

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (commandApdu.size < 4) return UNKNOWN_CMD_SW

        val ins = commandApdu[1].toInt() and 0xFF

        return when {
            // SELECT command
            ins == 0xA4 -> handleSelect(commandApdu)
            // READ BINARY command
            ins == 0xB0 -> handleReadBinary(commandApdu)
            else -> UNKNOWN_CMD_SW
        }
    }

    private fun handleSelect(apdu: ByteArray): ByteArray {
        if (apdu.size < 7) return UNKNOWN_CMD_SW

        val lc = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + lc) return UNKNOWN_CMD_SW
        val fileId = apdu.copyOfRange(5, 5 + lc)

        return when {
            // SELECT NDEF Application
            fileId.contentEquals(NDEF_AID) -> OK_SW
            // SELECT CC file (E103)
            fileId.size == 2 && fileId[0] == 0xE1.toByte() && fileId[1] == 0x03.toByte() -> {
                selectedFile = CC_FILE
                OK_SW
            }
            // SELECT NDEF file (E104)
            fileId.size == 2 && fileId[0] == 0xE1.toByte() && fileId[1] == 0x04.toByte() -> {
                selectedFile = ndefFileContent
                OK_SW
            }
            else -> FILE_NOT_FOUND_SW
        }
    }

    private fun handleReadBinary(apdu: ByteArray): ByteArray {
        val file = selectedFile ?: return FILE_NOT_FOUND_SW
        if (apdu.size < 5) return UNKNOWN_CMD_SW

        val offset = ((apdu[2].toInt() and 0xFF) shl 8) or (apdu[3].toInt() and 0xFF)
        val le = apdu[4].toInt() and 0xFF

        if (offset >= file.size) return FILE_NOT_FOUND_SW

        val end = minOf(offset + le, file.size)
        val data = file.copyOfRange(offset, end)

        return data + OK_SW
    }

    override fun onDeactivated(reason: Int) {
        selectedFile = null
    }
}

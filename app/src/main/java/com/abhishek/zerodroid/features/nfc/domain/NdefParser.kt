package com.abhishek.zerodroid.features.nfc.domain

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.charset.Charset

object NdefParser {

    fun parse(message: NdefMessage): List<NdefParsedContent> {
        return message.records.mapNotNull { record ->
            when (record.tnf) {
                NdefRecord.TNF_WELL_KNOWN -> parseWellKnown(record)
                NdefRecord.TNF_MIME_MEDIA -> parseMime(record)
                NdefRecord.TNF_ABSOLUTE_URI -> NdefParsedContent(
                    type = NdefContentType.URI,
                    payload = String(record.payload),
                    rawType = String(record.type)
                )
                NdefRecord.TNF_EXTERNAL_TYPE -> NdefParsedContent(
                    type = NdefContentType.UNKNOWN,
                    payload = String(record.payload),
                    rawType = String(record.type)
                )
                else -> NdefParsedContent(
                    type = NdefContentType.UNKNOWN,
                    payload = record.payload.joinToString(" ") { "%02X".format(it) },
                    rawType = record.type?.let { String(it) } ?: ""
                )
            }
        }
    }

    private fun parseWellKnown(record: NdefRecord): NdefParsedContent {
        val type = String(record.type)
        return when (type) {
            "U" -> parseUri(record)
            "T" -> parseText(record)
            "Sp" -> NdefParsedContent(
                type = NdefContentType.SMART_POSTER,
                payload = "Smart Poster",
                rawType = type
            )
            else -> NdefParsedContent(
                type = NdefContentType.UNKNOWN,
                payload = record.payload.joinToString(" ") { "%02X".format(it) },
                rawType = type
            )
        }
    }

    private fun parseUri(record: NdefRecord): NdefParsedContent {
        val payload = record.payload
        if (payload.isEmpty()) return NdefParsedContent(NdefContentType.URI, "", "U")
        val prefixByte = payload[0].toInt() and 0xFF
        val prefix = uriPrefixes.getOrElse(prefixByte) { "" }
        val uri = prefix + String(payload, 1, payload.size - 1, Charsets.UTF_8)
        return NdefParsedContent(NdefContentType.URI, uri, "U")
    }

    private fun parseText(record: NdefRecord): NdefParsedContent {
        val payload = record.payload
        if (payload.isEmpty()) return NdefParsedContent(NdefContentType.TEXT, "", "T")
        val status = payload[0].toInt() and 0xFF
        val langLen = status and 0x3F
        val encoding = if (status and 0x80 != 0) Charsets.UTF_16 else Charsets.UTF_8
        val text = String(payload, 1 + langLen, payload.size - 1 - langLen, encoding)
        return NdefParsedContent(NdefContentType.TEXT, text, "T")
    }

    private fun parseMime(record: NdefRecord): NdefParsedContent {
        val mimeType = String(record.type)
        return when {
            mimeType == "application/vnd.wfa.wsc" -> NdefParsedContent(
                type = NdefContentType.WIFI,
                payload = "Wi-Fi Configuration",
                rawType = mimeType
            )
            mimeType.startsWith("text/vcard") -> NdefParsedContent(
                type = NdefContentType.VCARD,
                payload = String(record.payload),
                rawType = mimeType
            )
            else -> NdefParsedContent(
                type = NdefContentType.MIME,
                payload = String(record.payload),
                rawType = mimeType
            )
        }
    }

    private val uriPrefixes = arrayOf(
        "", "http://www.", "https://www.", "http://", "https://",
        "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.",
        "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://",
        "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:",
        "sip:", "sips:", "tftp:", "btspp://", "btl2cap://",
        "btgoep://", "tcpobex://", "irdaobex://", "file://",
        "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:",
        "urn:epc:", "urn:nfc:"
    )
}

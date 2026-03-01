package com.abhishek.zerodroid.features.nfc.domain

data class NfcTagInfo(
    val uid: String,
    val techList: List<String>,
    val atqa: String?,
    val sak: String?,
    val tagType: String,
    val ndefMessages: List<NdefParsedContent> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class NdefParsedContent(
    val type: NdefContentType,
    val payload: String,
    val rawType: String = ""
)

enum class NdefContentType {
    URI, TEXT, MIME, SMART_POSTER, WIFI, VCARD, UNKNOWN
}

data class NfcState(
    val isNfcAvailable: Boolean = false,
    val isNfcEnabled: Boolean = false,
    val lastTag: NfcTagInfo? = null,
    val tagHistory: List<NfcTagInfo> = emptyList(),
    val writeMode: Boolean = false,
    val writeResult: WriteResult? = null
)

sealed class WriteResult {
    data object Success : WriteResult()
    data class Error(val message: String) : WriteResult()
}

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

enum class NfcTab(val label: String) { READ("Read"), WRITE("Write"), MIFARE("MIFARE"), EMULATE("Emulate") }

data class NfcState(
    val isNfcAvailable: Boolean = false,
    val isNfcEnabled: Boolean = false,
    val lastTag: NfcTagInfo? = null,
    val tagHistory: List<NfcTagInfo> = emptyList(),
    val tab: NfcTab = NfcTab.READ,
    val writeResult: WriteResult? = null,
    /** Sectors from the last MIFARE Classic tag read on the MIFARE tab. */
    val mifareSectors: List<MifareSectorData> = emptyList(),
    val isReadingMifare: Boolean = false,
    val mifareMessage: String? = null,
    val customKeys: List<ByteArray> = emptyList(),
    /** What this phone serves when another reader taps it (HCE Type 4 tag). */
    val emulatedPayload: String = "ZeroDroid HCE",
    val emulatedIsUrl: Boolean = false
) {
    val writeMode: Boolean get() = tab == NfcTab.WRITE
}

sealed class WriteResult {
    data object Success : WriteResult()
    data class Error(val message: String) : WriteResult()
}

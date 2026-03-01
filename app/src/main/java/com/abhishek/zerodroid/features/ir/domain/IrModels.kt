package com.abhishek.zerodroid.features.ir.domain

enum class IrProtocol(val displayName: String, val defaultFrequency: Int) {
    NEC("NEC", 38000),
    SAMSUNG32("Samsung32", 38000),
    RC5("RC5", 36000),
    RC6("RC6", 36000),
    SONY("Sony SIRC", 40000),
    RAW("Raw", 38000)
}

data class IrSignal(
    val protocol: IrProtocol,
    val frequency: Int,
    val code: String,
    val name: String = "",
    val rawPattern: IntArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IrSignal) return false
        return protocol == other.protocol && frequency == other.frequency && code == other.code
    }
    override fun hashCode(): Int {
        var result = protocol.hashCode()
        result = 31 * result + frequency
        result = 31 * result + code.hashCode()
        return result
    }
}

enum class IrScreenTab(val displayName: String) {
    REMOTE("Remote"),
    CUSTOM("Custom"),
    IMPORT("Import")
}

data class IrRemoteState(
    val isIrAvailable: Boolean = false,
    val selectedProtocol: IrProtocol = IrProtocol.NEC,
    val frequency: Int = 38000,
    val code: String = "",
    val lastTransmitResult: TransmitResult? = null,
    val importedSignals: List<IrSignal> = emptyList(),
    val activeTab: IrScreenTab = IrScreenTab.REMOTE,
    val selectedProfile: IrRemoteProfile? = null
)

sealed class TransmitResult {
    data object Success : TransmitResult()
    data class Error(val message: String) : TransmitResult()
}

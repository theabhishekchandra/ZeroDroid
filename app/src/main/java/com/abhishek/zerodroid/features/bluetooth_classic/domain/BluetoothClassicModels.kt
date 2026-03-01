package com.abhishek.zerodroid.features.bluetooth_classic.domain

data class ClassicBluetoothDevice(
    val name: String?,
    val address: String,
    val rssi: Int = 0,
    val bondState: Int = 0,
    val majorClass: String = "Unknown",
    val minorClass: String = "",
    val isPaired: Boolean = false
) {
    val displayName: String get() = name ?: "Unknown Device"
    val bondStateLabel: String
        get() = when (bondState) {
            10 -> "Not Paired"
            11 -> "Pairing..."
            12 -> "Paired"
            else -> "Unknown"
        }
}

data class SppState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val lines: List<TerminalLine> = emptyList(),
    val error: String? = null
)

data class TerminalLine(
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class BluetoothClassicState(
    val isScanning: Boolean = false,
    val discoveredDevices: List<ClassicBluetoothDevice> = emptyList(),
    val pairedDevices: List<ClassicBluetoothDevice> = emptyList(),
    val error: String? = null
)

package com.abhishek.zerodroid.features.ble.domain

import android.bluetooth.BluetoothGattCharacteristic

/**
 * Connection status for a GATT connection.
 */
enum class GattConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Disconnecting
}

/**
 * Write mode for characteristic write operations.
 */
enum class WriteMode { Hex, Text }

/**
 * Overall GATT connection state emitted as a StateFlow.
 */
data class GattConnectionState(
    val deviceAddress: String = "",
    val deviceName: String? = null,
    val connectionStatus: GattConnectionStatus = GattConnectionStatus.Disconnected,
    val mtu: Int = 23,
    val services: List<GattServiceInfo> = emptyList(),
    val error: String? = null
) {
    val isConnected: Boolean get() = connectionStatus == GattConnectionStatus.Connected
    val payloadSize: Int get() = (mtu - 3).coerceAtLeast(0)
    val totalCharacteristics: Int get() = services.sumOf { it.characteristics.size }
}

/**
 * Represents a discovered GATT service.
 */
data class GattServiceInfo(
    val uuid: String,
    val displayName: String,
    val isPrimary: Boolean,
    val characteristics: List<GattCharacteristicInfo>
)

/**
 * Represents a discovered GATT characteristic.
 */
data class GattCharacteristicInfo(
    val uuid: String,
    val displayName: String,
    val properties: Int,
    val descriptors: List<GattDescriptorInfo>,
    val serviceUuid: String
) {
    val isReadable: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_READ != 0

    val isWritable: Boolean
        get() = properties and (
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
                        BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
                ) != 0

    val isWritableWithResponse: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0

    val isWritableWithoutResponse: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0

    val isNotifiable: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

    val isIndicatable: Boolean
        get() = properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0

    val propertiesList: List<String>
        get() = buildList {
            if (isReadable) add("Read")
            if (isWritableWithResponse) add("Write")
            if (isWritableWithoutResponse) add("WriteNoResp")
            if (isNotifiable) add("Notify")
            if (isIndicatable) add("Indicate")
            if (properties and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) add("Broadcast")
            if (properties and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) add("SignedWrite")
            if (properties and BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS != 0) add("ExtendedProps")
        }
}

/**
 * Represents a discovered GATT descriptor.
 */
data class GattDescriptorInfo(
    val uuid: String,
    val displayName: String
)

/**
 * Holds a raw characteristic value along with its timestamp.
 */
data class CharacteristicValue(
    val rawBytes: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    val hexString: String
        get() = rawBytes.joinToString(" ") { "%02X".format(it) }

    val asciiString: String
        get() = rawBytes.map { byte ->
            val c = byte.toInt().toChar()
            if (c.isLetterOrDigit() || c.isWhitespace() || c in "!@#\$%^&*()-_=+[]{}|;:',.<>?/~`\"\\") c else '.'
        }.joinToString("")

    val byteCount: Int get() = rawBytes.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CharacteristicValue) return false
        return rawBytes.contentEquals(other.rawBytes) && timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = rawBytes.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Result of a GATT read/write/notify operation.
 */
sealed class GattOperationResult {
    data class Success(val value: CharacteristicValue) : GattOperationResult()
    data class WriteSuccess(val charUuid: String) : GattOperationResult()
    data class Error(val message: String) : GattOperationResult()
}

/**
 * UI state for the characteristic detail panel.
 */
data class CharacteristicDetailState(
    val info: GattCharacteristicInfo? = null,
    val lastReadValue: CharacteristicValue? = null,
    val notificationValues: List<CharacteristicValue> = emptyList(),
    val isNotifying: Boolean = false,
    val descriptorValues: Map<String, CharacteristicValue> = emptyMap(),
    val parsedDisplay: String? = null,
    val writeInput: String = "",
    val writeMode: WriteMode = WriteMode.Hex,
    val isLoading: Boolean = false,
    val error: String? = null
)

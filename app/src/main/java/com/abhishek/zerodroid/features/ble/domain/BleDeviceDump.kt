package com.abhishek.zerodroid.features.ble.domain

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

// ── Data Models ─────────────────────────────────────────────────────────────────

data class BleDeviceDump(
    val deviceAddress: String,
    val deviceName: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val mtu: Int,
    val services: List<DumpedService>
) {
    val totalCharacteristics: Int
        get() = services.sumOf { it.characteristics.size }

    val successfulReads: Int
        get() = services.sumOf { svc ->
            svc.characteristics.count { it.value != null }
        }

    val failedReads: Int
        get() = services.sumOf { svc ->
            svc.characteristics.count { it.readError != null }
        }

    val formattedTimestamp: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            return sdf.format(Date(timestamp))
        }

    fun toJson(): JSONObject {
        val root = JSONObject()
        root.put("deviceAddress", deviceAddress)
        root.put("deviceName", deviceName ?: JSONObject.NULL)
        root.put("timestamp", timestamp)
        root.put("mtu", mtu)

        val servicesArray = JSONArray()
        for (service in services) {
            val svcObj = JSONObject()
            svcObj.put("uuid", service.uuid)
            svcObj.put("displayName", service.displayName)

            val charsArray = JSONArray()
            for (char in service.characteristics) {
                val charObj = JSONObject()
                charObj.put("uuid", char.uuid)
                charObj.put("displayName", char.displayName)
                charObj.put("serviceUuid", char.serviceUuid)
                charObj.put("properties", char.properties)
                if (char.value != null) {
                    charObj.put("valueHex", char.hexString)
                    // Store raw bytes as hex for faithful round-trip
                    charObj.put("rawBytesHex", char.value.joinToString("") { "%02x".format(it) })
                } else {
                    charObj.put("valueHex", JSONObject.NULL)
                    charObj.put("rawBytesHex", JSONObject.NULL)
                }
                charObj.put("readError", char.readError ?: JSONObject.NULL)
                charsArray.put(charObj)
            }
            svcObj.put("characteristics", charsArray)
            servicesArray.put(svcObj)
        }
        root.put("services", servicesArray)
        return root
    }

    companion object {
        fun fromJson(json: JSONObject): BleDeviceDump {
            val servicesArray = json.getJSONArray("services")
            val services = mutableListOf<DumpedService>()

            for (i in 0 until servicesArray.length()) {
                val svcObj = servicesArray.getJSONObject(i)
                val charsArray = svcObj.getJSONArray("characteristics")
                val characteristics = mutableListOf<DumpedCharacteristic>()

                for (j in 0 until charsArray.length()) {
                    val charObj = charsArray.getJSONObject(j)
                    val rawHex = if (charObj.isNull("rawBytesHex")) null
                        else charObj.getString("rawBytesHex")
                    val rawBytes = rawHex?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()

                    characteristics.add(
                        DumpedCharacteristic(
                            uuid = charObj.getString("uuid"),
                            displayName = charObj.getString("displayName"),
                            serviceUuid = charObj.getString("serviceUuid"),
                            properties = charObj.getInt("properties"),
                            value = rawBytes,
                            hexString = if (charObj.isNull("valueHex")) "" else charObj.getString("valueHex"),
                            readError = if (charObj.isNull("readError")) null else charObj.getString("readError")
                        )
                    )
                }

                services.add(
                    DumpedService(
                        uuid = svcObj.getString("uuid"),
                        displayName = svcObj.getString("displayName"),
                        characteristics = characteristics
                    )
                )
            }

            return BleDeviceDump(
                deviceAddress = json.getString("deviceAddress"),
                deviceName = if (json.isNull("deviceName")) null else json.getString("deviceName"),
                timestamp = json.getLong("timestamp"),
                mtu = json.getInt("mtu"),
                services = services
            )
        }
    }
}

data class DumpedService(
    val uuid: String,
    val displayName: String,
    val characteristics: List<DumpedCharacteristic>
)

data class DumpedCharacteristic(
    val uuid: String,
    val displayName: String,
    val serviceUuid: String,
    val properties: Int,
    val value: ByteArray?,
    val hexString: String,
    val readError: String?
) {
    /** Whether this characteristic had a writable property and a captured value. */
    val isReplayable: Boolean
        get() = value != null && (properties and 0x0C) != 0 // WRITE or WRITE_NO_RESPONSE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DumpedCharacteristic) return false
        return uuid == other.uuid &&
                serviceUuid == other.serviceUuid &&
                properties == other.properties &&
                value.contentEquals(other.value) &&
                hexString == other.hexString &&
                readError == other.readError
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + serviceUuid.hashCode()
        result = 31 * result + properties
        result = 31 * result + (value?.contentHashCode() ?: 0)
        result = 31 * result + hexString.hashCode()
        result = 31 * result + (readError?.hashCode() ?: 0)
        return result
    }
}

// ── Dumper ───────────────────────────────────────────────────────────────────────

class BleDeviceDumper(private val explorer: GattExplorer) {

    data class DumpProgress(
        val current: Int,
        val total: Int,
        val currentChar: String
    ) {
        val fraction: Float get() = if (total == 0) 0f else current.toFloat() / total
    }

    /**
     * Reads every readable characteristic on the connected device and returns a
     * full [BleDeviceDump]. Returns `null` if the device is not connected.
     */
    suspend fun dumpDevice(
        onProgress: (DumpProgress) -> Unit = {}
    ): BleDeviceDump? {
        val state = explorer.connectionState.value
        if (!state.isConnected) return null

        val services = state.services
        // Count total readable characteristics for progress reporting
        val allReadableChars = services.flatMap { svc ->
            svc.characteristics.filter { it.isReadable }.map { char -> svc to char }
        }
        val total = allReadableChars.size

        val dumpedServices = mutableListOf<DumpedService>()
        var index = 0

        for (service in services) {
            val dumpedChars = mutableListOf<DumpedCharacteristic>()

            for (char in service.characteristics) {
                if (char.isReadable) {
                    index++
                    onProgress(
                        DumpProgress(
                            current = index,
                            total = total,
                            currentChar = char.displayName.ifBlank { char.uuid }
                        )
                    )

                    val result = explorer.readCharacteristic(service.uuid, char.uuid)

                    when (result) {
                        is GattOperationResult.Success -> {
                            dumpedChars.add(
                                DumpedCharacteristic(
                                    uuid = char.uuid,
                                    displayName = char.displayName,
                                    serviceUuid = service.uuid,
                                    properties = char.properties,
                                    value = result.value.rawBytes,
                                    hexString = result.value.hexString,
                                    readError = null
                                )
                            )
                        }
                        is GattOperationResult.Error -> {
                            dumpedChars.add(
                                DumpedCharacteristic(
                                    uuid = char.uuid,
                                    displayName = char.displayName,
                                    serviceUuid = service.uuid,
                                    properties = char.properties,
                                    value = null,
                                    hexString = "",
                                    readError = result.message
                                )
                            )
                        }
                        else -> {
                            dumpedChars.add(
                                DumpedCharacteristic(
                                    uuid = char.uuid,
                                    displayName = char.displayName,
                                    serviceUuid = service.uuid,
                                    properties = char.properties,
                                    value = null,
                                    hexString = "",
                                    readError = "Unexpected result type"
                                )
                            )
                        }
                    }

                    // Small delay to avoid overwhelming the BLE stack
                    delay(100)
                } else {
                    // Non-readable characteristic — still record it in the dump
                    dumpedChars.add(
                        DumpedCharacteristic(
                            uuid = char.uuid,
                            displayName = char.displayName,
                            serviceUuid = service.uuid,
                            properties = char.properties,
                            value = null,
                            hexString = "",
                            readError = null // not an error, just not readable
                        )
                    )
                }
            }

            dumpedServices.add(
                DumpedService(
                    uuid = service.uuid,
                    displayName = service.displayName,
                    characteristics = dumpedChars
                )
            )
        }

        return BleDeviceDump(
            deviceAddress = state.deviceAddress,
            deviceName = state.deviceName,
            timestamp = System.currentTimeMillis(),
            mtu = state.mtu,
            services = dumpedServices
        )
    }

    /**
     * Replays all writable characteristic values from a previously captured dump
     * back to the connected device.
     */
    suspend fun replayWrites(
        dump: BleDeviceDump,
        onProgress: (DumpProgress) -> Unit = {}
    ) {
        val writableChars = dump.services.flatMap { svc ->
            svc.characteristics.filter { it.isReplayable }
        }
        val total = writableChars.size

        writableChars.forEachIndexed { index, char ->
            onProgress(
                DumpProgress(
                    current = index + 1,
                    total = total,
                    currentChar = char.displayName.ifBlank { char.uuid }
                )
            )

            val writeType = if ((char.properties and 0x04) != 0) {
                // WRITE (with response)
                android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                // WRITE_NO_RESPONSE
                android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }

            explorer.writeCharacteristic(
                serviceUuid = char.serviceUuid,
                charUuid = char.uuid,
                value = char.value!!,
                writeType = writeType
            )

            // Small delay to avoid overwhelming the BLE stack
            delay(100)
        }
    }
}

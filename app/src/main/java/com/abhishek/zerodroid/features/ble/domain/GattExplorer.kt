package com.abhishek.zerodroid.features.ble.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Core GATT connection manager. Serializes all BLE operations through a Channel
 * to satisfy the Android BLE requirement of one outstanding operation at a time.
 */
class GattExplorer(
    private val context: Context,
    private val bluetoothManager: BluetoothManager?
) {

    companion object {
        private const val TAG = "GattExplorer"
        private const val OPERATION_TIMEOUT_MS = 10_000L
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var gatt: BluetoothGatt? = null
    private val activeNotifications = mutableSetOf<String>() // characteristic UUIDs

    // Operation serialization
    private val operationChannel = Channel<GattOperation>(Channel.UNLIMITED)
    private var currentDeferred: CompletableDeferred<GattOperationResult>? = null

    // Public state
    private val _connectionState = MutableStateFlow(GattConnectionState())
    val connectionState: StateFlow<GattConnectionState> = _connectionState.asStateFlow()

    private val _notificationFlow = MutableSharedFlow<Pair<String, CharacteristicValue>>(extraBufferCapacity = 64)
    val notificationFlow: SharedFlow<Pair<String, CharacteristicValue>> = _notificationFlow.asSharedFlow()

    init {
        // Process operations sequentially
        scope.launch {
            for (operation in operationChannel) {
                currentDeferred = operation.deferred
                try {
                    operation.execute()
                    val result = withTimeoutOrNull(OPERATION_TIMEOUT_MS) {
                        operation.deferred.await()
                    }
                    if (result == null) {
                        operation.deferred.complete(
                            GattOperationResult.Error("Operation timed out after ${OPERATION_TIMEOUT_MS / 1000}s")
                        )
                    }
                } catch (e: Exception) {
                    if (!operation.deferred.isCompleted) {
                        operation.deferred.complete(GattOperationResult.Error(e.message ?: "Unknown error"))
                    }
                } finally {
                    currentDeferred = null
                }
            }
        }
    }

    // ---- GATT Callback ----

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: status=$status, newState=$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = _connectionState.value.copy(
                        connectionStatus = GattConnectionStatus.Connected,
                        error = null
                    )
                    // Request high MTU on connect
                    gatt.requestMtu(517)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    activeNotifications.clear()
                    _connectionState.value = _connectionState.value.copy(
                        connectionStatus = GattConnectionStatus.Disconnected,
                        services = emptyList(),
                        error = if (status != BluetoothGatt.GATT_SUCCESS) "Disconnected with status $status" else null
                    )
                    currentDeferred?.let {
                        if (!it.isCompleted) it.complete(GattOperationResult.Error("Disconnected"))
                    }
                    this@GattExplorer.gatt?.close()
                    this@GattExplorer.gatt = null
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d(TAG, "onMtuChanged: mtu=$mtu, status=$status")
            _connectionState.value = _connectionState.value.copy(mtu = mtu)
            // Discover services after MTU negotiation
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(TAG, "onServicesDiscovered: status=$status, ${gatt.services.size} services")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services = gatt.services.map { service ->
                    GattServiceInfo(
                        uuid = service.uuid.toString(),
                        displayName = BleUuidDatabase.serviceDisplayName(service.uuid.toString()),
                        isPrimary = service.type == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, // type 0 = primary
                        characteristics = service.characteristics.map { char ->
                            GattCharacteristicInfo(
                                uuid = char.uuid.toString(),
                                displayName = BleUuidDatabase.characteristicDisplayName(char.uuid.toString()),
                                properties = char.properties,
                                serviceUuid = service.uuid.toString(),
                                descriptors = char.descriptors.map { desc ->
                                    GattDescriptorInfo(
                                        uuid = desc.uuid.toString(),
                                        displayName = BleUuidDatabase.descriptorDisplayName(desc.uuid.toString())
                                    )
                                }
                            )
                        }
                    )
                }
                _connectionState.value = _connectionState.value.copy(services = services)
            } else {
                _connectionState.value = _connectionState.value.copy(
                    error = "Service discovery failed (status $status)"
                )
            }
        }

        // Characteristic Read - API 33+ callback
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            handleCharacteristicRead(characteristic.uuid.toString(), value, status)
        }

        // Characteristic Read - deprecated callback for API < 33
        @Deprecated("Deprecated in API 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            handleCharacteristicRead(characteristic.uuid.toString(), characteristic.value ?: byteArrayOf(), status)
        }

        private fun handleCharacteristicRead(uuid: String, value: ByteArray, status: Int) {
            Log.d(TAG, "onCharacteristicRead: uuid=$uuid, status=$status, ${value.size} bytes")
            currentDeferred?.let { deferred ->
                if (!deferred.isCompleted) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        deferred.complete(GattOperationResult.Success(CharacteristicValue(value)))
                    } else {
                        deferred.complete(GattOperationResult.Error("Read failed (status $status)"))
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.d(TAG, "onCharacteristicWrite: uuid=${characteristic.uuid}, status=$status")
            currentDeferred?.let { deferred ->
                if (!deferred.isCompleted) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        deferred.complete(GattOperationResult.WriteSuccess(characteristic.uuid.toString()))
                    } else {
                        deferred.complete(GattOperationResult.Error("Write failed (status $status)"))
                    }
                }
            }
        }

        // Characteristic Changed (notification) - API 33+
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleNotification(characteristic.uuid.toString(), value)
        }

        // Characteristic Changed (notification) - deprecated for API < 33
        @Deprecated("Deprecated in API 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            handleNotification(characteristic.uuid.toString(), characteristic.value ?: byteArrayOf())
        }

        private fun handleNotification(uuid: String, value: ByteArray) {
            Log.d(TAG, "onCharacteristicChanged: uuid=$uuid, ${value.size} bytes")
            _notificationFlow.tryEmit(uuid to CharacteristicValue(value))
        }

        // Descriptor Read - API 33+
        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
            value: ByteArray
        ) {
            handleDescriptorRead(value, status)
        }

        // Descriptor Read - deprecated for API < 33
        @Deprecated("Deprecated in API 33")
        @Suppress("DEPRECATION")
        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            handleDescriptorRead(descriptor.value ?: byteArrayOf(), status)
        }

        private fun handleDescriptorRead(value: ByteArray, status: Int) {
            currentDeferred?.let { deferred ->
                if (!deferred.isCompleted) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        deferred.complete(GattOperationResult.Success(CharacteristicValue(value)))
                    } else {
                        deferred.complete(GattOperationResult.Error("Descriptor read failed (status $status)"))
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Log.d(TAG, "onDescriptorWrite: uuid=${descriptor.uuid}, status=$status")
            currentDeferred?.let { deferred ->
                if (!deferred.isCompleted) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        deferred.complete(GattOperationResult.WriteSuccess(descriptor.uuid.toString()))
                    } else {
                        deferred.complete(GattOperationResult.Error("Descriptor write failed (status $status)"))
                    }
                }
            }
        }
    }

    // ---- Public API ----

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        val adapter = bluetoothManager?.adapter ?: run {
            _connectionState.value = _connectionState.value.copy(
                error = "Bluetooth not available"
            )
            return
        }
        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            _connectionState.value = _connectionState.value.copy(
                error = "Invalid device address: $address"
            )
            return
        }

        _connectionState.value = GattConnectionState(
            deviceAddress = address,
            deviceName = device.name,
            connectionStatus = GattConnectionStatus.Connecting
        )

        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        _connectionState.value = _connectionState.value.copy(
            connectionStatus = GattConnectionStatus.Disconnecting
        )
        gatt?.disconnect()
    }

    @SuppressLint("MissingPermission")
    fun requestMtu(mtu: Int = 517) {
        gatt?.requestMtu(mtu)
    }

    suspend fun readCharacteristic(serviceUuid: String, charUuid: String): GattOperationResult {
        val g = gatt ?: return GattOperationResult.Error("Not connected")
        val characteristic = findCharacteristic(g, serviceUuid, charUuid)
            ?: return GattOperationResult.Error("Characteristic not found")

        val op = GattOperation {
            @SuppressLint("MissingPermission")
            val success = g.readCharacteristic(characteristic)
            if (!success) {
                it.complete(GattOperationResult.Error("readCharacteristic returned false"))
            }
        }
        operationChannel.send(op)
        return op.deferred.await()
    }

    @SuppressLint("MissingPermission")
    suspend fun writeCharacteristic(
        serviceUuid: String,
        charUuid: String,
        value: ByteArray,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    ): GattOperationResult {
        val g = gatt ?: return GattOperationResult.Error("Not connected")
        val characteristic = findCharacteristic(g, serviceUuid, charUuid)
            ?: return GattOperationResult.Error("Characteristic not found")

        val op = GattOperation {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val result = g.writeCharacteristic(characteristic, value, writeType)
                if (result != BluetoothStatusCodes.SUCCESS) {
                    it.complete(GattOperationResult.Error("writeCharacteristic failed (code $result)"))
                }
            } else {
                @Suppress("DEPRECATION")
                characteristic.writeType = writeType
                @Suppress("DEPRECATION")
                characteristic.value = value
                @Suppress("DEPRECATION")
                val success = g.writeCharacteristic(characteristic)
                if (!success) {
                    it.complete(GattOperationResult.Error("writeCharacteristic returned false"))
                }
            }
        }
        operationChannel.send(op)
        return op.deferred.await()
    }

    @SuppressLint("MissingPermission")
    suspend fun toggleNotification(
        serviceUuid: String,
        charUuid: String,
        enable: Boolean
    ): GattOperationResult {
        val g = gatt ?: return GattOperationResult.Error("Not connected")
        val characteristic = findCharacteristic(g, serviceUuid, charUuid)
            ?: return GattOperationResult.Error("Characteristic not found")

        // Set local notification
        val success = g.setCharacteristicNotification(characteristic, enable)
        if (!success) return GattOperationResult.Error("setCharacteristicNotification failed")

        // Write CCCD
        val cccd = characteristic.getDescriptor(CCCD_UUID)
            ?: return GattOperationResult.Error("CCCD descriptor not found")

        val isIndicate = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        val cccdValue = when {
            !enable -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            isIndicate -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            else -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        }

        val op = GattOperation {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val result = g.writeDescriptor(cccd, cccdValue)
                if (result != BluetoothStatusCodes.SUCCESS) {
                    it.complete(GattOperationResult.Error("writeDescriptor failed (code $result)"))
                }
            } else {
                @Suppress("DEPRECATION")
                cccd.value = cccdValue
                @Suppress("DEPRECATION")
                val wrote = g.writeDescriptor(cccd)
                if (!wrote) {
                    it.complete(GattOperationResult.Error("writeDescriptor returned false"))
                }
            }
        }
        operationChannel.send(op)
        val result = op.deferred.await()

        if (result is GattOperationResult.WriteSuccess) {
            if (enable) {
                activeNotifications.add(charUuid.lowercase())
            } else {
                activeNotifications.remove(charUuid.lowercase())
            }
        }
        return result
    }

    fun isNotifying(charUuid: String): Boolean {
        return charUuid.lowercase() in activeNotifications
    }

    suspend fun readDescriptor(
        serviceUuid: String,
        charUuid: String,
        descUuid: String
    ): GattOperationResult {
        val g = gatt ?: return GattOperationResult.Error("Not connected")
        val characteristic = findCharacteristic(g, serviceUuid, charUuid)
            ?: return GattOperationResult.Error("Characteristic not found")
        val descriptor = characteristic.getDescriptor(UUID.fromString(descUuid))
            ?: return GattOperationResult.Error("Descriptor not found")

        val op = GattOperation {
            @SuppressLint("MissingPermission")
            val success = g.readDescriptor(descriptor)
            if (!success) {
                it.complete(GattOperationResult.Error("readDescriptor returned false"))
            }
        }
        operationChannel.send(op)
        return op.deferred.await()
    }

    @SuppressLint("MissingPermission")
    fun close() {
        activeNotifications.clear()
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        operationChannel.close()
        scope.cancel()
        _connectionState.value = GattConnectionState()
    }

    // ---- Helpers ----

    private fun findCharacteristic(
        gatt: BluetoothGatt,
        serviceUuid: String,
        charUuid: String
    ): BluetoothGattCharacteristic? {
        val service = gatt.getService(UUID.fromString(serviceUuid)) ?: return null
        return service.getCharacteristic(UUID.fromString(charUuid))
    }

    /**
     * Wraps a GATT operation with its CompletableDeferred result.
     */
    private class GattOperation(
        val deferred: CompletableDeferred<GattOperationResult> = CompletableDeferred(),
        private val block: (CompletableDeferred<GattOperationResult>) -> Unit
    ) {
        fun execute() = block(deferred)
    }
}

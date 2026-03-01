package com.abhishek.zerodroid.features.bluetooth_classic.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class SdpServiceInfo(
    val uuid: String,
    val shortUuid: String,
    val profileName: String,
    val description: String
)

/**
 * Comprehensive Bluetooth UUID database mapping standard 16-bit short UUIDs
 * to human-readable profile names and descriptions.
 *
 * Reference: https://www.bluetooth.com/specifications/assigned-numbers/
 */
object BluetoothUuidDatabase {

    private val BASE_UUID_SUFFIX = "-0000-1000-8000-00805F9B34FB".lowercase()

    private val profiles = mapOf(
        "0001" to ("SDP" to "Service Discovery Protocol"),
        "0003" to ("RFCOMM" to "Serial Port Emulation"),
        "0005" to ("TCS-BIN" to "Telephony Control"),
        "0008" to ("OBEX" to "Object Exchange"),
        "000F" to ("BNEP" to "Bluetooth Network Encapsulation"),
        "0100" to ("L2CAP" to "Logical Link Control"),
        "1101" to ("SPP" to "Serial Port Profile"),
        "1102" to ("LAN Access" to "LAN Access Using PPP"),
        "1103" to ("DUN" to "Dialup Networking"),
        "1104" to ("IrMC Sync" to "Infrared Mobile Communications Sync"),
        "1105" to ("OPP" to "OBEX Object Push"),
        "1106" to ("FTP" to "OBEX File Transfer"),
        "1107" to ("IrMC Sync Cmd" to "IrMC Sync Command"),
        "1108" to ("HSP" to "Headset Profile"),
        "1109" to ("CTP" to "Cordless Telephony"),
        "110A" to ("A2DP Source" to "Advanced Audio Distribution Source"),
        "110B" to ("A2DP Sink" to "Advanced Audio Distribution Sink"),
        "110C" to ("AVRCP Target" to "AV Remote Control Target"),
        "110D" to ("A2DP" to "Advanced Audio Distribution"),
        "110E" to ("AVRCP" to "AV Remote Control"),
        "110F" to ("AVRCP Controller" to "AV Remote Control Controller"),
        "1112" to ("HSP AG" to "Headset Audio Gateway"),
        "1115" to ("PANU" to "Personal Area Networking User"),
        "1116" to ("NAP" to "Network Access Point"),
        "1117" to ("GN" to "Group Ad-hoc Network"),
        "111E" to ("HFP" to "Hands-Free Profile"),
        "111F" to ("HFP AG" to "Hands-Free Audio Gateway"),
        "1124" to ("HID" to "Human Interface Device"),
        "112D" to ("SIM Access" to "SIM Access Profile"),
        "112F" to ("PBAP Client" to "Phonebook Access Client"),
        "1130" to ("PBAP Server" to "Phonebook Access Server"),
        "1132" to ("MAP" to "Message Access Profile"),
        "1133" to ("MAP Server" to "Message Access Server"),
        "1134" to ("MAP Client" to "MAP Notification Server"),
        "1200" to ("PnP" to "Device Identification"),
        "1800" to ("GAP" to "Generic Access"),
        "1801" to ("GATT" to "Generic Attribute"),
    )

    /**
     * Looks up a full 128-bit UUID string and returns a resolved [SdpServiceInfo].
     *
     * Standard Bluetooth UUIDs follow the form `0000XXXX-0000-1000-8000-00805F9B34FB`
     * where `XXXX` is the 16-bit short UUID. If the UUID matches this pattern we
     * extract the short form and resolve it against the known profiles database.
     * Non-standard (vendor) UUIDs are returned as "Unknown Service".
     */
    fun lookup(uuid: String): SdpServiceInfo {
        val normalized = uuid.lowercase()

        val shortUuid = extractShortUuid(normalized)

        if (shortUuid != null) {
            val upper = shortUuid.uppercase()
            val entry = profiles[upper]
            if (entry != null) {
                return SdpServiceInfo(
                    uuid = normalized,
                    shortUuid = "0x$upper",
                    profileName = entry.first,
                    description = entry.second
                )
            }
            // Standard base but unrecognized short UUID
            return SdpServiceInfo(
                uuid = normalized,
                shortUuid = "0x$upper",
                profileName = "Unknown Profile (0x$upper)",
                description = "Standard Bluetooth UUID not in local database"
            )
        }

        // Vendor-specific / non-standard UUID
        return SdpServiceInfo(
            uuid = normalized,
            shortUuid = "vendor",
            profileName = "Vendor Service",
            description = "Non-standard vendor-specific UUID"
        )
    }

    /**
     * Extracts the 16-bit (or 32-bit) short UUID from a standard 128-bit Bluetooth UUID.
     * Returns `null` if the UUID does not follow the standard base UUID pattern.
     */
    private fun extractShortUuid(uuid: String): String? {
        // Standard form: 0000XXXX-0000-1000-8000-00805F9B34FB
        // 32-bit form:   XXXXXXXX-0000-1000-8000-00805F9B34FB
        if (!uuid.endsWith(BASE_UUID_SUFFIX)) return null

        val prefix = uuid.substringBefore("-")
        if (prefix.length != 8) return null

        // If it starts with "0000" it's a 16-bit short UUID
        return if (prefix.startsWith("0000")) {
            prefix.substring(4) // return 4-char short UUID
        } else {
            prefix // return full 32-bit short UUID
        }
    }
}

/**
 * Performs SDP (Service Discovery Protocol) queries against Bluetooth Classic devices
 * to enumerate supported profiles and services.
 *
 * Uses [BluetoothDevice.fetchUuidsWithSdp] to trigger a live SDP query and listens
 * for [BluetoothDevice.ACTION_UUID] broadcasts to collect results. Also supports
 * reading cached UUIDs without triggering a new SDP query.
 */
class SdpServiceDiscovery(
    private val context: Context,
    private val bluetoothManager: BluetoothManager?
) {

    /**
     * Triggers a live SDP query against the device at [deviceAddress] and emits
     * the resolved list of [SdpServiceInfo] when the system returns the UUID results.
     *
     * The flow completes after the first result set is received. The caller should
     * collect from this flow while the query is in progress and cancel when done.
     */
    @SuppressLint("MissingPermission")
    fun discoverServices(deviceAddress: String): Flow<List<SdpServiceInfo>> = callbackFlow {
        val adapter = bluetoothManager?.adapter
        if (adapter == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val device: BluetoothDevice? = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: IllegalArgumentException) {
            null
        }

        if (device == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != BluetoothDevice.ACTION_UUID) return

                val receivedDevice: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                // Only process results for the device we queried
                if (receivedDevice?.address != deviceAddress) return

                val uuids: Array<ParcelUuid>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID, ParcelUuid::class.java)
                } else {
                    @Suppress("DEPRECATION", "UNCHECKED_CAST")
                    intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                        ?.filterIsInstance<ParcelUuid>()
                        ?.toTypedArray()
                }

                val services = uuids
                    ?.map { parcelUuid -> BluetoothUuidDatabase.lookup(parcelUuid.uuid.toString()) }
                    ?.sortedBy { it.profileName }
                    ?: emptyList()

                trySend(services)
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_UUID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        // Trigger the SDP query
        device.fetchUuidsWithSdp()

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Receiver was already unregistered
            }
        }
    }

    /**
     * Returns the cached list of service UUIDs for the device at [deviceAddress]
     * without triggering a new SDP query. This uses [BluetoothDevice.getUuids]
     * which returns previously discovered UUIDs stored by the system.
     *
     * Returns an empty list if the device is unknown or has no cached UUIDs.
     */
    @SuppressLint("MissingPermission")
    fun getCachedServices(deviceAddress: String): List<SdpServiceInfo> {
        val adapter = bluetoothManager?.adapter ?: return emptyList()

        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: IllegalArgumentException) {
            return emptyList()
        }

        val cachedUuids: Array<ParcelUuid> = device.uuids ?: return emptyList()

        return cachedUuids
            .map { parcelUuid -> BluetoothUuidDatabase.lookup(parcelUuid.uuid.toString()) }
            .sortedBy { it.profileName }
    }
}

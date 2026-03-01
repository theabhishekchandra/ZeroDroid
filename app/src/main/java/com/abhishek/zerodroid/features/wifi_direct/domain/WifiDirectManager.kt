package com.abhishek.zerodroid.features.wifi_direct.domain

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

class WifiDirectManager(private val context: Context) {

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null

    private val _state = MutableStateFlow(WifiDirectState())
    val state: StateFlow<WifiDirectState> = _state.asStateFlow()

    fun initialize() {
        channel = manager?.initialize(context, Looper.getMainLooper(), null)
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        _state.value = _state.value.copy(isDiscovering = true, error = null)
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {
                _state.value = _state.value.copy(
                    isDiscovering = false,
                    error = "Discovery failed: ${failureReason(reason)}"
                )
            }
        })
    }

    fun stopDiscovery() {
        manager?.stopPeerDiscovery(channel, null)
        _state.value = _state.value.copy(isDiscovering = false)
    }

    @SuppressLint("MissingPermission")
    fun requestPeers() {
        manager?.requestPeers(channel) { peers: WifiP2pDeviceList? ->
            val peerList = peers?.deviceList?.map { device ->
                WifiDirectPeer(
                    deviceName = device.deviceName ?: "Unknown",
                    deviceAddress = device.deviceAddress,
                    isGroupOwner = device.isGroupOwner,
                    status = device.status
                )
            } ?: emptyList()
            _state.value = _state.value.copy(peers = peerList)
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String) {
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
        }
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {
                _state.value = _state.value.copy(
                    error = "Connection failed: ${failureReason(reason)}"
                )
            }
        })
    }

    fun disconnect() {
        manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _state.value = _state.value.copy(connectedGroup = null)
            }
            override fun onFailure(reason: Int) {
                _state.value = _state.value.copy(
                    error = "Disconnect failed: ${failureReason(reason)}"
                )
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun requestGroupInfo() {
        manager?.requestGroupInfo(channel) { group: WifiP2pGroup? ->
            if (group != null) {
                _state.value = _state.value.copy(
                    connectedGroup = WifiDirectGroup(
                        networkName = group.networkName ?: "",
                        passphrase = group.passphrase,
                        isGroupOwner = group.isGroupOwner,
                        ownerAddress = group.owner?.deviceAddress,
                        clients = group.clientList?.map { it.deviceAddress } ?: emptyList()
                    )
                )
            }
        }
    }

    fun registerReceiver(): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(
                            WifiP2pManager.EXTRA_WIFI_STATE, -1
                        )
                        _state.value = _state.value.copy(
                            isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                        )
                    }
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        requestPeers()
                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        requestGroupInfo()
                    }
                    WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {}
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        return receiver
    }

    fun unregisterReceiver(receiver: BroadcastReceiver) {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
    }

    fun cleanup() {
        channel?.close()
        channel = null
    }

    private fun failureReason(code: Int): String = when (code) {
        WifiP2pManager.ERROR -> "Internal error"
        WifiP2pManager.P2P_UNSUPPORTED -> "P2P unsupported"
        WifiP2pManager.BUSY -> "Busy"
        else -> "Unknown ($code)"
    }
}

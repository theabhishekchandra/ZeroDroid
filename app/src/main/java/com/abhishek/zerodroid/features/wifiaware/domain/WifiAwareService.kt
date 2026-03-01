package com.abhishek.zerodroid.features.wifiaware.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

class WifiAwareService(
    private val context: Context,
    private val wifiAwareManager: WifiAwareManager?
) {
    private var session: WifiAwareSession? = null
    private var publishSession: PublishDiscoverySession? = null
    private var subscribeSession: SubscribeDiscoverySession? = null

    val isAvailable: Boolean
        get() = wifiAwareManager?.isAvailable == true

    fun observeAvailability(): Flow<Boolean> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED) {
                    trySend(wifiAwareManager?.isAvailable == true)
                }
            }
        }
        val filter = IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        trySend(wifiAwareManager?.isAvailable == true)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    fun attach(onAttached: (Boolean) -> Unit) {
        wifiAwareManager?.attach(object : AttachCallback() {
            override fun onAttached(wifiAwareSession: WifiAwareSession) {
                session = wifiAwareSession
                onAttached(true)
            }

            override fun onAttachFailed() {
                onAttached(false)
            }
        }, null)
    }

    fun detach() {
        publishSession?.close()
        subscribeSession?.close()
        session?.close()
        publishSession = null
        subscribeSession = null
        session = null
    }

    fun publish(serviceName: String, onPeerDiscovered: (WifiAwarePeer) -> Unit) {
        val config = PublishConfig.Builder()
            .setServiceName(serviceName)
            .build()

        session?.publish(config, object : DiscoverySessionCallback() {
            override fun onPublishStarted(session: PublishDiscoverySession) {
                publishSession = session
            }

            override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                onPeerDiscovered(
                    WifiAwarePeer(
                        serviceId = peerHandle.hashCode().toString(),
                        serviceName = serviceName,
                        matchFilter = String(message)
                    )
                )
            }
        }, null)
    }

    fun subscribe(serviceName: String, onPeerDiscovered: (WifiAwarePeer) -> Unit) {
        val config = SubscribeConfig.Builder()
            .setServiceName(serviceName)
            .build()

        session?.subscribe(config, object : DiscoverySessionCallback() {
            override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                subscribeSession = session
            }

            override fun onServiceDiscovered(
                peerHandle: PeerHandle,
                serviceSpecificInfo: ByteArray,
                matchFilter: List<ByteArray>
            ) {
                onPeerDiscovered(
                    WifiAwarePeer(
                        serviceId = peerHandle.hashCode().toString(),
                        serviceName = serviceName,
                        matchFilter = matchFilter.joinToString(",") { String(it) }
                    )
                )
            }
        }, null)
    }

    fun stopPublish() {
        publishSession?.close()
        publishSession = null
    }

    fun stopSubscribe() {
        subscribeSession?.close()
        subscribeSession = null
    }
}

package com.abhishek.zerodroid.features.wifi_direct.viewmodel

import android.content.BroadcastReceiver
import androidx.lifecycle.ViewModel
import com.abhishek.zerodroid.features.wifi_direct.domain.WifiDirectManager
import com.abhishek.zerodroid.features.wifi_direct.domain.WifiDirectState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class WifiDirectViewModel @Inject constructor(
    private val manager: WifiDirectManager
) : ViewModel() {

    val state: StateFlow<WifiDirectState> = manager.state

    private var receiver: BroadcastReceiver? = null

    fun initialize() {
        manager.initialize()
        receiver = manager.registerReceiver()
    }

    fun startDiscovery() = manager.startDiscovery()
    fun stopDiscovery() = manager.stopDiscovery()
    fun connect(deviceAddress: String) = manager.connect(deviceAddress)
    fun disconnect() = manager.disconnect()

    override fun onCleared() {
        manager.stopDiscovery()
        receiver?.let { manager.unregisterReceiver(it) }
        manager.cleanup()
    }
}

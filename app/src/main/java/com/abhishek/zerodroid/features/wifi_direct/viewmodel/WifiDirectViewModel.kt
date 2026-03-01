package com.abhishek.zerodroid.features.wifi_direct.viewmodel

import android.content.BroadcastReceiver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.wifi_direct.domain.WifiDirectManager
import com.abhishek.zerodroid.features.wifi_direct.domain.WifiDirectState
import kotlinx.coroutines.flow.StateFlow

class WifiDirectViewModel(
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
        super.onCleared()
        manager.stopDiscovery()
        receiver?.let { manager.unregisterReceiver(it) }
        manager.cleanup()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return WifiDirectViewModel(app.container.wifiDirectManager) as T
            }
        }
    }
}

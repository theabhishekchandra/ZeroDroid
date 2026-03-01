package com.abhishek.zerodroid.features.wifiaware.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.abhishek.zerodroid.ZeroDroidApp
import com.abhishek.zerodroid.features.wifiaware.domain.WifiAwareService
import com.abhishek.zerodroid.features.wifiaware.domain.WifiAwareState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WifiAwareViewModel(
    private val service: WifiAwareService
) : ViewModel() {

    private val _state = MutableStateFlow(WifiAwareState(isAvailable = service.isAvailable))
    val state: StateFlow<WifiAwareState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            service.observeAvailability().collect { available ->
                _state.value = _state.value.copy(isAvailable = available)
            }
        }
    }

    fun setServiceName(name: String) {
        _state.value = _state.value.copy(serviceName = name)
    }

    fun attachSession() {
        service.attach { attached ->
            _state.value = if (attached) {
                _state.value.copy(isSessionAttached = true, error = null)
            } else {
                _state.value.copy(error = "Failed to attach Wi-Fi Aware session")
            }
        }
    }

    fun detachSession() {
        service.detach()
        _state.value = _state.value.copy(
            isSessionAttached = false,
            isPublishing = false,
            isSubscribing = false,
            discoveredPeers = emptyList()
        )
    }

    fun togglePublish() {
        if (_state.value.isPublishing) {
            service.stopPublish()
            _state.value = _state.value.copy(isPublishing = false)
        } else {
            service.publish(_state.value.serviceName) { peer ->
                _state.value = _state.value.copy(
                    discoveredPeers = _state.value.discoveredPeers + peer
                )
            }
            _state.value = _state.value.copy(isPublishing = true)
        }
    }

    fun toggleSubscribe() {
        if (_state.value.isSubscribing) {
            service.stopSubscribe()
            _state.value = _state.value.copy(isSubscribing = false)
        } else {
            service.subscribe(_state.value.serviceName) { peer ->
                _state.value = _state.value.copy(
                    discoveredPeers = _state.value.discoveredPeers + peer
                )
            }
            _state.value = _state.value.copy(isSubscribing = true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        service.detach()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZeroDroidApp
                return WifiAwareViewModel(app.container.wifiAwareService) as T
            }
        }
    }
}

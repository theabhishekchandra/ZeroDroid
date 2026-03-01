package com.abhishek.zerodroid.features.wifiaware.domain

data class WifiAwarePeer(
    val serviceId: String,
    val serviceName: String,
    val matchFilter: String?,
    val discoveredAt: Long = System.currentTimeMillis()
)

data class WifiAwareState(
    val isAvailable: Boolean = false,
    val isSessionAttached: Boolean = false,
    val isPublishing: Boolean = false,
    val isSubscribing: Boolean = false,
    val discoveredPeers: List<WifiAwarePeer> = emptyList(),
    val serviceName: String = "zerodroid",
    val error: String? = null
)

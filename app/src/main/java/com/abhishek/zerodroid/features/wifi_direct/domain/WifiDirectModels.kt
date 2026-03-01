package com.abhishek.zerodroid.features.wifi_direct.domain

data class WifiDirectPeer(
    val deviceName: String,
    val deviceAddress: String,
    val isGroupOwner: Boolean = false,
    val status: Int = 0
) {
    val statusLabel: String
        get() = when (status) {
            0 -> "Connected"
            1 -> "Invited"
            2 -> "Failed"
            3 -> "Available"
            4 -> "Unavailable"
            else -> "Unknown"
        }
}

data class WifiDirectGroup(
    val networkName: String,
    val passphrase: String?,
    val isGroupOwner: Boolean,
    val ownerAddress: String?,
    val clients: List<String> = emptyList()
)

data class WifiDirectState(
    val isEnabled: Boolean = false,
    val isDiscovering: Boolean = false,
    val peers: List<WifiDirectPeer> = emptyList(),
    val connectedGroup: WifiDirectGroup? = null,
    val error: String? = null
)

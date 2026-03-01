package com.abhishek.zerodroid.features.gps.domain

data class GpsState(
    val isTracking: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val accuracy: Float = 0f,
    val satelliteCount: Int = 0,
    val satellites: List<SatelliteInfo> = emptyList(),
    val nmeaSentences: List<String> = emptyList(),
    val provider: String = "",
    val lastUpdateTime: Long = 0L,
    val error: String? = null
)

data class SatelliteInfo(
    val svid: Int,
    val constellationType: Int,
    val cn0DbHz: Float,
    val elevationDeg: Float,
    val azimuthDeg: Float,
    val usedInFix: Boolean
) {
    val constellationName: String
        get() = when (constellationType) {
            1 -> "GPS"
            2 -> "SBAS"
            3 -> "GLONASS"
            4 -> "QZSS"
            5 -> "Beidou"
            6 -> "Galileo"
            7 -> "IRNSS"
            else -> "Unknown"
        }

    val signalQuality: String
        get() = when {
            cn0DbHz >= 35f -> "Strong"
            cn0DbHz >= 25f -> "Good"
            cn0DbHz >= 15f -> "Weak"
            else -> "Very Weak"
        }
}

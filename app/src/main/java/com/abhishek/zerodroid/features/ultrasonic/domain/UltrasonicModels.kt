package com.abhishek.zerodroid.features.ultrasonic.domain

data class FrequencyBin(
    val frequencyHz: Float,
    val magnitude: Float
)

data class UltrasonicBeacon(
    val centerFrequencyHz: Float,
    val bandwidth: Float,
    val magnitude: Float,
    val detectedAt: Long = System.currentTimeMillis()
)

enum class UltrasonicScreenTab(val displayName: String) {
    DETECT("Detect"),
    GENERATE("Generate")
}

data class UltrasonicState(
    val isRecording: Boolean = false,
    val spectrumData: List<FrequencyBin> = emptyList(),
    val detectedBeacons: List<UltrasonicBeacon> = emptyList(),
    val peakFrequency: Float = 0f,
    val peakMagnitude: Float = 0f,
    val error: String? = null,
    val activeTab: UltrasonicScreenTab = UltrasonicScreenTab.DETECT,
    val toneFrequency: Int = 20000,
    val isTonePlaying: Boolean = false
)

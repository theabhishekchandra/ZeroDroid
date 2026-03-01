package com.abhishek.zerodroid.features.sensors.domain

data class SensorReading(
    val name: String,
    val values: FloatArray = floatArrayOf(),
    val accuracy: Int = 0,
    val unit: String = "",
    val isAvailable: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorReading) return false
        return name == other.name && values.contentEquals(other.values) &&
                accuracy == other.accuracy && unit == other.unit && isAvailable == other.isAvailable
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + accuracy
        result = 31 * result + unit.hashCode()
        result = 31 * result + isAvailable.hashCode()
        return result
    }
}

data class MetalDetectorState(
    val isActive: Boolean = false,
    val baseline: Float = 0f,
    val currentMagnitude: Float = 0f,
    val deviation: Float = 0f
)

data class FloorState(
    val pressureHpa: Float = 0f,
    val altitudeM: Float = 0f,
    val estimatedFloor: Int = 0
)

data class TiltState(
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val isLevel: Boolean = false
)

enum class VibrationSeverity(val label: String) {
    NONE("None"),
    LOW("Low"),
    MODERATE("Moderate"),
    HIGH("High"),
    EXTREME("Extreme")
}

data class VibrationState(
    val currentMagnitude: Float = 0f,
    val peakMagnitude: Float = 0f,
    val severity: VibrationSeverity = VibrationSeverity.NONE,
    val history: List<Float> = emptyList()
)

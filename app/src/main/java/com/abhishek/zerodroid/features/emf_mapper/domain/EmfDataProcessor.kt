package com.abhishek.zerodroid.features.emf_mapper.domain

import kotlin.math.sqrt

enum class EmfLevel { NORMAL, ELEVATED, HIGH, EXTREME }

data class EmfReading(
    val magnitude: Float,
    val x: Float,
    val y: Float,
    val z: Float,
    val deviation: Float,
    val level: EmfLevel,
    val timestamp: Long = System.currentTimeMillis()
)

data class EmfMapperState(
    val isRecording: Boolean = false,
    val currentReading: EmfReading? = null,
    val baseline: Float = 0f,
    val peakMagnitude: Float = 0f,
    val minMagnitude: Float = Float.MAX_VALUE,
    val avgMagnitude: Float = 0f,
    val history: List<EmfReading> = emptyList(),
    val hotspots: Int = 0,
    val recordingDurationMs: Long = 0,
    val sensorAvailable: Boolean = true,
    val error: String? = null
)

class EmfDataProcessor {

    fun processReading(values: FloatArray, baseline: Float): EmfReading {
        val x = values.getOrElse(0) { 0f }
        val y = values.getOrElse(1) { 0f }
        val z = values.getOrElse(2) { 0f }
        val magnitude = sqrt(x * x + y * y + z * z)
        val deviation = magnitude - baseline
        val level = classifyLevel(magnitude, deviation)
        return EmfReading(
            magnitude = magnitude,
            x = x,
            y = y,
            z = z,
            deviation = deviation,
            level = level
        )
    }

    fun detectHotspot(reading: EmfReading): Boolean {
        return reading.level == EmfLevel.HIGH || reading.level == EmfLevel.EXTREME
    }

    fun getStatistics(history: List<EmfReading>): Triple<Float, Float, Float> {
        if (history.isEmpty()) return Triple(0f, 0f, 0f)
        val min = history.minOf { it.magnitude }
        val max = history.maxOf { it.magnitude }
        val avg = history.map { it.magnitude }.average().toFloat()
        return Triple(min, max, avg)
    }

    private fun classifyLevel(magnitude: Float, deviation: Float): EmfLevel {
        val absDeviation = kotlin.math.abs(deviation)
        return when {
            absDeviation > 100f -> EmfLevel.EXTREME
            absDeviation > 40f -> EmfLevel.HIGH
            absDeviation > 15f -> EmfLevel.ELEVATED
            else -> EmfLevel.NORMAL
        }
    }
}

package com.abhishek.zerodroid.features.sensors.domain

import kotlin.math.sqrt

class MetalDetector {

    private var baseline: Float = 0f
    private var calibrated = false

    fun calibrate(magnitude: Float) {
        baseline = magnitude
        calibrated = true
    }

    fun update(values: FloatArray): MetalDetectorState {
        if (values.size < 3) return MetalDetectorState()
        val magnitude = sqrt(
            values[0] * values[0] +
            values[1] * values[1] +
            values[2] * values[2]
        )
        if (!calibrated) {
            calibrate(magnitude)
        }
        return MetalDetectorState(
            isActive = true,
            baseline = baseline,
            currentMagnitude = magnitude,
            deviation = magnitude - baseline
        )
    }

    fun reset() {
        calibrated = false
        baseline = 0f
    }
}

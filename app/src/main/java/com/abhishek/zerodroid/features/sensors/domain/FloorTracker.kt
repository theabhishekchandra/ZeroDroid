package com.abhishek.zerodroid.features.sensors.domain

import android.hardware.SensorManager
import kotlin.math.roundToInt

class FloorTracker {

    private var referencePressure: Float = SensorManager.PRESSURE_STANDARD_ATMOSPHERE
    private var referenceSet = false

    fun setReference(pressureHpa: Float) {
        referencePressure = pressureHpa
        referenceSet = true
    }

    fun update(pressureHpa: Float): FloorState {
        if (!referenceSet && pressureHpa > 0) {
            setReference(pressureHpa)
        }
        val altitude = SensorManager.getAltitude(referencePressure, pressureHpa)
        val floor = (altitude / 3.0f).roundToInt()
        return FloorState(
            pressureHpa = pressureHpa,
            altitudeM = altitude,
            estimatedFloor = floor
        )
    }
}

package com.abhishek.zerodroid.features.sensors.domain

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorDataCollector(
    private val sensorManager: SensorManager
) : SensorEventListener {

    private val _accelerometer = MutableStateFlow(SensorReading("Accelerometer", unit = "m/s²"))
    val accelerometer: StateFlow<SensorReading> = _accelerometer.asStateFlow()

    private val _gyroscope = MutableStateFlow(SensorReading("Gyroscope", unit = "rad/s"))
    val gyroscope: StateFlow<SensorReading> = _gyroscope.asStateFlow()

    private val _magnetometer = MutableStateFlow(SensorReading("Magnetometer", unit = "μT"))
    val magnetometer: StateFlow<SensorReading> = _magnetometer.asStateFlow()

    private val _barometer = MutableStateFlow(SensorReading("Barometer", unit = "hPa"))
    val barometer: StateFlow<SensorReading> = _barometer.asStateFlow()

    private val _light = MutableStateFlow(SensorReading("Light", unit = "lux"))
    val light: StateFlow<SensorReading> = _light.asStateFlow()

    private val _proximity = MutableStateFlow(SensorReading("Proximity", unit = "cm"))
    val proximity: StateFlow<SensorReading> = _proximity.asStateFlow()

    private val sensorTypes = listOf(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_PRESSURE,
        Sensor.TYPE_LIGHT,
        Sensor.TYPE_PROXIMITY
    )

    fun start() {
        sensorTypes.forEach { type ->
            val sensor = sensorManager.getDefaultSensor(type)
            if (sensor != null) {
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                markAvailable(type)
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    private fun markAvailable(type: Int) {
        when (type) {
            Sensor.TYPE_ACCELEROMETER -> _accelerometer.value = _accelerometer.value.copy(isAvailable = true)
            Sensor.TYPE_GYROSCOPE -> _gyroscope.value = _gyroscope.value.copy(isAvailable = true)
            Sensor.TYPE_MAGNETIC_FIELD -> _magnetometer.value = _magnetometer.value.copy(isAvailable = true)
            Sensor.TYPE_PRESSURE -> _barometer.value = _barometer.value.copy(isAvailable = true)
            Sensor.TYPE_LIGHT -> _light.value = _light.value.copy(isAvailable = true)
            Sensor.TYPE_PROXIMITY -> _proximity.value = _proximity.value.copy(isAvailable = true)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val valueCount = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD -> 3
            else -> 1
        }
        val reading = SensorReading(
            name = "",
            values = event.values.copyOfRange(0, valueCount),
            accuracy = event.accuracy,
            isAvailable = true
        )
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> _accelerometer.value = reading.copy(name = "Accelerometer", unit = "m/s²")
            Sensor.TYPE_GYROSCOPE -> _gyroscope.value = reading.copy(name = "Gyroscope", unit = "rad/s")
            Sensor.TYPE_MAGNETIC_FIELD -> _magnetometer.value = reading.copy(name = "Magnetometer", unit = "μT")
            Sensor.TYPE_PRESSURE -> _barometer.value = reading.copy(name = "Barometer", unit = "hPa")
            Sensor.TYPE_LIGHT -> _light.value = reading.copy(name = "Light", unit = "lux")
            Sensor.TYPE_PROXIMITY -> _proximity.value = reading.copy(name = "Proximity", unit = "cm")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

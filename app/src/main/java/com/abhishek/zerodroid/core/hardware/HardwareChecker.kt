package com.abhishek.zerodroid.core.hardware

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.nfc.NfcAdapter

class HardwareChecker(private val context: Context) {

    fun hasBluetooth(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

    fun hasBluetoothLe(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    fun hasWifi(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)

    fun hasNfc(): Boolean =
        NfcAdapter.getDefaultAdapter(context) != null

    fun hasIr(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)

    fun hasUwb(): Boolean =
        context.packageManager.hasSystemFeature("android.hardware.uwb")

    fun hasCamera(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    fun hasUsbHost(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

    fun hasWifiAware(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)

    fun hasTelephony(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    fun hasAccelerometer(): Boolean =
        (context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)
            ?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

    fun hasGyroscope(): Boolean =
        (context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)
            ?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null

    fun hasBarometer(): Boolean =
        (context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)
            ?.getDefaultSensor(Sensor.TYPE_PRESSURE) != null

    fun hasMagnetometer(): Boolean =
        (context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)
            ?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null

    fun hasGps(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)

    fun hasWifiDirect(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
}

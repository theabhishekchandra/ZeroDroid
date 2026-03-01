package com.abhishek.zerodroid.core.di

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.ConsumerIrManager
import android.hardware.SensorManager
import android.hardware.usb.UsbManager
import android.net.wifi.WifiManager
import android.net.wifi.aware.WifiAwareManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.telephony.TelephonyManager
import androidx.room.Room
import com.abhishek.zerodroid.core.database.AppDatabase
import com.abhishek.zerodroid.core.hardware.HardwareChecker
import com.abhishek.zerodroid.features.ble.data.BleRepository
import com.abhishek.zerodroid.features.ble.domain.BleScanner
import com.abhishek.zerodroid.features.camera.data.QrRepository
import com.abhishek.zerodroid.features.celltower.domain.CellTowerAnalyzer
import com.abhishek.zerodroid.features.ir.domain.IrTransmitter
import com.abhishek.zerodroid.features.nfc.data.NfcRepository
import com.abhishek.zerodroid.features.nfc.domain.NfcTagManager
import com.abhishek.zerodroid.features.sdr.domain.SdrDetector
import com.abhishek.zerodroid.features.sensors.domain.SensorDataCollector
import com.abhishek.zerodroid.features.ultrasonic.domain.UltrasonicAnalyzer
import com.abhishek.zerodroid.features.usb.domain.UsbDeviceManager
import com.abhishek.zerodroid.features.usbcamera.domain.UsbCameraDetector
import com.abhishek.zerodroid.features.uwb.domain.UwbService
import com.abhishek.zerodroid.features.wardriving.data.WardrivingRepository
import com.abhishek.zerodroid.features.bluetooth_classic.domain.BluetoothClassicScanner
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SppConnectionManager
import com.abhishek.zerodroid.features.gps.domain.GpsTracker
import com.abhishek.zerodroid.features.nfc.domain.MifareClassicReader
import com.abhishek.zerodroid.features.wardriving.domain.WardrivingCollector
import com.abhishek.zerodroid.features.wifi.domain.WifiScanner
import com.abhishek.zerodroid.features.wifi_direct.domain.WifiDirectManager
import com.abhishek.zerodroid.features.hidden_camera.domain.HiddenCameraDetector
import com.abhishek.zerodroid.features.wifi_direct.domain.WifiDirectFileTransfer
import com.abhishek.zerodroid.features.bluetooth_classic.domain.SdpServiceDiscovery
import com.abhishek.zerodroid.features.wifiaware.domain.WifiAwareService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // System services
    val sensorManager: SensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val wifiManager: WifiManager =
        appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    val usbManager: UsbManager? =
        appContext.getSystemService(Context.USB_SERVICE) as? UsbManager

    val telephonyManager: TelephonyManager? =
        appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(appContext)

    val irManager: ConsumerIrManager? =
        appContext.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val wifiAwareManager: WifiAwareManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.getSystemService(Context.WIFI_AWARE_SERVICE) as? WifiAwareManager
        } else null

    // Database
    val database: AppDatabase = Room.databaseBuilder(
        appContext, AppDatabase::class.java, "zerodroid.db"
    ).addMigrations().build()

    // Utilities
    val hardwareChecker = HardwareChecker(appContext)

    // NFC tag flow (emitted from MainActivity)
    private val _nfcTagFlow = MutableSharedFlow<Tag>(extraBufferCapacity = 1)
    val nfcTagFlow: SharedFlow<Tag> = _nfcTagFlow
    fun emitNfcTag(tag: Tag) {
        _nfcTagFlow.tryEmit(tag)
    }

    // Phase 1 features
    val sensorDataCollector by lazy { SensorDataCollector(sensorManager) }
    val wifiScanner by lazy { WifiScanner(appContext, wifiManager) }
    val bleScanner by lazy { BleScanner(bluetoothManager) }
    val bleRepository by lazy { BleRepository(bleScanner, database.bleDeviceDao()) }

    // Phase 2 features
    val usbDeviceManager by lazy { UsbDeviceManager(appContext, usbManager) }
    val cellTowerAnalyzer by lazy { CellTowerAnalyzer(telephonyManager) }
    val irTransmitter by lazy { IrTransmitter(irManager) }
    val nfcTagManager by lazy { NfcTagManager() }
    val nfcRepository by lazy { NfcRepository(nfcTagManager, database.nfcTagDao()) }
    val sdrDetector by lazy { SdrDetector(usbManager) }
    val ultrasonicAnalyzer by lazy { UltrasonicAnalyzer() }
    val wifiAwareService by lazy { WifiAwareService(appContext, wifiAwareManager) }
    val qrRepository by lazy { QrRepository(database.qrScanResultDao()) }
    val wardrivingCollector by lazy { WardrivingCollector(appContext, wifiManager) }
    val wardrivingRepository by lazy { WardrivingRepository(wardrivingCollector, database.wardrivingDao()) }
    val uwbService by lazy { UwbService(appContext) }
    val usbCameraDetector by lazy { UsbCameraDetector(appContext, usbManager) }
    val gpsTracker by lazy { GpsTracker(appContext) }
    val wifiDirectManager by lazy { WifiDirectManager(appContext) }

    // Phase 3 features
    val bluetoothClassicScanner by lazy { BluetoothClassicScanner(appContext, bluetoothManager) }
    val sppConnectionManager by lazy { SppConnectionManager(bluetoothManager) }
    val mifareClassicReader by lazy { MifareClassicReader() }
    val sdpServiceDiscovery by lazy { SdpServiceDiscovery(appContext, bluetoothManager) }
    val wifiDirectFileTransfer by lazy { WifiDirectFileTransfer(appContext) }

    // Phase 4 features
    val hiddenCameraDetector by lazy { HiddenCameraDetector(appContext) }
}

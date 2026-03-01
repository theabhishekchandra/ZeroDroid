package com.abhishek.zerodroid.features.usb.domain

import androidx.compose.ui.graphics.Color
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

data class KnownUsbDevice(
    val vendorName: String,
    val productName: String,
    val category: String,
    val threatLevel: ThreatLevel,
    val description: String
)

enum class ThreatLevel(val label: String, val color: Color) {
    SAFE("Safe", TerminalGreen),
    NORMAL("Normal", TerminalCyan),
    CAUTION("Caution", TerminalAmber),
    DANGER("Danger!", TerminalRed)
}

object UsbDeviceDatabase {
    private val knownDevices = mapOf(
        // Attack tools
        "1FC9:0083" to KnownUsbDevice("Hak5", "USB Rubber Ducky", "HID Attack", ThreatLevel.DANGER, "Keystroke injection attack tool"),
        "1D50:6089" to KnownUsbDevice("Great Scott", "HackRF One", "SDR", ThreatLevel.CAUTION, "Software-defined radio transceiver"),
        "0CF3:9271" to KnownUsbDevice("Hak5", "WiFi Pineapple", "Network Attack", ThreatLevel.DANGER, "Rogue access point / MITM tool"),
        "1FC9:0082" to KnownUsbDevice("Hak5", "Bash Bunny", "Multi-Attack", ThreatLevel.DANGER, "Multi-vector USB attack platform"),
        "1D50:6002" to KnownUsbDevice("Great Scott", "Ubertooth One", "BLE Sniffer", ThreatLevel.CAUTION, "Bluetooth packet sniffer"),
        "9AC4:4B8F" to KnownUsbDevice("Proxmark", "Proxmark3", "RFID Tool", ThreatLevel.CAUTION, "RFID research and cloning tool"),
        "0483:5740" to KnownUsbDevice("Flipper", "Flipper Zero", "Multi-Tool", ThreatLevel.CAUTION, "Multi-protocol hacking tool"),
        // RTL-SDR
        "0BDA:2838" to KnownUsbDevice("Realtek", "RTL2838U", "SDR Dongle", ThreatLevel.NORMAL, "RTL-SDR compatible DVB-T dongle"),
        "0BDA:2832" to KnownUsbDevice("Realtek", "RTL2832U", "SDR Dongle", ThreatLevel.NORMAL, "RTL-SDR compatible DVB-T dongle"),
        // Consumer
        "05AC:12A8" to KnownUsbDevice("Apple", "iPhone", "Phone", ThreatLevel.SAFE, "Apple iPhone"),
        "04E8:6860" to KnownUsbDevice("Samsung", "Galaxy", "Phone", ThreatLevel.SAFE, "Samsung Galaxy device"),
        "18D1:4EE7" to KnownUsbDevice("Google", "Pixel", "Phone", ThreatLevel.SAFE, "Google Pixel device"),
        "046D:C52B" to KnownUsbDevice("Logitech", "Unifying Receiver", "Input", ThreatLevel.SAFE, "Logitech wireless receiver"),
        "045E:07A5" to KnownUsbDevice("Microsoft", "Wireless Adapter", "Input", ThreatLevel.SAFE, "Microsoft wireless adapter"),
        "0781:5567" to KnownUsbDevice("SanDisk", "Cruzer Blade", "Storage", ThreatLevel.SAFE, "USB flash drive"),
        "1B1C:1B20" to KnownUsbDevice("Corsair", "Keyboard", "Input", ThreatLevel.SAFE, "Corsair gaming keyboard"),
        "1532:0043" to KnownUsbDevice("Razer", "Mouse", "Input", ThreatLevel.SAFE, "Razer gaming mouse")
    )

    fun lookup(vid: Int, pid: Int): KnownUsbDevice? {
        val key = "%04X:%04X".format(vid, pid)
        return knownDevices[key]
    }

    fun lookup(vidPid: String): KnownUsbDevice? {
        return knownDevices[vidPid.uppercase()]
    }
}

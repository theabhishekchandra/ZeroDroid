package com.abhishek.zerodroid.features.ble.domain

import androidx.compose.ui.graphics.Color
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen

data class BleDeviceType(
    val category: String,
    val icon: String,
    val color: Color
)

object BleDeviceTypeIdentifier {
    private val namePatterns = listOf(
        Regex("(?i)(airpods|buds|earbuds|pods|jabra|jbl|beats|bose|sony|wf-|wh-)") to BleDeviceType("Audio", "headphones", TerminalCyan),
        Regex("(?i)(band|mi band|fitbit|garmin|watch|huawei band|amazfit|versa|charge)") to BleDeviceType("Fitness", "watch", TerminalGreen),
        Regex("(?i)(tile|airtag|smarttag|chipolo|nut|finder|tracker)") to BleDeviceType("Tracker", "location", TerminalAmber),
        Regex("(?i)(keyboard|mouse|trackpad|logitech|mx|k380)") to BleDeviceType("Input", "keyboard", TerminalCyan),
        Regex("(?i)(tv|roku|fire|chromecast|shield|apple.tv)") to BleDeviceType("TV/Media", "tv", TerminalCyan),
        Regex("(?i)(iphone|pixel|galaxy|oneplus|samsung|huawei|xiaomi|oppo|vivo|redmi)") to BleDeviceType("Phone", "phone", TerminalGreen),
        Regex("(?i)(ipad|tab|tablet|surface)") to BleDeviceType("Tablet", "tablet", TerminalCyan),
        Regex("(?i)(printer|hp |epson|canon|brother)") to BleDeviceType("Printer", "print", TerminalCyan),
        Regex("(?i)(thermostat|nest|ecobee|hue|light|bulb|plug|switch|smart)") to BleDeviceType("Smart Home", "home", TerminalGreen),
        Regex("(?i)(car|tesla|bmw|ford|obd|elm327)") to BleDeviceType("Automotive", "car", TerminalAmber)
    )

    /** 16-bit service UUIDs registered to tracker makers (Tile, Samsung SmartTag). */
    private val trackerServiceIds = setOf("feed", "feec", "fd5a")

    fun identify(name: String?, serviceUuids: List<String> = emptyList()): BleDeviceType {
        if (serviceUuids.any { it.lowercase().removePrefix("0000").take(4) in trackerServiceIds }) {
            return BleDeviceType("Tracker", "location", TerminalAmber)
        }
        if (name != null) {
            for ((pattern, type) in namePatterns) {
                if (pattern.containsMatchIn(name)) return type
            }
        }
        return BleDeviceType("Unknown", "bluetooth", TerminalCyan)
    }
}

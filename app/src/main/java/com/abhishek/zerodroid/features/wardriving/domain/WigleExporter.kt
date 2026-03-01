package com.abhishek.zerodroid.features.wardriving.domain

import com.abhishek.zerodroid.core.database.entity.WardrivingRecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WigleExporter {

    fun export(records: List<WardrivingRecordEntity>): String {
        val sb = StringBuilder()
        // WiGLE CSV header
        sb.appendLine("WigleWifi-1.4,appRelease=ZeroDroid,model=Android,release=1.0,device=ZeroDroid,display=ZeroDroid,board=,brand=")
        sb.appendLine("MAC,SSID,AuthMode,FirstSeen,Channel,RSSI,CurrentLatitude,CurrentLongitude,AltitudeMeters,AccuracyMeters,Type")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        records.forEach { record ->
            val date = dateFormat.format(Date(record.timestamp))
            val channel = frequencyToChannel(record.frequency)
            sb.appendLine(
                "${record.bssid},${record.ssid ?: ""},${record.capabilities ?: ""},$date,$channel,${record.rssi},${record.lat},${record.lng},0,0,WIFI"
            )
        }

        return sb.toString()
    }

    private fun frequencyToChannel(freq: Int): Int = when {
        freq in 2412..2484 -> (freq - 2407) / 5
        freq in 5170..5825 -> (freq - 5000) / 5
        else -> 0
    }
}

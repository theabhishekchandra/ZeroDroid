package com.abhishek.zerodroid.features.camera.domain

object QrContentParser {

    fun parse(rawValue: String): Pair<QrContentType, String> {
        return when {
            rawValue.startsWith("http://") || rawValue.startsWith("https://") ->
                QrContentType.URL to rawValue

            rawValue.startsWith("WIFI:") -> {
                val ssid = extractField(rawValue, "S:")
                val password = extractField(rawValue, "P:")
                val type = extractField(rawValue, "T:")
                QrContentType.WIFI to "SSID: $ssid\nPassword: $password\nType: $type"
            }

            rawValue.startsWith("BEGIN:VCARD") ->
                QrContentType.VCARD to rawValue

            rawValue.startsWith("mailto:") ->
                QrContentType.EMAIL to rawValue.removePrefix("mailto:")

            rawValue.startsWith("MAILTO:") ->
                QrContentType.EMAIL to rawValue.removePrefix("MAILTO:")

            rawValue.startsWith("tel:") ->
                QrContentType.PHONE to rawValue.removePrefix("tel:")

            rawValue.startsWith("TEL:") ->
                QrContentType.PHONE to rawValue.removePrefix("TEL:")

            rawValue.startsWith("smsto:") || rawValue.startsWith("SMSTO:") ->
                QrContentType.SMS to rawValue.substringAfter(":")

            rawValue.startsWith("sms:") || rawValue.startsWith("SMS:") ->
                QrContentType.SMS to rawValue.substringAfter(":")

            rawValue.startsWith("geo:") ->
                QrContentType.GEO to rawValue.removePrefix("geo:")

            else -> QrContentType.TEXT to rawValue
        }
    }

    private fun extractField(wifiString: String, prefix: String): String {
        val start = wifiString.indexOf(prefix)
        if (start == -1) return ""
        val valueStart = start + prefix.length
        val end = wifiString.indexOf(';', valueStart)
        return if (end == -1) wifiString.substring(valueStart) else wifiString.substring(valueStart, end)
    }
}

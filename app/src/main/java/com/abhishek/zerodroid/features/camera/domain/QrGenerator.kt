package com.abhishek.zerodroid.features.camera.domain

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrGenerator {
    fun generate(
        content: String,
        size: Int = 512,
        foregroundColor: Int = Color.WHITE,
        backgroundColor: Int = Color.TRANSPARENT
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) foregroundColor else backgroundColor)
                }
            }
            bitmap
        } catch (e: Exception) { null }
    }

    fun formatWifiContent(ssid: String, password: String, security: WifiSecurity): String {
        return "WIFI:T:${security.code};S:$ssid;P:$password;;"
    }

    enum class WifiSecurity(val displayName: String, val code: String) {
        WPA("WPA/WPA2", "WPA"),
        WEP("WEP", "WEP"),
        NONE("None", "nopass")
    }
}

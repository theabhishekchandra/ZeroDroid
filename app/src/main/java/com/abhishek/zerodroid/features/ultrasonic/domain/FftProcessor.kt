package com.abhishek.zerodroid.features.ultrasonic.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object FftProcessor {

    /**
     * Cooley-Tukey radix-2 FFT.
     * Input size must be a power of 2.
     * Returns magnitude array of size n/2.
     */
    fun fft(input: FloatArray): FloatArray {
        val n = input.size
        require(n > 0 && (n and (n - 1)) == 0) { "Input size must be power of 2" }

        val real = input.copyOf()
        val imag = FloatArray(n)

        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tmpR = real[i]; real[i] = real[j]; real[j] = tmpR
                val tmpI = imag[i]; imag[i] = imag[j]; imag[j] = tmpI
            }
        }

        // FFT butterfly
        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angle = -2.0 * PI / len
            val wR = cos(angle).toFloat()
            val wI = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var curR = 1f
                var curI = 0f
                for (k in 0 until halfLen) {
                    val tR = curR * real[i + k + halfLen] - curI * imag[i + k + halfLen]
                    val tI = curR * imag[i + k + halfLen] + curI * real[i + k + halfLen]
                    real[i + k + halfLen] = real[i + k] - tR
                    imag[i + k + halfLen] = imag[i + k] - tI
                    real[i + k] = real[i + k] + tR
                    imag[i + k] = imag[i + k] + tI
                    val newR = curR * wR - curI * wI
                    val newI = curR * wI + curI * wR
                    curR = newR
                    curI = newI
                }
                i += len
            }
            len = len shl 1
        }

        // Compute magnitudes for first half
        val magnitudes = FloatArray(n / 2)
        for (i in 0 until n / 2) {
            magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i]) / n
        }
        return magnitudes
    }

    /**
     * Apply Hanning window to reduce spectral leakage.
     */
    fun applyHanningWindow(data: FloatArray): FloatArray {
        val n = data.size
        return FloatArray(n) { i ->
            val w = 0.5f * (1f - cos(2.0 * PI * i / (n - 1)).toFloat())
            data[i] * w
        }
    }
}

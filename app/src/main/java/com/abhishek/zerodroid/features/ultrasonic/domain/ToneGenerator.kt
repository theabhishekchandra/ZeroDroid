package com.abhishek.zerodroid.features.ultrasonic.domain

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

class ToneGenerator {
    companion object {
        const val SAMPLE_RATE = 48000
        const val MIN_FREQUENCY = 18000
        const val MAX_FREQUENCY = 24000
        private const val AMPLITUDE = 0.8f
        private const val BUFFER_DURATION_MS = 100
    }

    private var audioTrack: AudioTrack? = null
    @Volatile private var isPlaying = false
    @Volatile private var currentFrequency: Int = 20000
    val playing: Boolean get() = isPlaying

    fun start(frequencyHz: Int) {
        if (isPlaying) stop()
        currentFrequency = frequencyHz.coerceIn(MIN_FREQUENCY, MAX_FREQUENCY)
        val bufferSizeSamples = SAMPLE_RATE * BUFFER_DURATION_MS / 1000
        val bufferSizeBytes = bufferSizeSamples * 4
        val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val actualBufferSize = maxOf(bufferSizeBytes, minBufferSize)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(actualBufferSize).setTransferMode(AudioTrack.MODE_STREAM).build()

        isPlaying = true
        audioTrack?.play()

        val buffer = FloatArray(bufferSizeSamples)
        var phase = 0.0
        while (isPlaying) {
            val freq = currentFrequency
            val phaseIncrement = 2.0 * PI * freq / SAMPLE_RATE
            for (i in buffer.indices) {
                buffer[i] = (AMPLITUDE * sin(phase)).toFloat()
                phase += phaseIncrement
                if (phase >= 2.0 * PI) phase -= 2.0 * PI
            }
            val written = audioTrack?.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING) ?: -1
            if (written < 0) break
        }
    }

    fun setFrequency(frequencyHz: Int) { currentFrequency = frequencyHz.coerceIn(MIN_FREQUENCY, MAX_FREQUENCY) }

    fun stop() {
        isPlaying = false
        try { audioTrack?.stop() } catch (_: Exception) {}
        try { audioTrack?.release() } catch (_: Exception) {}
        audioTrack = null
    }

    fun generatePreviewSamples(frequencyHz: Int, sampleCount: Int = 200): FloatArray {
        val samples = FloatArray(sampleCount)
        val cyclesShown = 2.0
        for (i in samples.indices) {
            val t = i.toDouble() / sampleCount * cyclesShown * 2.0 * PI
            samples[i] = sin(t).toFloat()
        }
        return samples
    }
}

package com.abhishek.zerodroid.features.ultrasonic.domain

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class UltrasonicAnalyzer {

    companion object {
        const val SAMPLE_RATE = 48000
        const val FFT_SIZE = 4096
        const val BIN_RESOLUTION = SAMPLE_RATE.toFloat() / FFT_SIZE // ~11.7 Hz
        const val ULTRASONIC_START_HZ = 18000f
        const val ULTRASONIC_END_HZ = 24000f
    }

    @SuppressLint("MissingPermission")
    fun analyze(): Flow<UltrasonicState> = flow {
        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            ),
            FFT_SIZE * 4
        )

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                bufferSize
            )
        } catch (e: Exception) {
            emit(UltrasonicState(error = "Failed to initialize audio: ${e.message}"))
            return@flow
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            emit(UltrasonicState(error = "AudioRecord failed to initialize"))
            recorder.release()
            return@flow
        }

        try {
            recorder.startRecording()
            val buffer = FloatArray(FFT_SIZE)

            while (true) {
                val read = recorder.read(buffer, 0, FFT_SIZE, AudioRecord.READ_BLOCKING)
                if (read < FFT_SIZE) {
                    delay(10)
                    continue
                }

                val windowed = FftProcessor.applyHanningWindow(buffer)
                val magnitudes = FftProcessor.fft(windowed)

                // Build frequency bins for ultrasonic range
                val startBin = (ULTRASONIC_START_HZ / BIN_RESOLUTION).toInt()
                val endBin = minOf((ULTRASONIC_END_HZ / BIN_RESOLUTION).toInt(), magnitudes.size - 1)

                val spectrumData = (startBin..endBin).map { bin ->
                    FrequencyBin(
                        frequencyHz = bin * BIN_RESOLUTION,
                        magnitude = magnitudes[bin]
                    )
                }

                val peak = spectrumData.maxByOrNull { it.magnitude }
                val beacons = BeaconDetector.detect(spectrumData, SAMPLE_RATE, FFT_SIZE)

                emit(
                    UltrasonicState(
                        isRecording = true,
                        spectrumData = spectrumData,
                        detectedBeacons = beacons,
                        peakFrequency = peak?.frequencyHz ?: 0f,
                        peakMagnitude = peak?.magnitude ?: 0f
                    )
                )

                delay(50) // ~20 FPS
            }
        } catch (e: Exception) {
            emit(UltrasonicState(error = e.message))
        } finally {
            try {
                recorder.stop()
                recorder.release()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.Default)
}

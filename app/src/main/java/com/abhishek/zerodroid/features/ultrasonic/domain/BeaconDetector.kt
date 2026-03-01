package com.abhishek.zerodroid.features.ultrasonic.domain

object BeaconDetector {

    private const val THRESHOLD_DB = 20f // dB above noise floor
    private const val MIN_BEACON_FREQ = 18000f
    private const val MAX_BEACON_FREQ = 24000f
    private const val MIN_SUSTAINED_BINS = 2

    fun detect(
        spectrum: List<FrequencyBin>,
        sampleRate: Int = 48000,
        fftSize: Int = 4096
    ): List<UltrasonicBeacon> {
        if (spectrum.isEmpty()) return emptyList()

        // Filter to ultrasonic range
        val ultrasonicBins = spectrum.filter {
            it.frequencyHz in MIN_BEACON_FREQ..MAX_BEACON_FREQ
        }
        if (ultrasonicBins.isEmpty()) return emptyList()

        // Calculate noise floor (median magnitude)
        val sorted = ultrasonicBins.map { it.magnitude }.sorted()
        val noiseFloor = sorted[sorted.size / 2]

        // Find peaks above threshold
        val threshold = noiseFloor * THRESHOLD_DB
        val peaks = mutableListOf<UltrasonicBeacon>()
        var i = 0

        while (i < ultrasonicBins.size) {
            if (ultrasonicBins[i].magnitude > threshold) {
                var j = i
                var maxMag = ultrasonicBins[i].magnitude
                var maxIdx = i
                while (j < ultrasonicBins.size && ultrasonicBins[j].magnitude > threshold) {
                    if (ultrasonicBins[j].magnitude > maxMag) {
                        maxMag = ultrasonicBins[j].magnitude
                        maxIdx = j
                    }
                    j++
                }
                if (j - i >= MIN_SUSTAINED_BINS) {
                    peaks.add(
                        UltrasonicBeacon(
                            centerFrequencyHz = ultrasonicBins[maxIdx].frequencyHz,
                            bandwidth = ultrasonicBins[j - 1].frequencyHz - ultrasonicBins[i].frequencyHz,
                            magnitude = maxMag
                        )
                    )
                }
                i = j
            } else {
                i++
            }
        }

        return peaks
    }
}

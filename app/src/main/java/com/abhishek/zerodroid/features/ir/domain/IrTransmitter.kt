package com.abhishek.zerodroid.features.ir.domain

import android.hardware.ConsumerIrManager

class IrTransmitter(
    private val irManager: ConsumerIrManager?
) {
    val isAvailable: Boolean
        get() = irManager?.hasIrEmitter() == true

    fun transmit(signal: IrSignal): TransmitResult {
        val manager = irManager ?: return TransmitResult.Error("IR hardware not available")
        if (!manager.hasIrEmitter()) return TransmitResult.Error("No IR emitter found")

        val pattern = if (signal.protocol == IrProtocol.RAW) {
            signal.rawPattern ?: return TransmitResult.Error("No raw pattern provided")
        } else {
            IrProtocolEncoder.encode(signal.protocol, signal.code)
                ?: return TransmitResult.Error("Invalid code for ${signal.protocol.displayName}")
        }

        return try {
            val ranges = manager.carrierFrequencies
            val inRange = ranges?.any { signal.frequency in it.minFrequency..it.maxFrequency } ?: true
            if (!inRange) {
                return TransmitResult.Error("Frequency ${signal.frequency}Hz not supported by hardware")
            }
            manager.transmit(signal.frequency, pattern)
            TransmitResult.Success
        } catch (e: Exception) {
            TransmitResult.Error("Transmit failed: ${e.message}")
        }
    }
}

package com.aboezzat.remote

import android.content.Context
import android.hardware.ConsumerIrManager

/**
 * Sends a verified raw infrared timing pattern.
 *
 * Profiles must contain timings captured from, or independently verified against,
 * the exact target remote/device. Do not populate this with guessed values.
 */
class InfraredTransmitter(context: Context) {
    private val manager =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun isAvailable(): Boolean = manager?.hasIrEmitter() == true

    fun send(carrierFrequencyHz: Int, patternMicros: IntArray): Result<Unit> {
        val ir = manager ?: return Result.failure(IllegalStateException("IR service is unavailable"))
        if (!ir.hasIrEmitter()) {
            return Result.failure(IllegalStateException("This phone has no built-in IR emitter"))
        }
        if (carrierFrequencyHz !in 20_000..60_000) {
            return Result.failure(IllegalArgumentException("Invalid IR carrier frequency"))
        }
        if (patternMicros.size < 4 || patternMicros.size % 2 != 0) {
            return Result.failure(IllegalArgumentException("IR pattern must contain alternating mark/space timings"))
        }
        if (patternMicros.any { it <= 0 }) {
            return Result.failure(IllegalArgumentException("IR timings must be positive"))
        }

        return try {
            ir.transmit(carrierFrequencyHz, patternMicros)
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

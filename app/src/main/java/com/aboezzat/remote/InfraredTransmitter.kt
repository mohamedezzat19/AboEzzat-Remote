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

/**
 * Encoder for the standard 16-bit JVC protocol.
 *
 * The address byte is transmitted first, followed by the command byte.
 * Bits within each byte are sent least-significant-bit first. This encoder
 * does not provide model-specific address/command values; those must be
 * independently identified before any signal is sent to a device.
 */
object JvcProtocolEncoder {
    const val CARRIER_FREQUENCY_HZ = 37_900
    private const val HEADER_MARK_MICROS = 8_440
    private const val HEADER_SPACE_MICROS = 4_220
    private const val BIT_MARK_MICROS = 527
    private const val ZERO_SPACE_MICROS = 1_055
    private const val ONE_SPACE_MICROS = 2_110
    private const val TRAILING_GAP_MICROS = 42_000

    fun encode(address: Int, command: Int): IntArray {
        require(address in 0..0xFF) { "JVC address must be one byte" }
        require(command in 0..0xFF) { "JVC command must be one byte" }

        val pattern = IntArray(36)
        var index = 0
        pattern[index++] = HEADER_MARK_MICROS
        pattern[index++] = HEADER_SPACE_MICROS

        for (value in intArrayOf(address, command)) {
            for (bitIndex in 0..7) {
                pattern[index++] = BIT_MARK_MICROS
                pattern[index++] =
                    if (((value shr bitIndex) and 1) == 1) ONE_SPACE_MICROS
                    else ZERO_SPACE_MICROS
            }
        }

        pattern[index++] = BIT_MARK_MICROS
        pattern[index] = TRAILING_GAP_MICROS
        return pattern
    }
}

/**
 * NEC encoder for analysis/testing of candidate codes only.
 *
 * Values are represented in the usual hexadecimal form (for example 0x03FC14EB)
 * and NEC transmits the least-significant bit first. These candidates are NOT
 * verified for any particular JVC model and must never be sent as normal remote
 * commands until independently tested against the exact device.
 */
object NecCandidateEncoder {
    data class Candidate(
        val buttonId: String,
        val hexCode: Long,
        val note: String = "Unverified NEC candidate; not enabled for transmission"
    )

    const val CARRIER_FREQUENCY_HZ = 38_000

    val JVC_RC_EZ38S_CANDIDATES: List<Candidate> = listOf(
        Candidate("power", 0x03FC14EBL),
        Candidate("volume_up", 0x03FC02FDL),
        Candidate("volume_down", 0x03FC03FCL),
        Candidate("muting", 0x03FC0DF2L),
        Candidate("play", 0x03FC12EDL),
        Candidate("next", 0x03FC15EAL),
        Candidate("previous", 0x03FC16E9L),
        Candidate("tuner_band", 0x03FC21DEL)
    )

    /**
     * Creates an alternating mark/space pattern suitable for ConsumerIrManager.
     * The final 560us mark is followed by a 40ms space, so the array has an even
     * number of entries and satisfies InfraredTransmitter's validation.
     */
    fun encode32(hexCode: Long, trailingGapMicros: Int = 40_000): IntArray {
        require(hexCode in 0L..0xFFFF_FFFFL) { "NEC code must be an unsigned 32-bit value" }
        require(trailingGapMicros > 0) { "Trailing gap must be positive" }

        val pattern = IntArray(68)
        var index = 0
        pattern[index++] = 9_000
        pattern[index++] = 4_500

        for (bitIndex in 0..31) {
            val bit = (hexCode shr bitIndex) and 1L
            pattern[index++] = 560
            pattern[index++] = if (bit == 1L) 1_690 else 560
        }

        pattern[index++] = 560
        pattern[index] = trailingGapMicros
        return pattern
    }
}


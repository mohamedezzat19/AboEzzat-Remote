package com.aboezzat.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NecCandidateEncoderTest {
    @Test
    fun encode32ProducesValidAlternatingPatternLengthAndHeader() {
        val pattern = NecCandidateEncoder.encode32(0x03FC14EBL)

        assertEquals(68, pattern.size)
        assertEquals(9_000, pattern[0])
        assertEquals(4_500, pattern[1])
        assertEquals(560, pattern[66])
        assertEquals(40_000, pattern[67])
    }

    @Test
    fun encode32UsesLeastSignificantBitFirst() {
        val pattern = NecCandidateEncoder.encode32(0x00000001L)

        // The least-significant bit is sent first: bit 0 has a long space.
        assertEquals(560, pattern[2])
        assertEquals(1_690, pattern[3])
        // The remaining 31 bits are zero and use short spaces.
        assertEquals(560, pattern[4])
        assertEquals(560, pattern[5])
    }

    @Test
    fun encode32AllowsCustomTrailingGap() {
        val pattern = NecCandidateEncoder.encode32(0L, trailingGapMicros = 50_000)

        assertEquals(50_000, pattern.last())
    }

    @Test
    fun encode32RejectsValuesOutsideUnsigned32BitRange() {
        assertThrows(IllegalArgumentException::class.java) {
            NecCandidateEncoder.encode32(-1L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            NecCandidateEncoder.encode32(0x1_0000_0000L)
        }
    }

    @Test
    fun encode32RejectsNonPositiveTrailingGap() {
        assertThrows(IllegalArgumentException::class.java) {
            NecCandidateEncoder.encode32(0L, trailingGapMicros = 0)
        }
    }

    @Test
    fun candidateListContainsEightExplicitlyUnverifiedEntries() {
        assertEquals(8, NecCandidateEncoder.JVC_RC_EZ38S_CANDIDATES.size)
        assertEquals(
            listOf("power", "volume_up", "volume_down", "muting", "play", "next", "previous", "tuner_band"),
            NecCandidateEncoder.JVC_RC_EZ38S_CANDIDATES.map { it.buttonId }
        )
        assert(NecCandidateEncoder.JVC_RC_EZ38S_CANDIDATES.all { it.note.contains("Unverified") })
    }
}

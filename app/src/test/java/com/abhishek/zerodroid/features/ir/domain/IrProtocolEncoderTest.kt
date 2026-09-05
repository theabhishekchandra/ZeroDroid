package com.abhishek.zerodroid.features.ir.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IrProtocolEncoderTest {

    @Test
    fun `NEC frame has leader 32 bits and stop bit`() {
        val pattern = IrProtocolEncoder.encode(IrProtocol.NEC, "20DF10EF")!!

        assertEquals(2 + 64 + 2, pattern.size)
        assertEquals(9000, pattern[0])
        assertEquals(4500, pattern[1])
        assertEquals(560, pattern[pattern.size - 2])
        assertEquals(560, pattern[pattern.size - 1])
    }

    @Test
    fun `NEC encodes ones as long spaces and zeros as short spaces`() {
        val zeros = IrProtocolEncoder.encode(IrProtocol.NEC, "00000000")!!
        val ones = IrProtocolEncoder.encode(IrProtocol.NEC, "FFFFFFFF")!!

        val zeroSpaces = (0 until 32).map { zeros[2 + it * 2 + 1] }
        val oneSpaces = (0 until 32).map { ones[2 + it * 2 + 1] }
        assertTrue(zeroSpaces.all { it == 560 })
        assertTrue(oneSpaces.all { it == 1690 })
        // marks are always 560
        assertTrue((0 until 32).all { zeros[2 + it * 2] == 560 && ones[2 + it * 2] == 560 })
    }

    @Test
    fun `NEC sends most significant bit first`() {
        val pattern = IrProtocolEncoder.encode(IrProtocol.NEC, "80000000")!!

        assertEquals(1690, pattern[3])   // bit 31 set
        assertEquals(560, pattern[5])    // bit 30 clear
    }

    @Test
    fun `Samsung32 uses 4500 leader and 1590 one space`() {
        val pattern = IrProtocolEncoder.encode(IrProtocol.SAMSUNG32, "E0E040BF")!!

        assertEquals(68, pattern.size)
        assertEquals(4500, pattern[0])
        assertEquals(4500, pattern[1])
        assertEquals(1590, pattern[3]) // first bit of 0xE0 is 1
    }

    @Test
    fun `Sony picks 12 15 or 20 bit frames from the code width`() {
        val twelve = IrProtocolEncoder.encode(IrProtocol.SONY, "A90")!!
        val fifteen = IrProtocolEncoder.encode(IrProtocol.SONY, "1A90")!!
        val twenty = IrProtocolEncoder.encode(IrProtocol.SONY, "1A900")!!

        assertEquals(2 + 12 * 2, twelve.size)
        assertEquals(2 + 15 * 2, fifteen.size)
        assertEquals(2 + 20 * 2, twenty.size)
        assertEquals(2400, twelve[0])
        assertEquals(600, twelve[1])
        // Sony marks 1200 for one, 600 for zero; spaces always 600
        assertEquals(1200, twelve[2]) // 0xA90 top bit of 12 is 1
        assertEquals(600, twelve[3])
    }

    @Test
    fun `RC5 and RC6 produce fixed length Manchester frames`() {
        val rc5 = IrProtocolEncoder.encode(IrProtocol.RC5, "1234")!!
        val rc6 = IrProtocolEncoder.encode(IrProtocol.RC6, "ABCD")!!

        assertEquals(14 * 2, rc5.size)
        assertTrue(rc5.all { it == 889 })
        assertEquals(2 + 16 * 2, rc6.size)
        assertEquals(2666, rc6[0])
        assertEquals(889, rc6[1])
        // trailer bit (index 4 from LSB, i.e. 12th bit sent) is double width
        val trailerPos = 2 + (15 - 4) * 2
        assertEquals(1778, rc6[trailerPos])
        assertEquals(1778, rc6[trailerPos + 1])
    }

    @Test
    fun `invalid hex and RAW protocol return null`() {
        assertNull(IrProtocolEncoder.encode(IrProtocol.NEC, "not-hex"))
        assertNull(IrProtocolEncoder.encode(IrProtocol.NEC, ""))
        assertNull(IrProtocolEncoder.encode(IrProtocol.RAW, "20DF10EF"))
    }
}

package com.abhishek.zerodroid.features.ir.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipperIrParserTest {

    @Test
    fun `parses a parsed NEC record and joins address and command`() {
        val file = """
            Filetype: IR signals file
            Version: 1
            #
            name: Power
            type: parsed
            protocol: NEC
            address: 20 DF 00 00
            command: 10 EF 00 00
            #
        """.trimIndent()

        val signals = FlipperIrParser.parse(file)

        assertEquals(1, signals.size)
        val s = signals[0]
        assertEquals("Power", s.name)
        assertEquals(IrProtocol.NEC, s.protocol)
        assertEquals("20DF000010EF0000", s.code)
        assertEquals(38000, s.frequency)
        assertNull(s.rawPattern)
    }

    @Test
    fun `parses raw records with frequency and pattern`() {
        val file = """
            name: Custom
            type: raw
            frequency: 40000
            duty_cycle: 0.33
            data: 9000 4500 560 560 560 1690
        """.trimIndent()

        val signals = FlipperIrParser.parse(file)

        assertEquals(1, signals.size)
        val s = signals[0]
        assertEquals(IrProtocol.RAW, s.protocol)
        assertEquals(40000, s.frequency)
        assertEquals("", s.code)
        assertArrayEquals(intArrayOf(9000, 4500, 560, 560, 560, 1690), s.rawPattern)
    }

    @Test
    fun `handles multiple records and the last record without trailing separator`() {
        val file = """
            name: One
            type: parsed
            protocol: Samsung32
            address: E0 E0 00 00
            command: 40 BF 00 00
            #
            name: Two
            type: parsed
            protocol: SIRC
            address: 01 00 00 00
            command: 15 00 00 00
        """.trimIndent()

        val signals = FlipperIrParser.parse(file)

        assertEquals(listOf("One", "Two"), signals.map { it.name })
        assertEquals(IrProtocol.SAMSUNG32, signals[0].protocol)
        assertEquals(IrProtocol.SONY, signals[1].protocol)
    }

    @Test
    fun `unknown protocol falls back to NEC and unknown type is skipped`() {
        val file = """
            name: Odd
            type: parsed
            protocol: Kaseikyo
            address: 00 00 00 00
            command: 01 00 00 00
            #
            name: Broken
            type: something
            #
        """.trimIndent()

        val signals = FlipperIrParser.parse(file)

        assertEquals(1, signals.size)
        assertEquals(IrProtocol.NEC, signals[0].protocol)
    }

    @Test
    fun `non numeric frequency and raw tokens are tolerated`() {
        val file = """
            name: Noisy
            type: raw
            frequency: abc
            data: 100 x 200
            #
        """.trimIndent()

        val s = FlipperIrParser.parse(file).single()

        assertEquals(38000, s.frequency)
        assertArrayEquals(intArrayOf(100, 200), s.rawPattern)
    }

    @Test
    fun `empty input yields no signals`() {
        assertTrue(FlipperIrParser.parse("").isEmpty())
        assertTrue(FlipperIrParser.parse("Filetype: IR signals file\nVersion: 1\n").isEmpty())
    }
}

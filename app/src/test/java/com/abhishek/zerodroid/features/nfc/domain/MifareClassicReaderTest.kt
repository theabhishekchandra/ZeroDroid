package com.abhishek.zerodroid.features.nfc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MifareClassicReaderTest {

    private val reader = MifareClassicReader()

    private fun block(index: Int, bytes: ByteArray, hex: String = bytes.joinToString("") { "%02X".format(it) }) =
        MifareBlockData(index, bytes, hex)

    @Test
    fun `default key list starts with factory key and has no duplicates`() {
        val keys = MifareClassicReader.DEFAULT_KEYS
        assertEquals(10, keys.size)
        assertTrue(keys.all { it.size == 6 })
        assertTrue(keys[0].all { it == 0xFF.toByte() })
        assertEquals(keys.size, keys.map { it.toList() }.toSet().size)
    }

    @Test
    fun `formatDump prints header keys hex ascii and trailer marker`() {
        val data = "HELLO WORLD!!!!!".toByteArray()
        val trailer = ByteArray(16) { 0 }
        val sectors = listOf(
            MifareSectorData(0, listOf(block(0, data), block(3, trailer)), keyUsed = "FFFFFFFFFFFF", isAuthenticated = true),
            MifareSectorData(1, emptyList(), keyUsed = "", isAuthenticated = false)
        )

        val dump = reader.formatDump(sectors)

        assertTrue(dump.contains("Sectors: 2"))
        assertTrue(dump.contains("Authenticated: 1/2"))
        assertTrue(dump.contains("Key: FFFFFFFFFFFF (Key A)"))
        assertTrue(dump.contains("Block   0: 48 45 4C 4C 4F 20 57 4F 52 4C 44 21 21 21 21 21 | HELLO WORLD!!!!!"))
        assertTrue(dump.contains("Block   3: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 | ................ [TRAILER]"))
        assertTrue(dump.contains("+Sector: 1"))
        assertTrue(dump.contains("[NOT AUTHENTICATED]"))
    }

    @Test
    fun `formatDump flags read errors`() {
        val sectors = listOf(
            MifareSectorData(2, listOf(block(8, ByteArray(0), hex = "READ ERROR")), keyUsed = "A0A1A2A3A4A5", keyType = "B", isAuthenticated = true)
        )

        val dump = reader.formatDump(sectors)

        assertTrue(dump.contains("Key: A0A1A2A3A4A5 (Key B)"))
        assertTrue(dump.contains("Block   8: [READ ERROR]"))
    }

    @Test
    fun `interprets factory transport access bits`() {
        // Key A (6) + access bits FF 07 80 + GPB 69 + Key B (6)
        val trailer = ByteArray(16).also {
            it[6] = 0xFF.toByte(); it[7] = 0x07; it[8] = 0x80.toByte(); it[9] = 0x69
        }

        val lines = reader.interpretAccessBits(trailer)

        assertEquals(4, lines.size)
        assertEquals("Block 0: C1C2C3=000 -> R/W with Key A|B", lines[0])
        assertEquals("Block 2: C1C2C3=000 -> R/W with Key A|B", lines[2])
        assertEquals("Trailer: C1C2C3=001 -> KeyA: W(A) | Bits: R(A) W(A) | KeyB: R(A) W(A)", lines[3])
    }

    @Test
    fun `interprets a locked configuration`() {
        // C1=1 for all blocks (byte7 high nibble), C2=1 (byte8 low nibble), C3=1 (byte8 high nibble)
        val trailer = ByteArray(16).also { it[7] = 0xF0.toByte(); it[8] = 0xFF.toByte() }

        val lines = reader.interpretAccessBits(trailer)

        assertTrue(lines[0].endsWith("C1C2C3=111 -> Never"))
        assertTrue(lines[3].contains("C1C2C3=111"))
    }

    @Test
    fun `short trailer data is rejected`() {
        assertEquals(listOf("Invalid trailer data"), reader.interpretAccessBits(ByteArray(9)))
    }

    @Test
    fun `block equality compares index and content`() {
        assertEquals(block(1, byteArrayOf(1, 2)), block(1, byteArrayOf(1, 2), hex = "differs"))
        assertTrue(block(1, byteArrayOf(1, 2)) != block(2, byteArrayOf(1, 2)))
    }
}

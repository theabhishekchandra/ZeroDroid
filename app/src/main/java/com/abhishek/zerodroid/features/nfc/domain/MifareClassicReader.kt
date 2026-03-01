package com.abhishek.zerodroid.features.nfc.domain

import android.nfc.Tag
import android.nfc.tech.MifareClassic

data class MifareSectorData(
    val sectorIndex: Int,
    val blocks: List<MifareBlockData>,
    val keyUsed: String,
    val keyType: String = "A",
    val isAuthenticated: Boolean
)

data class MifareBlockData(
    val blockIndex: Int,
    val data: ByteArray,
    val hexString: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MifareBlockData) return false
        return blockIndex == other.blockIndex && data.contentEquals(other.data)
    }
    override fun hashCode(): Int = blockIndex * 31 + data.contentHashCode()
}

class MifareClassicReader {

    companion object {
        val DEFAULT_KEYS: List<ByteArray> = listOf(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
            byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte()),
            byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
            byteArrayOf(0xB0.toByte(), 0xB1.toByte(), 0xB2.toByte(), 0xB3.toByte(), 0xB4.toByte(), 0xB5.toByte()),
            byteArrayOf(0x4D.toByte(), 0x3A.toByte(), 0x99.toByte(), 0xC3.toByte(), 0x51.toByte(), 0xDD.toByte()),
            byteArrayOf(0x1A.toByte(), 0x98.toByte(), 0x2C.toByte(), 0x7E.toByte(), 0x45.toByte(), 0x9A.toByte()),
            byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()),
            byteArrayOf(0x71.toByte(), 0x4C.toByte(), 0x5C.toByte(), 0x88.toByte(), 0x6E.toByte(), 0x97.toByte()),
            byteArrayOf(0x58.toByte(), 0x7E.toByte(), 0xE5.toByte(), 0xF9.toByte(), 0x35.toByte(), 0x0F.toByte())
        )

        private fun ByteArray.toHexKey(): String = joinToString("") { "%02X".format(it) }
    }

    /**
     * Reads all sectors from a MIFARE Classic tag using default keys merged with optional custom keys.
     * Custom keys are tried first for priority.
     */
    fun readAllSectors(
        tag: Tag,
        customKeys: List<ByteArray> = emptyList()
    ): List<MifareSectorData> {
        val mifare = MifareClassic.get(tag) ?: return emptyList()
        val sectors = mutableListOf<MifareSectorData>()

        // Merge keys: custom first (priority), then defaults, deduplicated
        val allKeys = buildList {
            addAll(customKeys)
            addAll(DEFAULT_KEYS)
        }.distinctBy { it.toHexKey() }

        try {
            mifare.connect()

            for (sectorIndex in 0 until mifare.sectorCount) {
                var authenticated = false
                var keyUsed = ""
                var keyType = ""

                for (key in allKeys) {
                    // Try Key A first
                    try {
                        if (mifare.authenticateSectorWithKeyA(sectorIndex, key)) {
                            authenticated = true
                            keyUsed = key.toHexKey()
                            keyType = "A"
                            break
                        }
                    } catch (_: Exception) {}

                    // Try Key B
                    try {
                        if (mifare.authenticateSectorWithKeyB(sectorIndex, key)) {
                            authenticated = true
                            keyUsed = key.toHexKey()
                            keyType = "B"
                            break
                        }
                    } catch (_: Exception) {}
                }

                val blocks = mutableListOf<MifareBlockData>()
                if (authenticated) {
                    val firstBlock = mifare.sectorToBlock(sectorIndex)
                    val blockCount = mifare.getBlockCountInSector(sectorIndex)
                    for (blockOffset in 0 until blockCount) {
                        try {
                            val blockIndex = firstBlock + blockOffset
                            val data = mifare.readBlock(blockIndex)
                            blocks.add(
                                MifareBlockData(
                                    blockIndex = blockIndex,
                                    data = data,
                                    hexString = data.joinToString(" ") { "%02X".format(it) }
                                )
                            )
                        } catch (_: Exception) {
                            blocks.add(
                                MifareBlockData(
                                    blockIndex = firstBlock + blockOffset,
                                    data = ByteArray(0),
                                    hexString = "READ ERROR"
                                )
                            )
                        }
                    }
                }

                sectors.add(
                    MifareSectorData(
                        sectorIndex = sectorIndex,
                        blocks = blocks,
                        keyUsed = keyUsed,
                        keyType = keyType,
                        isAuthenticated = authenticated
                    )
                )
            }
        } catch (_: Exception) {
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }

        return sectors
    }

    /**
     * Writes 16 bytes of data to a specific block on a MIFARE Classic tag.
     * WARNING: Writing to trailer blocks (last block of each sector) can permanently brick the sector.
     *
     * @param tag The NFC tag to write to
     * @param blockIndex The absolute block index to write
     * @param data Exactly 16 bytes of data to write
     * @param key The 6-byte authentication key
     * @param useKeyB If true, authenticate with Key B instead of Key A
     * @return true if the write was successful, false otherwise
     */
    fun writeBlock(
        tag: Tag,
        blockIndex: Int,
        data: ByteArray,
        key: ByteArray,
        useKeyB: Boolean = false
    ): Boolean {
        if (data.size != 16) return false
        if (key.size != 6) return false

        val mifare = MifareClassic.get(tag) ?: return false

        return try {
            mifare.connect()

            val sectorIndex = mifare.blockToSector(blockIndex)

            val authenticated = if (useKeyB) {
                mifare.authenticateSectorWithKeyB(sectorIndex, key)
            } else {
                mifare.authenticateSectorWithKeyA(sectorIndex, key)
            }

            if (!authenticated) return false

            mifare.writeBlock(blockIndex, data)
            true
        } catch (_: Exception) {
            false
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    /**
     * Formats sector data into a standard MIFARE Classic hex dump string
     * suitable for clipboard copy or file export.
     *
     * Format:
     * ```
     * +Sector: 0
     *   Block  0: FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
     *   Block  1: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
     *   ...
     * ```
     */
    fun formatDump(sectors: List<MifareSectorData>): String {
        val sb = StringBuilder()
        sb.appendLine("---------- MIFARE Classic Dump ----------")
        sb.appendLine("Sectors: ${sectors.size}")
        sb.appendLine("Authenticated: ${sectors.count { it.isAuthenticated }}/${sectors.size}")
        sb.appendLine()

        for (sector in sectors) {
            sb.appendLine("+Sector: ${sector.sectorIndex}")

            if (!sector.isAuthenticated) {
                sb.appendLine("  [NOT AUTHENTICATED]")
            } else {
                sb.appendLine("  Key: ${sector.keyUsed} (Key ${sector.keyType})")

                for (block in sector.blocks) {
                    val blockLabel = "Block %3d".format(block.blockIndex)
                    val isTrailer = block == sector.blocks.lastOrNull()

                    if (block.hexString == "READ ERROR") {
                        sb.appendLine("  $blockLabel: [READ ERROR]")
                    } else {
                        // Hex representation
                        val hex = block.data.joinToString(" ") { "%02X".format(it) }

                        // ASCII representation (printable chars only, else '.')
                        val ascii = block.data.map { b ->
                            val c = b.toInt() and 0xFF
                            if (c in 0x20..0x7E) c.toChar() else '.'
                        }.joinToString("")

                        val trailerMarker = if (isTrailer) " [TRAILER]" else ""
                        sb.appendLine("  $blockLabel: $hex | $ascii$trailerMarker")
                    }
                }
            }
            sb.appendLine()
        }

        sb.appendLine("---------- End of Dump ----------")
        return sb.toString()
    }

    /**
     * Interprets the 3 access bits bytes from a MIFARE Classic trailer block.
     * The trailer block is the last block of each sector (bytes 6-8 are access bits).
     *
     * @param trailerData The full 16-byte trailer block data
     * @return A human-readable description of the access conditions for each block
     */
    fun interpretAccessBits(trailerData: ByteArray): List<String> {
        if (trailerData.size < 10) return listOf("Invalid trailer data")

        // Access bits are in bytes 6, 7, 8 of the trailer block
        val byte6 = trailerData[6].toInt() and 0xFF
        val byte7 = trailerData[7].toInt() and 0xFF
        val byte8 = trailerData[8].toInt() and 0xFF

        // Extract C1, C2, C3 bits for each block (0-3, where 3 is the trailer)
        // byte6 = ~C2_b1 ~C2_b0 ~C1_b3 ~C1_b2 ~C1_b1 ~C1_b0 (inverted in low nibble)
        // byte7 = C1_b3 C1_b2 C1_b1 C1_b0 ~C3_b3 ~C3_b2 ~C3_b1 ~C3_b0
        // byte8 = C3_b3 C3_b2 C3_b1 C3_b0 C2_b3 C2_b2 C2_b1 C2_b0

        val c1 = IntArray(4)
        val c2 = IntArray(4)
        val c3 = IntArray(4)

        for (i in 0..3) {
            c1[i] = (byte7 shr (4 + i)) and 1
            c2[i] = (byte8 shr i) and 1
            c3[i] = (byte8 shr (4 + i)) and 1
        }

        val results = mutableListOf<String>()

        for (i in 0..2) {
            val bits = "${c1[i]}${c2[i]}${c3[i]}"
            val desc = when (bits) {
                "000" -> "R/W with Key A|B"
                "010" -> "R with Key A|B"
                "100" -> "R with Key A|B, W with Key B"
                "110" -> "R with Key A|B, W with Key B"
                "001" -> "R with Key A|B, W never"
                "011" -> "R with Key B, W with Key B"
                "101" -> "R with Key B, W never"
                "111" -> "Never"
                else -> "Unknown ($bits)"
            }
            results.add("Block $i: C1C2C3=$bits -> $desc")
        }

        // Trailer block (block 3) access conditions
        val trailerBits = "${c1[3]}${c2[3]}${c3[3]}"
        val trailerDesc = when (trailerBits) {
            "000" -> "KeyA: W(A) | Bits: R(A) W(never) | KeyB: R(A) W(A)"
            "010" -> "KeyA: W(never) | Bits: R(A) W(never) | KeyB: R(A) W(never)"
            "100" -> "KeyA: W(B) | Bits: R(A|B) W(never) | KeyB: W(never)"
            "110" -> "KeyA: W(never) | Bits: R(A|B) W(never) | KeyB: W(never)"
            "001" -> "KeyA: W(A) | Bits: R(A) W(A) | KeyB: R(A) W(A)"
            "011" -> "KeyA: W(B) | Bits: R(A|B) W(B) | KeyB: W(never)"
            "101" -> "KeyA: W(never) | Bits: R(A|B) W(B) | KeyB: W(never)"
            "111" -> "KeyA: W(never) | Bits: R(A|B) W(never) | KeyB: W(never)"
            else -> "Unknown ($trailerBits)"
        }
        results.add("Trailer: C1C2C3=$trailerBits -> $trailerDesc")

        return results
    }
}

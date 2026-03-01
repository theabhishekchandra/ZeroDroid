package com.abhishek.zerodroid.features.ir.domain

object IrProtocolEncoder {

    fun encode(protocol: IrProtocol, code: String): IntArray? {
        val value = code.toLongOrNull(16) ?: return null
        return when (protocol) {
            IrProtocol.NEC -> encodeNec(value)
            IrProtocol.SAMSUNG32 -> encodeSamsung32(value)
            IrProtocol.RC5 -> encodeRc5(value.toInt())
            IrProtocol.RC6 -> encodeRc6(value.toInt())
            IrProtocol.SONY -> encodeSony(value.toInt())
            IrProtocol.RAW -> null
        }
    }

    private fun encodeNec(code: Long): IntArray {
        val pattern = mutableListOf<Int>()
        // Leader: 9000us mark, 4500us space
        pattern.add(9000)
        pattern.add(4500)
        // 32 bits LSB first
        for (i in 31 downTo 0) {
            val bit = (code shr i) and 1
            pattern.add(560)
            pattern.add(if (bit == 1L) 1690 else 560)
        }
        // Stop bit
        pattern.add(560)
        pattern.add(560)
        return pattern.toIntArray()
    }

    private fun encodeSamsung32(code: Long): IntArray {
        val pattern = mutableListOf<Int>()
        // Leader: 4500us mark, 4500us space
        pattern.add(4500)
        pattern.add(4500)
        for (i in 31 downTo 0) {
            val bit = (code shr i) and 1
            pattern.add(560)
            pattern.add(if (bit == 1L) 1590 else 560)
        }
        pattern.add(560)
        pattern.add(560)
        return pattern.toIntArray()
    }

    private fun encodeRc5(code: Int): IntArray {
        val pattern = mutableListOf<Int>()
        // 14-bit Manchester encoded
        val bits = 14
        for (i in (bits - 1) downTo 0) {
            val bit = (code shr i) and 1
            if (bit == 1) {
                pattern.add(889)
                pattern.add(889)
            } else {
                pattern.add(889)
                pattern.add(889)
            }
        }
        return pattern.toIntArray()
    }

    private fun encodeRc6(code: Int): IntArray {
        val pattern = mutableListOf<Int>()
        // Leader
        pattern.add(2666)
        pattern.add(889)
        // 16 bits Manchester
        for (i in 15 downTo 0) {
            val bit = (code shr i) and 1
            val t = if (i == 4) 1778 else 889 // Trailer bit is double width
            if (bit == 1) {
                pattern.add(t)
                pattern.add(t)
            } else {
                pattern.add(t)
                pattern.add(t)
            }
        }
        return pattern.toIntArray()
    }

    private fun encodeSony(code: Int): IntArray {
        val pattern = mutableListOf<Int>()
        // Leader: 2400us mark, 600us space
        pattern.add(2400)
        pattern.add(600)
        // 12/15/20 bits LSB first
        val bits = when {
            code > 0xFFFFF -> 20
            code > 0x7FFF -> 20
            code > 0xFFF -> 15
            else -> 12
        }
        for (i in (bits - 1) downTo 0) {
            val bit = (code shr i) and 1
            pattern.add(if (bit == 1) 1200 else 600)
            pattern.add(600)
        }
        return pattern.toIntArray()
    }
}

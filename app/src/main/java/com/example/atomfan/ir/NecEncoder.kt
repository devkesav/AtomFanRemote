package com.example.atomfan.ir

object NecEncoder {
    val DEFAULT_SUPPORTED_FREQUENCIES = intArrayOf(30000, 33000, 36000, 38000, 40000, 56000)

    fun encodeNec(address: Int, command: Int): IntArray {
        val pattern = ArrayList<Int>(67)
        // Header: 9000 us mark, 4500 us space
        pattern.add(9000)
        pattern.add(4500)

        // Data bits (32 bits total: address 8 bits LSB-first, ~address 8 bits LSB-first, command 8 bits LSB-first, ~command 8 bits LSB-first)
        val addr8 = address and 0xFF
        val invAddr8 = (address.inv()) and 0xFF
        val cmd8 = command and 0xFF
        val invCmd8 = (command.inv()) and 0xFF

        val bytes = intArrayOf(addr8, invAddr8, cmd8, invCmd8)

        for (b in bytes) {
            for (i in 0 until 8) {
                val bit = (b ushr i) and 1
                pattern.add(560)
                if (bit == 1) {
                    pattern.add(1690)
                } else {
                    pattern.add(560)
                }
            }
        }

        // Stop mark: 560 us mark
        pattern.add(560)

        return pattern.toIntArray()
    }

    fun getNearestSupportedFrequency(requestedFreq: Int, phoneSupported: IntArray?): Int {
        val candidates = if (phoneSupported != null && phoneSupported.isNotEmpty()) {
            phoneSupported
        } else {
            DEFAULT_SUPPORTED_FREQUENCIES
        }
        return candidates.minByOrNull { kotlin.math.abs(it - requestedFreq) } ?: requestedFreq
    }
}

package com.example.atomfan

import com.example.atomfan.ir.NecEncoder
import org.junit.Assert.assertEquals
import org.junit.Test

class NecEncoderTest {

    @Test
    fun testEncodeNecFrameTimings() {
        val address = 0x00
        val command = 0x01

        val pattern = NecEncoder.encodeNec(address, command)

        // Total elements = 1 (header mark) + 1 (header space) + 32*2 (data bits) + 1 (stop mark) = 67
        assertEquals(67, pattern.size)

        // Header
        assertEquals(9000, pattern[0])
        assertEquals(4500, pattern[1])

        // Address 0x00 -> 8 bits of 0: (560, 560) x 8
        for (i in 0 until 8) {
            val markIndex = 2 + (i * 2)
            val spaceIndex = markIndex + 1
            assertEquals("Address bit $i mark", 560, pattern[markIndex])
            assertEquals("Address bit $i space", 560, pattern[spaceIndex])
        }

        // ~Address 0xFF -> 8 bits of 1: (560, 1690) x 8
        for (i in 0 until 8) {
            val markIndex = 18 + (i * 2)
            val spaceIndex = markIndex + 1
            assertEquals("InvAddress bit $i mark", 560, pattern[markIndex])
            assertEquals("InvAddress bit $i space", 1690, pattern[spaceIndex])
        }

        // Command 0x01 -> bit 0 is 1 (560, 1690), bits 1..7 are 0 (560, 560)
        assertEquals("Cmd bit 0 mark", 560, pattern[34])
        assertEquals("Cmd bit 0 space", 1690, pattern[35])
        for (i in 1 until 8) {
            val markIndex = 34 + (i * 2)
            val spaceIndex = markIndex + 1
            assertEquals("Cmd bit $i mark", 560, pattern[markIndex])
            assertEquals("Cmd bit $i space", 560, pattern[spaceIndex])
        }

        // ~Command 0xFE -> bit 0 is 0 (560, 560), bits 1..7 are 1 (560, 1690)
        assertEquals("InvCmd bit 0 mark", 560, pattern[50])
        assertEquals("InvCmd bit 0 space", 560, pattern[51])
        for (i in 1 until 8) {
            val markIndex = 50 + (i * 2)
            val spaceIndex = markIndex + 1
            assertEquals("InvCmd bit $i mark", 560, pattern[markIndex])
            assertEquals("InvCmd bit $i space", 1690, pattern[spaceIndex])
        }

        // Stop mark
        assertEquals(560, pattern[66])
    }

    @Test
    fun testGetNearestSupportedFrequencyWithNull() {
        val freq = NecEncoder.getNearestSupportedFrequency(38000, null)
        assertEquals(38000, freq)
    }

    @Test
    fun testGetNearestSupportedFrequencyWithCustomArray() {
        val phoneSupported = intArrayOf(30000, 36000, 40000)
        val freq = NecEncoder.getNearestSupportedFrequency(37000, phoneSupported)
        assertEquals(36000, freq)
    }
}

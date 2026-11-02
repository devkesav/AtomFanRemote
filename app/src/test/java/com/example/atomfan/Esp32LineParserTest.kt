package com.example.atomfan

import com.example.atomfan.ble.Esp32LineParser
import com.example.atomfan.ble.Esp32Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Esp32LineParserTest {

    private lateinit var parser: Esp32LineParser

    @Before
    fun setUp() {
        parser = Esp32LineParser()
    }

    @Test
    fun testParsePongAndOk() {
        val res1 = parser.appendChunk("PONG\n")
        assertEquals(1, res1.size)
        assertEquals(Esp32Response.Pong, res1[0])

        val res2 = parser.appendChunk("OK\n")
        assertEquals(1, res2.size)
        assertEquals(Esp32Response.Ok, res2[0])
    }

    @Test
    fun testParseError() {
        val res = parser.appendChunk("ERR Sensor timeout\n")
        assertEquals(1, res.size)
        assertTrue(res[0] is Esp32Response.Error)
        assertEquals("Sensor timeout", (res[0] as Esp32Response.Error).message)
    }

    @Test
    fun testParseLearnedNec() {
        val res = parser.appendChunk("LEARNED NEC 0x00 0x01\n")
        assertEquals(1, res.size)
        assertEquals(Esp32Response.LearnedNec(address = 0, command = 1), res[0])
    }

    @Test
    fun testParseLearnedRaw() {
        val res = parser.appendChunk("LEARNED RAW 38000 9000,4500,560,560\n")
        assertEquals(1, res.size)
        assertTrue(res[0] is Esp32Response.LearnedRaw)
        val raw = res[0] as Esp32Response.LearnedRaw
        assertEquals(38000, raw.frequency)
        assertEquals(listOf(9000, 4500, 560, 560), raw.timings)
    }

    @Test
    fun testChunkReassembly() {
        val chunk1 = parser.appendChunk("LEARNED NE")
        assertTrue(chunk1.isEmpty())

        val chunk2 = parser.appendChunk("C 0x12 0x")
        assertTrue(chunk2.isEmpty())

        val chunk3 = parser.appendChunk("34\n")
        assertEquals(1, chunk3.size)
        assertEquals(Esp32Response.LearnedNec(address = 0x12, command = 0x34), chunk3[0])
    }

    @Test
    fun testMultipleLinesInSingleChunk() {
        val res = parser.appendChunk("PONG\nOK\nERR Device Busy\n")
        assertEquals(3, res.size)
        assertEquals(Esp32Response.Pong, res[0])
        assertEquals(Esp32Response.Ok, res[1])
        assertEquals(Esp32Response.Error("Device Busy"), res[2])
    }
}

package com.example.atomfan.ble

sealed class Esp32Response {
    data object Pong : Esp32Response()
    data object Ok : Esp32Response()
    data class Error(val message: String) : Esp32Response()
    data class LearnedNec(val address: Int, val command: Int) : Esp32Response()
    data class LearnedRaw(val frequency: Int, val timings: List<Int>) : Esp32Response()
    data class Unknown(val rawLine: String) : Esp32Response()
}

class Esp32LineParser {
    private val buffer = StringBuilder()

    fun appendChunk(chunk: String): List<Esp32Response> {
        buffer.append(chunk)
        val responses = mutableListOf<Esp32Response>()

        var newlineIndex = buffer.indexOf('\n')
        while (newlineIndex != -1) {
            val line = buffer.substring(0, newlineIndex).trimEnd('\r', '\n', ' ')
            buffer.delete(0, newlineIndex + 1)
            if (line.isNotEmpty()) {
                responses.add(parseLine(line))
            }
            newlineIndex = buffer.indexOf('\n')
        }

        return responses
    }

    fun clearBuffer() {
        buffer.clear()
    }

    fun parseLine(line: String): Esp32Response {
        val trimmed = line.trim()
        return when {
            trimmed.equals("PONG", ignoreCase = true) -> Esp32Response.Pong
            trimmed.equals("OK", ignoreCase = true) -> Esp32Response.Ok
            trimmed.startsWith("ERR ", ignoreCase = true) -> {
                Esp32Response.Error(trimmed.substring(4).trim())
            }
            trimmed.startsWith("LEARNED NEC ", ignoreCase = true) -> {
                parseLearnedNec(trimmed.substring("LEARNED NEC ".length).trim(), trimmed)
            }
            trimmed.startsWith("LEARNED RAW ", ignoreCase = true) -> {
                parseLearnedRaw(trimmed.substring("LEARNED RAW ".length).trim(), trimmed)
            }
            else -> Esp32Response.Unknown(trimmed)
        }
    }

    private fun parseLearnedNec(args: String, fullLine: String): Esp32Response {
        val parts = args.split("\\s+".toRegex())
        if (parts.size >= 2) {
            val addr = parseHexOrDec(parts[0])
            val cmd = parseHexOrDec(parts[1])
            if (addr != null && cmd != null) {
                return Esp32Response.LearnedNec(addr, cmd)
            }
        }
        return Esp32Response.Unknown(fullLine)
    }

    private fun parseLearnedRaw(args: String, fullLine: String): Esp32Response {
        val parts = args.split("\\s+".toRegex())
        if (parts.size >= 2) {
            val freq = parts[0].toIntOrNull()
            val timingsStr = parts[1]
            val timings = timingsStr.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (freq != null) {
                return Esp32Response.LearnedRaw(freq, timings)
            }
        }
        return Esp32Response.Unknown(fullLine)
    }

    private fun parseHexOrDec(str: String): Int? {
        val clean = str.trim()
        return if (clean.startsWith("0x", ignoreCase = true)) {
            clean.substring(2).toIntOrNull(16)
        } else {
            clean.toIntOrNull(16) ?: clean.toIntOrNull(10)
        }
    }
}

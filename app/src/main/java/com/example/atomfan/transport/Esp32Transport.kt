package com.example.atomfan.transport

import com.example.atomfan.ble.Esp32BleManager
import com.example.atomfan.model.IrCode

class Esp32Transport(
    private val esp32BleManager: Esp32BleManager
) : FanTransport {

    override suspend fun send(code: IrCode): Result<Unit> {
        if (!esp32BleManager.isConnected.value) {
            return Result.failure(IllegalStateException("ESP32 Bridge device is not connected."))
        }

        return try {
            if (code.protocol.equals("NEC", ignoreCase = true)) {
                esp32BleManager.sendNec(code.address, code.command)
            } else if (code.rawTimings.isNotEmpty()) {
                esp32BleManager.sendRaw(code.frequency, code.rawTimings)
            } else {
                return Result.failure(IllegalArgumentException("Invalid IR code for ESP32 transport."))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.atomfan.transport

import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.model.IrCode

class FanBleTransport(
    private val bleManager: FanBleManager,
    private val isExperimentalWriteEnabled: () -> Boolean
) : FanTransport {

    override suspend fun send(code: IrCode): Result<Unit> {
        if (!bleManager.isConnected.value) {
            return Result.failure(IllegalStateException("Fan BLE device is not connected."))
        }
        if (!isExperimentalWriteEnabled()) {
            return Result.failure(IllegalStateException("Experimental BLE Write is disabled in settings."))
        }

        val hexPayload = "%02X%02X".format(code.address, code.command)
        return try {
            bleManager.writeExperimentalHexString(hexPayload)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.atomfan.transport

import com.example.atomfan.model.IrCode

interface FanTransport {
    suspend fun send(code: IrCode): Result<Unit>
}

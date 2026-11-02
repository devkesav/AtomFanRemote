package com.example.atomfan

import android.app.Application
import com.example.atomfan.ble.Esp32BleManager
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.transport.PhoneIrTransport

class AtomFanApplication : Application() {
    val repository: CodeRepository by lazy { CodeRepository(this) }
    val fanBleManager: FanBleManager by lazy { FanBleManager(this) }
    val esp32BleManager: Esp32BleManager by lazy { Esp32BleManager(this) }
    val phoneIrTransport: PhoneIrTransport by lazy { PhoneIrTransport(this) }
}

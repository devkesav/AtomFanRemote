package com.example.atomfan

import android.app.Application
import com.example.atomfan.ble.Esp32BleManager
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.data.DeviceRepository
import com.example.atomfan.database.AtomFanDatabase
import com.example.atomfan.transport.PhoneIrTransport

class AtomFanApplication : Application() {
    val database: AtomFanDatabase by lazy { AtomFanDatabase.getDatabase(this) }
    val repository: CodeRepository by lazy { CodeRepository(this) }
    val deviceRepository: DeviceRepository by lazy {
        DeviceRepository(
            fanDeviceDao = database.fanDeviceDao(),
            usageLogDao = database.usageLogDao(),
            automationRuleDao = database.automationRuleDao(),
            comfortProfileDao = database.comfortProfileDao()
        )
    }
    val fanBleManager: FanBleManager by lazy { FanBleManager(this) }
    val esp32BleManager: Esp32BleManager by lazy { Esp32BleManager(this) }
    val phoneIrTransport: PhoneIrTransport by lazy { PhoneIrTransport(this) }
}

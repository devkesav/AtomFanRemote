package com.example.atomfan.data

import com.example.atomfan.database.AutomationRule
import com.example.atomfan.database.AutomationRuleDao
import com.example.atomfan.database.ComfortProfile
import com.example.atomfan.database.ComfortProfileDao
import com.example.atomfan.database.FanDevice
import com.example.atomfan.database.FanDeviceDao
import com.example.atomfan.database.UsageLog
import com.example.atomfan.database.UsageLogDao
import kotlinx.coroutines.flow.Flow

class DeviceRepository(
    private val fanDeviceDao: FanDeviceDao,
    private val usageLogDao: UsageLogDao,
    private val automationRuleDao: AutomationRuleDao,
    private val comfortProfileDao: ComfortProfileDao
) {
    val pairedDevices: Flow<List<FanDevice>> = fanDeviceDao.getAllDevices()
    val activeDevice: Flow<FanDevice?> = fanDeviceDao.getDefaultDevice()

    suspend fun getDeviceById(id: Long): FanDevice? = fanDeviceDao.getDeviceById(id)
    suspend fun getDeviceByMac(mac: String): FanDevice? = fanDeviceDao.getDeviceByMac(mac)

    suspend fun addOrUpdateDevice(device: FanDevice): Long {
        val existing = fanDeviceDao.getDeviceByMac(device.macAddress)
        return if (existing != null) {
            val updated = device.copy(
                name = device.name,
                roomName = device.roomName,
                transportType = device.transportType
            )
            fanDeviceDao.updateDevice(updated)
            existing.id
        } else {
            val isFirst = fanDeviceDao.getDefaultDeviceSync() == null
            val newDevice = device.copy(isDefault = isFirst || device.isDefault)
            val newId = fanDeviceDao.insertDevice(newDevice)
            if (isFirst || device.isDefault) {
                fanDeviceDao.setDefaultDevice(newId)
            }
            newId
        }
    }

    suspend fun setActiveDevice(deviceId: Long) {
        fanDeviceDao.setDefaultDevice(deviceId)
    }

    suspend fun updateDeviceDetails(deviceId: Long, name: String, roomName: String) {
        val device = fanDeviceDao.getDeviceById(deviceId) ?: return
        fanDeviceDao.updateDevice(device.copy(name = name, roomName = roomName))
    }

    suspend fun updateLastConnected(deviceId: Long, timestamp: Long = System.currentTimeMillis()) {
        val device = fanDeviceDao.getDeviceById(deviceId) ?: return
        fanDeviceDao.updateDevice(device.copy(lastConnected = timestamp))
    }

    suspend fun removeDevice(deviceId: Long) {
        fanDeviceDao.deleteDeviceById(deviceId)
    }

    // Usage logs
    fun getUsageLogs(deviceId: Long): Flow<List<UsageLog>> = usageLogDao.getLogsForDevice(deviceId)
    fun getAllUsageLogs(): Flow<List<UsageLog>> = usageLogDao.getAllLogs()

    suspend fun logUsage(deviceId: Long, speed: Int, powerWatts: Double, mode: String, durationSeconds: Long): Long {
        val log = UsageLog(
            deviceId = deviceId,
            speed = speed,
            powerWatts = powerWatts,
            mode = mode,
            durationSeconds = durationSeconds
        )
        return usageLogDao.insertLog(log)
    }

    // Automation rules
    fun getAutomationRules(deviceId: Long): Flow<List<AutomationRule>> = automationRuleDao.getRulesForDevice(deviceId)
    suspend fun saveAutomationRule(rule: AutomationRule): Long = automationRuleDao.insertRule(rule)
    suspend fun deleteAutomationRule(ruleId: Long) = automationRuleDao.deleteRuleById(ruleId)

    // Comfort profiles
    fun getComfortProfiles(): Flow<List<ComfortProfile>> = comfortProfileDao.getAllProfiles()
    suspend fun saveComfortProfile(profile: ComfortProfile): Long = comfortProfileDao.insertProfile(profile)
    suspend fun deleteComfortProfile(profile: ComfortProfile) = comfortProfileDao.deleteProfile(profile)
}

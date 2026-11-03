package com.example.atomfan

import com.example.atomfan.data.DeviceRepository
import com.example.atomfan.database.AutomationRule
import com.example.atomfan.database.AutomationRuleDao
import com.example.atomfan.database.ComfortProfile
import com.example.atomfan.database.ComfortProfileDao
import com.example.atomfan.database.FanDevice
import com.example.atomfan.database.FanDeviceDao
import com.example.atomfan.database.UsageLog
import com.example.atomfan.database.UsageLogDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceRepositoryTest {

    private lateinit var fakeFanDeviceDao: FakeFanDeviceDao
    private lateinit var fakeUsageLogDao: FakeUsageLogDao
    private lateinit var fakeAutomationRuleDao: FakeAutomationRuleDao
    private lateinit var fakeComfortProfileDao: FakeComfortProfileDao
    private lateinit var repository: DeviceRepository

    @Before
    fun setUp() {
        fakeFanDeviceDao = FakeFanDeviceDao()
        fakeUsageLogDao = FakeUsageLogDao()
        fakeAutomationRuleDao = FakeAutomationRuleDao()
        fakeComfortProfileDao = FakeComfortProfileDao()

        repository = DeviceRepository(
            fanDeviceDao = fakeFanDeviceDao,
            usageLogDao = fakeUsageLogDao,
            automationRuleDao = fakeAutomationRuleDao,
            comfortProfileDao = fakeComfortProfileDao
        )
    }

    @Test
    fun testAddAndGetDevice() = runTest {
        val device = FanDevice(
            name = "Living Room Fan",
            roomName = "Living Room",
            macAddress = "AA:BB:CC:DD:EE:FF",
            transportType = "FAN_BLE"
        )

        val id = repository.addOrUpdateDevice(device)
        assertTrue(id > 0)

        val paired = repository.pairedDevices.first()
        assertEquals(1, paired.size)
        assertEquals("Living Room Fan", paired[0].name)
        assertTrue(paired[0].isDefault)

        val active = repository.activeDevice.first()
        assertNotNull(active)
        assertEquals(id, active?.id)
    }

    @Test
    fun testUpdateDeviceDetails() = runTest {
        val id = repository.addOrUpdateDevice(
            FanDevice(name = "Old Name", roomName = "Old Room", macAddress = "11:22:33:44:55:66", transportType = "FAN_BLE")
        )

        repository.updateDeviceDetails(id, "New Name", "Master Bedroom")
        val updated = repository.getDeviceById(id)

        assertNotNull(updated)
        assertEquals("New Name", updated?.name)
        assertEquals("Master Bedroom", updated?.roomName)
    }

    @Test
    fun testMultipleDevicesAndSetActive() = runTest {
        val id1 = repository.addOrUpdateDevice(
            FanDevice(name = "Fan 1", roomName = "Room 1", macAddress = "AA:11", transportType = "FAN_BLE")
        )
        val id2 = repository.addOrUpdateDevice(
            FanDevice(name = "Fan 2", roomName = "Room 2", macAddress = "BB:22", transportType = "ESP32_BRIDGE")
        )

        assertEquals(2, repository.pairedDevices.first().size)
        assertEquals(id1, repository.activeDevice.first()?.id)

        repository.setActiveDevice(id2)
        assertEquals(id2, repository.activeDevice.first()?.id)
    }

    @Test
    fun testRemoveDevice() = runTest {
        val id = repository.addOrUpdateDevice(
            FanDevice(name = "Temp Fan", roomName = "Balcony", macAddress = "99:88", transportType = "FAN_BLE")
        )

        assertEquals(1, repository.pairedDevices.first().size)

        repository.removeDevice(id)
        assertEquals(0, repository.pairedDevices.first().size)
    }

    @Test
    fun testRemoveActiveDevicePromotesNextDevice() = runTest {
        val id1 = repository.addOrUpdateDevice(
            FanDevice(name = "Fan 1", roomName = "Room 1", macAddress = "AA:11", transportType = "FAN_BLE", isDefault = true)
        )
        val id2 = repository.addOrUpdateDevice(
            FanDevice(name = "Fan 2", roomName = "Room 2", macAddress = "BB:22", transportType = "FAN_BLE", isDefault = false)
        )

        assertEquals(id1, repository.activeDevice.first()?.id)

        repository.removeDevice(id1)
        val paired = repository.pairedDevices.first()
        assertEquals(1, paired.size)
        assertEquals(id2, repository.activeDevice.first()?.id)
    }

    @Test
    fun testLogUsage() = runTest {
        val id = repository.logUsage(
            deviceId = 1L,
            speed = 3,
            powerWatts = 16.0,
            mode = "FAN_BLE",
            durationSeconds = 300
        )

        assertTrue(id > 0)
        val logs = repository.getUsageLogs(1L).first()
        assertEquals(1, logs.size)
        assertEquals(3, logs[0].speed)
        assertEquals(16.0, logs[0].powerWatts, 0.01)
    }
}

class FakeFanDeviceDao : FanDeviceDao {
    private val devices = MutableStateFlow<List<FanDevice>>(emptyList())
    private var nextId = 1L

    override fun getAllDevices(): Flow<List<FanDevice>> = devices

    override suspend fun getAllDevicesList(): List<FanDevice> = devices.value

    override suspend fun getDeviceById(id: Long): FanDevice? {
        return devices.value.firstOrNull { it.id == id }
    }

    override suspend fun getDeviceByMac(macAddress: String): FanDevice? {
        return devices.value.firstOrNull { it.macAddress.equals(macAddress, ignoreCase = true) }
    }

    override fun getDefaultDevice(): Flow<FanDevice?> {
        return devices.map { list -> list.firstOrNull { it.isDefault } }
    }

    override suspend fun getDefaultDeviceSync(): FanDevice? {
        return devices.value.firstOrNull { it.isDefault }
    }

    override suspend fun insertDevice(device: FanDevice): Long {
        val id = if (device.id == 0L) nextId++ else device.id
        val newDev = device.copy(id = id)
        devices.value = devices.value.filterNot { it.id == id } + newDev
        return id
    }

    override suspend fun updateDevice(device: FanDevice) {
        devices.value = devices.value.map { if (it.id == device.id) device else it }
    }

    override suspend fun deleteDevice(device: FanDevice) {
        devices.value = devices.value.filterNot { it.id == device.id }
    }

    override suspend fun deleteDeviceById(id: Long) {
        devices.value = devices.value.filterNot { it.id == id }
    }

    override suspend fun setDefaultDevice(deviceId: Long) {
        devices.value = devices.value.map { dev ->
            dev.copy(isDefault = dev.id == deviceId)
        }
    }
}

class FakeUsageLogDao : UsageLogDao {
    private val logs = MutableStateFlow<List<UsageLog>>(emptyList())
    private var nextId = 1L

    override fun getLogsForDevice(deviceId: Long): Flow<List<UsageLog>> {
        return logs.map { list -> list.filter { it.deviceId == deviceId } }
    }

    override fun getAllLogs(): Flow<List<UsageLog>> = logs

    override suspend fun insertLog(log: UsageLog): Long {
        val id = if (log.id == 0L) nextId++ else log.id
        val newLog = log.copy(id = id)
        logs.value = logs.value + newLog
        return id
    }

    override suspend fun clearLogsForDevice(deviceId: Long) {
        logs.value = logs.value.filterNot { it.deviceId == deviceId }
    }
}

class FakeAutomationRuleDao : AutomationRuleDao {
    private val rules = MutableStateFlow<List<AutomationRule>>(emptyList())
    private var nextId = 1L

    override fun getRulesForDevice(deviceId: Long): Flow<List<AutomationRule>> {
        return rules.map { list -> list.filter { it.deviceId == deviceId } }
    }

    override fun getAllRules(): Flow<List<AutomationRule>> = rules

    override suspend fun insertRule(rule: AutomationRule): Long {
        val id = if (rule.id == 0L) nextId++ else rule.id
        val newRule = rule.copy(id = id)
        rules.value = rules.value + newRule
        return id
    }

    override suspend fun updateRule(rule: AutomationRule) {
        rules.value = rules.value.map { if (it.id == rule.id) rule else it }
    }

    override suspend fun deleteRule(rule: AutomationRule) {
        rules.value = rules.value.filterNot { it.id == rule.id }
    }

    override suspend fun deleteRuleById(id: Long) {
        rules.value = rules.value.filterNot { it.id == id }
    }
}

class FakeComfortProfileDao : ComfortProfileDao {
    private val profiles = MutableStateFlow<List<ComfortProfile>>(emptyList())
    private var nextId = 1L

    override fun getAllProfiles(): Flow<List<ComfortProfile>> = profiles

    override suspend fun getProfileById(id: Long): ComfortProfile? {
        return profiles.value.firstOrNull { it.id == id }
    }

    override suspend fun insertProfile(profile: ComfortProfile): Long {
        val id = if (profile.id == 0L) nextId++ else profile.id
        val newProf = profile.copy(id = id)
        profiles.value = profiles.value + newProf
        return id
    }

    override suspend fun updateProfile(profile: ComfortProfile) {
        profiles.value = profiles.value.map { if (it.id == profile.id) profile else it }
    }

    override suspend fun deleteProfile(profile: ComfortProfile) {
        profiles.value = profiles.value.filterNot { it.id == profile.id }
    }
}

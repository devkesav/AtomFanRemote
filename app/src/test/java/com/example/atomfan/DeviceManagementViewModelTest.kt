package com.example.atomfan

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.atomfan.ble.Esp32BleManager
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.data.DeviceRepository
import com.example.atomfan.database.FanDevice
import com.example.atomfan.model.TransportMode
import com.example.atomfan.viewmodel.DeviceManagementViewModel
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceManagementViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var application: Application
    private lateinit var repository: CodeRepository
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var fanBleManager: FanBleManager
    private lateinit var esp32BleManager: Esp32BleManager
    private lateinit var viewModel: DeviceManagementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = Application()

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tmpFolder.root, "test_settings.preferences_pb") }
        )

        repository = CodeRepository(application, customDataStore = testDataStore)
        deviceRepository = DeviceRepository(
            fanDeviceDao = FakeFanDeviceDao(),
            usageLogDao = FakeUsageLogDao(),
            automationRuleDao = FakeAutomationRuleDao(),
            comfortProfileDao = FakeComfortProfileDao()
        )
        fanBleManager = FanBleManager(application)
        esp32BleManager = Esp32BleManager(application)

        viewModel = DeviceManagementViewModel(
            application = application,
            repository = repository,
            deviceRepository = deviceRepository,
            fanBleManager = fanBleManager,
            esp32BleManager = esp32BleManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertTrue(state.pairedDevices.isEmpty())
        assertNull(state.activeDevice)
    }

    @Test
    fun testSaveNewDevice() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.saveNewDevice(
            name = "Living Room Fan",
            roomName = "Living Room",
            mac = "AA:BB:CC:DD:EE:FF",
            transportType = TransportMode.FAN_BLE
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.pairedDevices.size)
        assertEquals("Living Room Fan", state.pairedDevices[0].name)
        assertNotNull(state.activeDevice)
        assertEquals("AA:BB:CC:DD:EE:FF", state.activeDevice?.macAddress)
    }

    @Test
    fun testEditDeviceDetails() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.saveNewDevice(
            name = "Fan 1",
            roomName = "Room 1",
            mac = "11:22:33:44:55:66",
            transportType = TransportMode.FAN_BLE
        )
        advanceUntilIdle()

        val dev = viewModel.uiState.value.pairedDevices[0]
        viewModel.saveDeviceDetails(dev.id, "Renamed Fan", "Master Bedroom")
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals("Renamed Fan", updatedState.pairedDevices[0].name)
        assertEquals("Master Bedroom", updatedState.pairedDevices[0].roomName)
    }

    @Test
    fun testDialogStateTransitions() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.openAddDialogForScanned("AA:BB", "Scanned Fan", TransportMode.FAN_BLE)
        assertTrue(viewModel.uiState.value.showAddDialog)

        viewModel.dismissAddDialog()
        assertFalse(viewModel.uiState.value.showAddDialog)

        val dev = FanDevice(id = 10L, name = "Test", roomName = "Room", macAddress = "00:11", transportType = "FAN_BLE")
        viewModel.openEditDialog(dev)
        assertEquals(dev, viewModel.uiState.value.editingDevice)

        viewModel.dismissEditDialog()
        assertNull(viewModel.uiState.value.editingDevice)

        viewModel.openDeleteConfirm(dev)
        assertEquals(dev, viewModel.uiState.value.deviceToDelete)

        viewModel.dismissDeleteConfirm()
        assertNull(viewModel.uiState.value.deviceToDelete)
    }
}

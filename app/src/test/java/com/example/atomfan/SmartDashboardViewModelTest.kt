package com.example.atomfan

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.atomfan.ble.Esp32BleManager
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.data.DeviceRepository
import com.example.atomfan.transport.PhoneIrTransport
import com.example.atomfan.model.TransportMode
import com.example.atomfan.viewmodel.FanPreset
import com.example.atomfan.viewmodel.SmartDashboardViewModel
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
class SmartDashboardViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var application: Application
    private lateinit var repository: CodeRepository
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var fanBleManager: FanBleManager
    private lateinit var esp32BleManager: Esp32BleManager
    private lateinit var phoneIrTransport: PhoneIrTransport
    private lateinit var viewModel: SmartDashboardViewModel

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
        phoneIrTransport = PhoneIrTransport(application)

        viewModel = SmartDashboardViewModel(
            application = application,
            repository = repository,
            deviceRepository = deviceRepository,
            fanBleManager = fanBleManager,
            esp32BleManager = esp32BleManager,
            phoneIrTransport = phoneIrTransport
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertFalse(state.fanState.powerOn)
        assertEquals(0.0, state.estimatedPowerWatts, 0.01)
    }

    @Test
    fun testTogglePower() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.togglePower()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.fanState.powerOn)
        assertTrue(state.estimatedPowerWatts > 0)
    }

    @Test
    fun testSetSpeed() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.setSpeed(4)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.fanState.powerOn)
        assertEquals(4, state.fanState.speed)
        assertEquals(24.0, state.estimatedPowerWatts, 0.01)
    }

    @Test
    fun testApplyPresets() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.applyPreset(FanPreset.ECO)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertTrue(state.fanState.powerOn)
        assertEquals(2, state.fanState.speed)
        assertEquals(FanPreset.ECO, state.activePreset)

        viewModel.applyPreset(FanPreset.TURBO)
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertTrue(state.fanState.powerOn)
        assertTrue(state.fanState.boost)
        assertEquals(FanPreset.TURBO, state.activePreset)
    }

    @Test
    fun testToggleModes() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.toggleBoost()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.fanState.boost)

        viewModel.toggleSleep()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.fanState.sleep)

        viewModel.toggleLed()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.fanState.led)
    }

    @Test
    fun testSetTimer() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.setTimer(4)
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.fanState.timer)
    }

    @Test
    fun testSetTransportMode() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.setTransportMode(TransportMode.ESP32_BRIDGE)
        advanceUntilIdle()
        assertEquals(TransportMode.ESP32_BRIDGE, viewModel.uiState.value.activeMode)
    }

    @Test
    fun testSetRepeatCount() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.setRepeatCount(2)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.repeatCount)
    }

    @Test
    fun testNaturalBreezeAndBatteryFlags() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        // Verify protocol constraints: Atomberg hardware does not support natural breeze opcode
        assertFalse(state.isNaturalBreezeSupported)
        // Verify AC line powered ceiling fan does not expose battery level
        assertNull(state.batteryLevel)
        assertTrue(state.batteryStatus.contains("AC", ignoreCase = true))
    }

    @Test
    fun testRefreshState() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.refreshState()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.lastSyncTimestamp)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }
}

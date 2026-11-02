package com.example.atomfan

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.atomfan.ble.Esp32BleManager
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.model.RemoteButton
import com.example.atomfan.model.TransportMode
import com.example.atomfan.transport.PhoneIrTransport
import com.example.atomfan.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var application: Application
    private lateinit var repository: CodeRepository
    private lateinit var fanBleManager: FanBleManager
    private lateinit var esp32BleManager: Esp32BleManager
    private lateinit var phoneIrTransport: PhoneIrTransport
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = Application()

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_settings.preferences_pb") }
        )

        repository = CodeRepository(application, customDataStore = testDataStore)
        fanBleManager = FanBleManager(application)
        esp32BleManager = Esp32BleManager(application)
        phoneIrTransport = PhoneIrTransport(application)

        viewModel = MainViewModel(
            application = application,
            repository = repository,
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
        assertNotNull(state.transportStatusMessage)
        assertEquals(1, state.repeatCount)
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
    fun testSendButtonCodeExecution() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.setTransportMode(TransportMode.ESP32_BRIDGE)
        advanceUntilIdle()
        viewModel.sendButtonCode(RemoteButton.POWER)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value)
    }
}

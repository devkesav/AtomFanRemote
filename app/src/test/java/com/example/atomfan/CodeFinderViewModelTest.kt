package com.example.atomfan

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.atomfan.ble.Esp32BleManager
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.transport.Esp32Transport
import com.example.atomfan.transport.PhoneIrTransport
import com.example.atomfan.viewmodel.CodeFinderViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class CodeFinderViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var application: Application
    private lateinit var repository: CodeRepository
    private lateinit var viewModel: CodeFinderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = Application()

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_finder_settings.preferences_pb") }
        )

        repository = CodeRepository(application, customDataStore = testDataStore)
        val fanBleManager = FanBleManager(application)
        val phoneIrTransport = PhoneIrTransport(application)
        val esp32Transport = Esp32Transport(Esp32BleManager(application))

        viewModel = CodeFinderViewModel(
            application = application,
            fanBleManager = fanBleManager,
            phoneIrTransport = phoneIrTransport,
            esp32Transport = esp32Transport,
            repository = repository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialFinderState() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("0", state.addressStartText)
        assertEquals("0", state.commandStartText)
        assertEquals("255", state.commandEndText)
        assertFalse(state.isRunning)
    }

    @Test
    fun testUpdateFinderParameters() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.updateAddressStart("10")
        viewModel.updateCommandStart("0")
        viewModel.updateCommandEnd("10")
        viewModel.updateIntervalMs(100L)

        val state = viewModel.uiState.value
        assertEquals("10", state.addressStartText)
        assertEquals("10", state.commandEndText)
        assertEquals(100L, state.intervalMs)
    }

    @Test
    fun testStopSweep() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.stopSweep()
        assertFalse(viewModel.uiState.value.isRunning)
    }
}

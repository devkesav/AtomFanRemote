package com.example.atomfan

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.atomfan.ble.Esp32BleManager
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.model.RemoteButton
import com.example.atomfan.transport.Esp32Transport
import com.example.atomfan.transport.FanBleTransport
import com.example.atomfan.transport.PhoneIrTransport
import com.example.atomfan.viewmodel.CodeEditorViewModel
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class CodeEditorViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var application: Application
    private lateinit var repository: CodeRepository
    private lateinit var viewModel: CodeEditorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = Application()

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_editor_settings.preferences_pb") }
        )

        repository = CodeRepository(application, customDataStore = testDataStore)
        val phoneIrTransport = PhoneIrTransport(application)
        val esp32Transport = Esp32Transport(Esp32BleManager(application))
        val fanBleTransport = FanBleTransport(FanBleManager(application)) { false }

        viewModel = CodeEditorViewModel(
            application = application,
            repository = repository,
            phoneIrTransport = phoneIrTransport,
            esp32Transport = esp32Transport,
            fanBleTransport = fanBleTransport
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialEditorState() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(RemoteButton.POWER, state.selectedButton)
        assertEquals("NEC", state.protocol)
    }

    @Test
    fun testSelectButtonAndFormUpdate() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.selectButton(RemoteButton.SPEED_3)
        advanceUntilIdle()
        assertEquals(RemoteButton.SPEED_3, viewModel.uiState.value.selectedButton)
    }

    @Test
    fun testBuildAndSaveIrCode() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.selectButton(RemoteButton.BOOST)
        advanceUntilIdle()
        viewModel.updateProtocol("NEC")
        viewModel.updateAddress("0")
        viewModel.updateCommand("128")
        viewModel.updateFrequency("38000")
        viewModel.updateComment("Boost code")

        val code = viewModel.buildCurrentIrCode()
        assertEquals(RemoteButton.BOOST, code.button)
        assertEquals(128, code.command)
        assertEquals(38000, code.frequency)

        viewModel.saveCode()
        advanceUntilIdle()

        val saved = repository.getIrCode(RemoteButton.BOOST)
        assertEquals(128, saved.command)
        assertEquals("Boost code", saved.comment)
    }
}

package com.example.atomfan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.atomfan.transport.Esp32Transport
import com.example.atomfan.transport.FanBleTransport
import com.example.atomfan.ui.BleDashboardScreen
import com.example.atomfan.ui.CodeEditorScreen
import com.example.atomfan.ui.CodeFinderScreen
import com.example.atomfan.ui.Esp32LearnScreen
import com.example.atomfan.ui.MainScreen
import com.example.atomfan.ui.theme.AtomFanTheme
import com.example.atomfan.viewmodel.CodeEditorViewModel
import com.example.atomfan.viewmodel.CodeFinderViewModel
import com.example.atomfan.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

enum class AppDestination {
    MAIN_REMOTE,
    BLE_DASHBOARD,
    ESP32_LEARN,
    CODE_EDITOR,
    CODE_FINDER
}

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        val app = application as AtomFanApplication
        MainViewModel.Factory(
            application = app,
            repository = app.repository,
            fanBleManager = app.fanBleManager,
            esp32BleManager = app.esp32BleManager,
            phoneIrTransport = app.phoneIrTransport
        )
    }

    private val codeEditorViewModel: CodeEditorViewModel by viewModels {
        val app = application as AtomFanApplication
        CodeEditorViewModel.Factory(
            application = app,
            repository = app.repository,
            phoneIrTransport = app.phoneIrTransport,
            esp32Transport = Esp32Transport(app.esp32BleManager),
            fanBleTransport = FanBleTransport(app.fanBleManager) {
                runBlocking { app.repository.experimentalBleWriteFlow.first() }
            }
        )
    }

    private val codeFinderViewModel: CodeFinderViewModel by viewModels {
        val app = application as AtomFanApplication
        CodeFinderViewModel.Factory(
            application = app,
            fanBleManager = app.fanBleManager,
            phoneIrTransport = app.phoneIrTransport,
            esp32Transport = Esp32Transport(app.esp32BleManager),
            repository = app.repository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AtomFanTheme {
                val app = application as AtomFanApplication
                var currentDestination by rememberSaveable { mutableStateOf(AppDestination.MAIN_REMOTE) }

                when (currentDestination) {
                    AppDestination.MAIN_REMOTE -> {
                        MainScreen(
                            viewModel = mainViewModel,
                            onNavigateToBleDashboard = { currentDestination = AppDestination.BLE_DASHBOARD },
                            onNavigateToEsp32Learn = { currentDestination = AppDestination.ESP32_LEARN },
                            onNavigateToCodeEditor = { currentDestination = AppDestination.CODE_EDITOR },
                            onNavigateToCodeFinder = { currentDestination = AppDestination.CODE_FINDER }
                        )
                    }
                    AppDestination.BLE_DASHBOARD -> {
                        BleDashboardScreen(
                            fanBleManager = app.fanBleManager,
                            onBack = { currentDestination = AppDestination.MAIN_REMOTE }
                        )
                    }
                    AppDestination.ESP32_LEARN -> {
                        Esp32LearnScreen(
                            esp32BleManager = app.esp32BleManager,
                            repository = app.repository,
                            onBack = { currentDestination = AppDestination.MAIN_REMOTE }
                        )
                    }
                    AppDestination.CODE_EDITOR -> {
                        CodeEditorScreen(
                            viewModel = codeEditorViewModel,
                            onBack = { currentDestination = AppDestination.MAIN_REMOTE }
                        )
                    }
                    AppDestination.CODE_FINDER -> {
                        CodeFinderScreen(
                            viewModel = codeFinderViewModel,
                            onBack = { currentDestination = AppDestination.MAIN_REMOTE }
                        )
                    }
                }
            }
        }
    }
}

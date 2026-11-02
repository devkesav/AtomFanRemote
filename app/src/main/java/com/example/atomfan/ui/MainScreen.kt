package com.example.atomfan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.atomfan.model.FanState
import com.example.atomfan.model.TransportMode
import com.example.atomfan.ui.theme.AtomFanTheme
import com.example.atomfan.viewmodel.MainUiState
import com.example.atomfan.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToBleDashboard: () -> Unit = {},
    onNavigateToEsp32Learn: () -> Unit = {},
    onNavigateToCodeEditor: () -> Unit = {},
    onNavigateToCodeFinder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScreenContent(
        uiState = uiState,
        onModeSelected = { viewModel.setTransportMode(it) },
        onButtonClick = { viewModel.sendButtonCode(it) },
        onRepeatCountChanged = { viewModel.setRepeatCount(it) },
        onExperimentalBleWriteChanged = { viewModel.setExperimentalBleWrite(it) },
        onNavigateToBleDashboard = onNavigateToBleDashboard,
        onNavigateToEsp32Learn = onNavigateToEsp32Learn,
        onNavigateToCodeEditor = onNavigateToCodeEditor,
        onNavigateToCodeFinder = onNavigateToCodeFinder,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onModeSelected: (TransportMode) -> Unit,
    onButtonClick: (com.example.atomfan.model.RemoteButton) -> Unit,
    onRepeatCountChanged: (Int) -> Unit,
    onExperimentalBleWriteChanged: (Boolean) -> Unit,
    onNavigateToBleDashboard: () -> Unit,
    onNavigateToEsp32Learn: () -> Unit,
    onNavigateToCodeEditor: () -> Unit,
    onNavigateToCodeFinder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AtomFan Remote",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToBleDashboard) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "BLE Dashboard"
                        )
                    }
                    IconButton(onClick = onNavigateToEsp32Learn) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = "ESP32 Learn"
                        )
                    }
                    IconButton(onClick = onNavigateToCodeEditor) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Code Editor"
                        )
                    }
                    IconButton(onClick = onNavigateToCodeFinder) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Code Finder"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        PermissionHandler {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Segmented Mode Selector
                ModeSegmentedSelector(
                    selectedMode = uiState.activeMode,
                    hasIrEmitter = uiState.hasIrEmitter,
                    onModeSelected = onModeSelected
                )

                // Status Bar Card
                StatusBarCard(
                    transportStatusMessage = uiState.transportStatusMessage,
                    fanState = uiState.fanState,
                    verificationResult = uiState.verificationResult,
                    isFanBleConnected = uiState.isFanBleConnected
                )

                // Remote Control Buttons Grid
                RemoteButtonGrid(
                    onButtonClick = onButtonClick
                )

                // Quick Settings (Repeat count & Experimental BLE)
                QuickSettingsRow(
                    repeatCount = uiState.repeatCount,
                    onRepeatCountChanged = onRepeatCountChanged,
                    isExperimentalBleWrite = uiState.isExperimentalBleWrite,
                    onExperimentalBleWriteChanged = onExperimentalBleWriteChanged,
                    activeMode = uiState.activeMode
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    AtomFanTheme {
        MainScreenContent(
            uiState = MainUiState(
                activeMode = TransportMode.PHONE_IR,
                transportStatusMessage = "Phone IR Ready",
                verificationResult = "fan responded",
                fanState = FanState(powerOn = true, speed = 3, led = true),
                hasIrEmitter = true
            ),
            onModeSelected = {},
            onButtonClick = {},
            onRepeatCountChanged = {},
            onExperimentalBleWriteChanged = {},
            onNavigateToBleDashboard = {},
            onNavigateToEsp32Learn = {},
            onNavigateToCodeEditor = {},
            onNavigateToCodeFinder = {}
        )
    }
}

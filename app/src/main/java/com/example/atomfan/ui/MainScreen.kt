package com.example.atomfan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.atomfan.model.FanState
import com.example.atomfan.model.RemoteButton
import com.example.atomfan.model.TransportMode
import com.example.atomfan.ui.theme.AtomFanTheme
import com.example.atomfan.viewmodel.MainUiState
import com.example.atomfan.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSmartDashboard: () -> Unit = {},
    onNavigateToDeviceManagement: () -> Unit = {},
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
        onButtonClick = { viewModel.onRemoteButtonClicked(it) },
        onRepeatCountChanged = { viewModel.setRepeatCount(it) },
        onExperimentalBleWriteChanged = { viewModel.setExperimentalBleWrite(it) },
        onDismissTimerDialog = { viewModel.setTimerDialogVisible(false) },
        onConfirmTimerDialog = { mins ->
            viewModel.setTimerMinutes(mins)
            viewModel.setTimerDialogVisible(false)
        },
        onDismissSleepDialog = { viewModel.setSleepDialogVisible(false) },
        onConfirmSleepDialog = { start, end, days ->
            viewModel.setSleepSchedule(start, end, days)
            viewModel.setSleepDialogVisible(false)
        },
        onNavigateToSmartDashboard = onNavigateToSmartDashboard,
        onNavigateToDeviceManagement = onNavigateToDeviceManagement,
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
    onButtonClick: (RemoteButton) -> Unit,
    onRepeatCountChanged: (Int) -> Unit,
    onExperimentalBleWriteChanged: (Boolean) -> Unit,
    onDismissTimerDialog: () -> Unit,
    onConfirmTimerDialog: (Int) -> Unit,
    onDismissSleepDialog: () -> Unit,
    onConfirmSleepDialog: (String, String, Set<String>) -> Unit,
    onNavigateToSmartDashboard: () -> Unit,
    onNavigateToDeviceManagement: () -> Unit,
    onNavigateToBleDashboard: () -> Unit,
    onNavigateToEsp32Learn: () -> Unit,
    onNavigateToCodeEditor: () -> Unit,
    onNavigateToCodeFinder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.activeMode, uiState.transportStatusMessage) {
        if (uiState.activeMode == TransportMode.PHONE_IR && uiState.transportStatusMessage.contains("Phone IR Ready", ignoreCase = true)) {
            snackbarHostState.showSnackbar("Phone IR Ready")
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateToSmartDashboard) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Smart Dashboard"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Atomberg Remote",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Transport Mode", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                            onClick = {},
                            enabled = false
                        )
                        DropdownMenuItem(
                            text = { Text("Phone IR ${if (!uiState.hasIrEmitter) "(Not Available)" else ""}") },
                            onClick = {
                                if (uiState.hasIrEmitter) {
                                    onModeSelected(TransportMode.PHONE_IR)
                                    menuExpanded = false
                                }
                            },
                            enabled = uiState.hasIrEmitter,
                            leadingIcon = {
                                if (uiState.activeMode == TransportMode.PHONE_IR) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(imageVector = Icons.Default.Radio, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fan BLE") },
                            onClick = {
                                onModeSelected(TransportMode.FAN_BLE)
                                menuExpanded = false
                            },
                            leadingIcon = {
                                if (uiState.activeMode == TransportMode.FAN_BLE) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(imageVector = Icons.Default.Bluetooth, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ESP32 Bridge") },
                            onClick = {
                                onModeSelected(TransportMode.ESP32_BRIDGE)
                                menuExpanded = false
                            },
                            leadingIcon = {
                                if (uiState.activeMode == TransportMode.ESP32_BRIDGE) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(imageVector = Icons.Default.Router, contentDescription = null)
                                }
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("IR Repeat Count: ${uiState.repeatCount}x") },
                            onClick = {
                                onRepeatCountChanged(if (uiState.repeatCount == 1) 2 else 1)
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Speed, contentDescription = null)
                            }
                        )
                        if (uiState.activeMode == TransportMode.FAN_BLE) {
                            DropdownMenuItem(
                                text = { Text("Experimental BLE: ${if (uiState.isExperimentalBleWrite) "ON" else "OFF"}") },
                                onClick = {
                                    onExperimentalBleWriteChanged(!uiState.isExperimentalBleWrite)
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = null)
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Smart Fan Dashboard") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToSmartDashboard()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Device Management") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToDeviceManagement()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Devices,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fan BLE Dashboard") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToBleDashboard()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ESP32 IR Learn Screen") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToEsp32Learn()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Code Editor") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToCodeEditor()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Code Finder") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToCodeFinder()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null
                                )
                            }
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
                // Sleek Atomberg Remote Widget (Telemetry included inside)
                AtombergRemoteWidget(
                    fanState = uiState.fanState,
                    onButtonClick = onButtonClick
                )

                // Real-time Energy Statistics Section
                EnergyStatisticsCard(
                    fanState = uiState.fanState
                )
            }
        }

        // Dialogs
        if (uiState.showTimerDialog) {
            TimerDialog(
                currentMinutes = uiState.timerMinutes,
                onDismiss = onDismissTimerDialog,
                onConfirm = { mins ->
                    onConfirmTimerDialog(mins)
                    val timeText = if (mins == 0) "OFF" else if (mins >= 60) "${mins / 60} hrs" else "$mins mins"
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Timer set to $timeText")
                    }
                }
            )
        }

        if (uiState.showSleepDialog) {
            SleepDialog(
                startTime = uiState.sleepStartTime,
                endTime = uiState.sleepEndTime,
                selectedDays = uiState.sleepDays,
                onDismiss = onDismissSleepDialog,
                onConfirm = { start, end, days ->
                    onConfirmSleepDialog(start, end, days)
                    val daysText = if (days.size == 7) "Every day" else days.sorted().joinToString(", ")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Sleep schedule set: $start - $end ($daysText)")
                    }
                }
            )
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
            onDismissTimerDialog = {},
            onConfirmTimerDialog = {},
            onDismissSleepDialog = {},
            onConfirmSleepDialog = { _, _, _ -> },
            onNavigateToSmartDashboard = {},
            onNavigateToDeviceManagement = {},
            onNavigateToBleDashboard = {},
            onNavigateToEsp32Learn = {},
            onNavigateToCodeEditor = {},
            onNavigateToCodeFinder = {}
        )
    }
}

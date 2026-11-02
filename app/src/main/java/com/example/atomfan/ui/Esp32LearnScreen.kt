package com.example.atomfan.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.atomfan.ble.Esp32BleManager
import com.example.atomfan.ble.Esp32Response
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.model.IrCode
import com.example.atomfan.model.RemoteButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Esp32LearnScreen(
    esp32BleManager: Esp32BleManager,
    repository: CodeRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val isScanning by esp32BleManager.isScanning.collectAsStateWithLifecycle()
    val isConnected by esp32BleManager.isConnected.collectAsStateWithLifecycle()
    val connectedName by esp32BleManager.connectedDeviceName.collectAsStateWithLifecycle()
    val scannedDevices by esp32BleManager.scannedDevices.collectAsStateWithLifecycle()

    var selectedButton by remember { mutableStateOf(RemoteButton.POWER) }
    var isLearning by remember { mutableStateOf(false) }
    var learnStatusMessage by remember { mutableStateOf("Idle") }
    var capturedIrCode by remember { mutableStateOf<IrCode?>(null) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    var timeoutJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        esp32BleManager.responses.collect { response ->
            when (response) {
                is Esp32Response.LearnedNec -> {
                    timeoutJob?.cancel()
                    isLearning = false
                    learnStatusMessage = "Successfully learned NEC code!"
                    capturedIrCode = IrCode(
                        button = selectedButton,
                        protocol = "NEC",
                        address = response.address,
                        command = response.command,
                        comment = "Learned via ESP32 Bridge for ${selectedButton.displayName}"
                    )
                }
                is Esp32Response.LearnedRaw -> {
                    timeoutJob?.cancel()
                    isLearning = false
                    learnStatusMessage = "Successfully learned RAW code (${response.timings.size} timings)!"
                    capturedIrCode = IrCode(
                        button = selectedButton,
                        protocol = "RAW",
                        frequency = response.frequency,
                        rawTimings = response.timings,
                        comment = "Learned RAW via ESP32 Bridge for ${selectedButton.displayName}"
                    )
                }
                is Esp32Response.Error -> {
                    timeoutJob?.cancel()
                    isLearning = false
                    learnStatusMessage = "ESP32 Error: ${response.message}"
                }
                is Esp32Response.Ok -> {}
                else -> {}
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("ESP32 IR Learn Screen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    ) { innerPadding ->
        PermissionHandler {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. ESP32 Bridge Device Scanner / Status Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "ESP32 BRIDGE (AtomFan-Bridge)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isConnected) "Connected: ${connectedName ?: "Bridge"}" else "Disconnected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (isScanning) esp32BleManager.stopScan() else esp32BleManager.startScan()
                                    }
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Stop Scan")
                                    } else {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Scan Bridge")
                                    }
                                }
                            }

                            if (isConnected) {
                                HorizontalDivider()
                                Button(
                                    onClick = { esp32BleManager.disconnect() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                                ) {
                                    Text("Disconnect Bridge")
                                }
                            }

                            val bridgeDevices = scannedDevices.filter {
                                (it.name ?: "").contains("AtomFan-Bridge", ignoreCase = true)
                            }
                            if (bridgeDevices.isNotEmpty() && !isConnected) {
                                HorizontalDivider()
                                Text("Discovered ESP32 Bridges:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                bridgeDevices.forEach { device ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(device.name ?: "AtomFan-Bridge", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                            Text(device.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Button(onClick = { esp32BleManager.connect(device) }) {
                                            Text("Connect")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Target Remote Button Selector
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "SELECT TARGET REMOTE BUTTON",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val buttons = RemoteButton.entries
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                buttons.chunked(3).forEach { rowButtons ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowButtons.forEach { btn ->
                                            FilterChip(
                                                selected = selectedButton == btn,
                                                onClick = {
                                                    selectedButton = btn
                                                    capturedIrCode = null
                                                    learnStatusMessage = "Selected ${btn.displayName}"
                                                },
                                                label = { Text(btn.displayName, style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Learn Workflow Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "IR LEARN WORKFLOW (${selectedButton.displayName})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (isLearning) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Text(
                                            text = "Point original Atomberg remote at ESP32 receiver and press button...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = learnStatusMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = learnStatusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isLearning = true
                                        learnStatusMessage = "Waiting for signal (10s timeout)..."
                                        capturedIrCode = null
                                        saveSuccessMessage = null
                                        esp32BleManager.learn()

                                        timeoutJob?.cancel()
                                        timeoutJob = scope.launch {
                                            delay(10000L)
                                            if (isLearning) {
                                                isLearning = false
                                                learnStatusMessage = "Timeout: No IR signal received within 10s."
                                                esp32BleManager.cancel()
                                            }
                                        }
                                    },
                                    enabled = isConnected && !isLearning,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Start Learning")
                                }

                                OutlinedButton(
                                    onClick = {
                                        timeoutJob?.cancel()
                                        isLearning = false
                                        learnStatusMessage = "Learning cancelled by user."
                                        esp32BleManager.cancel()
                                    },
                                    enabled = isConnected && isLearning,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                            }

                            capturedIrCode?.let { code ->
                                HorizontalDivider()
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "CAPTURED CODE DETAILS:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text("Protocol: ${code.protocol}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    if (code.protocol == "NEC") {
                                        Text("Address: 0x%04X (${code.address})".format(code.address), style = MaterialTheme.typography.bodySmall)
                                        Text("Command: 0x%04X (${code.command})".format(code.command), style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        Text("Frequency: ${code.frequency} Hz", style = MaterialTheme.typography.bodySmall)
                                        Text("Raw Timings count: ${code.rawTimings.size}", style = MaterialTheme.typography.bodySmall)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (code.protocol == "NEC") {
                                                    esp32BleManager.sendNec(code.address, code.command)
                                                } else {
                                                    esp32BleManager.sendRaw(code.frequency, code.rawTimings)
                                                }
                                                learnStatusMessage = "Test code sent to fan!"
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                        ) {
                                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Test Code")
                                        }

                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    repository.saveIrCode(selectedButton, code)
                                                    saveSuccessMessage = "Successfully saved code for ${selectedButton.displayName} to CodeRepository!"
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save Code")
                                        }
                                    }

                                    saveSuccessMessage?.let { msg ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

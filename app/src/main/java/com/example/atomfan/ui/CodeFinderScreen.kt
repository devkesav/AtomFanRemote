package com.example.atomfan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.atomfan.ui.theme.AtomFanTheme
import com.example.atomfan.viewmodel.CodeFinderUiState
import com.example.atomfan.viewmodel.CodeFinderViewModel
import com.example.atomfan.viewmodel.CodeHit

@Composable
fun CodeFinderScreen(
    viewModel: CodeFinderViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CodeFinderContent(
        uiState = uiState,
        onAddressStartChanged = { viewModel.updateAddressStart(it) },
        onAddressEndChanged = { viewModel.updateAddressEnd(it) },
        onIsFixedAddressChanged = { viewModel.updateIsFixedAddress(it) },
        onCommandStartChanged = { viewModel.updateCommandStart(it) },
        onCommandEndChanged = { viewModel.updateCommandEnd(it) },
        onIntervalChanged = { viewModel.updateIntervalMs(it) },
        onStart = { viewModel.startSweep() },
        onPause = { viewModel.pauseSweep() },
        onStop = { viewModel.stopSweep() },
        onClearHits = { viewModel.clearHits() },
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeFinderContent(
    uiState: CodeFinderUiState,
    onAddressStartChanged: (String) -> Unit,
    onAddressEndChanged: (String) -> Unit,
    onIsFixedAddressChanged: (Boolean) -> Unit,
    onCommandStartChanged: (String) -> Unit,
    onCommandEndChanged: (String) -> Unit,
    onIntervalChanged: (Long) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onClearHits: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Code Finder (Mode A & C)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // BLE Response Detection Status Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isFanBleConnected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isFanBleConnected) "Fan BLE Connected (Auto-Hit Detection Active)" else "Fan BLE Disconnected (Blind Sweep Mode)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isFanBleConnected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = if (uiState.isFanBleConnected) {
                                "Listening for state changes within 400ms after each transmission."
                            } else {
                                "Connect Fan via BLE to automatically detect successful code responses."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isFanBleConnected) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            }
                        )
                    }
                }
            }

            // Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "NEC Sweep Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Fixed address checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = uiState.isFixedAddress,
                            onCheckedChange = onIsFixedAddressChanged
                        )
                        Text("Fixed Address (Single Address)")
                    }

                    if (uiState.isFixedAddress) {
                        OutlinedTextField(
                            value = uiState.addressStartText,
                            onValueChange = onAddressStartChanged,
                            label = { Text("Address (Dec)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.addressStartText,
                                onValueChange = onAddressStartChanged,
                                label = { Text("Address Start") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.addressEndText,
                                onValueChange = onAddressEndChanged,
                                label = { Text("Address End") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.commandStartText,
                            onValueChange = onCommandStartChanged,
                            label = { Text("Command Start") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = uiState.commandEndText,
                            onValueChange = onCommandEndChanged,
                            label = { Text("Command End") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Interval slider
                    Column {
                        Text(
                            text = "Interval per code: ${uiState.intervalMs} ms",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = uiState.intervalMs.toFloat(),
                            onValueChange = { onIntervalChanged(it.toLong()) },
                            valueRange = 50f..1000f,
                            steps = 19,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Controls & Progress Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = uiState.sweepStatus,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (uiState.isRunning) {
                        val progress = if (uiState.totalCount > 0) {
                            uiState.currentIndex.toFloat() / uiState.totalCount.toFloat()
                        } else 0f

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Progress: ${uiState.currentIndex} / ${uiState.totalCount} (Addr: ${uiState.currentAddress}, Cmd: ${uiState.currentCommand})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStart,
                            enabled = !uiState.isRunning || uiState.isPaused,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(if (uiState.isPaused) "Resume" else "Start")
                        }

                        OutlinedButton(
                            onClick = onPause,
                            enabled = uiState.isRunning && !uiState.isPaused,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Pause")
                        }

                        OutlinedButton(
                            onClick = onStop,
                            enabled = uiState.isRunning || uiState.isPaused,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Stop")
                        }
                    }
                }
            }

            // Hits Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detected Hits (${uiState.hits.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.hits.isNotEmpty()) {
                    OutlinedButton(onClick = onClearHits) {
                        Text("Clear Hits")
                    }
                }
            }

            if (uiState.hits.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "No hits detected yet. Start sweep with Fan BLE connected to auto-detect responses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (hit in uiState.hits) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Column {
                                        Text(
                                            text = "HIT! Fan responded",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Address: ${hit.address} (0x%02X), Command: ${hit.command} (0x%02X)".format(hit.address, hit.command),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
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

@Preview(showBackground = true)
@Composable
fun CodeFinderScreenPreview() {
    AtomFanTheme {
        CodeFinderContent(
            uiState = CodeFinderUiState(
                isFanBleConnected = true,
                sweepStatus = "Trying Address: 0x00 (0), Command: 0x45 (69)",
                isRunning = true,
                currentIndex = 70,
                totalCount = 256,
                hits = listOf(CodeHit(address = 0, command = 69))
            ),
            onAddressStartChanged = {},
            onAddressEndChanged = {},
            onIsFixedAddressChanged = {},
            onCommandStartChanged = {},
            onCommandEndChanged = {},
            onIntervalChanged = {},
            onStart = {},
            onPause = {},
            onStop = {},
            onClearHits = {},
            onBack = {}
        )
    }
}

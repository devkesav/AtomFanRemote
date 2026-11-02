package com.example.atomfan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.atomfan.model.RemoteButton
import com.example.atomfan.ui.theme.AtomFanTheme
import com.example.atomfan.viewmodel.CodeEditorUiState
import com.example.atomfan.viewmodel.CodeEditorViewModel

@Composable
fun CodeEditorScreen(
    viewModel: CodeEditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CodeEditorContent(
        uiState = uiState,
        onSelectButton = { viewModel.selectButton(it) },
        onProtocolChanged = { viewModel.updateProtocol(it) },
        onAddressChanged = { viewModel.updateAddress(it) },
        onCommandChanged = { viewModel.updateCommand(it) },
        onFrequencyChanged = { viewModel.updateFrequency(it) },
        onRawTimingsChanged = { viewModel.updateRawTimings(it) },
        onCommentChanged = { viewModel.updateComment(it) },
        onTestCode = { viewModel.testCode() },
        onSaveCode = { viewModel.saveCode() },
        onResetToDefault = { viewModel.resetToDefault() },
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorContent(
    uiState: CodeEditorUiState,
    onSelectButton: (RemoteButton) -> Unit,
    onProtocolChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onCommandChanged: (String) -> Unit,
    onFrequencyChanged: (String) -> Unit,
    onRawTimingsChanged: (String) -> Unit,
    onCommentChanged: (String) -> Unit,
    onTestCode: () -> Unit,
    onSaveCode: () -> Unit,
    onResetToDefault: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var protocolExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("IR Code Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onResetToDefault) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset to Default"
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
            // Status feedback banner
            if (uiState.statusMessage.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Button selector chips
            Text(
                text = "Select Remote Button",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chunks = RemoteButton.entries.chunked(3)
                for (chunk in chunks) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (btn in chunk) {
                            FilterChip(
                                selected = uiState.selectedButton == btn,
                                onClick = { onSelectButton(btn) },
                                label = { Text(btn.displayName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        for (idx in 0 until (3 - chunk.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Edit Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Properties for: ${uiState.selectedButton.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Protocol Dropdown
                    ExposedDropdownMenuBox(
                        expanded = protocolExpanded,
                        onExpandedChange = { protocolExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.protocol,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Protocol") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(
                            expanded = protocolExpanded,
                            onDismissRequest = { protocolExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("NEC") },
                                onClick = {
                                    onProtocolChanged("NEC")
                                    protocolExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Raw") },
                                onClick = {
                                    onProtocolChanged("Raw")
                                    protocolExpanded = false
                                }
                            )
                        }
                    }

                    if (uiState.protocol.equals("NEC", ignoreCase = true)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.addressText,
                                onValueChange = onAddressChanged,
                                label = { Text("Address (Dec/Hex)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.commandText,
                                onValueChange = onCommandChanged,
                                label = { Text("Command (Dec/Hex)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = uiState.rawTimingsText,
                            onValueChange = onRawTimingsChanged,
                            label = { Text("Raw Timings (Comma-separated)") },
                            placeholder = { Text("9000, 4500, 560, 560...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = uiState.frequencyText,
                        onValueChange = onFrequencyChanged,
                        label = { Text("Frequency (Hz)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.commentText,
                        onValueChange = onCommentChanged,
                        label = { Text("Comment / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onTestCode,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Test Code")
                }

                Button(
                    onClick = onSaveCode,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(if (uiState.isSaving) "Saving..." else "Save Changes")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CodeEditorScreenPreview() {
    AtomFanTheme {
        CodeEditorContent(
            uiState = CodeEditorUiState(
                selectedButton = RemoteButton.POWER,
                protocol = "NEC",
                addressText = "0",
                commandText = "64",
                frequencyText = "38000",
                commentText = "Power button NEC"
            ),
            onSelectButton = {},
            onProtocolChanged = {},
            onAddressChanged = {},
            onCommandChanged = {},
            onFrequencyChanged = {},
            onRawTimingsChanged = {},
            onCommentChanged = {},
            onTestCode = {},
            onSaveCode = {},
            onResetToDefault = {},
            onBack = {}
        )
    }
}

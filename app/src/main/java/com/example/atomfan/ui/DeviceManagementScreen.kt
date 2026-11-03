package com.example.atomfan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.atomfan.database.FanDevice
import com.example.atomfan.model.TransportMode
import com.example.atomfan.ui.theme.AtomFanTheme
import com.example.atomfan.viewmodel.DeviceManagementUiState
import com.example.atomfan.viewmodel.DeviceManagementViewModel
import com.example.atomfan.viewmodel.ScannedDeviceItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceManagementScreen(
    viewModel: DeviceManagementViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DeviceManagementContent(
        uiState = uiState,
        onBack = onBack,
        onStartScan = { viewModel.startScan() },
        onStopScan = { viewModel.stopScan() },
        onConnectDevice = { viewModel.connectDevice(it) },
        onDisconnectDevice = { viewModel.disconnectDevice() },
        onSetActiveDevice = { viewModel.setActiveDevice(it) },
        onOpenAddDialogScanned = { mac, name, transport -> viewModel.openAddDialogForScanned(mac, name, transport) },
        onDismissAddDialog = { viewModel.dismissAddDialog() },
        onSaveNewDevice = { name, room, mac, transport -> viewModel.saveNewDevice(name, room, mac, transport) },
        onOpenEditDialog = { viewModel.openEditDialog(it) },
        onDismissEditDialog = { viewModel.dismissEditDialog() },
        onSaveDeviceDetails = { id, name, room -> viewModel.saveDeviceDetails(id, name, room) },
        onOpenDeleteConfirm = { viewModel.openDeleteConfirm(it) },
        onDismissDeleteConfirm = { viewModel.dismissDeleteConfirm() },
        onConfirmDeleteDevice = { viewModel.confirmDeleteDevice() },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementContent(
    uiState: DeviceManagementUiState,
    onBack: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (FanDevice) -> Unit,
    onDisconnectDevice: () -> Unit,
    onSetActiveDevice: (Long) -> Unit,
    onOpenAddDialogScanned: (String, String, TransportMode) -> Unit,
    onDismissAddDialog: () -> Unit,
    onSaveNewDevice: (String, String, String, TransportMode) -> Unit,
    onOpenEditDialog: (FanDevice) -> Unit,
    onDismissEditDialog: () -> Unit,
    onSaveDeviceDetails: (Long, String, String) -> Unit,
    onOpenDeleteConfirm: (FanDevice) -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    onConfirmDeleteDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanRotate")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "spin"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Device Management",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (uiState.isScanning) onStopScan() else onStartScan()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan BLE",
                            modifier = if (uiState.isScanning) Modifier.rotate(spinAngle) else Modifier
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
            // Active Device Banner
            ActiveDeviceBannerCard(
                activeDevice = uiState.activeDevice,
                connectedMac = uiState.connectedDeviceMac,
                onDisconnect = onDisconnectDevice
            )

            // Paired Fans Section
            PairedDevicesSection(
                pairedDevices = uiState.pairedDevices,
                activeDeviceId = uiState.activeDevice?.id,
                connectedMac = uiState.connectedDeviceMac,
                onSetActive = onSetActiveDevice,
                onConnect = onConnectDevice,
                onEdit = onOpenEditDialog,
                onDelete = onOpenDeleteConfirm
            )

            // Scanned Nearby Devices Section
            ScannedDevicesSection(
                scannedDevices = uiState.scannedDevices,
                isScanning = uiState.isScanning,
                onStartScan = onStartScan,
                onStopScan = onStopScan,
                onAddScanned = onOpenAddDialogScanned
            )
        }
    }

    // Add Device Dialog
    if (uiState.showAddDialog) {
        AddDeviceDialog(
            defaultMac = uiState.addDialogMac,
            defaultName = uiState.addDialogDefaultName,
            defaultTransport = uiState.addDialogTransport,
            onDismiss = onDismissAddDialog,
            onConfirm = onSaveNewDevice
        )
    }

    // Edit Device Details Dialog
    if (uiState.editingDevice != null) {
        EditDeviceDialog(
            device = uiState.editingDevice,
            onDismiss = onDismissEditDialog,
            onConfirm = { name, room ->
                onSaveDeviceDetails(uiState.editingDevice.id, name, room)
            }
        )
    }

    // Delete Confirmation Dialog
    if (uiState.deviceToDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirm,
            title = { Text("Remove Device") },
            text = { Text("Are you sure you want to remove '${uiState.deviceToDelete.name}' from saved devices?") },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteDevice) {
                    Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteConfirm) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ActiveDeviceBannerCard(
    activeDevice: FanDevice?,
    connectedMac: String?,
    onDisconnect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACTIVE FAN DEVICE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                val isConnected = connectedMac != null && (activeDevice == null || connectedMac.equals(activeDevice.macAddress, ignoreCase = true) || connectedMac == "ESP32_BRIDGE_CONNECTED")
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = if (isConnected) "Connected" else "Disconnected",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.errorContainer,
                        labelColor = if (isConnected) Color.White else MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeDevice != null) {
                Text(
                    text = activeDevice.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Room: ${activeDevice.roomName} • MAC: ${activeDevice.macAddress}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            } else {
                Text(
                    text = "No Fan Device Selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Scan nearby BLE devices or select a paired device below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun PairedDevicesSection(
    pairedDevices: List<FanDevice>,
    activeDeviceId: Long?,
    connectedMac: String?,
    onSetActive: (Long) -> Unit,
    onConnect: (FanDevice) -> Unit,
    onEdit: (FanDevice) -> Unit,
    onDelete: (FanDevice) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Saved Devices (${pairedDevices.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (pairedDevices.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved devices yet. Scan nearby BLE devices below to pair a fan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pairedDevices.forEach { device ->
                    val isActive = device.id == activeDeviceId
                    PairedDeviceCardItem(
                        device = device,
                        isActive = isActive,
                        connectedMac = connectedMac,
                        onSetActive = { onSetActive(device.id) },
                        onConnect = { onConnect(device) },
                        onEdit = { onEdit(device) },
                        onDelete = { onDelete(device) }
                    )
                }
            }
        }
    }
}

@Composable
fun PairedDeviceCardItem(
    device: FanDevice,
    isActive: Boolean,
    connectedMac: String?,
    onSetActive: () -> Unit,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isConnected = connectedMac != null && (connectedMac.equals(device.macAddress, ignoreCase = true) || (device.transportType == TransportMode.ESP32_BRIDGE.name && connectedMac == "ESP32_BRIDGE_CONNECTED"))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSetActive) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Active Device",
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = device.roomName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = device.transportType,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        val icon = when (device.transportType) {
                            TransportMode.FAN_BLE.name -> Icons.Default.Bluetooth
                            TransportMode.ESP32_BRIDGE.name -> Icons.Default.Router
                            else -> Icons.Default.Radio
                        }
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "MAC: ${device.macAddress} • Last Connected: ${formatTime(device.lastConnected)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Device Details")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Device", tint = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConnect,
                    enabled = !isConnected
                ) {
                    Text(if (isConnected) "Connected" else "Connect")
                }
            }
        }
    }
}

@Composable
fun ScannedDevicesSection(
    scannedDevices: List<ScannedDeviceItem>,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onAddScanned: (String, String, TransportMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nearby BLE Devices (${scannedDevices.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(onClick = {
                if (isScanning) onStopScan() else onStartScan()
            }) {
                Text(if (isScanning) "Stop Scan" else "Scan Nearby")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (scannedDevices.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isScanning) "Scanning for nearby Atomberg BLE fans & ESP32 bridges..." else "Click 'Scan Nearby' to discover BLE devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                scannedDevices.forEach { scanned ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = scanned.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${scanned.macAddress} • ${scanned.rssi} dBm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (scanned.isAlreadyPaired) {
                                Text(
                                    text = "Paired",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Button(onClick = {
                                    onAddScanned(scanned.macAddress, scanned.name, scanned.transportType)
                                }) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Fan")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddDeviceDialog(
    defaultMac: String,
    defaultName: String,
    defaultTransport: TransportMode,
    onDismiss: () -> Unit,
    onConfirm: (name: String, roomName: String, mac: String, transportType: TransportMode) -> Unit
) {
    var name by remember { mutableStateOf(defaultName.ifEmpty { "Living Room Fan" }) }
    var roomName by remember { mutableStateOf("Living Room") }
    var mac by remember { mutableStateOf(defaultMac) }
    var selectedTransport by remember { mutableStateOf(defaultTransport) }

    val roomSuggestions = listOf("Living Room", "Master Bedroom", "Kid's Room", "Guest Room", "Office", "Dining Room")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add & Pair Fan Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Custom Device Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Room Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Quick Room Selection:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    roomSuggestions.forEach { suggestion ->
                        FilterChip(
                            selected = roomName == suggestion,
                            onClick = { roomName = suggestion },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = mac,
                    onValueChange = { mac = it },
                    label = { Text("MAC Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Transport Mode:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransportMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { selectedTransport = mode }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedTransport == mode,
                                onClick = { selectedTransport = mode }
                            )
                            Text(mode.displayName, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(name, roomName, mac, selectedTransport)
            }) {
                Text("Save & Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditDeviceDialog(
    device: FanDevice,
    onDismiss: () -> Unit,
    onConfirm: (name: String, roomName: String) -> Unit
) {
    var name by remember { mutableStateOf(device.name) }
    var roomName by remember { mutableStateOf(device.roomName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Device Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Room Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, roomName) }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0) return "Never"
    return SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
fun DeviceManagementPreview() {
    AtomFanTheme {
        DeviceManagementContent(
            uiState = DeviceManagementUiState(
                pairedDevices = listOf(
                    FanDevice(id = 1, name = "Living Room Fan", roomName = "Living Room", macAddress = "AA:BB:CC:DD:EE:FF", transportType = "FAN_BLE", isDefault = true),
                    FanDevice(id = 2, name = "Bedroom Fan", roomName = "Master Bedroom", macAddress = "11:22:33:44:55:66", transportType = "ESP32_BRIDGE", isDefault = false)
                ),
                activeDevice = FanDevice(id = 1, name = "Living Room Fan", roomName = "Living Room", macAddress = "AA:BB:CC:DD:EE:FF", transportType = "FAN_BLE", isDefault = true),
                scannedDevices = listOf(
                    ScannedDeviceItem(name = "Atomberg_Studio", macAddress = "AA:BB:CC:11:22:33", rssi = -55, transportType = TransportMode.FAN_BLE)
                )
            ),
            onBack = {},
            onStartScan = {},
            onStopScan = {},
            onConnectDevice = {},
            onDisconnectDevice = {},
            onSetActiveDevice = {},
            onOpenAddDialogScanned = { _, _, _ -> },
            onDismissAddDialog = {},
            onSaveNewDevice = { _, _, _, _ -> },
            onOpenEditDialog = {},
            onDismissEditDialog = {},
            onSaveDeviceDetails = { _, _, _ -> },
            onOpenDeleteConfirm = {},
            onDismissDeleteConfirm = {},
            onConfirmDeleteDevice = {}
        )
    }
}

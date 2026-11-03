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
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.atomfan.model.BleConnectionState
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    DeviceManagementContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onStartScan = { viewModel.startScan() },
        onStopScan = { viewModel.stopScan() },
        onConnectDevice = { viewModel.connectDevice(it) },
        onDisconnectDevice = { viewModel.disconnectDevice() },
        onSwitchActiveFan = { viewModel.switchActiveFan(it) },
        onOpenAddDialog = { viewModel.openAddDialog() },
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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (FanDevice) -> Unit,
    onDisconnectDevice: () -> Unit,
    onSwitchActiveFan: (Long) -> Unit,
    onOpenAddDialog: () -> Unit,
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Device Management",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Manage & Switch Multiple Atomberg Fans",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    IconButton(onClick = onOpenAddDialog) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Fan"
                        )
                    }
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAddDialog,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Fan", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Active Fan Banner Card
            ActiveDeviceBannerCard(
                activeDevice = uiState.activeDevice,
                connectedMac = uiState.connectedDeviceMac,
                connectionState = uiState.activeDevice?.let { uiState.deviceConnectionStates[it.macAddress] } ?: BleConnectionState.DISCONNECTED,
                reconnectAttempts = uiState.reconnectAttemptCount,
                onConnect = { uiState.activeDevice?.let { onConnectDevice(it) } },
                onDisconnect = onDisconnectDevice
            )

            // Registered Fans Section
            RegisteredFansSection(
                pairedDevices = uiState.pairedDevices,
                activeDeviceId = uiState.activeDevice?.id,
                connectionStates = uiState.deviceConnectionStates,
                onSwitchActive = onSwitchActiveFan,
                onConnect = onConnectDevice,
                onDisconnect = onDisconnectDevice,
                onEdit = onOpenEditDialog,
                onDelete = onOpenDeleteConfirm,
                onAddNewFan = onOpenAddDialog
            )

            // Scanned Nearby Devices Section
            ScannedDevicesSection(
                scannedDevices = uiState.scannedDevices,
                isScanning = uiState.isScanning,
                spinAngle = spinAngle,
                onStartScan = onStartScan,
                onStopScan = onStopScan,
                onAddScanned = onOpenAddDialogScanned
            )

            Spacer(modifier = Modifier.height(72.dp)) // Leave space for FAB
        }
    }

    // Add Fan Dialog
    if (uiState.showAddDialog) {
        AddDeviceDialog(
            defaultMac = uiState.addDialogMac,
            defaultName = uiState.addDialogDefaultName,
            defaultTransport = uiState.addDialogTransport,
            onDismiss = onDismissAddDialog,
            onConfirm = onSaveNewDevice
        )
    }

    // Edit Fan Dialog
    if (uiState.editingDevice != null) {
        EditDeviceDialog(
            device = uiState.editingDevice,
            onDismiss = onDismissEditDialog,
            onConfirm = { name, room ->
                onSaveDeviceDetails(uiState.editingDevice.id, name, room)
            }
        )
    }

    // Remove Confirmation Dialog
    if (uiState.deviceToDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirm,
            title = { Text("Remove Registered Fan") },
            text = {
                Text(
                    "Are you sure you want to remove '${uiState.deviceToDelete.name}' in '${uiState.deviceToDelete.roomName}'? " +
                    "If this fan is currently connected, it will be disconnected."
                )
            },
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
    connectionState: BleConnectionState,
    reconnectAttempts: Int,
    onConnect: () -> Unit,
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
                        text = "ACTIVE TARGET FAN",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Connection status chip
                val statusText = when (connectionState) {
                    BleConnectionState.CONNECTED -> "Connected"
                    BleConnectionState.CONNECTING -> "Connecting..."
                    BleConnectionState.RECONNECTING -> if (reconnectAttempts > 0) "Reconnecting #$reconnectAttempts" else "Reconnecting..."
                    BleConnectionState.DISCONNECTED -> "Disconnected"
                }

                val statusBg = when (connectionState) {
                    BleConnectionState.CONNECTED -> Color(0xFF2E7D32)
                    BleConnectionState.CONNECTING, BleConnectionState.RECONNECTING -> Color(0xFFE65100)
                    BleConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.errorContainer
                }

                val statusTextColor = when (connectionState) {
                    BleConnectionState.CONNECTED, BleConnectionState.CONNECTING, BleConnectionState.RECONNECTING -> Color.White
                    BleConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onErrorContainer
                }

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusBg,
                        labelColor = statusTextColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (activeDevice != null) {
                Text(
                    text = activeDevice.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activeDevice.roomName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "•  MAC: ${activeDevice.macAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (connectionState == BleConnectionState.CONNECTED) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Disconnect")
                        }
                    } else {
                        Button(
                            onClick = onConnect,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.BluetoothConnected, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect")
                        }
                    }
                }
            } else {
                Text(
                    text = "No Fan Device Selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Add a fan or select one of your registered fans below to start controlling it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun RegisteredFansSection(
    pairedDevices: List<FanDevice>,
    activeDeviceId: Long?,
    connectionStates: Map<String, BleConnectionState>,
    onSwitchActive: (Long) -> Unit,
    onConnect: (FanDevice) -> Unit,
    onDisconnect: () -> Unit,
    onEdit: (FanDevice) -> Unit,
    onDelete: (FanDevice) -> Unit,
    onAddNewFan: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Registered Fans (${pairedDevices.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TextButton(onClick = onAddNewFan) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add New")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (pairedDevices.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "No fans registered yet.",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Scan for nearby Atomberg BLE fans or add a fan manually to begin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onAddNewFan) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Your First Fan")
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                pairedDevices.forEach { device ->
                    val isActive = device.id == activeDeviceId
                    val connState = connectionStates[device.macAddress] ?: BleConnectionState.DISCONNECTED

                    RegisteredFanCard(
                        device = device,
                        isActive = isActive,
                        connectionState = connState,
                        onSwitchActive = { onSwitchActive(device.id) },
                        onConnect = { onConnect(device) },
                        onDisconnect = onDisconnect,
                        onEdit = { onEdit(device) },
                        onDelete = { onDelete(device) }
                    )
                }
            }
        }
    }
}

@Composable
fun RegisteredFanCard(
    device: FanDevice,
    isActive: Boolean,
    connectionState: BleConnectionState,
    onSwitchActive: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isActive) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onSwitchActive) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isActive) "Active Fan" else "Make Active",
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

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

                // Per-device connection status badge
                ConnectionStatusChip(connectionState = connectionState)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MAC: ${device.macAddress} • Last: ${formatTime(device.lastConnected)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = when (device.transportType) {
                                TransportMode.FAN_BLE.name -> "BLE"
                                TransportMode.ESP32_BRIDGE.name -> "ESP32"
                                else -> "Phone IR"
                            },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Fan Details")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove Fan", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isActive) {
                        OutlinedButton(onClick = onSwitchActive) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch to Fan")
                        }
                    }

                    if (connectionState == BleConnectionState.CONNECTED) {
                        OutlinedButton(onClick = onDisconnect) {
                            Text("Disconnect")
                        }
                    } else {
                        Button(
                            onClick = onConnect,
                            enabled = connectionState != BleConnectionState.CONNECTING && connectionState != BleConnectionState.RECONNECTING
                        ) {
                            if (connectionState == BleConnectionState.CONNECTING || connectionState == BleConnectionState.RECONNECTING) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connecting")
                            } else {
                                Text("Connect")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusChip(connectionState: BleConnectionState) {
    val (label, bg, textClr) = when (connectionState) {
        BleConnectionState.CONNECTED -> Triple("Connected", Color(0xFF2E7D32), Color.White)
        BleConnectionState.CONNECTING -> Triple("Connecting...", Color(0xFFE65100), Color.White)
        BleConnectionState.RECONNECTING -> Triple("Reconnecting...", Color(0xFFE65100), Color.White)
        BleConnectionState.DISCONNECTED -> Triple("Disconnected", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = CircleShape,
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (connectionState) {
                BleConnectionState.CONNECTED -> {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp), tint = textClr)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                BleConnectionState.CONNECTING, BleConnectionState.RECONNECTING -> {
                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = textClr)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                BleConnectionState.DISCONNECTED -> {}
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textClr
            )
        }
    }
}

@Composable
fun ScannedDevicesSection(
    scannedDevices: List<ScannedDeviceItem>,
    isScanning: Boolean,
    spinAngle: Float,
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
            Column {
                Text(
                    text = "Nearby BLE Devices (${scannedDevices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Atomberg Smart Fans & ESP32 Bridges in BLE range",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = {
                if (isScanning) onStopScan() else onStartScan()
            }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = if (isScanning) Modifier.rotate(spinAngle).size(16.dp) else Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isScanning) "Stop" else "Scan Nearby")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                        text = if (isScanning) "Scanning nearby for Atomberg fans & ESP32 bridges..." else "No devices found. Tap 'Scan Nearby' to scan for Atomberg fans.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                scannedDevices.forEach { scanned ->
                    ScannedDeviceCard(
                        scanned = scanned,
                        onAdd = { onAddScanned(scanned.macAddress, scanned.name, scanned.transportType) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScannedDeviceCard(
    scanned: ScannedDeviceItem,
    onAdd: () -> Unit
) {
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (scanned.transportType == TransportMode.FAN_BLE) Icons.Default.Bluetooth else Icons.Default.Router,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = scanned.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${scanned.macAddress} • ${scanned.rssi} dBm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (scanned.isAlreadyPaired) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Registered",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                Button(onClick = onAdd) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Fan")
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

    val roomSuggestions = listOf("Living Room", "Master Bedroom", "Kid's Room", "Guest Room", "Office", "Dining Room", "Kitchen", "Balcony")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add & Register Fan", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Custom Device Name") },
                    placeholder = { Text("e.g. Renesa Master Bed") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Room Name") },
                    placeholder = { Text("e.g. Master Bedroom") },
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
                    onValueChange = { mac = it.uppercase() },
                    label = { Text("MAC Address") },
                    placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Connection Transport:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TransportMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTransport = mode }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedTransport == mode,
                                onClick = { selectedTransport = mode }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(mode.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, roomName, mac, selectedTransport)
                },
                enabled = name.isNotBlank() && roomName.isNotBlank() && (selectedTransport == TransportMode.PHONE_IR || mac.isNotBlank())
            ) {
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

    val roomSuggestions = listOf("Living Room", "Master Bedroom", "Kid's Room", "Guest Room", "Office", "Dining Room", "Kitchen", "Balcony")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Fan Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, roomName) },
                enabled = name.isNotBlank() && roomName.isNotBlank()
            ) {
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
                connectedDeviceMac = "AA:BB:CC:DD:EE:FF",
                deviceConnectionStates = mapOf(
                    "AA:BB:CC:DD:EE:FF" to BleConnectionState.CONNECTED,
                    "11:22:33:44:55:66" to BleConnectionState.DISCONNECTED
                ),
                scannedDevices = listOf(
                    ScannedDeviceItem(name = "Atomberg_Studio", macAddress = "AA:BB:CC:11:22:33", rssi = -55, transportType = TransportMode.FAN_BLE)
                )
            ),
            onBack = {},
            onStartScan = {},
            onStopScan = {},
            onConnectDevice = {},
            onDisconnectDevice = {},
            onSwitchActiveFan = {},
            onOpenAddDialog = {},
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

package com.example.atomfan.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.model.BleLogEntry

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleDashboardScreen(
    fanBleManager: FanBleManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isScanning by fanBleManager.isScanning.collectAsStateWithLifecycle()
    val isConnected by fanBleManager.isConnected.collectAsStateWithLifecycle()
    val connectedDeviceName by fanBleManager.connectedDeviceName.collectAsStateWithLifecycle()
    val scannedDevices by fanBleManager.scannedDevices.collectAsStateWithLifecycle()
    val rssiMap by fanBleManager.scannedDeviceRssiMap.collectAsStateWithLifecycle()
    val fanState by fanBleManager.fanState.collectAsStateWithLifecycle()
    val logs by fanBleManager.logs.collectAsStateWithLifecycle()
    val lastWriteStatus by fanBleManager.lastWriteGattStatus.collectAsStateWithLifecycle()

    var experimentalWriteEnabled by remember { mutableStateOf(false) }
    var hexPayloadInput by remember { mutableStateOf("11 F8 00 00") }

    val connectedDevice = fanBleManager.connectedDevice
    val bondState = connectedDevice?.bondState ?: BluetoothDevice.BOND_NONE
    val isBonded = bondState == BluetoothDevice.BOND_BONDED

    val atombergDevices = scannedDevices.filter {
        val name = it.name ?: ""
        name.startsWith("Atomberg_", ignoreCase = true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Fan BLE Live Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { fanBleManager.refreshState() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh State")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
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
                // 1. Device Scanner Section
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
                                        text = "ATOMBERG BLE SCANNER",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isConnected) "Connected: ${connectedDeviceName ?: "Fan"}" else "Disconnected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (isScanning) fanBleManager.stopScan() else fanBleManager.startScan()
                                    }
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Stop Scan")
                                    } else {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Scan Devices")
                                    }
                                }
                            }

                            if (connectedDevice != null) {
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = connectedDevice.name ?: "Atomberg Fan",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = connectedDevice.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = { fanBleManager.disconnect() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                                    ) {
                                        Text("Disconnect")
                                    }
                                }
                            }

                            if (atombergDevices.isNotEmpty()) {
                                HorizontalDivider()
                                Text("Discovered Atomberg Devices:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                atombergDevices.forEach { device ->
                                    val rssi = rssiMap[device.address] ?: 0
                                    val isThisConnected = connectedDevice?.address == device.address
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(device.name ?: "Atomberg Fan", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                            Text("${device.address} | RSSI: $rssi dBm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (!isThisConnected) {
                                            Button(onClick = { fanBleManager.connect(device) }) {
                                                Text("Connect")
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "Connected",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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

                // 2. Live Dashboard Card
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE DASHBOARD TELEMETRY",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { fanBleManager.refreshState() }) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                                }
                            }

                            val speedText = if (fanState.boost) "BOOST (6)" else "${fanState.speed} / 5"
                            val powerText = if (fanState.powerOn) "ON" else "OFF"
                            val ledText = if (fanState.led) "ON" else "OFF"
                            val sleepText = if (fanState.sleep) "ON" else "OFF"
                            val timerText = if (fanState.timer > 0) "${fanState.timer} Hours" else "OFF"

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                DashboardMetricItem(label = "Power", value = powerText, active = fanState.powerOn)
                                DashboardMetricItem(label = "Speed", value = speedText, active = fanState.speed > 0 || fanState.boost)
                                DashboardMetricItem(label = "LED", value = ledText, active = fanState.led)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                DashboardMetricItem(label = "Sleep", value = sleepText, active = fanState.sleep)
                                DashboardMetricItem(label = "Timer", value = timerText, active = fanState.timer > 0)
                                DashboardMetricItem(label = "Raw Bytes", value = if (fanState.rawBytesHex.isNotEmpty()) "0x${fanState.rawBytesHex}" else "None", active = fanState.rawBytesHex.isNotEmpty())
                            }
                        }
                    }
                }

                // 3. Experimental BLE Control Section
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "EXPERIMENTAL BLE CONTROL",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Direct GATT write to e29ee02c-af3d-11ec-b909-0242ac120002",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = experimentalWriteEnabled,
                                    onCheckedChange = { experimentalWriteEnabled = it }
                                )
                            }

                            if (experimentalWriteEnabled) {
                                HorizontalDivider()

                                if (isConnected && connectedDevice != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isBonded) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = if (isBonded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBonded) "Device Bonded" else "Unbonded Device Warning",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isBonded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                        if (!isBonded) {
                                            Button(
                                                onClick = { connectedDevice.createBond() },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                                            ) {
                                                Text("Pair / Create Bond")
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = hexPayloadInput,
                                    onValueChange = { hexPayloadInput = it },
                                    label = { Text("HEX Payload (e.g. 11 F8 00 00)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            fanBleManager.writeExperimentalHexString(hexPayloadInput)
                                        },
                                        enabled = isConnected
                                    ) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Write Characteristic")
                                    }

                                    lastWriteStatus?.let { status ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (status == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Text(
                                                text = "GATT Status: 0x%02X".format(status),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (status == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Log View Section & Export
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BLE LOGS & GATT EVENTS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row {
                                    TextButton(onClick = { fanBleManager.clearLogs() }) {
                                        Text("Clear")
                                    }
                                    TextButton(onClick = {
                                        val logText = fanBleManager.exportLogs()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("AtomFan BLE Logs", logText))
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, logText)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Export AtomFan BLE Logs")
                                        context.startActivity(shareIntent)
                                    }) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Export")
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    reverseLayout = true
                                ) {
                                    items(logs.reversed()) { entry ->
                                        LogEntryRow(entry = entry)
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

@Composable
fun DashboardMetricItem(label: String, value: String, active: Boolean) {
    Column(
        modifier = Modifier
            .width(105.dp)
            .background(
                color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LogEntryRow(entry: BleLogEntry) {
    val textColor = if (entry.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val statusStr = entry.gattStatus?.let { " [GATT: $it]" } ?: ""
    val errStr = if (entry.isError) " [ERROR]" else ""

    Text(
        text = "[${entry.formattedTime}]$errStr$statusStr ${entry.message}",
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = textColor,
        fontSize = 11.sp
    )
}

package com.example.atomfan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.atomfan.database.FanDevice
import com.example.atomfan.model.FanState
import com.example.atomfan.model.TransportMode
import com.example.atomfan.ui.theme.AtomFanTheme
import com.example.atomfan.viewmodel.FanPreset
import com.example.atomfan.viewmodel.SmartDashboardUiState
import com.example.atomfan.viewmodel.SmartDashboardViewModel

@Composable
fun SmartDashboardScreen(
    viewModel: SmartDashboardViewModel,
    onNavigateToPhysicalRemote: () -> Unit = {},
    onNavigateToDeviceManagement: () -> Unit = {},
    onNavigateToBleDashboard: () -> Unit = {},
    onNavigateToEsp32Learn: () -> Unit = {},
    onNavigateToCodeEditor: () -> Unit = {},
    onNavigateToCodeFinder: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SmartDashboardContent(
        uiState = uiState,
        onTogglePower = { viewModel.togglePower() },
        onSetSpeed = { viewModel.setSpeed(it) },
        onApplyPreset = { viewModel.applyPreset(it) },
        onToggleBoost = { viewModel.toggleBoost() },
        onToggleSleep = { viewModel.toggleSleep() },
        onToggleLed = { viewModel.toggleLed() },
        onSetTimer = { viewModel.setTimer(it) },
        onSelectDevice = { viewModel.selectActiveDevice(it) },
        onSetTransportMode = { viewModel.setTransportMode(it) },
        onSetRepeatCount = { viewModel.setRepeatCount(it) },
        onSetExperimentalBleWrite = { viewModel.setExperimentalBleWrite(it) },
        onRefreshState = { viewModel.refreshState() },
        onNavigateToPhysicalRemote = onNavigateToPhysicalRemote,
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
fun SmartDashboardContent(
    uiState: SmartDashboardUiState,
    onTogglePower: () -> Unit,
    onSetSpeed: (Int) -> Unit,
    onApplyPreset: (FanPreset) -> Unit,
    onToggleBoost: () -> Unit,
    onToggleSleep: () -> Unit,
    onToggleLed: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onSelectDevice: (Long) -> Unit,
    onSetTransportMode: (TransportMode) -> Unit,
    onSetRepeatCount: (Int) -> Unit,
    onSetExperimentalBleWrite: (Boolean) -> Unit,
    onRefreshState: () -> Unit,
    onNavigateToPhysicalRemote: () -> Unit,
    onNavigateToDeviceManagement: () -> Unit,
    onNavigateToBleDashboard: () -> Unit,
    onNavigateToEsp32Learn: () -> Unit,
    onNavigateToCodeEditor: () -> Unit,
    onNavigateToCodeFinder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeviceSheet by remember { mutableStateOf(false) }
    var showTransportDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Sync rotation animation for refresh icon
    val syncRotation by animateFloatAsState(
        targetValue = if (uiState.isRefreshing) 360f else 0f,
        animationSpec = if (uiState.isRefreshing) {
            infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart)
        } else {
            tween(200)
        },
        label = "syncRotation"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable { showDeviceSheet = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.activeDevice?.name ?: "Atomberg Smart Fan",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Select Device",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        val roomName = uiState.activeDevice?.roomName ?: "Default Room"
                        Text(
                            text = "$roomName • ${uiState.connectionStatusText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Quick Refresh Button
                    IconButton(onClick = onRefreshState) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Fan Telemetry",
                            modifier = Modifier.rotate(syncRotation),
                            tint = if (uiState.isRefreshing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Device Selector Quick Action
                    IconButton(onClick = { showDeviceSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Devices",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Overflow Menu
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Hardware Transport Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                menuExpanded = false
                                showTransportDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Physical Remote Widget") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToPhysicalRemote()
                            },
                            leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Device Management") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToDeviceManagement()
                            },
                            leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Fan BLE Telemetry Dashboard") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToBleDashboard()
                            },
                            leadingIcon = { Icon(Icons.Default.Bluetooth, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("ESP32 IR Learn Screen") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToEsp32Learn()
                            },
                            leadingIcon = { Icon(Icons.Default.Router, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("IR Code Editor") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToCodeEditor()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("IR Code Finder") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToCodeFinder()
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Connection, Sync & Power Status Header
                ConnectionStatusHeader(
                    uiState = uiState,
                    onOpenTransportDialog = { showTransportDialog = true },
                    onOpenDeviceSheet = { showDeviceSheet = true }
                )

                // Hero Fan Graphic Card with Animated Blades & Power Toggle
                HeroFanGraphicCard(
                    fanState = uiState.fanState,
                    estimatedWatts = uiState.estimatedPowerWatts,
                    onTogglePower = onTogglePower
                )

                // Interactive Speed Slider & Direct Step Selector
                InteractiveSpeedCard(
                    fanState = uiState.fanState,
                    onSetSpeed = onSetSpeed
                )

                // Quick Presets: Eco, Turbo, Sleep, Normal (with Natural Breeze check)
                QuickPresetsCard(
                    activePreset = uiState.activePreset,
                    powerOn = uiState.fanState.powerOn,
                    isNaturalBreezeSupported = uiState.isNaturalBreezeSupported,
                    onApplyPreset = onApplyPreset
                )

                // Quick Action Controls: Boost, Sleep, LED, and Stepped Timer
                QuickActionsCard(
                    fanState = uiState.fanState,
                    onToggleBoost = onToggleBoost,
                    onToggleSleep = onToggleSleep,
                    onToggleLed = onToggleLed,
                    onSetTimer = onSetTimer
                )

                // Real-time Energy Telemetry & Power Specs
                EnergyTelemetryCard(
                    fanState = uiState.fanState,
                    estimatedWatts = uiState.estimatedPowerWatts,
                    batteryStatus = uiState.batteryStatus,
                    batteryLevel = uiState.batteryLevel,
                    lastSyncTimestamp = uiState.lastSyncTimestamp
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Device Selection Bottom Sheet
    if (showDeviceSheet) {
        DeviceSelectionSheet(
            activeDevice = uiState.activeDevice,
            pairedDevices = uiState.pairedDevices,
            onSelectDevice = {
                onSelectDevice(it)
                showDeviceSheet = false
            },
            onManageDevices = {
                showDeviceSheet = false
                onNavigateToDeviceManagement()
            },
            onDismiss = { showDeviceSheet = false }
        )
    }

    // Transport Settings Dialog
    if (showTransportDialog) {
        TransportSettingsDialog(
            currentMode = uiState.activeMode,
            repeatCount = uiState.repeatCount,
            isExperimentalBle = uiState.isExperimentalBleWrite,
            hasIrEmitter = uiState.hasIrEmitter,
            onModeSelected = { onSetTransportMode(it) },
            onRepeatCountSelected = { onSetRepeatCount(it) },
            onExperimentalBleChanged = { onSetExperimentalBleWrite(it) },
            onDismiss = { showTransportDialog = false }
        )
    }
}

@Composable
fun ConnectionStatusHeader(
    uiState: SmartDashboardUiState,
    onOpenTransportDialog: () -> Unit,
    onOpenDeviceSheet: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Connection Dot & Sync Timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val dotColor = if (uiState.isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = uiState.connectionStatusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Synced: ${uiState.lastSyncTimestamp}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Transport Mode Pill Chip
            AssistChip(
                onClick = onOpenTransportDialog,
                label = {
                    Text(
                        text = uiState.activeMode.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    val icon = when (uiState.activeMode) {
                        TransportMode.PHONE_IR -> Icons.Default.Radio
                        TransportMode.FAN_BLE -> Icons.Default.Bluetooth
                        TransportMode.ESP32_BRIDGE -> Icons.Default.Router
                    }
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun HeroFanGraphicCard(
    fanState: FanState,
    estimatedWatts: Double,
    onTogglePower: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isPowerOn = fanState.powerOn
    val isBoost = fanState.boost
    val currentSpeed = fanState.speed

    // Infinite rotation transition for fan blades
    val infiniteTransition = rememberInfiniteTransition(label = "fanBladeSpin")

    // Rotation speed based on fan speed level (faster spin for higher speed)
    val spinDuration = when {
        !isPowerOn -> 0
        isBoost -> 250
        currentSpeed >= 5 -> 350
        currentSpeed == 4 -> 550
        currentSpeed == 3 -> 850
        currentSpeed == 2 -> 1300
        else -> 1900
    }

    val rotationAngle by if (isPowerOn) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = spinDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "fanRotation"
        )
    } else {
        animateFloatAsState(targetValue = 0f, label = "stopFan")
    }

    val powerColor by animateColorAsState(
        targetValue = if (isPowerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "powerColor"
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fan Blade Dynamic Canvas Graphic with Ambient Radial Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isPowerOn) {
                                if (isBoost) {
                                    listOf(Color(0xFFFF9800).copy(alpha = 0.35f), MaterialTheme.colorScheme.surfaceContainer)
                                } else if (fanState.sleep) {
                                    listOf(Color(0xFF7E57C2).copy(alpha = 0.35f), MaterialTheme.colorScheme.surfaceContainer)
                                } else {
                                    listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), MaterialTheme.colorScheme.surfaceContainer)
                                }
                            } else {
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.surfaceContainer
                                )
                            }
                        )
                    )
            ) {
                // Outer ring arc gauge
                val primaryColor = if (isBoost) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                val sweepAngle by animateFloatAsState(
                    targetValue = if (!isPowerOn) 0f else if (isBoost) 360f else (currentSpeed / 5f) * 360f,
                    animationSpec = tween(600),
                    label = "sweepAngle"
                )

                Canvas(modifier = Modifier.size(196.dp)) {
                    drawCircle(
                        color = outlineColor,
                        style = Stroke(width = 8.dp.toPx())
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx())
                    )
                }

                // Rotating Fan Blade Vector
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = "Aerodynamic Fan Blades",
                    tint = if (isPowerOn) {
                        if (isBoost) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                    modifier = Modifier
                        .size(125.dp)
                        .rotate(rotationAngle)
                )

                // Central Speed / State Badge Overlay
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (!isPowerOn) "OFF" else if (isBoost) "TURBO" else "L$currentSpeed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPowerOn) {
                                if (isBoost) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Large Animated Power Toggle Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPowerOn) {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            )
                        }
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTogglePower()
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Toggle Power",
                    tint = if (isPowerOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isPowerOn) "FAN IS ACTIVE" else "TAP TO TURN ON",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = powerColor
            )

            AnimatedVisibility(visible = isPowerOn, enter = fadeIn(), exit = fadeOut()) {
                val speedDesc = when {
                    isBoost -> "Turbo Maximum Velocity"
                    currentSpeed == 1 -> "Quiet Gentle Airflow"
                    currentSpeed == 2 -> "Eco Balanced Breeze"
                    currentSpeed == 3 -> "Comfort Daily Flow"
                    currentSpeed == 4 -> "High Air Delivery"
                    else -> "Maximum Continuous Flow"
                }
                Text(
                    text = "${estimatedWatts.toInt()} Watts • $speedDesc",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun InteractiveSpeedCard(
    fanState: FanState,
    onSetSpeed: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isPowerOn = fanState.powerOn
    val speed = fanState.speed.coerceIn(1, 5)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Speed Control",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (fanState.boost) "Turbo Boost (Level 6)" else "Level $speed of 5",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (fanState.boost) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Speed Slider
            Slider(
                value = speed.toFloat(),
                onValueChange = { newVal ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSetSpeed(newVal.toInt())
                },
                valueRange = 1f..5f,
                steps = 3,
                enabled = isPowerOn,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Speed indicator labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..5).forEach { step ->
                    Text(
                        text = "Spd $step",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (step == speed && isPowerOn && !fanState.boost) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (step == speed && isPowerOn && !fanState.boost) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Speed Preset Pills [1] [2] [3] [4] [5] [TURBO]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..5).forEach { step ->
                    val isSelected = isPowerOn && speed == step && !fanState.boost
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSetSpeed(step)
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "$step",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Turbo Button Pill (Speed 6)
                val isTurboSelected = isPowerOn && fanState.boost
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isTurboSelected) Color(0xFFFF9800) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1.3f)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSetSpeed(6)
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = "TURBO",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTurboSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slower / Faster Stepper Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (speed > 1) onSetSpeed(speed - 1)
                    },
                    enabled = isPowerOn && speed > 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Speed", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Slower")
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (speed < 5) onSetSpeed(speed + 1)
                    },
                    enabled = isPowerOn && speed < 5,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Speed", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Faster")
                }
            }
        }
    }
}

@Composable
fun QuickPresetsCard(
    activePreset: FanPreset?,
    powerOn: Boolean,
    isNaturalBreezeSupported: Boolean,
    onApplyPreset: (FanPreset) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                Text(
                    text = "Smart Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = activePreset?.description ?: "Select an airflow preset",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FanPreset.entries.forEach { preset ->
                    val isSelected = powerOn && activePreset == preset
                    val containerColor = if (isSelected) {
                        when (preset) {
                            FanPreset.TURBO -> Color(0xFFFF9800)
                            FanPreset.SLEEP -> Color(0xFF673AB7)
                            FanPreset.ECO -> Color(0xFF2E7D32)
                            FanPreset.NORMAL -> MaterialTheme.colorScheme.primary
                        }
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(containerColor)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onApplyPreset(preset)
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val icon = when (preset) {
                                FanPreset.SLEEP -> Icons.Default.Nightlight
                                FanPreset.ECO -> Icons.Default.Thermostat
                                FanPreset.NORMAL -> Icons.Default.ElectricBolt
                                FanPreset.TURBO -> Icons.Default.RocketLaunch
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = preset.displayName,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        }
                    }
                }
            }

            // Natural Breeze Mode check: Only shown if supported by existing fan protocol
            if (isNaturalBreezeSupported) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Air, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Natural Breeze Mode: Active", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Protocol note: Atomberg NEC/BLE firmware does not provide a breeze mode opcode.",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionsCard(
    fanState: FanState,
    onToggleBoost: () -> Unit,
    onToggleSleep: () -> Unit,
    onToggleLed: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Quick Actions & Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Boost, Sleep, and LED Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Boost Toggle
                ToggleActionItem(
                    label = "Boost (Turbo)",
                    icon = Icons.Default.RocketLaunch,
                    isActive = fanState.boost,
                    activeColor = Color(0xFFFF9800),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleBoost()
                    },
                    modifier = Modifier.weight(1f)
                )

                // Sleep Toggle
                ToggleActionItem(
                    label = "Sleep Mode",
                    icon = Icons.Default.Nightlight,
                    isActive = fanState.sleep,
                    activeColor = Color(0xFF673AB7),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleSleep()
                    },
                    modifier = Modifier.weight(1f)
                )

                // LED Toggle
                ToggleActionItem(
                    label = "LED Light",
                    icon = Icons.Default.Lightbulb,
                    isActive = fanState.led,
                    activeColor = Color(0xFF00ACC1),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleLed()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Timer Preset Chips (OFF, 1h, 2h, 4h, 8h)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Auto-Off Timer",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = if (fanState.timer == 0) "Inactive" else "Active (${fanState.timer} hrs)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (fanState.timer > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val timerOptions = listOf(0 to "OFF", 1 to "1h", 2 to "2h", 4 to "4h", 8 to "8h")
                timerOptions.forEach { (hrs, label) ->
                    val isSelected = fanState.timer == hrs
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSetTimer(hrs)
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleActionItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) activeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isActive) 1.5.dp else 0.dp,
                color = if (isActive) activeColor else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
fun EnergyTelemetryCard(
    fanState: FanState,
    estimatedWatts: Double,
    batteryStatus: String,
    batteryLevel: Int?,
    lastSyncTimestamp: String
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                Text(
                    text = "Telemetry & Power Diagnostics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Battery status: only rendered when exposed by the device, otherwise AC Line Power
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (batteryLevel != null) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$batteryLevel%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AC Line Powered",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMetricBox(
                    title = "Motor Draw",
                    value = "${estimatedWatts.toInt()} W",
                    subtitle = if (fanState.powerOn) "Active BLDC Inverter" else "Standby Mode",
                    modifier = Modifier.weight(1f)
                )

                TelemetryMetricBox(
                    title = "Daily Est.",
                    value = "%.2f kWh".format(estimatedWatts * 8 / 1000.0),
                    subtitle = "8 hrs continuous",
                    modifier = Modifier.weight(1f)
                )

                TelemetryMetricBox(
                    title = "Status Sync",
                    value = if (fanState.powerOn) "Running" else "Standby",
                    subtitle = lastSyncTimestamp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TelemetryMetricBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectionSheet(
    activeDevice: FanDevice?,
    pairedDevices: List<FanDevice>,
    onSelectDevice: (Long) -> Unit,
    onManageDevices: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Select Fan Device",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Switch active fan or configure device rooms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (pairedDevices.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No fans paired yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add and pair your Atomberg fans to switch between rooms seamlessly.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pairedDevices.forEach { device ->
                        val isSelected = activeDevice?.id == device.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectDevice(device.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Air,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = device.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${device.roomName} • ${device.transportType}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active Device",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = onManageDevices,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Paired Devices & Rooms")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TransportSettingsDialog(
    currentMode: TransportMode,
    repeatCount: Int,
    isExperimentalBle: Boolean,
    hasIrEmitter: Boolean,
    onModeSelected: (TransportMode) -> Unit,
    onRepeatCountSelected: (Int) -> Unit,
    onExperimentalBleChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Hardware Transport & Signal",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Select Hardware Route:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                TransportMode.entries.forEach { mode ->
                    val isAvailable = mode != TransportMode.PHONE_IR || hasIrEmitter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = isAvailable) { onModeSelected(mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { if (isAvailable) onModeSelected(mode) },
                            enabled = isAvailable
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal
                            )
                            if (mode == TransportMode.PHONE_IR && !hasIrEmitter) {
                                Text(
                                    text = "No IR emitter found on this device",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "IR Signal Transmission Repeat:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2).forEach { count ->
                        FilterChip(
                            selected = repeatCount == count,
                            onClick = { onRepeatCountSelected(count) },
                            label = { Text("${count}x Repeat") }
                        )
                    }
                }

                if (currentMode == TransportMode.FAN_BLE) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Direct GATT Control (Exp.)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        FilterChip(
                            selected = isExperimentalBle,
                            onClick = { onExperimentalBleChanged(!isExperimentalBle) },
                            label = { Text(if (isExperimentalBle) "ON" else "OFF") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SmartDashboardPreview() {
    AtomFanTheme {
        SmartDashboardContent(
            uiState = SmartDashboardUiState(
                fanState = FanState(powerOn = true, speed = 3, led = true),
                activeMode = TransportMode.FAN_BLE,
                isConnected = true,
                connectionStatusText = "BLE Connected (Living Room Fan)",
                lastSyncTimestamp = "10:45:12 AM",
                activePreset = FanPreset.NORMAL,
                estimatedPowerWatts = 16.0
            ),
            onTogglePower = {},
            onSetSpeed = {},
            onApplyPreset = {},
            onToggleBoost = {},
            onToggleSleep = {},
            onToggleLed = {},
            onSetTimer = {},
            onSelectDevice = {},
            onSetTransportMode = {},
            onSetRepeatCount = {},
            onSetExperimentalBleWrite = {},
            onRefreshState = {},
            onNavigateToPhysicalRemote = {},
            onNavigateToDeviceManagement = {},
            onNavigateToBleDashboard = {},
            onNavigateToEsp32Learn = {},
            onNavigateToCodeEditor = {},
            onNavigateToCodeFinder = {}
        )
    }
}

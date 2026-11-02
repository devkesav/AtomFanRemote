package com.example.atomfan.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    content: @Composable () -> Unit
) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)
    var showRationaleDialog by remember { mutableStateOf(true) }

    if (permissionsState.allPermissionsGranted) {
        content()
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            if (showRationaleDialog) {
                AlertDialog(
                    onDismissRequest = { showRationaleDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Permissions Required",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = {
                        Text(
                            text = "Bluetooth Permissions Required",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "AtomFan requires Bluetooth and Location permissions to discover and connect to your Atomberg Smart Fan (Mode B) and ESP32 IR Bridge (Mode C).",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Please grant permissions to enable wireless remote control and real-time response telemetry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                permissionsState.launchMultiplePermissionRequest()
                            }
                        ) {
                            Text("Grant Permissions")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showRationaleDialog = false
                            }
                        ) {
                            Text("Use Phone IR Only")
                        }
                    }
                )
            }
        }
    }
}

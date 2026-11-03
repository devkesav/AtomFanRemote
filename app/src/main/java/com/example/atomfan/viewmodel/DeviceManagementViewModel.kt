package com.example.atomfan.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.atomfan.ble.Esp32BleManager
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.data.DeviceRepository
import com.example.atomfan.database.FanDevice
import com.example.atomfan.model.TransportMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannedDeviceItem(
    val name: String,
    val macAddress: String,
    val rssi: Int = -60,
    val transportType: TransportMode = TransportMode.FAN_BLE,
    val isAlreadyPaired: Boolean = false
)

data class DeviceManagementUiState(
    val pairedDevices: List<FanDevice> = emptyList(),
    val activeDevice: FanDevice? = null,
    val scannedDevices: List<ScannedDeviceItem> = emptyList(),
    val isScanning: Boolean = false,
    val connectedDeviceMac: String? = null,
    val isConnecting: Boolean = false,
    val showAddDialog: Boolean = false,
    val addDialogMac: String = "",
    val addDialogDefaultName: String = "",
    val addDialogTransport: TransportMode = TransportMode.FAN_BLE,
    val editingDevice: FanDevice? = null,
    val deviceToDelete: FanDevice? = null,
    val statusMessage: String? = null
)

class DeviceManagementViewModel(
    application: Application,
    private val repository: CodeRepository,
    private val deviceRepository: DeviceRepository,
    private val fanBleManager: FanBleManager,
    private val esp32BleManager: Esp32BleManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DeviceManagementUiState())
    val uiState: StateFlow<DeviceManagementUiState> = _uiState.asStateFlow()

    init {
        observeState()
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeState() {
        viewModelScope.launch {
            combine(
                deviceRepository.pairedDevices,
                deviceRepository.activeDevice,
                fanBleManager.scannedDevices,
                fanBleManager.scannedDeviceRssiMap,
                fanBleManager.isScanning,
                fanBleManager.isConnected,
                esp32BleManager.scannedDevices,
                esp32BleManager.isScanning,
                esp32BleManager.isConnected
            ) { flows: Array<Any?> ->
                val paired = flows[0] as List<FanDevice>
                val active = flows[1] as FanDevice?
                val fanScanned = flows[2] as List<BluetoothDevice>
                val fanRssiMap = flows[3] as Map<String, Int>
                val fanScanning = flows[4] as Boolean
                val fanConn = flows[5] as Boolean
                val espScanned = flows[6] as List<BluetoothDevice>
                val espScanning = flows[7] as Boolean
                val espConn = flows[8] as Boolean

                val scannedList = mutableListOf<ScannedDeviceItem>()

                fanScanned.forEach { dev ->
                    val mac = dev.address
                    val name = dev.name ?: "Atomberg Fan ($mac)"
                    val rssi = fanRssiMap[mac] ?: -60
                    val isPaired = paired.any { it.macAddress.equals(mac, ignoreCase = true) }
                    scannedList.add(
                        ScannedDeviceItem(
                            name = name,
                            macAddress = mac,
                            rssi = rssi,
                            transportType = TransportMode.FAN_BLE,
                            isAlreadyPaired = isPaired
                        )
                    )
                }

                espScanned.forEach { dev ->
                    val mac = dev.address
                    val name = dev.name ?: "ESP32 Bridge ($mac)"
                    val isPaired = paired.any { it.macAddress.equals(mac, ignoreCase = true) }
                    scannedList.add(
                        ScannedDeviceItem(
                            name = name,
                            macAddress = mac,
                            rssi = -65,
                            transportType = TransportMode.ESP32_BRIDGE,
                            isAlreadyPaired = isPaired
                        )
                    )
                }

                val connectedMac = when {
                    fanConn -> fanBleManager.connectedDevice?.address
                    espConn -> "ESP32_BRIDGE_CONNECTED"
                    else -> null
                }

                DeviceManagementUiState(
                    pairedDevices = paired,
                    activeDevice = active,
                    scannedDevices = scannedList,
                    isScanning = fanScanning || espScanning,
                    connectedDeviceMac = connectedMac,
                    showAddDialog = _uiState.value.showAddDialog,
                    addDialogMac = _uiState.value.addDialogMac,
                    addDialogDefaultName = _uiState.value.addDialogDefaultName,
                    addDialogTransport = _uiState.value.addDialogTransport,
                    editingDevice = _uiState.value.editingDevice,
                    deviceToDelete = _uiState.value.deviceToDelete
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun startScan() {
        fanBleManager.startScan()
        esp32BleManager.startScan()
    }

    fun stopScan() {
        fanBleManager.stopScan()
        esp32BleManager.stopScan()
    }

    fun connectDevice(device: FanDevice) {
        viewModelScope.launch {
            deviceRepository.setActiveDevice(device.id)
            deviceRepository.updateLastConnected(device.id)

            when (device.transportType) {
                TransportMode.FAN_BLE.name -> {
                    repository.setTransportMode(TransportMode.FAN_BLE)
                    val scannedDev = fanBleManager.scannedDevices.value.firstOrNull {
                        it.address.equals(device.macAddress, ignoreCase = true)
                    }
                    if (scannedDev != null) {
                        fanBleManager.connect(scannedDev)
                    } else {
                        fanBleManager.startScan()
                    }
                }
                TransportMode.ESP32_BRIDGE.name -> {
                    repository.setTransportMode(TransportMode.ESP32_BRIDGE)
                    val scannedDev = esp32BleManager.scannedDevices.value.firstOrNull {
                        it.address.equals(device.macAddress, ignoreCase = true)
                    }
                    if (scannedDev != null) {
                        esp32BleManager.connect(scannedDev)
                    } else {
                        esp32BleManager.startScan()
                    }
                }
                else -> {
                    repository.setTransportMode(TransportMode.PHONE_IR)
                }
            }
        }
    }

    fun disconnectDevice() {
        fanBleManager.disconnect()
        esp32BleManager.disconnect()
    }

    fun setActiveDevice(deviceId: Long) {
        viewModelScope.launch {
            deviceRepository.setActiveDevice(deviceId)
            val dev = deviceRepository.getDeviceById(deviceId)
            if (dev != null) {
                connectDevice(dev)
            }
        }
    }

    fun openAddDialogForScanned(mac: String, name: String, transport: TransportMode) {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                addDialogMac = mac,
                addDialogDefaultName = name,
                addDialogTransport = transport
            )
        }
    }

    fun dismissAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun saveNewDevice(name: String, roomName: String, mac: String, transportType: TransportMode) {
        viewModelScope.launch {
            val newDevice = FanDevice(
                name = name.ifEmpty { "Atomberg Fan" },
                roomName = roomName.ifEmpty { "Living Room" },
                macAddress = mac,
                transportType = transportType.name,
                isDefault = false,
                lastConnected = System.currentTimeMillis()
            )
            val id = deviceRepository.addOrUpdateDevice(newDevice)
            dismissAddDialog()
            setActiveDevice(id)
        }
    }

    fun openEditDialog(device: FanDevice) {
        _uiState.update { it.copy(editingDevice = device) }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(editingDevice = null) }
    }

    fun saveDeviceDetails(deviceId: Long, name: String, roomName: String) {
        viewModelScope.launch {
            deviceRepository.updateDeviceDetails(deviceId, name, roomName)
            dismissEditDialog()
        }
    }

    fun openDeleteConfirm(device: FanDevice) {
        _uiState.update { it.copy(deviceToDelete = device) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(deviceToDelete = null) }
    }

    fun confirmDeleteDevice() {
        val dev = uiState.value.deviceToDelete ?: return
        viewModelScope.launch {
            deviceRepository.removeDevice(dev.id)
            dismissDeleteConfirm()
        }
    }

    class Factory(
        private val application: Application,
        private val repository: CodeRepository,
        private val deviceRepository: DeviceRepository,
        private val fanBleManager: FanBleManager,
        private val esp32BleManager: Esp32BleManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeviceManagementViewModel(
                application,
                repository,
                deviceRepository,
                fanBleManager,
                esp32BleManager
            ) as T
        }
    }
}

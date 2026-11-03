package com.example.atomfan.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import com.example.atomfan.model.BleConnectionState
import com.example.atomfan.model.BleLogEntry
import com.example.atomfan.model.FanState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ============================================================================
 * ATOMBERG BLE DECODER & MANAGER
 * ============================================================================
 * Easy Editing Guide for BLE Decoding:
 * To modify how Atomberg 4-byte BLE notification payloads are parsed, edit the
 * [decodeFanState] function below.
 *
 * Atomberg BLE Service / Characteristic UUID:
 *   e29ee02c-af3d-11ec-b909-0242ac120002
 * Client Characteristic Configuration Descriptor (CCCD):
 *   00002902-0000-1000-8000-00805f9b34fb
 * ============================================================================
 */
@SuppressLint("MissingPermission")
class FanBleManager(private val context: Context) {

    companion object {
        val FAN_SERVICE_UUID: UUID = UUID.fromString("e29ee02c-af3d-11ec-b909-0242ac120002")
        val FAN_CHAR_UUID: UUID = UUID.fromString("e29ee02c-af3d-11ec-b909-0242ac120002")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        const val MAX_RECONNECT_ATTEMPTS = 5
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var bluetoothGatt: BluetoothGatt? = null
    private var targetCharacteristic: BluetoothGattCharacteristic? = null
    private var lastConnectedDevice: BluetoothDevice? = null

    private var isUserDisconnect = false
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _targetMac = MutableStateFlow<String?>(null)
    val targetMac: StateFlow<String?> = _targetMac.asStateFlow()

    private val _reconnectAttemptCount = MutableStateFlow(0)
    val reconnectAttemptCount: StateFlow<Int> = _reconnectAttemptCount.asStateFlow()

    private val _fanState = MutableStateFlow(FanState())
    val fanState: StateFlow<FanState> = _fanState.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices.asStateFlow()

    private val _scannedDeviceRssiMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val scannedDeviceRssiMap: StateFlow<Map<String, Int>> = _scannedDeviceRssiMap.asStateFlow()

    private val _lastWriteGattStatus = MutableStateFlow<Int?>(null)
    val lastWriteGattStatus: StateFlow<Int?> = _lastWriteGattStatus.asStateFlow()

    val connectedDevice: BluetoothDevice?
        get() = bluetoothGatt?.device ?: lastConnectedDevice

    private val _logs = MutableStateFlow<List<BleLogEntry>>(emptyList())
    val logs: StateFlow<List<BleLogEntry>> = _logs.asStateFlow()

    fun log(message: String, gattStatus: Int? = null, isError: Boolean = false) {
        val entry = BleLogEntry(
            message = message,
            gattStatus = gattStatus,
            isError = isError
        )
        _logs.value = _logs.value + entry
    }

    fun exportLogs(): String {
        return _logs.value.joinToString("\n") { entry ->
            val statusStr = entry.gattStatus?.let { " [GATT Status: $it]" } ?: ""
            val errStr = if (entry.isError) " [ERROR]" else ""
            "[${entry.formattedTime}]$errStr$statusStr ${entry.message}"
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            log("BLE Scanner unavailable.", isError = true)
            return
        }
        _scannedDevices.value = emptyList()
        _isScanning.value = true
        log("Started scanning for Atomberg BLE devices...")

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            // Broad scan so Atomberg devices broadcasting local name without service UUID in the advertising packet are captured
            scanner.startScan(emptyList(), settings, scanCallback)
        } catch (e: Exception) {
            try {
                scanner.startScan(scanCallback)
            } catch (ex: Exception) {
                log("Failed to start BLE scan: ${ex.message}", isError = true)
                _isScanning.value = false
            }
        }
    }

    fun stopScan() {
        if (!_isScanning.value) return
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            log("Error stopping scan: ${e.message}", isError = true)
        } finally {
            _isScanning.value = false
            log("Stopped BLE scan.")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: result.scanRecord?.deviceName ?: ""
            val isAtomberg = name.contains("Atomberg", ignoreCase = true) || hasFanService(result)

            if (isAtomberg) {
                _scannedDeviceRssiMap.value = _scannedDeviceRssiMap.value + (device.address to result.rssi)
                val current = _scannedDevices.value
                if (current.none { it.address.equals(device.address, ignoreCase = true) }) {
                    log("Found Atomberg device: ${name.ifEmpty { "Unknown" }} (${device.address}) [RSSI: ${result.rssi} dBm]")
                    _scannedDevices.value = current + device
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            log("BLE Scan failed with code $errorCode", gattStatus = errorCode, isError = true)
            _isScanning.value = false
        }
    }

    private fun hasFanService(result: ScanResult): Boolean {
        val uuids = result.scanRecord?.serviceUuids ?: return false
        return uuids.any { it.uuid == FAN_SERVICE_UUID }
    }

    /**
     * Connect directly by MAC address without scanning first.
     */
    fun connect(macAddress: String) {
        if (macAddress.isBlank() || !BluetoothAdapter.checkBluetoothAddress(macAddress)) {
            log("Cannot connect: Invalid MAC address format ($macAddress).", isError = true)
            return
        }
        val adapter = bluetoothAdapter
        if (adapter == null) {
            log("Cannot connect: BluetoothAdapter is null.", isError = true)
            return
        }
        val device = try {
            adapter.getRemoteDevice(macAddress)
        } catch (e: Exception) {
            log("Failed to resolve BluetoothDevice for $macAddress: ${e.message}", isError = true)
            null
        }
        if (device != null) {
            connect(device)
        }
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        reconnectJob?.cancel()
        reconnectJob = null

        // If switching from another connected device, close it first
        if (bluetoothGatt != null && !bluetoothGatt?.device?.address.equals(device.address, ignoreCase = true)) {
            log("Switching connection from ${bluetoothGatt?.device?.address} to ${device.address}...")
            closeGatt()
        }

        isUserDisconnect = false
        reconnectAttempt = 0
        _reconnectAttemptCount.value = 0
        lastConnectedDevice = device
        _targetMac.value = device.address
        _connectionState.value = BleConnectionState.CONNECTING

        log("Connecting to BLE device: ${device.name ?: device.address}...")
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        isUserDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempt = 0
        _reconnectAttemptCount.value = 0
        _targetMac.value = null
        _connectionState.value = BleConnectionState.DISCONNECTED

        log("Disconnecting from device by user request...")
        closeGatt()
    }

    private fun closeGatt() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            // ignore
        } finally {
            bluetoothGatt = null
            targetCharacteristic = null
            _isConnected.value = false
            _connectedDeviceName.value = null
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            log("GATT ConnectionStateChange: status=$status, newState=$newState")
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                _isConnected.value = true
                _connectionState.value = BleConnectionState.CONNECTED
                _connectedDeviceName.value = gatt.device.name ?: gatt.device.address
                _targetMac.value = gatt.device.address
                reconnectAttempt = 0
                _reconnectAttemptCount.value = 0
                log("Successfully connected to ${gatt.device.address}. Discovering services...")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _isConnected.value = false
                _connectedDeviceName.value = null
                targetCharacteristic = null
                log("Disconnected from GATT server (status $status).", gattStatus = status, isError = status != 0)

                if (!isUserDisconnect && lastConnectedDevice != null) {
                    scheduleAutoReconnect()
                } else {
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    closeGatt()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            log("Services discovered status: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                var charFound: BluetoothGattCharacteristic? = null
                for (service in gatt.services) {
                    val c = service.getCharacteristic(FAN_CHAR_UUID)
                    if (c != null) {
                        charFound = c
                        break
                    }
                }

                if (charFound != null) {
                    targetCharacteristic = charFound
                    log("Found Fan Characteristic: ${charFound.uuid}")
                    enableNotifications(gatt, charFound)
                } else {
                    log("Fan characteristic $FAN_CHAR_UUID not found in discovered services.", isError = true)
                }
            } else {
                log("Failed to discover services. GATT status: $status", gattStatus = status, isError = true)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val bytes = characteristic.value ?: ByteArray(0)
                handleIncomingBytes(bytes, "Read")
            } else {
                log("Characteristic read failed. Status: $status", gattStatus = status, isError = true)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleIncomingBytes(value, "Read")
            } else {
                log("Characteristic read failed. Status: $status", gattStatus = status, isError = true)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val bytes = characteristic.value ?: ByteArray(0)
            handleIncomingBytes(bytes, "Notification")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingBytes(value, "Notification")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            _lastWriteGattStatus.value = status
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log("Experimental write succeeded. GATT status: $status", gattStatus = status)
            } else {
                log("Experimental write failed. GATT status: $status", gattStatus = status, isError = true)
            }
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val success = gatt.setCharacteristicNotification(characteristic, true)
        log("setCharacteristicNotification result: $success")

        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
            log("Enabling CCCD notifications...")
        } else {
            log("CCCD descriptor not found on characteristic.", isError = true)
        }
    }

    fun refreshState() {
        val gatt = bluetoothGatt
        val char = targetCharacteristic
        if (gatt == null || char == null || !_isConnected.value) {
            log("Cannot refresh: device not connected or characteristic missing.", isError = true)
            return
        }
        log("Requesting characteristic read...")
        gatt.readCharacteristic(char)
    }

    /**
     * Sends an experimental hex payload to the connected fan.
     *
     * Feature 12 (Command Isolation):
     * If [targetMacAddress] is specified, verifies that the active GATT connection
     * matches [targetMacAddress]. If not matched, prevents transmission and returns failure.
     */
    fun writeExperimentalHexString(hexString: String, targetMacAddress: String? = null): Result<Unit> {
        val gatt = bluetoothGatt
        val char = targetCharacteristic
        val device = gatt?.device ?: lastConnectedDevice

        if (gatt == null || char == null || !_isConnected.value) {
            val err = "Cannot write: Fan BLE device is not connected."
            log(err, isError = true)
            return Result.failure(IllegalStateException(err))
        }

        // Multi-fan command isolation check
        if (targetMacAddress != null && !device?.address.equals(targetMacAddress, ignoreCase = true)) {
            val err = "Command rejected: Intended for fan ($targetMacAddress) but currently connected to (${device?.address})."
            log(err, isError = true)
            return Result.failure(IllegalStateException(err))
        }

        if (device != null && device.bondState == BluetoothDevice.BOND_NONE) {
            log("Device is unbonded. Requesting bond creation...")
            device.createBond()
        }

        val cleanHex = hexString.replace(" ", "").replace("0x", "")
        val bytes = hexToByteArray(cleanHex)
        log("Writing experimental payload ($hexString) to [${device?.address}] [${bytes.size} bytes]...")

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val res = gatt.writeCharacteristic(char, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                log("writeCharacteristic API 33+ status code: $res")
            } else {
                @Suppress("DEPRECATION")
                char.value = bytes
                @Suppress("DEPRECATION")
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                val res = gatt.writeCharacteristic(char)
                log("writeCharacteristic status code: $res")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            log("Write exception: ${e.message}", isError = true)
            Result.failure(e)
        }
    }

    private fun handleIncomingBytes(bytes: ByteArray, source: String) {
        val hexStr = bytes.joinToString("") { "%02X".format(it) }
        log("Received $source payload ($hexStr) [${bytes.size} bytes]")

        if (bytes.size >= 4) {
            val decodedState = decodeFanState(bytes)
            _fanState.value = decodedState
            log("Decoded FanState: power=${decodedState.powerOn}, speed=${decodedState.speed}, boost=${decodedState.boost}, led=${decodedState.led}, sleep=${decodedState.sleep}, timer=${decodedState.timer}h")
        } else {
            log("Payload size < 4 bytes, raw hex: $hexStr")
        }
    }

    /**
     * ============================================================================
     * ATOMBERG BLE STATUS PAYLOAD DECODER
     * ============================================================================
     * Decodes 4-byte notifications into [FanState]:
     *   byte0 & 0x07 = speed 1..5 (6 when boost on)
     *   byte0 & 0x10 = power on
     *   byte0 & 0x20 = LED
     *   byte0 & 0x40 = boost
     *   byte0 & 0x80 = sleep
     *   byte1 = 0xF8 when on, 0x00 when off
     *   byte2 = timer (0 = off)
     *   byte3 = unknown
     * ============================================================================
     */
    fun decodeFanState(bytes: ByteArray): FanState {
        if (bytes.size < 4) {
            return FanState(rawBytesHex = bytes.joinToString("") { "%02X".format(it) })
        }
        val b0 = bytes[0].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF

        val speed = b0 and 0x07
        val powerOn = (b0 and 0x10) != 0
        val led = (b0 and 0x20) != 0
        val boost = (b0 and 0x40) != 0
        val sleep = (b0 and 0x80) != 0
        val timer = b2

        val rawHex = bytes.joinToString("") { "%02X".format(it) }

        return FanState(
            powerOn = powerOn,
            speed = speed,
            led = led,
            boost = boost,
            sleep = sleep,
            timer = timer,
            rawBytesHex = rawHex,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }

    private fun scheduleAutoReconnect() {
        if (isUserDisconnect || _targetMac.value == null) {
            _connectionState.value = BleConnectionState.DISCONNECTED
            return
        }

        if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
            log("Max auto-reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached. Halting auto-reconnect.", isError = true)
            _connectionState.value = BleConnectionState.DISCONNECTED
            closeGatt()
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempt++
            _reconnectAttemptCount.value = reconnectAttempt
            _connectionState.value = BleConnectionState.RECONNECTING

            val backoffMs = (1000L * (1 shl (reconnectAttempt.coerceAtMost(5)))).coerceAtMost(30000L)
            log("Scheduling auto-reconnect attempt #$reconnectAttempt in ${backoffMs / 1000} seconds...")
            delay(backoffMs)

            val device = lastConnectedDevice
            if (device != null && !_isConnected.value && !isUserDisconnect) {
                log("Executing auto-reconnect attempt #$reconnectAttempt to ${device.address}...")
                bluetoothGatt = device.connectGatt(context, false, gattCallback)
            }
        }
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            result[i] = hex.substring(index, index + 2).toInt(16).toByte()
        }
        return result
    }
}

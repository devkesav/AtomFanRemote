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
import com.example.atomfan.model.BleLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
class Esp32BleManager(private val context: Context) {

    companion object {
        val UART_SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val RX_CHAR_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        val TX_CHAR_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        const val DEFAULT_MTU = 23
        const val REQUESTED_MTU = 247
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val lineParser = Esp32LineParser()

    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    private var currentMtu = DEFAULT_MTU

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices.asStateFlow()

    private val _responses = MutableSharedFlow<Esp32Response>(extraBufferCapacity = 64)
    val responses: SharedFlow<Esp32Response> = _responses.asSharedFlow()

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

    fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            log("BLE Scanner unavailable.", isError = true)
            return
        }
        _scannedDevices.value = emptyList()
        _isScanning.value = true
        log("Started scanning for ESP32 Bridge devices...")

        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(UART_SERVICE_UUID)).build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(filters, settings, scanCallback)
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
            if (name.contains("AtomFan-Bridge", ignoreCase = true) || hasUartService(result)) {
                val current = _scannedDevices.value
                if (current.none { it.address == device.address }) {
                    log("Found ESP32 Bridge: ${name.ifEmpty { "Unknown" }} (${device.address})")
                    _scannedDevices.value = current + device
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            log("BLE Scan failed with code $errorCode", gattStatus = errorCode, isError = true)
            _isScanning.value = false
        }
    }

    private fun hasUartService(result: ScanResult): Boolean {
        val uuids = result.scanRecord?.serviceUuids ?: return false
        return uuids.any { it.uuid == UART_SERVICE_UUID }
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        log("Connecting to ESP32 Bridge: ${device.name ?: device.address}...")
        closeGatt()
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        log("Disconnecting from ESP32 Bridge...")
        bluetoothGatt?.disconnect()
    }

    private fun closeGatt() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            // ignore
        } finally {
            bluetoothGatt = null
            rxCharacteristic = null
            txCharacteristic = null
            currentMtu = DEFAULT_MTU
            _isConnected.value = false
            _connectedDeviceName.value = null
            lineParser.clearBuffer()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            log("ESP32 GATT ConnectionStateChange: status=$status, newState=$newState")
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                _isConnected.value = true
                _connectedDeviceName.value = gatt.device.name ?: gatt.device.address
                log("Connected to ESP32 Bridge. Requesting MTU $REQUESTED_MTU...")
                gatt.requestMtu(REQUESTED_MTU)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _isConnected.value = false
                _connectedDeviceName.value = null
                log("Disconnected from ESP32 Bridge.", gattStatus = status, isError = status != 0)
                closeGatt()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                currentMtu = mtu
                log("MTU changed to $mtu")
            } else {
                log("MTU request failed with status $status. Using default MTU $currentMtu", gattStatus = status)
            }
            log("Discovering services...")
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(UART_SERVICE_UUID)
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(RX_CHAR_UUID)
                    txCharacteristic = service.getCharacteristic(TX_CHAR_UUID)
                    log("Discovered UART service. RX: ${rxCharacteristic != null}, TX: ${txCharacteristic != null}")

                    val tx = txCharacteristic
                    if (tx != null) {
                        enableTxNotifications(gatt, tx)
                    }
                } else {
                    log("UART Service $UART_SERVICE_UUID not found.", isError = true)
                }
            } else {
                log("Failed to discover services. GATT status $status", gattStatus = status, isError = true)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val bytes = characteristic.value ?: ByteArray(0)
            handleIncomingBytes(bytes)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingBytes(value)
        }
    }

    private fun enableTxNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
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
            log("Enabled TX notifications.")
        }
    }

    private fun handleIncomingBytes(bytes: ByteArray) {
        val chunk = String(bytes, Charsets.UTF_8)
        log("Received TX chunk: ${chunk.replace("\n", "\\n").replace("\r", "\\r")}")
        val parsedResponses = lineParser.appendChunk(chunk)
        for (resp in parsedResponses) {
            log("Parsed ESP32 Response: $resp")
            scope.launch {
                _responses.emit(resp)
            }
        }
    }

    fun sendLine(command: String) {
        val gatt = bluetoothGatt
        val rx = rxCharacteristic
        if (gatt == null || rx == null || !_isConnected.value) {
            log("Cannot send: ESP32 Bridge not connected.", isError = true)
            return
        }

        val fullCmd = if (command.endsWith("\n")) command else "$command\n"
        val bytes = fullCmd.toByteArray(Charsets.UTF_8)
        val maxPayloadSize = (currentMtu - 3).coerceAtLeast(20)

        scope.launch {
            log("Sending line to ESP32: ${fullCmd.trim()}")
            var offset = 0
            while (offset < bytes.size) {
                val chunkSize = (bytes.size - offset).coerceAtMost(maxPayloadSize)
                val chunk = bytes.copyOfRange(offset, offset + chunkSize)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(rx, chunk, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                } else {
                    @Suppress("DEPRECATION")
                    rx.value = chunk
                    @Suppress("DEPRECATION")
                    rx.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(rx)
                }

                offset += chunkSize
                if (offset < bytes.size) {
                    delay(20)
                }
            }
        }
    }

    // Protocol helper methods
    fun ping() {
        sendLine("PING")
    }

    fun sendNec(address: Int, command: Int) {
        sendLine("SEND NEC $address $command")
    }

    fun sendRaw(frequency: Int, timings: List<Int>) {
        val timingsStr = timings.joinToString(",")
        sendLine("SEND RAW $frequency $timingsStr")
    }

    fun learn() {
        sendLine("LEARN")
    }

    fun cancel() {
        sendLine("CANCEL")
    }
}

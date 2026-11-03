package com.example.atomfan.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.atomfan.ble.Esp32BleManager
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.data.DeviceRepository
import com.example.atomfan.database.FanDevice
import com.example.atomfan.model.FanState
import com.example.atomfan.model.RemoteButton
import com.example.atomfan.model.TransportMode
import com.example.atomfan.transport.Esp32Transport
import com.example.atomfan.transport.FanBleTransport
import com.example.atomfan.transport.PhoneIrTransport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FanPreset(
    val displayName: String,
    val targetSpeed: Int,
    val boost: Boolean,
    val sleep: Boolean,
    val description: String
) {
    SLEEP("Sleep", 1, boost = false, sleep = true, "Ultra-quiet night operation"),
    ECO("Eco", 2, boost = false, sleep = false, "Optimized BLDC energy saver"),
    NORMAL("Normal", 3, boost = false, sleep = false, "Balanced everyday airflow"),
    TURBO("Turbo", 5, boost = true, sleep = false, "Maximum airflow & speed")
}

data class SmartDashboardUiState(
    val fanState: FanState = FanState(),
    val activeMode: TransportMode = TransportMode.PHONE_IR,
    val isConnected: Boolean = false,
    val connectionStatusText: String = "Disconnected",
    val activeDevice: FanDevice? = null,
    val pairedDevices: List<FanDevice> = emptyList(),
    val batteryStatus: String = "Direct AC Mains Powered (220V/110V)",
    val batteryLevel: Int? = null, // null when AC mains powered / not exposed by device
    val lastSyncTimestamp: String = "Not synced yet",
    val activePreset: FanPreset? = null,
    val estimatedPowerWatts: Double = 0.0,
    val statusMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isNaturalBreezeSupported: Boolean = false, // Atomberg NEC IR and BLE protocol do not support native breeze mode
    val repeatCount: Int = 1,
    val isExperimentalBleWrite: Boolean = false,
    val hasIrEmitter: Boolean = false
)

class SmartDashboardViewModel(
    application: Application,
    private val repository: CodeRepository,
    private val deviceRepository: DeviceRepository,
    private val fanBleManager: FanBleManager,
    private val esp32BleManager: Esp32BleManager,
    private val phoneIrTransport: PhoneIrTransport
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SmartDashboardUiState())
    val uiState: StateFlow<SmartDashboardUiState> = _uiState.asStateFlow()

    private val esp32Transport = Esp32Transport(esp32BleManager)
    private val fanBleTransport = FanBleTransport(fanBleManager) {
        kotlinx.coroutines.runBlocking {
            runCatching { repository.experimentalBleWriteFlow.first() }.getOrDefault(false)
        }
    }

    init {
        observeState()
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                repository.transportModeFlow,
                fanBleManager.fanState,
                fanBleManager.isConnected,
                esp32BleManager.isConnected,
                deviceRepository.activeDevice,
                deviceRepository.pairedDevices,
                repository.repeatCountFlow,
                repository.experimentalBleWriteFlow
            ) { args: Array<Any?> ->
                val mode = args[0] as TransportMode
                val fanState = args[1] as FanState
                val fanBleConn = args[2] as Boolean
                val esp32Conn = args[3] as Boolean
                val activeDev = args[4] as? FanDevice
                @Suppress("UNCHECKED_CAST")
                val pairedDevs = (args[5] as? List<FanDevice>) ?: emptyList()
                val repeatCount = (args[6] as? Int) ?: 1
                val isExpBle = (args[7] as? Boolean) ?: false

                val hasIr = phoneIrTransport.hasIrEmitter()
                val isConn = when (mode) {
                    TransportMode.FAN_BLE -> fanBleConn
                    TransportMode.ESP32_BRIDGE -> esp32Conn
                    TransportMode.PHONE_IR -> hasIr
                }

                val connText = when (mode) {
                    TransportMode.FAN_BLE -> if (fanBleConn) "BLE Connected (${activeDev?.name ?: "Fan"})" else "BLE Disconnected"
                    TransportMode.ESP32_BRIDGE -> if (esp32Conn) "Bridge Connected" else "Bridge Disconnected"
                    TransportMode.PHONE_IR -> if (hasIr) "Phone IR Emitter Ready" else "No IR Emitter"
                }

                val watts = calculatePowerWatts(fanState)
                val preset = determinePreset(fanState)
                val formattedTime = if (fanState.lastUpdatedTimestamp > 0) {
                    SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(fanState.lastUpdatedTimestamp))
                } else {
                    "Just now"
                }

                _uiState.value.copy(
                    fanState = fanState,
                    activeMode = mode,
                    isConnected = isConn,
                    connectionStatusText = connText,
                    activeDevice = activeDev,
                    pairedDevices = pairedDevs,
                    batteryStatus = "Direct AC Mains Powered (220V/110V)",
                    batteryLevel = null, // AC powered ceiling fan does not expose battery
                    lastSyncTimestamp = formattedTime,
                    activePreset = preset,
                    estimatedPowerWatts = watts,
                    repeatCount = repeatCount,
                    isExperimentalBleWrite = isExpBle,
                    hasIrEmitter = hasIr
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun calculatePowerWatts(state: FanState): Double {
        if (!state.powerOn) return 0.0
        return when {
            state.boost -> 32.0
            state.speed <= 1 -> 6.0
            state.speed == 2 -> 10.0
            state.speed == 3 -> 16.0
            state.speed == 4 -> 24.0
            else -> 30.0
        }
    }

    private fun determinePreset(state: FanState): FanPreset? {
        if (!state.powerOn) return null
        return when {
            state.boost -> FanPreset.TURBO
            state.sleep && state.speed == 1 -> FanPreset.SLEEP
            !state.sleep && !state.boost && state.speed == 2 -> FanPreset.ECO
            !state.sleep && !state.boost && state.speed == 3 -> FanPreset.NORMAL
            else -> null
        }
    }

    fun togglePower() {
        val currentState = uiState.value.fanState
        sendButtonCommand(RemoteButton.POWER)
        val newPower = !currentState.powerOn
        val updatedState = currentState.copy(
            powerOn = newPower,
            speed = if (newPower && currentState.speed == 0) 3 else currentState.speed,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        updateFanStateManually(updatedState)
    }

    fun setSpeed(speed: Int) {
        val targetButton = when (speed) {
            1 -> RemoteButton.SPEED_1
            2 -> RemoteButton.SPEED_2
            3 -> RemoteButton.SPEED_3
            4 -> RemoteButton.SPEED_4
            5 -> RemoteButton.SPEED_5
            6 -> RemoteButton.BOOST
            else -> RemoteButton.SPEED_3
        }
        sendButtonCommand(targetButton)
        val updatedState = uiState.value.fanState.copy(
            powerOn = true,
            speed = speed.coerceIn(1, 5),
            boost = speed >= 6,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        updateFanStateManually(updatedState)
    }

    fun applyPreset(preset: FanPreset) {
        viewModelScope.launch {
            val button = when (preset) {
                FanPreset.ECO -> RemoteButton.SPEED_2
                FanPreset.TURBO -> RemoteButton.BOOST
                FanPreset.SLEEP -> RemoteButton.SLEEP
                FanPreset.NORMAL -> RemoteButton.SPEED_3
            }
            sendButtonCommand(button)
            val updatedState = uiState.value.fanState.copy(
                powerOn = true,
                speed = preset.targetSpeed,
                boost = preset.boost,
                sleep = preset.sleep,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            updateFanStateManually(updatedState)
        }
    }

    fun toggleBoost() {
        sendButtonCommand(RemoteButton.BOOST)
        val currentState = uiState.value.fanState
        val updatedState = currentState.copy(
            powerOn = true,
            boost = !currentState.boost,
            speed = if (!currentState.boost) 6 else 5,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        updateFanStateManually(updatedState)
    }

    fun toggleSleep() {
        sendButtonCommand(RemoteButton.SLEEP)
        val currentState = uiState.value.fanState
        val updatedState = currentState.copy(
            powerOn = true,
            sleep = !currentState.sleep,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        updateFanStateManually(updatedState)
    }

    fun toggleLed() {
        sendButtonCommand(RemoteButton.LED)
        val currentState = uiState.value.fanState
        val updatedState = currentState.copy(
            led = !currentState.led,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        updateFanStateManually(updatedState)
    }

    fun setTimer(hours: Int) {
        sendButtonCommand(RemoteButton.TIMER)
        val currentState = uiState.value.fanState
        val updatedState = currentState.copy(
            timer = hours,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        updateFanStateManually(updatedState)
    }

    fun selectActiveDevice(deviceId: Long) {
        viewModelScope.launch {
            deviceRepository.setActiveDevice(deviceId)
            val dev = deviceRepository.getDeviceById(deviceId)
            if (dev != null && dev.macAddress.isNotBlank()) {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter != null && BluetoothAdapter.checkBluetoothAddress(dev.macAddress)) {
                    try {
                        val remoteDevice = bluetoothAdapter.getRemoteDevice(dev.macAddress)
                        fanBleManager.connect(remoteDevice)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
    }

    fun setTransportMode(mode: TransportMode) {
        viewModelScope.launch {
            repository.setTransportMode(mode)
        }
    }

    fun setRepeatCount(count: Int) {
        viewModelScope.launch {
            repository.setRepeatCount(count)
        }
    }

    fun setExperimentalBleWrite(enabled: Boolean) {
        viewModelScope.launch {
            repository.setExperimentalBleWrite(enabled)
        }
    }

    fun refreshState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            if (uiState.value.activeMode == TransportMode.FAN_BLE) {
                fanBleManager.refreshState()
            }
            delay(500)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    lastSyncTimestamp = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
                )
            }
        }
    }

    private fun updateFanStateManually(newState: FanState) {
        _uiState.update {
            it.copy(
                fanState = newState,
                estimatedPowerWatts = calculatePowerWatts(newState),
                activePreset = determinePreset(newState),
                lastSyncTimestamp = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            )
        }
        viewModelScope.launch {
            val dev = uiState.value.activeDevice
            if (dev != null) {
                deviceRepository.logUsage(
                    deviceId = dev.id,
                    speed = newState.speed,
                    powerWatts = calculatePowerWatts(newState),
                    mode = uiState.value.activeMode.name,
                    durationSeconds = 60
                )
            }
        }
    }

    private fun sendButtonCommand(button: RemoteButton) {
        viewModelScope.launch {
            val mode = repository.transportModeFlow.first()
            val irCode = repository.getIrCode(button)
            val repeats = repository.repeatCountFlow.first()

            repeat(repeats) {
                when (mode) {
                    TransportMode.PHONE_IR -> phoneIrTransport.send(irCode)
                    TransportMode.ESP32_BRIDGE -> esp32Transport.send(irCode)
                    TransportMode.FAN_BLE -> fanBleTransport.send(irCode)
                }
            }
        }
    }

    class Factory(
        private val application: Application,
        private val repository: CodeRepository,
        private val deviceRepository: DeviceRepository,
        private val fanBleManager: FanBleManager,
        private val esp32BleManager: Esp32BleManager,
        private val phoneIrTransport: PhoneIrTransport
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SmartDashboardViewModel(
                application,
                repository,
                deviceRepository,
                fanBleManager,
                esp32BleManager,
                phoneIrTransport
            ) as T
        }
    }
}

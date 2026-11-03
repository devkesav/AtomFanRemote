package com.example.atomfan.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.ConsumerIrManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.atomfan.ble.Esp32BleManager
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.model.FanState
import com.example.atomfan.model.IrCode
import com.example.atomfan.model.RemoteButton
import com.example.atomfan.model.TransportMode
import com.example.atomfan.transport.Esp32Transport
import com.example.atomfan.transport.FanBleTransport
import com.example.atomfan.transport.FanTransport
import com.example.atomfan.transport.PhoneIrTransport
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class MainUiState(
    val activeMode: TransportMode = TransportMode.PHONE_IR,
    val fanState: FanState = FanState(),
    val transportStatusMessage: String = "",
    val verificationResult: String? = null,
    val repeatCount: Int = 1,
    val hasIrEmitter: Boolean = true,
    val isExperimentalBleWrite: Boolean = false,
    val isFanBleConnected: Boolean = false,
    val fanBleDeviceName: String? = null,
    val isEsp32Connected: Boolean = false,
    val esp32DeviceName: String? = null,
    val customCodesMap: Map<RemoteButton, IrCode> = emptyMap(),
    val availableModes: List<TransportMode> = TransportMode.entries,
    val showTimerDialog: Boolean = false,
    val showSleepDialog: Boolean = false,
    val timerMinutes: Int = 30,
    val sleepStartTime: String = "22:00",
    val sleepEndTime: String = "06:00",
    val sleepDays: Set<String> = setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
)

class MainViewModel(
    application: Application,
    private val repository: CodeRepository,
    private val fanBleManager: FanBleManager,
    private val esp32BleManager: Esp32BleManager,
    private val phoneIrTransport: PhoneIrTransport
) : AndroidViewModel(application) {

    private val fanBleTransport = FanBleTransport(fanBleManager) {
        _uiState.value.isExperimentalBleWrite
    }
    private val esp32Transport = Esp32Transport(esp32BleManager)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkIrEmitter()
        observeRepository()
        observeBleManagers()
    }

    fun checkIrEmitter() {
        val context = getApplication<Application>()
        val irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        val hasIr = irManager?.hasIrEmitter() == true
        _uiState.update { current ->
            val actualMode = if (!hasIr && current.activeMode == TransportMode.PHONE_IR) {
                TransportMode.FAN_BLE
            } else {
                current.activeMode
            }
            current.copy(
                hasIrEmitter = hasIr,
                activeMode = actualMode,
                transportStatusMessage = computeStatusMessage(
                    actualMode,
                    hasIr,
                    current.isFanBleConnected,
                    current.fanBleDeviceName,
                    current.isEsp32Connected,
                    current.esp32DeviceName
                )
            )
        }
    }

    private fun observeRepository() {
        viewModelScope.launch {
            repository.transportModeFlow.collect { mode ->
                _uiState.update { current ->
                    val actualMode = if (!current.hasIrEmitter && mode == TransportMode.PHONE_IR) {
                        TransportMode.FAN_BLE
                    } else {
                        mode
                    }
                    current.copy(
                        activeMode = actualMode,
                        transportStatusMessage = computeStatusMessage(
                            actualMode,
                            current.hasIrEmitter,
                            current.isFanBleConnected,
                            current.fanBleDeviceName,
                            current.isEsp32Connected,
                            current.esp32DeviceName
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.repeatCountFlow.collect { count ->
                phoneIrTransport.setRepeatCount(count)
                _uiState.update { it.copy(repeatCount = count) }
            }
        }

        viewModelScope.launch {
            repository.experimentalBleWriteFlow.collect { enabled ->
                _uiState.update { it.copy(isExperimentalBleWrite = enabled) }
            }
        }

        viewModelScope.launch {
            repository.irCodesFlow.collect { codesMap ->
                _uiState.update { it.copy(customCodesMap = codesMap) }
            }
        }
    }

    private fun observeBleManagers() {
        viewModelScope.launch {
            fanBleManager.fanState.collect { state ->
                _uiState.update { it.copy(fanState = state) }
            }
        }

        viewModelScope.launch {
            fanBleManager.isConnected.collect { isConnected ->
                val name = fanBleManager.connectedDeviceName.value
                _uiState.update { current ->
                    current.copy(
                        isFanBleConnected = isConnected,
                        fanBleDeviceName = name,
                        transportStatusMessage = computeStatusMessage(
                            current.activeMode,
                            current.hasIrEmitter,
                            isConnected,
                            name,
                            current.isEsp32Connected,
                            current.esp32DeviceName
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            esp32BleManager.isConnected.collect { isConnected ->
                val name = esp32BleManager.connectedDeviceName.value
                _uiState.update { current ->
                    current.copy(
                        isEsp32Connected = isConnected,
                        esp32DeviceName = name,
                        transportStatusMessage = computeStatusMessage(
                            current.activeMode,
                            current.hasIrEmitter,
                            current.isFanBleConnected,
                            current.fanBleDeviceName,
                            isConnected,
                            name
                        )
                    )
                }
            }
        }
    }

    fun setTransportMode(mode: TransportMode) {
        if (mode == TransportMode.PHONE_IR && !_uiState.value.hasIrEmitter) {
            return
        }
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

    fun setTimerDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showTimerDialog = visible) }
    }

    fun setSleepDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showSleepDialog = visible) }
    }

    fun setTimerMinutes(minutes: Int) {
        _uiState.update { it.copy(timerMinutes = minutes) }
    }

    fun setSleepSchedule(startTime: String, endTime: String, days: Set<String>) {
        _uiState.update { it.copy(sleepStartTime = startTime, sleepEndTime = endTime, sleepDays = days) }
    }

    fun onRemoteButtonClicked(button: RemoteButton) {
        when (button) {
            RemoteButton.TIMER -> {
                sendButtonCode(button)
                setTimerDialogVisible(true)
            }
            RemoteButton.SLEEP -> {
                sendButtonCode(button)
                setSleepDialogVisible(true)
            }
            RemoteButton.LED -> {
                sendButtonCode(button)
                _uiState.update { current ->
                    current.copy(fanState = current.fanState.copy(led = !current.fanState.led))
                }
            }
            RemoteButton.POWER -> {
                sendButtonCode(button)
                _uiState.update { current ->
                    current.copy(fanState = current.fanState.copy(powerOn = !current.fanState.powerOn))
                }
            }
            else -> {
                sendButtonCode(button)
            }
        }
    }

    fun sendButtonCode(button: RemoteButton) {
        viewModelScope.launch {
            val code = repository.getIrCode(button)
            val currentMode = _uiState.value.activeMode
            val isModeBConnected = fanBleManager.isConnected.value

            var verificationDeferred: Deferred<Boolean>? = null

            if (isModeBConnected && (currentMode == TransportMode.PHONE_IR || currentMode == TransportMode.ESP32_BRIDGE)) {
                _uiState.update { it.copy(verificationResult = "verifying...") }

                verificationDeferred = async {
                    val updated = withTimeoutOrNull(1000L) {
                        fanBleManager.fanState.drop(1).first()
                        true
                    }
                    updated == true
                }
            }

            val transport = getTransport(currentMode)
            val result = transport.send(code)

            if (result.isFailure) {
                verificationDeferred?.cancel()
                _uiState.update { it.copy(verificationResult = "no response") }
            } else if (verificationDeferred != null) {
                val verified = verificationDeferred.await()
                _uiState.update {
                    it.copy(verificationResult = if (verified) "fan responded" else "no response")
                }
            } else {
                if (currentMode == TransportMode.FAN_BLE) {
                    if (result.isSuccess) {
                        _uiState.update { it.copy(verificationResult = "fan responded") }
                    } else {
                        _uiState.update { it.copy(verificationResult = "no response") }
                    }
                } else {
                    _uiState.update { it.copy(verificationResult = null) }
                }
            }
        }
    }

    fun getTransport(mode: TransportMode): FanTransport {
        return when (mode) {
            TransportMode.PHONE_IR -> phoneIrTransport
            TransportMode.FAN_BLE -> fanBleTransport
            TransportMode.ESP32_BRIDGE -> esp32Transport
        }
    }

    private fun computeStatusMessage(
        mode: TransportMode,
        hasIr: Boolean,
        isFanBleConnected: Boolean,
        fanDeviceName: String?,
        isEsp32Connected: Boolean,
        esp32DeviceName: String?
    ): String {
        return when (mode) {
            TransportMode.PHONE_IR -> {
                if (hasIr) "Phone IR Ready" else "No IR Emitter"
            }
            TransportMode.FAN_BLE -> {
                if (isFanBleConnected) {
                    "Fan BLE Connected (${fanDeviceName ?: "Atomberg Fan"})"
                } else {
                    "Fan BLE Disconnected"
                }
            }
            TransportMode.ESP32_BRIDGE -> {
                if (isEsp32Connected) {
                    "ESP32 Bridge Connected (${esp32DeviceName ?: "AtomFan Bridge"})"
                } else {
                    "ESP32 Bridge Disconnected"
                }
            }
        }
    }

    class Factory(
        private val application: Application,
        private val repository: CodeRepository,
        private val fanBleManager: FanBleManager,
        private val esp32BleManager: Esp32BleManager,
        private val phoneIrTransport: PhoneIrTransport
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(
                    application,
                    repository,
                    fanBleManager,
                    esp32BleManager,
                    phoneIrTransport
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

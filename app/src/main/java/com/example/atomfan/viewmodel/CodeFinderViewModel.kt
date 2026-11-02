package com.example.atomfan.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.atomfan.ble.FanBleManager
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.model.FanState
import com.example.atomfan.model.IrCode
import com.example.atomfan.model.TransportMode
import com.example.atomfan.transport.Esp32Transport
import com.example.atomfan.transport.FanTransport
import com.example.atomfan.transport.PhoneIrTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class CodeHit(
    val address: Int,
    val command: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class CodeFinderUiState(
    val addressStartText: String = "0",
    val addressEndText: String = "0",
    val isFixedAddress: Boolean = true,
    val commandStartText: String = "0",
    val commandEndText: String = "255",
    val intervalMs: Long = 200L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val currentAddress: Int = 0,
    val currentCommand: Int = 0,
    val currentIndex: Int = 0,
    val totalCount: Int = 256,
    val sweepStatus: String = "Idle",
    val hits: List<CodeHit> = emptyList(),
    val isFanBleConnected: Boolean = false,
    val activeMode: TransportMode = TransportMode.PHONE_IR
)

class CodeFinderViewModel(
    application: Application,
    private val fanBleManager: FanBleManager,
    private val phoneIrTransport: PhoneIrTransport,
    private val esp32Transport: Esp32Transport,
    private val repository: CodeRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CodeFinderUiState())
    val uiState: StateFlow<CodeFinderUiState> = _uiState.asStateFlow()

    private var sweepJob: Job? = null

    init {
        viewModelScope.launch {
            fanBleManager.isConnected.collect { connected ->
                _uiState.update { it.copy(isFanBleConnected = connected) }
            }
        }
        viewModelScope.launch {
            repository.transportModeFlow.collect { mode ->
                _uiState.update { it.copy(activeMode = mode) }
            }
        }
    }

    fun updateAddressStart(text: String) {
        _uiState.update { it.copy(addressStartText = text) }
    }

    fun updateAddressEnd(text: String) {
        _uiState.update { it.copy(addressEndText = text) }
    }

    fun updateIsFixedAddress(isFixed: Boolean) {
        _uiState.update { it.copy(isFixedAddress = isFixed) }
    }

    fun updateCommandStart(text: String) {
        _uiState.update { it.copy(commandStartText = text) }
    }

    fun updateCommandEnd(text: String) {
        _uiState.update { it.copy(commandEndText = text) }
    }

    fun updateIntervalMs(interval: Long) {
        _uiState.update { it.copy(intervalMs = interval) }
    }

    fun startSweep() {
        if (_uiState.value.isRunning && !_uiState.value.isPaused) return

        if (_uiState.value.isPaused) {
            _uiState.update { it.copy(isRunning = true, isPaused = false, sweepStatus = "Resumed...") }
            return
        }

        val addrStart = _uiState.value.addressStartText.toIntOrNull() ?: 0
        val addrEnd = if (_uiState.value.isFixedAddress) addrStart else (_uiState.value.addressEndText.toIntOrNull() ?: addrStart)
        val cmdStart = _uiState.value.commandStartText.toIntOrNull() ?: 0
        val cmdEnd = _uiState.value.commandEndText.toIntOrNull() ?: 255
        val interval = _uiState.value.intervalMs

        val addresses = if (addrStart <= addrEnd) (addrStart..addrEnd).toList() else listOf(addrStart)
        val commands = if (cmdStart <= cmdEnd) (cmdStart..cmdEnd).toList() else listOf(cmdStart)

        val total = addresses.size * commands.size
        if (total <= 0) return

        _uiState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                currentIndex = 0,
                totalCount = total,
                sweepStatus = "Sweep running..."
            )
        }

        sweepJob?.cancel()
        sweepJob = viewModelScope.launch {
            val mode = _uiState.value.activeMode
            val transport = getTransport(mode)
            var index = 0

            for (addr in addresses) {
                for (cmd in commands) {
                    while (_uiState.value.isPaused) {
                        delay(100)
                        if (!_uiState.value.isRunning) break
                    }
                    if (!_uiState.value.isRunning) break

                    _uiState.update {
                        it.copy(
                            currentAddress = addr,
                            currentCommand = cmd,
                            currentIndex = index + 1,
                            sweepStatus = "Trying Address: 0x%02X ($addr), Command: 0x%02X ($cmd)".format(addr, cmd)
                        )
                    }

                    // Check response via Fan BLE if connected
                    val isConnected = fanBleManager.isConnected.value
                    var stateChanged = false

                    val irCode = IrCode(
                        protocol = "NEC",
                        address = addr,
                        command = cmd,
                        frequency = 38000
                    )

                    if (isConnected) {
                        val responseDeferred = viewModelScope.launch {
                            val updated = withTimeoutOrNull(400L) {
                                fanBleManager.fanState.drop(1).first()
                                true
                            }
                            if (updated == true) {
                                stateChanged = true
                            }
                        }
                        transport.send(irCode)
                        responseDeferred.join()
                    } else {
                        transport.send(irCode)
                    }

                    if (stateChanged || (isConnected && fanBleManager.fanState.value != FanState())) {
                        val hit = CodeHit(addr, cmd)
                        if (_uiState.value.hits.none { it.address == addr && it.command == cmd }) {
                            _uiState.update { state ->
                                state.copy(hits = listOf(hit) + state.hits)
                            }
                        }
                    }

                    index++
                    delay(interval)
                }
            }

            _uiState.update {
                it.copy(
                    isRunning = false,
                    isPaused = false,
                    sweepStatus = "Sweep completed. Total hits: ${it.hits.size}"
                )
            }
        }
    }

    fun pauseSweep() {
        _uiState.update { it.copy(isPaused = true, sweepStatus = "Paused") }
    }

    fun stopSweep() {
        sweepJob?.cancel()
        sweepJob = null
        _uiState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                sweepStatus = "Sweep stopped."
            )
        }
    }

    fun clearHits() {
        _uiState.update { it.copy(hits = emptyList()) }
    }

    private fun getTransport(mode: TransportMode): FanTransport {
        return when (mode) {
            TransportMode.PHONE_IR -> phoneIrTransport
            TransportMode.ESP32_BRIDGE -> esp32Transport
            else -> phoneIrTransport
        }
    }

    class Factory(
        private val application: Application,
        private val fanBleManager: FanBleManager,
        private val phoneIrTransport: PhoneIrTransport,
        private val esp32Transport: Esp32Transport,
        private val repository: CodeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CodeFinderViewModel::class.java)) {
                return CodeFinderViewModel(
                    application,
                    fanBleManager,
                    phoneIrTransport,
                    esp32Transport,
                    repository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

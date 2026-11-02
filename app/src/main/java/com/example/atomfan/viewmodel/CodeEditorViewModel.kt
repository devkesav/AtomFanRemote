package com.example.atomfan.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.model.IrCode
import com.example.atomfan.model.RemoteButton
import com.example.atomfan.model.TransportMode
import com.example.atomfan.transport.Esp32Transport
import com.example.atomfan.transport.FanBleTransport
import com.example.atomfan.transport.FanTransport
import com.example.atomfan.transport.PhoneIrTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CodeEditorUiState(
    val selectedButton: RemoteButton = RemoteButton.POWER,
    val protocol: String = "NEC",
    val addressText: String = "0",
    val commandText: String = "0",
    val frequencyText: String = "38000",
    val rawTimingsText: String = "",
    val commentText: String = "",
    val activeMode: TransportMode = TransportMode.PHONE_IR,
    val statusMessage: String = "",
    val isSaving: Boolean = false
)

class CodeEditorViewModel(
    application: Application,
    private val repository: CodeRepository,
    private val phoneIrTransport: PhoneIrTransport,
    private val esp32Transport: Esp32Transport,
    private val fanBleTransport: FanBleTransport
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CodeEditorUiState())
    val uiState: StateFlow<CodeEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.transportModeFlow.collect { mode ->
                _uiState.update { it.copy(activeMode = mode) }
            }
        }
        loadCodeForButton(RemoteButton.POWER)
    }

    fun selectButton(button: RemoteButton) {
        loadCodeForButton(button)
    }

    private fun loadCodeForButton(button: RemoteButton) {
        viewModelScope.launch {
            val codesMap = repository.irCodesFlow.first()
            val code = codesMap[button] ?: repository.getIrCode(button)
            _uiState.update {
                it.copy(
                    selectedButton = button,
                    protocol = code.protocol,
                    addressText = formatNumber(code.address),
                    commandText = formatNumber(code.command),
                    frequencyText = code.frequency.toString(),
                    rawTimingsText = code.rawTimings.joinToString(", "),
                    commentText = code.comment,
                    statusMessage = "Loaded ${button.displayName}"
                )
            }
        }
    }

    private fun formatNumber(value: Int): String {
        return value.toString()
    }

    fun updateProtocol(protocol: String) {
        _uiState.update { it.copy(protocol = protocol) }
    }

    fun updateAddress(address: String) {
        _uiState.update { it.copy(addressText = address) }
    }

    fun updateCommand(command: String) {
        _uiState.update { it.copy(commandText = command) }
    }

    fun updateFrequency(frequency: String) {
        _uiState.update { it.copy(frequencyText = frequency) }
    }

    fun updateRawTimings(timings: String) {
        _uiState.update { it.copy(rawTimingsText = timings) }
    }

    fun updateComment(comment: String) {
        _uiState.update { it.copy(commentText = comment) }
    }

    private fun parseNumber(text: String): Int {
        val trimmed = text.trim()
        return if (trimmed.startsWith("0x", ignoreCase = true)) {
            trimmed.substring(2).toIntOrNull(16) ?: 0
        } else {
            trimmed.toIntOrNull() ?: 0
        }
    }

    private fun parseTimings(text: String): List<Int> {
        if (text.isBlank()) return emptyList()
        return text.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
    }

    fun buildCurrentIrCode(): IrCode {
        val state = _uiState.value
        return IrCode(
            button = state.selectedButton,
            protocol = state.protocol.ifBlank { "NEC" },
            address = parseNumber(state.addressText),
            command = parseNumber(state.commandText),
            frequency = state.frequencyText.trim().toIntOrNull() ?: 38000,
            rawTimings = parseTimings(state.rawTimingsText),
            comment = state.commentText
        )
    }

    fun testCode() {
        viewModelScope.launch {
            val code = buildCurrentIrCode()
            val mode = _uiState.value.activeMode
            val transport = getTransport(mode)
            val result = transport.send(code)
            _uiState.update {
                it.copy(
                    statusMessage = if (result.isSuccess) {
                        "Test transmission successful (${mode.displayName})"
                    } else {
                        "Test transmission failed: ${result.exceptionOrNull()?.message}"
                    }
                )
            }
        }
    }

    fun saveCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val code = buildCurrentIrCode()
            repository.saveIrCode(code.button, code)
            _uiState.update {
                it.copy(
                    isSaving = false,
                    statusMessage = "Saved ${code.button.displayName} successfully"
                )
            }
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            repository.resetToDefaultCodes()
            loadCodeForButton(_uiState.value.selectedButton)
            _uiState.update { it.copy(statusMessage = "Reset codes to default") }
        }
    }

    private fun getTransport(mode: TransportMode): FanTransport {
        return when (mode) {
            TransportMode.PHONE_IR -> phoneIrTransport
            TransportMode.ESP32_BRIDGE -> esp32Transport
            TransportMode.FAN_BLE -> fanBleTransport
        }
    }

    class Factory(
        private val application: Application,
        private val repository: CodeRepository,
        private val phoneIrTransport: PhoneIrTransport,
        private val esp32Transport: Esp32Transport,
        private val fanBleTransport: FanBleTransport
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CodeEditorViewModel::class.java)) {
                return CodeEditorViewModel(
                    application,
                    repository,
                    phoneIrTransport,
                    esp32Transport,
                    fanBleTransport
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

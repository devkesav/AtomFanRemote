package com.example.atomfan.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.atomfan.model.RemoteButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceCommandResult {
    data class ActionButton(val button: RemoteButton, val description: String) : VoiceCommandResult()
    data class ActionNavigation(val destination: String, val description: String) : VoiceCommandResult()
    data class ActionEco(val description: String) : VoiceCommandResult()
    data class ActionSleep(val description: String) : VoiceCommandResult()
    data class Unrecognized(val rawText: String) : VoiceCommandResult()
}

class VoiceControlManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    private val _commandResult = MutableStateFlow<VoiceCommandResult?>(null)
    val commandResult: StateFlow<VoiceCommandResult?> = _commandResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun isSpeechAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        if (!isSpeechAvailable()) {
            _errorMessage.value = "Speech recognition is not available on this device."
            return
        }

        stopListening()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _spokenText.value = "Listening..."
                    _errorMessage.value = null
                    _commandResult.value = null
                }

                override fun onBeginningOfSpeech() {
                    _spokenText.value = "Listening..."
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _errorMessage.value = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timed out."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                        else -> "Speech recognition error (Code: $error)"
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull() ?: ""
                    _spokenText.value = spoken
                    if (spoken.isNotBlank()) {
                        val parsed = parseCommand(spoken)
                        _commandResult.value = parsed
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull() ?: ""
                    if (partial.isNotBlank()) {
                        _spokenText.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening(intent)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        } finally {
            speechRecognizer = null
            _isListening.value = false
        }
    }

    fun parseCommand(input: String): VoiceCommandResult {
        val lower = input.lowercase(Locale.getDefault())

        return when {
            lower.contains("turn on") || lower.contains("power on") || lower.contains("start fan") -> {
                VoiceCommandResult.ActionButton(RemoteButton.POWER, "Turned Fan ON")
            }
            lower.contains("turn off") || lower.contains("power off") || lower.contains("stop fan") -> {
                VoiceCommandResult.ActionButton(RemoteButton.POWER, "Turned Fan OFF")
            }
            lower.contains("speed 1") || lower.contains("speed one") || lower.contains("set speed to 1") -> {
                VoiceCommandResult.ActionButton(RemoteButton.SPEED_1, "Set Fan Speed to 1")
            }
            lower.contains("speed 2") || lower.contains("speed two") || lower.contains("set speed to 2") -> {
                VoiceCommandResult.ActionButton(RemoteButton.SPEED_2, "Set Fan Speed to 2")
            }
            lower.contains("speed 3") || lower.contains("speed three") || lower.contains("set speed to 3") -> {
                VoiceCommandResult.ActionButton(RemoteButton.SPEED_3, "Set Fan Speed to 3")
            }
            lower.contains("speed 4") || lower.contains("speed four") || lower.contains("set speed to 4") -> {
                VoiceCommandResult.ActionButton(RemoteButton.SPEED_4, "Set Fan Speed to 4")
            }
            lower.contains("speed 5") || lower.contains("speed five") || lower.contains("set speed to 5") -> {
                VoiceCommandResult.ActionButton(RemoteButton.SPEED_5, "Set Fan Speed to 5")
            }
            lower.contains("boost") || lower.contains("maximum speed") -> {
                VoiceCommandResult.ActionButton(RemoteButton.BOOST, "Activated Boost Mode")
            }
            lower.contains("eco") || lower.contains("eco mode") || lower.contains("save energy") -> {
                VoiceCommandResult.ActionEco("Activated Eco Recommendation (Speed 3)")
            }
            lower.contains("sleep") || lower.contains("night mode") -> {
                VoiceCommandResult.ActionSleep("Activated Smart Sleep Mode")
            }
            lower.contains("today usage") || lower.contains("show usage") || lower.contains("energy") -> {
                VoiceCommandResult.ActionNavigation("ENERGY_DASHBOARD", "Navigating to Energy Analytics")
            }
            lower.contains("led") || lower.contains("light") -> {
                VoiceCommandResult.ActionButton(RemoteButton.LED, "Toggled Fan LED Light")
            }
            lower.contains("timer") -> {
                VoiceCommandResult.ActionButton(RemoteButton.TIMER, "Triggered Fan Timer")
            }
            else -> {
                VoiceCommandResult.Unrecognized(input)
            }
        }
    }
}

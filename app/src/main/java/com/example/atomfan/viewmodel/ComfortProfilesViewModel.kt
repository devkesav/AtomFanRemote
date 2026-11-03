package com.example.atomfan.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.atomfan.data.CodeRepository
import com.example.atomfan.data.DeviceRepository
import com.example.atomfan.database.ComfortProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpeedRecommendation(
    val speed: Int,
    val reason: String
)

data class ComfortProfilesUiState(
    val profiles: List<ComfortProfile> = emptyList(),
    val showAddDialog: Boolean = false,
    val editingProfile: ComfortProfile? = null,
    val sleepDurationMinutes: Int = 480,
    val autoShutdownMinutes: Int = 0,
    val statusMessage: String? = null
)

class ComfortProfilesViewModel(
    application: Application,
    private val deviceRepository: DeviceRepository,
    private val repository: CodeRepository,
    private val smartDashboardViewModel: SmartDashboardViewModel
) : AndroidViewModel(application) {

    val profiles: StateFlow<List<ComfortProfile>> = deviceRepository.getComfortProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ComfortProfilesUiState())
    val uiState: StateFlow<ComfortProfilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profiles.collect { profileList ->
                _uiState.update { it.copy(profiles = profileList) }
            }
        }
        viewModelScope.launch {
            repository.sleepDurationMinutesFlow.collect { duration ->
                _uiState.update { it.copy(sleepDurationMinutes = duration) }
            }
        }
        viewModelScope.launch {
            repository.autoShutdownMinutesFlow.collect { shutdown ->
                _uiState.update { it.copy(autoShutdownMinutes = shutdown) }
            }
        }
    }

    fun openAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, editingProfile = null) }
    }

    fun openEditDialog(profile: ComfortProfile) {
        _uiState.update { it.copy(showAddDialog = true, editingProfile = profile) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingProfile = null) }
    }

    fun saveProfile(name: String, daytimeSpeed: Int, nighttimeSpeed: Int, preferredMode: String) {
        viewModelScope.launch {
            val existing = _uiState.value.editingProfile
            val profile = if (existing != null) {
                existing.copy(
                    name = name,
                    daytimeSpeed = daytimeSpeed,
                    nighttimeSpeed = nighttimeSpeed,
                    preferredMode = preferredMode
                )
            } else {
                ComfortProfile(
                    name = name,
                    daytimeSpeed = daytimeSpeed,
                    nighttimeSpeed = nighttimeSpeed,
                    preferredMode = preferredMode
                )
            }
            deviceRepository.saveComfortProfile(profile)
            _uiState.update {
                it.copy(
                    showAddDialog = false,
                    editingProfile = null,
                    statusMessage = if (existing != null) "Profile updated" else "Profile saved"
                )
            }
        }
    }

    fun deleteProfile(profile: ComfortProfile) {
        viewModelScope.launch {
            deviceRepository.deleteComfortProfile(profile)
            _uiState.update { it.copy(statusMessage = "Profile deleted") }
        }
    }

    fun setSleepDuration(minutes: Int) {
        viewModelScope.launch {
            repository.setSleepDurationMinutes(minutes)
            _uiState.update { it.copy(sleepDurationMinutes = minutes) }
        }
    }

    fun setAutoShutdownMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.setAutoShutdownMinutes(minutes)
            _uiState.update { it.copy(autoShutdownMinutes = minutes) }
        }
    }

    /**
     * Generate a speed recommendation based on manual temperature input (no sensor).
     * Always clarifies that temperature input is manual, never claims sensor readings.
     */
    fun generateRecommendation(manualTempCelsius: Float?, isNighttime: Boolean): SpeedRecommendation {
        if (manualTempCelsius == null) {
            return SpeedRecommendation(speed = 3, reason = "Manual selection – no sensor available")
        }

        val baseSpeed = when {
            manualTempCelsius >= 32f -> 5
            manualTempCelsius >= 28f -> 4
            manualTempCelsius >= 24f -> 3
            manualTempCelsius >= 20f -> 2
            else -> 1
        }

        val baseReason = when {
            manualTempCelsius >= 32f -> "High temperature (manual input)"
            manualTempCelsius >= 28f -> "Warm temperature (manual input)"
            manualTempCelsius >= 24f -> "Comfortable temperature (manual input)"
            manualTempCelsius >= 20f -> "Mild temperature (manual input)"
            else -> "Cool temperature (manual input)"
        }

        // If nighttime, cap at the active profile's nighttime speed
        if (isNighttime) {
            val activeProfile = _uiState.value.profiles.firstOrNull()
            if (activeProfile != null) {
                val maxNightSpeed = activeProfile.nighttimeSpeed
                val finalSpeed = minOf(baseSpeed, maxNightSpeed)
                return SpeedRecommendation(
                    speed = finalSpeed,
                    reason = "$baseReason · Capped at Speed $maxNightSpeed (${activeProfile.name} nighttime limit)"
                )
            }
        }

        return SpeedRecommendation(speed = baseSpeed, reason = baseReason)
    }

    fun applyRecommendation(speed: Int) {
        smartDashboardViewModel.setSpeed(speed)
        _uiState.update { it.copy(statusMessage = "Applied Speed $speed") }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    class Factory(
        private val application: Application,
        private val deviceRepository: DeviceRepository,
        private val repository: CodeRepository,
        private val smartDashboardViewModel: SmartDashboardViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ComfortProfilesViewModel(
                application,
                deviceRepository,
                repository,
                smartDashboardViewModel
            ) as T
        }
    }
}

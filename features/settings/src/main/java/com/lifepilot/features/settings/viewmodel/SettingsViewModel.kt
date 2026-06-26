package com.lifepilot.features.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.model.Profile
import com.lifepilot.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val activeProfile: Profile? = null,
    val profiles: List<Profile> = emptyList(),
    val showCreateProfile: Boolean = false,
    val newProfileName: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeProfiles()
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            profileRepository.observeProfiles()
                .catch { e ->
                    Timber.e(e, "Error loading profiles")
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { profiles ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profiles = profiles,
                            activeProfile = profiles.find { p -> p.isPrimary },
                        )
                    }
                }
        }
    }

    fun onNewProfileNameChange(name: String) {
        _uiState.update { it.copy(newProfileName = name) }
    }

    fun showCreateProfile() {
        _uiState.update { it.copy(showCreateProfile = true) }
    }

    fun hideCreateProfile() {
        _uiState.update { it.copy(showCreateProfile = false, newProfileName = "") }
    }

    fun createProfile() {
        val name = _uiState.value.newProfileName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val isFirst = _uiState.value.profiles.isEmpty()
                profileRepository.createProfile(name, isFirst)
            }.onFailure {
                Timber.e(it, "Failed to create profile")
            }
            _uiState.update { it.copy(showCreateProfile = false, newProfileName = "") }
        }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            runCatching { profileRepository.setActiveProfile(profileId) }
                .onFailure { Timber.e(it, "Failed to switch profile") }
        }
    }
}

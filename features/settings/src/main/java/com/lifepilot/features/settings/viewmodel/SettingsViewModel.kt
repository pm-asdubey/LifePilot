package com.lifepilot.features.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.model.Profile
import com.lifepilot.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    val aiProvider: String = "",
    val aiApiKey: String = "",
    val aiModel: String = "",
    val showAiConfig: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferenceManager: PreferenceManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeState()
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                profileRepository.observeProfiles(),
                preferenceManager.aiProvider,
                preferenceManager.aiApiKey,
                preferenceManager.aiModel,
            ) { profiles, provider, apiKey, model ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profiles = profiles,
                        activeProfile = profiles.find { p -> p.isPrimary },
                        aiProvider = provider ?: "",
                        aiApiKey = apiKey ?: "",
                        aiModel = model ?: "",
                    )
                }
            }
                .catch { e -> Timber.e(e, "Error loading settings") }
                .collect {}
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

    fun showAiConfig() {
        _uiState.update { it.copy(showAiConfig = true) }
    }

    fun hideAiConfig() {
        _uiState.update { it.copy(showAiConfig = false) }
    }

    fun onAiProviderChange(provider: String) {
        _uiState.update { it.copy(aiProvider = provider) }
    }

    fun onAiApiKeyChange(key: String) {
        _uiState.update { it.copy(aiApiKey = key) }
    }

    fun onAiModelChange(model: String) {
        _uiState.update { it.copy(aiModel = model) }
    }

    fun saveAiConfig() {
        val state = _uiState.value
        viewModelScope.launch {
            runCatching {
                preferenceManager.setAiProvider(state.aiProvider)
                preferenceManager.setAiApiKey(state.aiApiKey)
                preferenceManager.setAiModel(state.aiModel)
            }.onFailure { Timber.e(it, "Failed to save AI config") }
            _uiState.update { it.copy(showAiConfig = false) }
        }
    }

    fun clearAiConfig() {
        viewModelScope.launch {
            runCatching { preferenceManager.clearAiConfig() }
                .onFailure { Timber.e(it, "Failed to clear AI config") }
            _uiState.update { it.copy(aiProvider = "", aiApiKey = "", aiModel = "") }
        }
    }
}

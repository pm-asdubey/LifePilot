package com.lifepilot.features.settings.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.model.Profile
import com.lifepilot.domain.model.UpdateStatus
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.UpdateRepository
import com.lifepilot.domain.usecase.ExportBundle
import com.lifepilot.domain.usecase.ExportDataUseCase
import com.lifepilot.domain.usecase.ImportDataUseCase
import com.lifepilot.domain.usecase.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val isExporting: Boolean = false,
    val exportResult: ExportBundle? = null,
    val exportShareUri: Uri? = null,
    val exportError: String? = null,
    val isImporting: Boolean = false,
    val importResult: ImportResult? = null,
    val importError: String? = null,
    val biometricLockEnabled: Boolean = false,
    val profileToEdit: Profile? = null,
    val editProfileName: String = "",
    val profileToDelete: Profile? = null,
    val updateStatus: UpdateStatus = UpdateStatus.Unknown,
    val isCheckingUpdate: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val preferenceManager: PreferenceManager,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val updateRepository: UpdateRepository,
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
                Triple(profiles, Triple(provider, apiKey, model), null)
            }
                .catch { e -> Timber.e(e, "Error loading settings") }
                .collect { (profiles, aiConfig, _) ->
                    val (provider, apiKey, model) = aiConfig
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
        }

        viewModelScope.launch {
            preferenceManager.biometricLockEnabled
                .catch { e -> Timber.e(e, "Error observing biometric setting") }
                .collect { enabled ->
                    _uiState.update { it.copy(biometricLockEnabled = enabled) }
                }
        }

        viewModelScope.launch {
            updateRepository.observeUpdateStatus()
                .catch { e -> Timber.w(e, "Error observing update status") }
                .collect { status ->
                    _uiState.update { it.copy(updateStatus = status) }
                }
        }
    }

    fun checkForUpdate() {
        if (_uiState.value.isCheckingUpdate) return
        _uiState.update { it.copy(isCheckingUpdate = true) }
        viewModelScope.launch {
            runCatching { updateRepository.checkForUpdate() }
                .onFailure { Timber.w(it, "Manual update check failed") }
            _uiState.update { it.copy(isCheckingUpdate = false) }
        }
    }

    fun toggleBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { preferenceManager.setBiometricLockEnabled(enabled) }
                .onFailure { Timber.e(it, "Failed to toggle biometric lock") }
        }
    }

    fun showEditProfile(profile: Profile) {
        _uiState.update { it.copy(profileToEdit = profile, editProfileName = profile.displayName) }
    }

    fun hideEditProfile() {
        _uiState.update { it.copy(profileToEdit = null, editProfileName = "") }
    }

    fun onEditProfileNameChange(name: String) {
        _uiState.update { it.copy(editProfileName = name) }
    }

    fun saveProfileEdit() {
        val profile = _uiState.value.profileToEdit ?: return
        val newName = _uiState.value.editProfileName.trim()
        if (newName.isBlank()) return
        viewModelScope.launch {
            runCatching {
                profileRepository.updateProfile(profile.copy(displayName = newName))
            }.onFailure { Timber.e(it, "Failed to update profile") }
            _uiState.update { it.copy(profileToEdit = null, editProfileName = "") }
        }
    }

    fun showDeleteProfile(profile: Profile) {
        _uiState.update { it.copy(profileToDelete = profile) }
    }

    fun hideDeleteProfile() {
        _uiState.update { it.copy(profileToDelete = null) }
    }

    fun deleteProfile() {
        val profile = _uiState.value.profileToDelete ?: return
        viewModelScope.launch {
            runCatching { profileRepository.deleteProfile(profile.profileId) }
                .onFailure { Timber.e(it, "Failed to delete profile") }
            _uiState.update { it.copy(profileToDelete = null) }
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

    fun exportData() {
        val profileId = _uiState.value.activeProfile?.profileId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportError = null, exportShareUri = null) }
            exportDataUseCase(profileId)
                .onSuccess { bundle ->
                    val uri = writeExportFile(bundle)
                    _uiState.update {
                        it.copy(isExporting = false, exportResult = bundle, exportShareUri = uri)
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Export failed")
                    _uiState.update { it.copy(isExporting = false, exportError = e.message) }
                }
        }
    }

    private fun writeExportFile(bundle: ExportBundle): Uri? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "lifepilot_export_$timestamp.json")
            file.writeText(bundle.jsonPayload)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Timber.e(e, "Failed to write export file")
            null
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null, exportError = null, exportShareUri = null) }
    }

    fun importData(uri: Uri) {
        val profileId = _uiState.value.activeProfile?.profileId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importError = null, importResult = null) }
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: error("Could not read import file")
                importDataUseCase(content, profileId)
            }
                .mapCatching { it.getOrThrow() }
                .onSuccess { result ->
                    _uiState.update { it.copy(isImporting = false, importResult = result) }
                }
                .onFailure { e ->
                    Timber.e(e, "Import failed")
                    _uiState.update { it.copy(isImporting = false, importError = e.message) }
                }
        }
    }

    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null, importError = null) }
    }
}

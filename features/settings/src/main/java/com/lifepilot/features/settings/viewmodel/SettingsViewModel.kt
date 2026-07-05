package com.lifepilot.features.settings.viewmodel

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.model.Profile
import com.lifepilot.domain.model.UpdateInfo
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
    val themeMode: String = "system",
    val profileToEdit: Profile? = null,
    val editProfileName: String = "",
    val profileToDelete: Profile? = null,
    val updateStatus: UpdateStatus = UpdateStatus.Unknown,
    val isCheckingUpdate: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val downloadError: String? = null,
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

        viewModelScope.launch {
            preferenceManager.themeMode
                .catch { e -> Timber.w(e, "Error observing theme mode") }
                .collect { mode ->
                    _uiState.update { it.copy(themeMode = mode) }
                }
        }
    }

    /** mode: "light", "dark", or "system". */
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            runCatching { preferenceManager.setThemeMode(mode) }
                .onFailure { Timber.e(it, "Failed to set theme mode") }
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

    /**
     * Downloads the APK from [info.apkDownloadUrl] via DownloadManager and opens the
     * system package installer when complete. Falls back to opening the release page in
     * a browser if no direct APK URL is available.
     *
     * On Android 8+, if "Install unknown apps" has not been granted for this app, opens
     * the system settings page so the user can grant it — they then need to tap again.
     */
    fun downloadAndInstall(info: UpdateInfo) {
        val apkUrl = info.apkDownloadUrl
        if (apkUrl == null) {
            // No direct APK link — open the release page in browser as a fallback.
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return
        }

        // Android 8+: check "Install unknown apps" permission for this package source.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            _uiState.update {
                it.copy(downloadError = "Allow 'Install unknown apps' in Settings, then tap again.")
            }
            return
        }

        if (_uiState.value.isDownloadingUpdate) return
        _uiState.update { it.copy(isDownloadingUpdate = true, downloadError = null) }

        viewModelScope.launch {
            runCatching {
                val fileName = "LifePilot-update-${info.latestVersion}.apk"
                val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
                    setTitle("LifePilot ${info.latestVersion}")
                    setDescription("Downloading update…")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                    setMimeType("application/vnd.android.package-archive")
                }

                val dm = context.getSystemService<DownloadManager>()!!
                val downloadId = dm.enqueue(request)

                // Poll until the download completes or fails.
                var status: Int
                var localUri: String? = null
                while (true) {
                    val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
                    if (cursor == null || !cursor.moveToFirst()) {
                        cursor?.close()
                        break
                    }
                    status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    cursor.close()

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> break
                        DownloadManager.STATUS_FAILED -> {
                            localUri = null
                            break
                        }
                        else -> kotlinx.coroutines.delay(500)
                    }
                }

                if (localUri != null) {
                    val file = java.io.File(Uri.parse(localUri).path!!)
                    val apkUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } else {
                    _uiState.update { it.copy(downloadError = "Download failed. Tap to try again.") }
                }
            }.onFailure { e ->
                Timber.w(e, "downloadAndInstall: failed")
                _uiState.update { it.copy(downloadError = "Download failed: ${e.message}") }
            }
            _uiState.update { it.copy(isDownloadingUpdate = false) }
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

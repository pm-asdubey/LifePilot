package com.lifepilot.features.settings.viewmodel

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.model.Profile
import com.lifepilot.domain.model.UpdateStatus
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.UpdateRepository
import com.lifepilot.domain.usecase.ExportDataUseCase
import com.lifepilot.domain.usecase.ImportDataUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val context = mockk<Context>(relaxed = true)
    private val profileRepository = mockk<ProfileRepository>(relaxed = true)
    private val preferenceManager = mockk<PreferenceManager>(relaxed = true)
    private val exportDataUseCase = mockk<ExportDataUseCase>(relaxed = true)
    private val importDataUseCase = mockk<ImportDataUseCase>(relaxed = true)
    private val updateRepository = mockk<UpdateRepository>(relaxed = true)

    private val primaryProfile = Profile(
        profileId = "profile-1",
        displayName = "Ashutosh",
        avatarPath = null,
        isPrimary = true,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
    private val secondProfile = Profile(
        profileId = "profile-2",
        displayName = "Shambhavi",
        avatarPath = null,
        isPrimary = false,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { profileRepository.observeProfiles() } returns flowOf(listOf(primaryProfile, secondProfile))
        every { preferenceManager.aiProvider } returns flowOf("nvidia")
        every { preferenceManager.aiApiKey } returns flowOf("key-123")
        every { preferenceManager.aiModel } returns flowOf("meta/llama-3.3-70b-instruct")
        every { preferenceManager.biometricLockEnabled } returns flowOf(false)
        every { updateRepository.observeUpdateStatus() } returns flowOf(UpdateStatus.Unknown)
        viewModel = SettingsViewModel(
            context, profileRepository, preferenceManager,
            exportDataUseCase, importDataUseCase, updateRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Profile list ──────────────────────────────────────────────────────────

    @Test
    fun `profiles loaded from repository appear in state`() {
        assertThat(viewModel.uiState.value.profiles).hasSize(2)
    }

    @Test
    fun `active profile is the primary profile`() {
        assertThat(viewModel.uiState.value.activeProfile?.profileId).isEqualTo("profile-1")
    }

    @Test
    fun `isLoading is false after state arrives`() {
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    // ── AI config from preferences ────────────────────────────────────────────

    @Test
    fun `ai config loaded from preferences on init`() {
        val state = viewModel.uiState.value
        assertThat(state.aiProvider).isEqualTo("nvidia")
        assertThat(state.aiApiKey).isEqualTo("key-123")
        assertThat(state.aiModel).isEqualTo("meta/llama-3.3-70b-instruct")
    }

    @Test
    fun `onAiProviderChange updates aiProvider in state`() {
        viewModel.onAiProviderChange("anthropic")
        assertThat(viewModel.uiState.value.aiProvider).isEqualTo("anthropic")
    }

    @Test
    fun `onAiApiKeyChange updates aiApiKey in state`() {
        viewModel.onAiApiKeyChange("new-key")
        assertThat(viewModel.uiState.value.aiApiKey).isEqualTo("new-key")
    }

    @Test
    fun `onAiModelChange updates aiModel in state`() {
        viewModel.onAiModelChange("claude-opus-4-8")
        assertThat(viewModel.uiState.value.aiModel).isEqualTo("claude-opus-4-8")
    }

    @Test
    fun `saveAiConfig persists all three fields to preferenceManager`() = runTest {
        viewModel.onAiProviderChange("anthropic")
        viewModel.onAiApiKeyChange("sk-ant-123")
        viewModel.onAiModelChange("claude-opus-4-8")
        viewModel.saveAiConfig()
        coVerify { preferenceManager.setAiProvider("anthropic") }
        coVerify { preferenceManager.setAiApiKey("sk-ant-123") }
        coVerify { preferenceManager.setAiModel("claude-opus-4-8") }
    }

    @Test
    fun `saveAiConfig hides AI config dialog`() = runTest {
        viewModel.showAiConfig()
        assertThat(viewModel.uiState.value.showAiConfig).isTrue()
        viewModel.saveAiConfig()
        assertThat(viewModel.uiState.value.showAiConfig).isFalse()
    }

    @Test
    fun `clearAiConfig clears all ai fields in state`() = runTest {
        viewModel.clearAiConfig()
        val state = viewModel.uiState.value
        assertThat(state.aiProvider).isEmpty()
        assertThat(state.aiApiKey).isEmpty()
        assertThat(state.aiModel).isEmpty()
    }

    // ── Edit profile ──────────────────────────────────────────────────────────

    @Test
    fun `showEditProfile populates profileToEdit and editProfileName`() {
        viewModel.showEditProfile(primaryProfile)
        val state = viewModel.uiState.value
        assertThat(state.profileToEdit).isEqualTo(primaryProfile)
        assertThat(state.editProfileName).isEqualTo("Ashutosh")
    }

    @Test
    fun `hideEditProfile clears profileToEdit`() {
        viewModel.showEditProfile(primaryProfile)
        viewModel.hideEditProfile()
        assertThat(viewModel.uiState.value.profileToEdit).isNull()
        assertThat(viewModel.uiState.value.editProfileName).isEmpty()
    }

    @Test
    fun `onEditProfileNameChange updates editProfileName in state`() {
        viewModel.showEditProfile(primaryProfile)
        viewModel.onEditProfileNameChange("Ash")
        assertThat(viewModel.uiState.value.editProfileName).isEqualTo("Ash")
    }

    @Test
    fun `saveProfileEdit calls updateProfile with trimmed new name`() = runTest {
        viewModel.showEditProfile(primaryProfile)
        viewModel.onEditProfileNameChange("  Ash  ")
        viewModel.saveProfileEdit()
        coVerify { profileRepository.updateProfile(primaryProfile.copy(displayName = "Ash")) }
    }

    @Test
    fun `saveProfileEdit with blank name does not call repository`() = runTest {
        viewModel.showEditProfile(primaryProfile)
        viewModel.onEditProfileNameChange("   ")
        viewModel.saveProfileEdit()
        coVerify(exactly = 0) { profileRepository.updateProfile(any()) }
    }

    @Test
    fun `saveProfileEdit clears profileToEdit after saving`() = runTest {
        viewModel.showEditProfile(primaryProfile)
        viewModel.onEditProfileNameChange("Ash")
        viewModel.saveProfileEdit()
        assertThat(viewModel.uiState.value.profileToEdit).isNull()
    }

    // ── Delete profile ────────────────────────────────────────────────────────

    @Test
    fun `showDeleteProfile sets profileToDelete`() {
        viewModel.showDeleteProfile(secondProfile)
        assertThat(viewModel.uiState.value.profileToDelete).isEqualTo(secondProfile)
    }

    @Test
    fun `hideDeleteProfile clears profileToDelete`() {
        viewModel.showDeleteProfile(secondProfile)
        viewModel.hideDeleteProfile()
        assertThat(viewModel.uiState.value.profileToDelete).isNull()
    }

    @Test
    fun `deleteProfile calls repository with correct profileId`() = runTest {
        viewModel.showDeleteProfile(secondProfile)
        viewModel.deleteProfile()
        coVerify { profileRepository.deleteProfile("profile-2") }
    }

    @Test
    fun `deleteProfile clears profileToDelete after deletion`() = runTest {
        viewModel.showDeleteProfile(secondProfile)
        viewModel.deleteProfile()
        assertThat(viewModel.uiState.value.profileToDelete).isNull()
    }

    // ── Create profile ────────────────────────────────────────────────────────

    @Test
    fun `showCreateProfile sets showCreateProfile flag`() {
        viewModel.showCreateProfile()
        assertThat(viewModel.uiState.value.showCreateProfile).isTrue()
    }

    @Test
    fun `hideCreateProfile clears showCreateProfile and newProfileName`() {
        viewModel.showCreateProfile()
        viewModel.onNewProfileNameChange("New Person")
        viewModel.hideCreateProfile()
        assertThat(viewModel.uiState.value.showCreateProfile).isFalse()
        assertThat(viewModel.uiState.value.newProfileName).isEmpty()
    }

    @Test
    fun `createProfile with blank name does not call repository`() = runTest {
        viewModel.onNewProfileNameChange("   ")
        viewModel.createProfile()
        coVerify(exactly = 0) { profileRepository.createProfile(any(), any()) }
    }

    @Test
    fun `createProfile calls repository with trimmed name`() = runTest {
        viewModel.showCreateProfile()
        viewModel.onNewProfileNameChange("  Aarav  ")
        viewModel.createProfile()
        coVerify { profileRepository.createProfile("Aarav", any()) }
    }

    // ── Biometric lock ────────────────────────────────────────────────────────

    @Test
    fun `biometricLockEnabled reflects preferenceManager value`() {
        every { preferenceManager.biometricLockEnabled } returns flowOf(true)
        viewModel = SettingsViewModel(
            context, profileRepository, preferenceManager,
            exportDataUseCase, importDataUseCase, updateRepository,
        )
        assertThat(viewModel.uiState.value.biometricLockEnabled).isTrue()
    }

    @Test
    fun `toggleBiometricLock calls preferenceManager setBiometricLockEnabled`() = runTest {
        viewModel.toggleBiometricLock(true)
        coVerify { preferenceManager.setBiometricLockEnabled(true) }
    }
}

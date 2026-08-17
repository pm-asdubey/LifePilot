package com.lifepilot.features.home.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.lifepilot.data.ai.AiProviderFactory
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.data.storage.FileStorageManager
import com.lifepilot.domain.engine.ActionPlanExecutor
import com.lifepilot.domain.engine.DomainLifeStateEngine
import com.lifepilot.domain.engine.LifeStateEngine
import com.lifepilot.domain.engine.PlanningEngine
import com.lifepilot.domain.engine.PromptBuilder
import com.lifepilot.domain.engine.RetrievalEngine
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.Conversation
import com.lifepilot.domain.ocr.OcrService
import com.lifepilot.domain.repository.ConversationRepository
import com.lifepilot.domain.repository.GoalRepository
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.ProjectRepository
import com.lifepilot.domain.usecase.UpdateObjectStatusUseCase
import com.lifepilot.domain.usecase.UploadDocumentUseCase
import com.lifepilot.features.home.state.HomeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Regression tests for document-attachment handling in HomeViewModel.
 *
 * CRITICAL INVARIANT: processAttachment must NEVER silently drop an attachment
 * regardless of whether a conversation has already been started (currentConversationId
 * may be null when the camera is opened on a cold workspace or after returnToBrief).
 *
 * Root cause of regression (2025-07-03): returnToBrief() now clears currentConversationId
 * so the user can start a fresh conversation after going back. The old processAttachment
 * implementation did `val convId = currentConversationId ?: return@launch`, which silently
 * dropped the attachment. Fix: auto-call getOrCreateConversation like sendMessage() does.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelAttachmentTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    // ── Mocked dependencies ──────────────────────────────────────────────────

    private val context = mockk<Context>(relaxed = true)
    private val contentResolver = mockk<ContentResolver>(relaxed = true)
    private val conversationRepository = mockk<ConversationRepository>(relaxed = true)
    private val preferenceManager = mockk<PreferenceManager>(relaxed = true)
    private val fileStorageManager = mockk<FileStorageManager>(relaxed = true)
    private val ocrService = mockk<OcrService>(relaxed = true)
    private val aiProviderFactory = mockk<AiProviderFactory>(relaxed = true)

    private val testConversation = Conversation(
        conversationId = "test-conv-123",
        profileId = "profile-1",
        title = "New conversation",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Context / ContentResolver plumbing
        every { context.contentResolver } returns contentResolver
        every { contentResolver.getType(any()) } returns "image/jpeg"
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null

        // Profile — getActiveProfileId is a suspend function
        coEvery { preferenceManager.getActiveProfileId() } returns "profile-1"

        // Conversation auto-creation (the invariant under test)
        coEvery {
            conversationRepository.getOrCreateConversation("profile-1", null)
        } returns testConversation
        coEvery {
            conversationRepository.getOrCreateConversation("profile-1", any())
        } returns testConversation

        // Flow-based repos return empty flows so init{} doesn't crash
        every { conversationRepository.observeConversations(any()) } returns emptyFlow()

        // FileStorageManager returns null → triggers the "couldn't read file" early-exit path
        // which is fine: we only care that getOrCreateConversation was reached first.
        coEvery { fileStorageManager.copyFromUri(any(), any(), any()) } returns null

        viewModel = buildViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Invariant tests ──────────────────────────────────────────────────────

    @Test
    fun `processAttachment auto-creates conversation when currentConversationId is null`() = runTest {
        // Precondition: fresh workspace, no conversation yet
        assertThat(viewModel.uiState.value.currentConversationId).isNull()

        viewModel.processAttachment(mockk<Uri>(relaxed = true))

        // The conversation repository must have been asked to create (or reuse) a conversation
        coVerify {
            conversationRepository.getOrCreateConversation("profile-1", null)
        }
    }

    @Test
    fun `processAttachment switches mode to AI_WORKSPACE when called from daily brief`() = runTest {
        // Precondition: daily brief mode (default)
        assertThat(viewModel.uiState.value.mode).isEqualTo(HomeMode.DAILY_BRIEF)

        viewModel.processAttachment(mockk<Uri>(relaxed = true))

        assertThat(viewModel.uiState.value.mode).isEqualTo(HomeMode.AI_WORKSPACE)
    }

    @Test
    fun `processAttachment updates currentConversationId from auto-created conversation`() = runTest {
        viewModel.processAttachment(mockk<Uri>(relaxed = true))

        assertThat(viewModel.uiState.value.currentConversationId).isEqualTo("test-conv-123")
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private fun buildViewModel(): HomeViewModel = HomeViewModel(
        context = context,
        savedStateHandle = SavedStateHandle(),
        profileRepository = mockk(relaxed = true) {
            every { observeActiveProfile() } returns emptyFlow()
        },
        goalRepository = mockk(relaxed = true) {
            every { observeActiveGoals(any()) } returns emptyFlow()
        },
        conversationRepository = conversationRepository,
        objectRepository = mockk(relaxed = true),
        metadataRepository = mockk(relaxed = true),
        updateObjectStatusUseCase = mockk(relaxed = true),
        planningEngine = mockk(relaxed = true),
        lifeStateEngine = mockk(relaxed = true),
        retrievalEngine = mockk(relaxed = true),
        promptBuilder = mockk(relaxed = true),
        aiProviderFactory = aiProviderFactory,
        preferenceManager = preferenceManager,
        domainLifeStateEngine = mockk(relaxed = true),
        actionPlanExecutor = mockk(relaxed = true),
        schemaEngine = mockk(relaxed = true),
        fileStorageManager = fileStorageManager,
        ocrService = ocrService,
        uploadDocumentUseCase = mockk(relaxed = true),
        projectRepository = mockk(relaxed = true) {
            every { observeProjects(any()) } returns emptyFlow()
        },
        aiActionParser = AiActionParser(schemaEngine = mockk(relaxed = true)),
        aiTaskNotifier = mockk(relaxed = true),
        retrievalPlanner = mockk(relaxed = true),
    )
}

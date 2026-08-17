package com.lifepilot.features.timeline.viewmodel

import com.google.common.truth.Truth.assertThat
import com.lifepilot.domain.model.Profile
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.model.TimelineSourceType
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.TimelineRepository
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
class TimelineViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val profileRepository = mockk<ProfileRepository>(relaxed = true)
    private val timelineRepository = mockk<TimelineRepository>(relaxed = true)

    private val testProfile = Profile(
        profileId = "profile-1",
        displayName = "Ashutosh",
        avatarPath = null,
        isPrimary = true,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private val entries = listOf(
        TimelineEntry(
            timelineId = "t1",
            sourceId = "src-1",
            sourceType = TimelineSourceType.DOCUMENT,
            timestamp = Instant.ofEpochSecond(1000),
            title = "Passport uploaded",
            summary = null,
            objectId = "obj-1",
            objectType = "Passport",
        ),
        TimelineEntry(
            timelineId = "t2",
            sourceId = "src-2",
            sourceType = TimelineSourceType.EVENT,
            timestamp = Instant.ofEpochSecond(2000),
            title = "Job offer received",
            summary = null,
            objectId = "obj-2",
            objectType = "Job",
        ),
        TimelineEntry(
            timelineId = "t3",
            sourceId = "src-3",
            sourceType = TimelineSourceType.TASK,
            timestamp = Instant.ofEpochSecond(3000),
            title = "Task completed",
            summary = null,
            objectId = null,
            objectType = null,
        ),
    )

    private lateinit var viewModel: TimelineViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { profileRepository.observeActiveProfile() } returns flowOf(testProfile)
        every { timelineRepository.observeTimeline("profile-1") } returns flowOf(entries)
        viewModel = TimelineViewModel(profileRepository, timelineRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is not loading after entries arrive`() {
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `entries from repository appear in state`() {
        assertThat(viewModel.uiState.value.entries).hasSize(3)
    }

    @Test
    fun `filteredEntries equals all entries when no filter selected`() {
        assertThat(viewModel.uiState.value.selectedSourceType).isNull()
        assertThat(viewModel.uiState.value.filteredEntries).hasSize(3)
    }

    @Test
    fun `selectFilter restricts filteredEntries to matching sourceType`() {
        viewModel.selectFilter("DOCUMENT")
        val filtered = viewModel.uiState.value.filteredEntries
        assertThat(filtered).hasSize(1)
        assertThat(filtered[0].sourceType).isEqualTo(TimelineSourceType.DOCUMENT)
    }

    @Test
    fun `selectFilter null clears filter and returns all entries`() {
        viewModel.selectFilter("DOCUMENT")
        viewModel.selectFilter(null)
        assertThat(viewModel.uiState.value.filteredEntries).hasSize(3)
        assertThat(viewModel.uiState.value.selectedSourceType).isNull()
    }

    @Test
    fun `availableSourceTypes contains distinct source type names`() {
        val types = viewModel.uiState.value.availableSourceTypes
        assertThat(types).containsExactly("DOCUMENT", "EVENT", "TASK").inOrder()
    }

    @Test
    fun `no active profile produces empty entries`() {
        every { profileRepository.observeActiveProfile() } returns flowOf(null)
        viewModel = TimelineViewModel(profileRepository, timelineRepository)
        assertThat(viewModel.uiState.value.entries).isEmpty()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `selectedSourceType is updated in state after selectFilter`() {
        viewModel.selectFilter("EVENT")
        assertThat(viewModel.uiState.value.selectedSourceType).isEqualTo("EVENT")
    }

    @Test
    fun `empty timeline produces empty availableSourceTypes`() {
        every { timelineRepository.observeTimeline("profile-1") } returns flowOf(emptyList())
        viewModel = TimelineViewModel(profileRepository, timelineRepository)
        assertThat(viewModel.uiState.value.availableSourceTypes).isEmpty()
    }
}

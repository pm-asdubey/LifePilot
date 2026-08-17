package com.lifepilot.features.search.viewmodel

import com.google.common.truth.Truth.assertThat
import com.lifepilot.domain.model.Profile
import com.lifepilot.domain.model.SearchEntityType
import com.lifepilot.domain.model.SearchResult
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.SearchRepository
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
class SearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val searchRepository = mockk<SearchRepository>(relaxed = true)
    private val profileRepository = mockk<ProfileRepository>(relaxed = true)

    private val testProfile = Profile(
        profileId = "profile-1",
        displayName = "Ashutosh",
        avatarPath = null,
        isPrimary = true,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { profileRepository.observeActiveProfile() } returns flowOf(testProfile)
        every { searchRepository.observeRecentSearches(any()) } returns flowOf(emptyList())
        coEvery { searchRepository.search(any(), any()) } returns emptyList()
        viewModel = SearchViewModel(searchRepository, profileRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty query and no results`() {
        val state = viewModel.uiState.value
        assertThat(state.query).isEmpty()
        assertThat(state.results).isEmpty()
        assertThat(state.isSearching).isFalse()
    }

    @Test
    fun `onQueryChange updates query in state`() {
        viewModel.onQueryChange("passport")
        assertThat(viewModel.uiState.value.query).isEqualTo("passport")
    }

    @Test
    fun `clearSearch resets query and results`() {
        viewModel.onQueryChange("passport")
        viewModel.clearSearch()
        val state = viewModel.uiState.value
        assertThat(state.query).isEmpty()
        assertThat(state.results).isEmpty()
    }

    @Test
    fun `recent searches from repository appear in state`() {
        val recent = listOf("passport", "insurance", "job")
        every { searchRepository.observeRecentSearches("profile-1") } returns flowOf(recent)
        viewModel = SearchViewModel(searchRepository, profileRepository)
        assertThat(viewModel.uiState.value.recentSearches).containsExactlyElementsIn(recent).inOrder()
    }

    @Test
    fun `onSearch saves recent search to repository`() = runTest {
        viewModel.onSearch("passport renewal")
        coVerify { searchRepository.saveRecentSearch("passport renewal", "profile-1") }
    }

    @Test
    fun `clearSearch does not leave error in state`() {
        viewModel.clearSearch()
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `no active profile — recent searches remain empty`() {
        every { profileRepository.observeActiveProfile() } returns flowOf(null)
        viewModel = SearchViewModel(searchRepository, profileRepository)
        assertThat(viewModel.uiState.value.recentSearches).isEmpty()
    }

    @Test
    fun `no active profile — onSearch does not crash or call repository`() = runTest {
        every { profileRepository.observeActiveProfile() } returns flowOf(null)
        viewModel = SearchViewModel(searchRepository, profileRepository)
        viewModel.onSearch("passport")
        coVerify(exactly = 0) { searchRepository.saveRecentSearch(any(), any()) }
    }
}

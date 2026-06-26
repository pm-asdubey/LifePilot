package com.lifepilot.features.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.SearchRepository
import com.lifepilot.features.search.state.SearchUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        observeRecentSearches()
        observeQueryChanges()
    }

    private fun observeRecentSearches() {
        viewModelScope.launch {
            profileRepository.observeActiveProfile()
                .flatMapLatest { profile ->
                    if (profile == null) flowOf(emptyList())
                    else searchRepository.observeRecentSearches(profile.profileId)
                }
                .catch { Timber.e(it) }
                .collect { recent ->
                    _uiState.update { it.copy(recentSearches = recent) }
                }
        }
    }

    private fun observeQueryChanges() {
        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _uiState.update { it.copy(results = emptyList(), isSearching = false) }
                        return@collect
                    }
                    _uiState.update { it.copy(isSearching = true) }
                    try {
                        val profile = profileRepository.observeActiveProfile()
                            .catch { }
                            .firstOrNull()
                        val profileId = profile?.profileId ?: return@collect
                        val results = searchRepository.search(query, profileId)
                        _uiState.update { it.copy(results = results, isSearching = false) }
                    } catch (e: Exception) {
                        Timber.e(e, "Search failed")
                        _uiState.update { it.copy(isSearching = false, error = e.message) }
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
    }

    fun onSearch(query: String) {
        viewModelScope.launch {
            val profile = profileRepository.observeActiveProfile()
                .catch { }
                .firstOrNull()
            profile?.let { searchRepository.saveRecentSearch(query, it.profileId) }
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(query = "", results = emptyList()) }
        queryFlow.value = ""
    }
}

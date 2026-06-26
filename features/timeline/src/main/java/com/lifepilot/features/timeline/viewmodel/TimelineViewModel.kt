package com.lifepilot.features.timeline.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.repository.TimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class TimelineUiState(
    val isLoading: Boolean = true,
    val entries: List<TimelineEntry> = emptyList(),
    val filteredEntries: List<TimelineEntry> = emptyList(),
    val availableSourceTypes: List<String> = emptyList(),
    val selectedSourceType: String? = null,
    val error: String? = null,
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val timelineRepository: TimelineRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow<String?>(null)

    init {
        observeTimeline()
    }

    private fun observeTimeline() {
        viewModelScope.launch {
            combine(
                profileRepository.observeActiveProfile()
                    .flatMapLatest { profile ->
                        if (profile == null) flowOf(emptyList())
                        else timelineRepository.observeTimeline(profile.profileId)
                    },
                _selectedFilter,
            ) { entries, filter ->
                val sourceTypes = entries
                    .map { it.sourceType.name }
                    .distinct()
                    .sorted()

                val filtered = if (filter == null) entries
                else entries.filter { it.sourceType.name == filter }

                Pair(
                    Triple(entries, sourceTypes, filtered),
                    filter,
                )
            }
                .catch { e ->
                    Timber.e(e, "Error loading timeline")
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { (data, filter) ->
                    val (entries, sourceTypes, filtered) = data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entries = entries,
                            filteredEntries = filtered,
                            availableSourceTypes = sourceTypes,
                            selectedSourceType = filter,
                        )
                    }
                }
        }
    }

    fun selectFilter(sourceType: String?) {
        _selectedFilter.value = sourceType
    }
}

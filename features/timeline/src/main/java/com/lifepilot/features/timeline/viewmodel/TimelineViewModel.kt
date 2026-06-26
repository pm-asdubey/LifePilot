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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class TimelineUiState(
    val isLoading: Boolean = true,
    val entries: List<TimelineEntry> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val timelineRepository: TimelineRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        observeTimeline()
    }

    private fun observeTimeline() {
        viewModelScope.launch {
            profileRepository.observeActiveProfile()
                .flatMapLatest { profile ->
                    if (profile == null) flowOf(emptyList())
                    else timelineRepository.observeTimeline(profile.profileId)
                }
                .catch { e ->
                    Timber.e(e, "Error loading timeline")
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { entries ->
                    _uiState.update {
                        it.copy(isLoading = false, entries = entries)
                    }
                }
        }
    }
}

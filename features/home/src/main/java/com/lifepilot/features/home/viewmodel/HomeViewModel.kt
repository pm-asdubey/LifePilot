package com.lifepilot.features.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.domain.engine.LifeStateEngine
import com.lifepilot.domain.repository.ProfileRepository
import com.lifepilot.domain.usecase.CompleteTaskUseCase
import com.lifepilot.domain.usecase.GetDashboardDataUseCase
import com.lifepilot.features.home.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val lifeStateEngine: LifeStateEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeDashboard()
        observeAttentionItems()
    }

    private fun observeDashboard() {
        viewModelScope.launch {
            profileRepository.observeActiveProfile()
                .flatMapLatest { profile ->
                    if (profile == null) {
                        kotlinx.coroutines.flow.flowOf(null)
                    } else {
                        combine(
                            kotlinx.coroutines.flow.flowOf(profile),
                            getDashboardDataUseCase(profile.profileId),
                        ) { p, dashboard -> Pair(p, dashboard) }
                    }
                }
                .catch { e ->
                    Timber.e(e, "Error loading dashboard")
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { result ->
                    if (result == null) {
                        _uiState.update { it.copy(isLoading = false) }
                        return@collect
                    }
                    val (profile, dashboard) = result
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profileName = profile.displayName,
                            objectCount = dashboard.objectCount,
                            pendingTaskCount = dashboard.pendingTaskCount,
                            pendingTasks = dashboard.pendingTasks,
                            recentActivity = dashboard.recentActivity,
                            domainCounts = dashboard.domainCounts,
                            error = null,
                        )
                    }
                }
        }
    }

    private fun observeAttentionItems() {
        viewModelScope.launch {
            profileRepository.observeActiveProfile()
                .flatMapLatest { profile ->
                    if (profile == null) {
                        kotlinx.coroutines.flow.flowOf(emptyList())
                    } else {
                        lifeStateEngine.observeAttentionRequired(profile.profileId)
                    }
                }
                .catch { }
                .collect { items ->
                    _uiState.update { it.copy(attentionItems = items) }
                }
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            completeTaskUseCase(taskId).onFailure { e ->
                Timber.e(e, "Failed to complete task: $taskId")
            }
        }
    }
}

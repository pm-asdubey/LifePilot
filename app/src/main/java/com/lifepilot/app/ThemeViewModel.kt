package com.lifepilot.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.repository.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Exposes the user's Light / Dark / System theme choice to the app root. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    preferenceManager: PreferenceManager,
) : ViewModel() {
    val themeMode: StateFlow<String> = preferenceManager.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")
}

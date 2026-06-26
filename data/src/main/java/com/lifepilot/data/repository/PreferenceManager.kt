package com.lifepilot.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lifepilot_prefs")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val activeProfileIdKey = stringPreferencesKey("active_profile_id")

    val activeProfileId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[activeProfileIdKey]
    }

    suspend fun getActiveProfileId(): String? =
        activeProfileId.firstOrNull()

    suspend fun setActiveProfileId(profileId: String) {
        context.dataStore.edit { preferences ->
            preferences[activeProfileIdKey] = profileId
        }
    }
}

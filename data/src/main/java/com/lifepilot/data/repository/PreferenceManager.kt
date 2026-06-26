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
    private val aiProviderKey = stringPreferencesKey("ai_provider")
    private val aiApiKeyKey = stringPreferencesKey("ai_api_key")
    private val aiModelKey = stringPreferencesKey("ai_model")

    val activeProfileId: Flow<String?> = context.dataStore.data.map { it[activeProfileIdKey] }
    val aiProvider: Flow<String?> = context.dataStore.data.map { it[aiProviderKey] }
    val aiApiKey: Flow<String?> = context.dataStore.data.map { it[aiApiKeyKey] }
    val aiModel: Flow<String?> = context.dataStore.data.map { it[aiModelKey] }

    suspend fun getActiveProfileId(): String? = activeProfileId.firstOrNull()

    suspend fun setActiveProfileId(profileId: String) {
        context.dataStore.edit { it[activeProfileIdKey] = profileId }
    }

    suspend fun setAiProvider(provider: String) {
        context.dataStore.edit { it[aiProviderKey] = provider }
    }

    suspend fun setAiApiKey(apiKey: String) {
        context.dataStore.edit { it[aiApiKeyKey] = apiKey }
    }

    suspend fun setAiModel(model: String) {
        context.dataStore.edit { it[aiModelKey] = model }
    }

    suspend fun clearAiConfig() {
        context.dataStore.edit {
            it.remove(aiProviderKey)
            it.remove(aiApiKeyKey)
            it.remove(aiModelKey)
        }
    }
}
